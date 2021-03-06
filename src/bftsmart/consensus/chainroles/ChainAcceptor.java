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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bftsmart.clientsmanagement.RequestList;
import bftsmart.consensus.Decision;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.TOMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.consensus.Consensus;
import bftsmart.consensus.Epoch;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.core.ExecutionManager;
import bftsmart.tom.core.TOMLayer;

import bftsmart.consensus.chainmessages.ChainConsensusMessage;
import bftsmart.consensus.chainmessages.ProposalMessage;
import bftsmart.consensus.chainmessages.VoteMessage;
import bftsmart.consensus.chainmessages.SyncMessage;
import bftsmart.consensus.chainmessages.ChainMessageFactory;
import bftsmart.consensus.Blockchain;

public final class ChainAcceptor {
    private Logger logger = LoggerFactory.getLogger(this.getClass());// logger
    private int me; // This replica ID
    private ExecutionManager executionManager; // Execution manager of consensus's executions
    private ChainMessageFactory factory; // Factory for OUR messages
    private ServerCommunicationSystem communication; // Replicas comunication system
    private TOMLayer tomLayer; // TOM layer
    private ServerViewController controller;// ServerViewController
    private ExecutorService proofExecutor = null;// thread pool used to paralelise creation of consensus proofs
    private Blockchain blockchain;
    private PrivateKey privKey;

    /**
     * Creates a new instance of Acceptor.
     *
     * @param communication Replicas communication system
     * @param factory       Message factory for PaW messages
     * @param controller
     */
    public ChainAcceptor(ServerCommunicationSystem communication,
                         ChainMessageFactory factory,
                         ServerViewController controller,
                         Blockchain blockchain) {
        this.communication = communication;
        this.me = controller.getStaticConf().getProcessId();
        this.factory = factory;
        this.controller = controller;
        this.proofExecutor = Executors.newSingleThreadExecutor();
        this.blockchain = blockchain;
        this.privKey = controller.getStaticConf().getPrivateKey();
    }

