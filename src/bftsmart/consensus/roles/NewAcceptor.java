package bftsmart.consensus.roles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.consensus.Consensus;
import bftsmart.consensus.Epoch;
import bftsmart.consensus.messages.ConsensusMessage;
import bftsmart.consensus.messages.MessageFactory;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.core.ExecutionManager;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.TOMUtil;

import bftsmart.consensus.messages.NewConsensusMessage;
import bftsmart.consensus.messages.NewMessageFactory;
import bftsmart.consensus.messages.NewConsensusMessageTest;
import bftsmart.communication.SystemMessage;

public final class NewAcceptor {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private int me; // This replica ID
    private ExecutionManager executionManager; // Execution manager of consensus's executions
    private NewMessageFactory factory; // Factory for OUR messages
    private ServerCommunicationSystem communication; // Replicas comunication system
    private TOMLayer tomLayer; // TOM layer
    private ServerViewController controller;
    private ExecutorService proofExecutor = null;// thread pool used to paralelise creation of consensus proofs
    private PrivateKey privKey;

    /**
     * Creates a new instance of Acceptor.
     *
     * @param communication Replicas communication system
     * @param factory       Message factory for PaW messages
     * @param controller
     */
    public NewAcceptor(ServerCommunicationSystem communication,
                       NewMessageFactory factory, ServerViewController controller) {
        this.communication = communication;
        this.me = controller.getStaticConf().getProcessId();
        this.factory = factory;
        this.controller = controller;
        this.privKey = controller.getStaticConf().getPrivateKey();
        this.proofExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * some get&set method for this class
     */
    public NewMessageFactory getFactory() {
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
    public final void deliver(NewConsensusMessage msg) {
        if (executionManager.checkLimits(msg)) {
            logger.info("Processing msg in view {}", msg.getViewNumber());
            processMessage(msg);
        }
        else {
            logger.info("Out of context msg in view {}", msg.getViewNumber());
            tomLayer.processOutOfContext();
        }
    }

    /**
     * called when a consensus message need to be processed, which processed
     * differently accroding to its type
     * @param msg
     */
    public final void processMessage(NewConsensusMessage msg) {
        Consensus consensus = executionManager.getConsensus(msg.getEpoch());
        consensus.lock.lock();
        Epoch epoch = consensus.getEpoch(msg.getEpoch(), controller);
        logger.info("epoch number = {}", msg.getEpoch());
        switch (msg.getMessageType()) {
            case NewMessageFactory.PROPOSE:
//                noConsensus(epoch, msg);
                proposeReceived(epoch, msg);
                break;
            case NewMessageFactory.VOTE:
                voteReceived(epoch, msg);
                break;
        }
        consensus.lock.unlock();
    }

    public void noConsensus(Epoch epoch, NewConsensusMessage msg) {
        logger.info("jumping!");
        byte[] value = msg.getData();
        epoch.propValue = value;
        epoch.deserializedPropValue = tomLayer.checkProposedValue(value, true);
        epoch.writeSent();
        epoch.acceptSent();
        epoch.acceptCreated();
        logger.info("deserializedPropValue = {}", epoch.deserializedPropValue);
        decide(epoch);
    }

    /**
     * called when a PROPOSE message is received, which checking whether it's
     * from current leader, if so, then call executePropose to execute
     * @param epoch
     * @param msg the PROPOSE message
     */
    public void proposeReceived(Epoch epoch, NewConsensusMessage msg) {
        int cid = epoch.getConsensus().getId();
        logger.info("PROPOSE received from:{}, for consensus cId:{}, I am:{}",
                msg.getSender(), cid, me);
        if (msg.getSender() == executionManager.getCurrentLeader()) {
            // Is the replica the leader?
            executePropose(epoch, msg);
        } else {
            logger.info("Propose received is not from the expected leader");
        }
    }

    /**
     * the specific actions related to a proposed value
     * @param epoch the current epoch of the consensus
     * @param data the data that proposed
     */
    public void executePropose(Epoch epoch, NewConsensusMessage msg) {
        long consensusStartTime = System.nanoTime();
        byte[] data = msg.getData();
        int cid = epoch.getConsensus().getId();
        logger.info("Executing propose for cId:{}, Epoch Timestamp:{}",
                cid, epoch.getTimestamp());
        if(epoch.propValue == null) {//one propose per epoch
            epoch.propValue = data;
            epoch.propValueHash = tomLayer.computeHash(data);
            epoch.getConsensus().addWritten(data);
            logger.info("I've written data {} in cid {} with timestamp {}.",
                    data, cid, epoch.getConsensus().getEts());
            epoch.deserializedPropValue = tomLayer.checkProposedValue(data, true);
            if(epoch.deserializedPropValue != null && !epoch.isWriteSent()) {
                //some usage-unknown settings for epoch..
                if (epoch.getConsensus().getDecision().firstMessageProposed == null) {
                    epoch.getConsensus().getDecision().firstMessageProposed = epoch.deserializedPropValue[0];
                }
                if (epoch.getConsensus().getDecision().firstMessageProposed.consensusStartTime == 0) {
                    epoch.getConsensus().getDecision().firstMessageProposed.consensusStartTime = consensusStartTime;
                }
                epoch.getConsensus().getDecision().firstMessageProposed.proposeReceivedTime = System.nanoTime();
                epoch.setWrite(me, epoch.propValueHash);
                epoch.getConsensus().getDecision().firstMessageProposed.writeSentTime = System.nanoTime();
                //some usage-unknown settings for epoch..
                int[] leader = new int[1];
                leader[0] = this.executionManager.getCurrentLeader();
                communication.send(leader, factory.createVOTE(msg.getViewNumber(), 1, cid));
                //1 stands for hash of block
                //some usage-unknown marks..
                epoch.writeSent();
                epoch.acceptSent();
                epoch.acceptCreated();
                //some usage-unknown marks..
                logger.info("Sent VOTE to {}",
                        this.executionManager.getCurrentLeader());
            }
        }
    }

    public void voteReceived(Epoch epoch, NewConsensusMessage msg) {
        int cid = epoch.getConsensus().getId();
        logger.info("VOTE from {} for consensus {}", msg.getSender(), cid);
        epoch.setVote(msg.getSender());
        computeVOTE(cid, epoch);
    }

    public void computeVOTE(int cid, Epoch epoch) {
        logger.info("I have {} VOTEs for cId:{}, Timestamp:{} ", epoch.countVote(), cid,
                epoch.getTimestamp());
        if (epoch.countVote() > controller.getQuorum() && !epoch.getConsensus().isDecided()) {
            logger.info("Deciding consensus {}", cid);
            logger.info("deserializedPropValue = {}", epoch.deserializedPropValue);
            decide(epoch);
        }
    }

    private void decide(Epoch epoch) {
        if (epoch.getConsensus().getDecision().firstMessageProposed != null)
            epoch.getConsensus().getDecision().firstMessageProposed.decisionTime = System.nanoTime();
        epoch.getConsensus().decided(epoch, true);
    }
}