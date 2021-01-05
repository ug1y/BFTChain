/*
Copyright (c) 2020 Hao Yin, Zhibo Xing

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package bftsmart.consensus.chainroles;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.consensus.Blockchain;
import bftsmart.consensus.Consensus;
import bftsmart.consensus.Epoch;
import bftsmart.consensus.chainmessages.*;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.core.ExecutionManager;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.util.TOMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.util.Arrays;

public class ChainAcceptor {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private ChainMessageFactory factory; // Factory for BFTChain messages
    private ServerCommunicationSystem communication; // Replicas comunication system
    private ServerViewController controller;

    private ExecutionManager executionManager; // Execution manager of consensus's executions
    private TOMLayer tomLayer; // TOM layer

    private Blockchain blockchain;
    private PrivateKey privKey;

    public ChainAcceptor(ServerCommunicationSystem communication,
                         ChainMessageFactory factory,
                         ServerViewController controller,
                         Blockchain blockchain) {
        this.communication = communication;
        this.factory = factory;
        this.controller = controller;
        this.blockchain = blockchain;
        this.privKey = controller.getStaticConf().getPrivateKey();
    }

    public void setExecutionManager(ExecutionManager executionManager) {
        this.executionManager = executionManager;
    }

    public void setTOMLayer(TOMLayer tomLayer) {
        this.tomLayer = tomLayer;
    }

    /**
     * called by communication layer to delivery messages, if the message
     * can be execute, then call other method to process, or just storing
     * it as an out of context message
     * @param msg the message delivered by communication layer
     */
    public final void deliver(ChainConsensusMessage msg) {
        if (executionManager.checkLimits(msg)) {
            logger.debug("Processing msg in view {}", msg.getViewNumber());
            processMessage(msg);
        }
        else {
            logger.debug("Out of context msg in view {}", msg.getViewNumber());
            tomLayer.processOutOfContext();
            // using SYNC asking for latest blocks
        }
    }

    /**
     * called when a consensus message need to be processed, which processed
     * differently accroding to its type
     * @param msg process chain message
     */
    public final void processMessage(ChainConsensusMessage msg) {
        Consensus consensus = executionManager.getConsensus(msg.getConsId());
        consensus.lock.lock();
        Epoch epoch = consensus.getEpoch(msg.getEpoch(), controller);
        epoch.consensusStartTime = System.nanoTime();
        switch (msg.getMsgType()){
            case ChainMessageFactory.PROPOSAL:{
                proposalReceived(epoch, (ProposalMessage) msg);
            }break;
            case ChainMessageFactory.SYNC:{
                syncReceived(epoch, (SyncMessage) msg);
            }break;
        }
        consensus.lock.unlock();
    }

    /**
     * the procedure when leader receives a PROPOSAL message
     * @param epoch the epoch related to the consensus, which usage is not clear
     * @param msg the PROPOSAL message
     */
    public void proposalReceived(Epoch epoch, ProposalMessage msg) {
        int cid = epoch.getConsensus().getId();
        logger.debug("I've received PROPOSAL with size {} in cid {} from leader {} ", TOMUtil.getBytes(msg).length, cid, msg.getSender());

        if(msg.getSender() == executionManager.getCurrentLeader() && // is the message from the leader?
                Arrays.equals(msg.getPrevHash(), blockchain.getCurrentHash())) { // is the hash link valid?
            int voteCount = 0;
            for (int i=0; i < msg.getVotes().length; i++) {
                if (msg.getVotes()[i] !=  null) {
                    voteCount ++;
                }
            }
            if (voteCount > controller.getQuorum()) {
                blockchain.appendBlock(msg);
                executePROPOSAL(epoch, msg.getData());
            }
        }


    }

    /**
     * Computes PROPOSAL according to the Byzantine consensus specification
     * @param epoch Epoch of the receives message
     * @param value Value sent in the message
     */
    public void executePROPOSAL(Epoch epoch, byte[] value) {
        if(epoch.propValue == null) { //only accept one propose per epoch
            epoch.propValue = value;
            epoch.propValueHash = tomLayer.computeHash(value);

            epoch.deserializedPropValue = tomLayer.checkProposedValue(value, true);

            if (epoch.deserializedPropValue != null) {
                if(epoch.getConsensus().getDecision().firstMessageProposed == null) {
                    epoch.getConsensus().getDecision().firstMessageProposed = epoch.deserializedPropValue[0];
                }
                decide(epoch);
            }
        }
    }

    /**
     * the procedure when leader receives a Sync message
     * @param epoch the epoch related to the consensus, which usage is not clear
     * @param msg the Sync message
     */
    public void syncReceived(Epoch epoch, SyncMessage msg) {

    }

    /**
     * decide the consensus through message and send this to the client
     * @param epoch which to be sent
     */
    private void decide(Epoch epoch) {
        if (epoch.getConsensus().getDecision().firstMessageProposed != null) {
            epoch.getConsensus().getDecision().firstMessageProposed.decisionTime = System.nanoTime();

            epoch.getConsensus().getDecision().firstMessageProposed.chainStartTime = epoch.chainStartTime;
            epoch.getConsensus().getDecision().firstMessageProposed.voteSentTime = epoch.voteSentTime;
            if (epoch.getConsensus().getDecision().firstMessageProposed.consensusStartTime == 0) {
                epoch.getConsensus().getDecision().firstMessageProposed.consensusStartTime = epoch.consensusStartTime;
            }
        }

        epoch.getConsensus().decided(epoch, true);
    }

    /**
     * start a consensus by follower's voting
     * @param cid start consensus with id
     */
    public void startConsensus(int cid) {
        long chainStartTime = System.nanoTime();

        VoteMessage vote = factory.createVOTE(blockchain.getCurrentHeight(), 0, cid, 0);
        vote.setSignature(TOMUtil.signMessage(privKey, blockchain.getCurrentHash()));

        int[] leader = new int[1];
        leader[0] = executionManager.getCurrentLeader();

        Epoch epoch = executionManager.getConsensus(cid).getEpoch(0, controller);
        epoch.chainStartTime = chainStartTime;
        epoch.voteSentTime = System.nanoTime();

        communication.send(leader, vote);
        logger.debug("I've sent VOTE with size {} in cid {} to leader {}", TOMUtil.getBytes(vote).length, cid, executionManager.getCurrentLeader());
    }
}