    /**
     * some get&set method for this class
     */
    public ChainMessageFactory getFactory() {
        return factory;
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
     * start a consensus by follower's voting
     * @param cid
     */
    public void startConsensus(int cid, long receiveTime) {
        //for benchmark
        Consensus con = executionManager.getConsensus(cid);
        con.lock.lock();
        Decision dec = con.getDecision();
        if(dec.firstMessageProposed == null) {
            dec.firstMessageProposed = new TOMMessage();
        }
        if(dec.firstMessageProposed != null) {
            dec.firstMessageProposed.consensusStartTime = System.nanoTime();
        }
        if(dec.firstMessageProposed != null) {
            dec.firstMessageProposed.deliveryTime = receiveTime;
        }
        con.lock.unlock();


        VoteMessage v = factory.createVOTE(blockchain.getCurrentHash(), 0, cid, 0);
        byte[] signature = TOMUtil.signMessage(privKey, TOMUtil.computeHash(v.getBytes()));
        v.addSignature(signature);

        int[] leader = new int[1];
        leader[0] = executionManager.getCurrentLeader();

        //for benchmark
        con.lock.lock();
        if(dec.firstMessageProposed != null) {
            dec.firstMessageProposed.voteSentTime = System.nanoTime();
        }
        con.lock.unlock();


        communication.send(leader, v);
        logger.debug("I've sent VOTE in cid {} to leader {}", cid, executionManager.getCurrentLeader());

    }

    /**
     * called when a consensus message need to be processed, which processed
     * differently accroding to its type
     * @param msg
     */
    public final void processMessage(ChainConsensusMessage msg) {
        Consensus consensus = executionManager.getConsensus(msg.getConsId());
        consensus.lock.lock();
        Epoch epoch = consensus.getEpoch(msg.getEpoch(), controller);
        logger.debug("message = " + msg.toString());
        switch (msg.getMsgType()) {
            case ChainMessageFactory.PROPOSAL:
                //for benchmark
                Decision dec = epoch.getConsensus().getDecision();
                if(dec.firstMessageProposed != null) {
                    dec.firstMessageProposed.proposalReceivedTime = System.nanoTime();
                }
                proposalReceived(epoch, (ProposalMessage)msg);
                break;
            case ChainMessageFactory.SYNC:
                syncReceived(epoch, (SyncMessage)msg);
                break;
            default:
                logger.info("unexpected type of message.");
        }
        consensus.lock.unlock();
    }


    /**
     * called when a PROPOSE message is received
     * @param epoch
     * @param msg the PROPOSE message
     */
    public void proposalReceived(Epoch epoch, ProposalMessage msg) {
        int cid = epoch.getConsensus().getId();

        logger.debug("PROPOSAL received from:{}, for consensus cId:{}",
                msg.getSender(), cid);
        if (checkPROPOSAL(msg)) {
            blockchain.appendBlock(msg);
            decide(msg);
        } else {
            logger.info("PROPOSAL invalid.");
        }
    }

    /**
     * check whether a PROPOSAL message is valid
     * @param msg the PROPOSAL message
     * @return valid(true) or not(false)
     */
    private boolean checkPROPOSAL(ProposalMessage msg) {
//        PublicKey pubKey = controller.getStaticConf().getPublicKey(msg.getSender());
        if(msg.getSender() == executionManager.getCurrentLeader() &&// is the message from the leader?
                Arrays.equals(msg.getPrevHash(), blockchain.getCurrentHash()) &&// is the hash link valid?
//                msg.verifySignature(pubKey) &&// is the signature valid?
                msg.verifyVotes(controller.getQuorum(), controller.getStaticConf())
        ) {//if all votes are valid?
            return true;
        }
        return false;
    }

    /**
     * called when a SYNC message is received
     * @param epoch
     * @param msg
     */
    public void syncReceived(Epoch epoch, SyncMessage msg) {
        int cid = epoch.getConsensus().getId();
        logger.debug("SYNC received from:{}, for consensus cId:{}",
                msg.getSender(), cid);
        if (checkSYNC(msg)) {
            //executeSYNC(epoch, msg);
        } else {
            logger.debug("SYNC invalid.");
        }
    }

    /**
     * check whether a SYNC message is valid
     * @param msg the SYNC message
     * @return valid(true) or not(false)
     */
    private boolean checkSYNC(SyncMessage msg) {
        return true;
    }


    /**
     * decide the consensus through message and send this to the client
     * @param msg which to be sent
     */
    private void decide(ProposalMessage msg) {
        Consensus consensus = executionManager.getConsensus(msg.getConsId());
        Epoch epoch = consensus.getEpoch(msg.getEpoch(), controller);
        byte[] value = msg.getData();
        epoch.propValue = value;
        epoch.deserializedPropValue = tomLayer.checkProposedValue(value, true);
        TOMMessage tmptom = epoch.getConsensus().getDecision().firstMessageProposed;
        epoch.getConsensus().getDecision().firstMessageProposed = epoch.deserializedPropValue[0];
        if(tmptom != null) {
            epoch.getConsensus().getDecision().firstMessageProposed.deliveryTime = tmptom.deliveryTime;
            epoch.getConsensus().getDecision().firstMessageProposed.voteSentTime = tmptom.voteSentTime;
            epoch.getConsensus().getDecision().firstMessageProposed.proposalReceivedTime = tmptom.proposalReceivedTime;
            epoch.getConsensus().getDecision().firstMessageProposed.decisionTime = tmptom.decisionTime;
            epoch.getConsensus().getDecision().firstMessageProposed.requestReplyTime = tmptom.requestReplyTime;
        }
        epoch.writeSent();
        epoch.acceptSent();
        epoch.acceptCreated();
        if (epoch.getConsensus().getDecision().firstMessageProposed != null)
            epoch.getConsensus().getDecision().firstMessageProposed.decisionTime = System.nanoTime();
        epoch.getConsensus().decided(epoch, true);
    }

}