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
import bftsmart.consensus.Decision;
import bftsmart.reconfiguration.ServerViewController;

///
import bftsmart.consensus.chainmessages.ChainMessageFactory;
import bftsmart.consensus.chainmessages.ProposalMessage;
import bftsmart.consensus.chainmessages.VoteMessage;
import bftsmart.consensus.chainmessages.ChainConsensusMessage;
import bftsmart.consensus.Blockchain;
import bftsmart.tom.core.ExecutionManager;
import bftsmart.tom.core.TOMLayer;
import bftsmart.consensus.Consensus;
import bftsmart.consensus.Epoch;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.TOMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Array;
import java.util.Arrays;

///
public class ChainProposer {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private ChainMessageFactory factory; // Factory for PaW messages
    private ServerCommunicationSystem communication; // Replicas comunication system
    private ServerViewController controller;
    private Blockchain blockchain;
    private ExecutionManager executionManager;// Execution manager of consensus's executions
    private TOMLayer tomLayer; // TOM layer
    private byte[] data;
//    private PrivateKey privKey;


    public ChainProposer(ServerCommunicationSystem communication,
                         ChainMessageFactory factory,
                         ServerViewController controller,
                         Blockchain blockchain) {
        this.communication = communication;
        this.factory = factory;
        this.controller = controller;
        this.blockchain = blockchain;
//        this.privKey = controller.getStaticConf().getPrivateKey();
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
            logger.debug("Processing msg in number {}", msg.getConsId());
            processMessage(msg);
        }
        else {
            logger.debug("Out of context msg in number {}", msg.getConsId());
        }
    }

    /**
     * called when a consensus message need to be processed, which processed
     * differently accroding to its type
     * @param msg
     */
    public final void processMessage(ChainConsensusMessage msg) {
        Consensus consensus = executionManager.getConsensus(msg.getConsId());
        consensus.lock.lock();
        Decision dec = consensus.getDecision();
        Epoch epoch = consensus.getEpoch(msg.getEpoch(), controller);
        logger.debug("message = " + msg.toString());
        switch (msg.getMsgType()) {
            case ChainMessageFactory.VOTE:
//                if(dec.firstMessageProposed == null) {
//                    dec.firstMessageProposed = new TOMMessage();
//                }
//                if(dec.firstMessageProposed != null && dec.firstMessageProposed.voteReceivedTime == 0) {
//                    dec.firstMessageProposed.voteReceivedTime = System.nanoTime();
//                }
                voteReceived(epoch, (VoteMessage)msg);
                break;
            default:
                logger.info("unexpected type of message.");
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
        logger.debug("VOTE received from:{}, for consensus cId:{}",
                msg.getSender(), cid);
        if (checkVOTE(msg)) {
            epoch.setVote(msg.getSender(), msg);//record the VOTEs
            executeVOTE(epoch, msg);
        } else {
            logger.debug("VOTE invalid.");
        }
    }

    /**
     * check whether a VOTE message is valid
     * @param msg the VOTE message
     * @return valid(true) or not(false)
     */
    private boolean checkVOTE(VoteMessage msg) {
//        PublicKey pubKey = controller.getStaticConf().getPublicKey(msg.getSender());
        if(Arrays.equals(msg.getBlockHash(), blockchain.getCurrentHash())) {// is the vote's blockhash equals to the one which voting for?
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * if enough VOTEs received, send a PROPOSAL to all
     * @param epoch
     * @param msg
     */
    public void executeVOTE(Epoch epoch, VoteMessage msg) {
        if(epoch.countVote() > controller.getQuorum() && // there are enough votes
                !epoch.isProposalSent() && // proposal haven't been sent yet
                tomLayer.clientsManager.havePendingRequests()){// there are requests can be gotten
            epoch.proposalSent();
            logger.debug("id {} proposalSent turned to true", msg.getConsId());
            this.data = tomLayer.createPropose(tomLayer.execManager.getConsensus(msg.getConsId()).getDecision());
            ProposalMessage p = factory.createPROPOSAL(this.data, blockchain.getCurrentHash(),
                    epoch.getVotes(), 0, msg.getConsId(),0);
//            byte[] pb =  p.getBytes();
//            byte[] signature = TOMUtil.signMessage(privKey, pb);
//            p.addSignature(signature);
            logger.debug("get enough votes, proposing");

            //for benchmark
            Decision dec = epoch.getConsensus().getDecision();
//            if(dec.firstMessageProposed != null) {
//                dec.firstMessageProposed.proposalSentTime = System.nanoTime();
//            }


            communication.send(this.controller.getCurrentViewAcceptors(), p);
        }
        executionManager.processOutOfContext(epoch.getConsensus());
    }
}