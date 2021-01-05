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
import bftsmart.consensus.Decision;
import bftsmart.consensus.Epoch;
import bftsmart.consensus.chainmessages.ChainConsensusMessage;
import bftsmart.consensus.chainmessages.ChainMessageFactory;
import bftsmart.consensus.chainmessages.ProposalMessage;
import bftsmart.consensus.chainmessages.VoteMessage;
import bftsmart.consensus.messages.MessageFactory;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.core.ExecutionManager;
import bftsmart.tom.core.TOMLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ChainProposer {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private ChainMessageFactory factory; // Factory for BFTChain messages
    private ServerCommunicationSystem communication; // Replicas comunication system
    private ServerViewController controller;

    private ExecutionManager executionManager; // Execution manager of consensus's executions
    private TOMLayer tomLayer; // TOM layer

    Blockchain blockchain;

    public ChainProposer(ServerCommunicationSystem communication,
                         ChainMessageFactory factory,
                         ServerViewController controller,
                         Blockchain blockchain) {
        this.communication = communication;
        this.factory = factory;
        this.controller = controller;
        this.blockchain = blockchain;
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
            logger.debug("Processing chain msg with id {}", msg.getConsId());
            processMessage(msg);
        }
        else {
            logger.debug("Out of context msg with id {}", msg.getConsId());
//            tomLayer.processOutOfContext();
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
        switch (msg.getMsgType()){
            case ChainMessageFactory.VOTE:{
                voteReceived(epoch, (VoteMessage) msg);
            }break;
        }
        consensus.lock.unlock();
    }

    /**
     * the procedure when leader receives a VOTE message
     * @param epoch the epoch related to the consensus, which usage is not clear
     * @param msg the VOTE message
     */
    public void voteReceived(Epoch epoch, VoteMessage msg) {
        int cid = epoch.getConsensus().getId();
        logger.debug("VOTE received from:{}, for consensus cId:{}", msg.getSender(), cid);

        if(!epoch.isProposalSent() && (msg.getBlockHeight() == blockchain.getCurrentHeight())) {
            epoch.setVote(msg.getSender(), msg); // record the VOTE messages
            executeVOTE(cid, epoch, msg);
        }
    }

    /**
     * if enough VOTEs received, send a PROPOSAL to all
     * @param epoch
     * @param msg
     */
    public void executeVOTE(int cid, Epoch epoch, VoteMessage msg) {
        int voteCount = epoch.countVote();

        logger.debug("I have " + voteCount +
                " VOTEs for " + cid + "," + epoch.getTimestamp());

        if (voteCount > controller.getQuorum() && // there are enough votes
                !epoch.isProposalSent() && // proposal haven't been sent yet
                tomLayer.clientsManager.havePendingRequests()){// there are requests can be gotten

            epoch.proposalSent();

            Decision dec = epoch.getConsensus().getDecision();
            byte[] data = tomLayer.createPropose(dec);
            ProposalMessage pm = factory.createPROPOSAL(data, blockchain.getCurrentHash(),
                    controller.getCurrentViewId(), cid, msg.getEpoch());
            pm.setVotes(epoch.getVote());

            logger.debug("sending proposal to all replicas for consensus " + cid);
            communication.send(this.controller.getCurrentViewAcceptors(), pm);
        }
        executionManager.processOutOfContextVote(epoch.getConsensus());
    }

}
