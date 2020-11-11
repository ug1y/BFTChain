package bftsmart.consensus.roles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.consensus.Consensus;
import bftsmart.consensus.Epoch;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.core.ExecutionManager;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.TOMUtil;

import bftsmart.communication.SystemMessage;
import bftsmart.consensus.messages.ChainConsensusMessage;
import bftsmart.consensus.messages.ProposalMessage;
import bftsmart.consensus.messages.VoteMessage;
import bftsmart.consensus.messages.SyncMessage;
import bftsmart.consensus.messages.NewMessageFactory;
import bftsmart.consensus.Blockchain;

public final class NewAcceptor {
    private Logger logger = LoggerFactory.getLogger(this.getClass());// logger
    private int me; // This replica ID
    private ExecutionManager executionManager; // Execution manager of consensus's executions
    private NewMessageFactory factory; // Factory for OUR messages
    private ServerCommunicationSystem communication; // Replicas comunication system
    private TOMLayer tomLayer; // TOM layer
    private ServerViewController controller;// ServerViewController
    private ExecutorService proofExecutor = null;// thread pool used to paralelise creation of consensus proofs
    private byte[] data;
    private int cid;
    private Blockchain blockchain;

    /**
     * Creates a new instance of Acceptor.
     *
     * @param communication Replicas communication system
     * @param factory       Message factory for PaW messages
     * @param controller
     */
    public NewAcceptor(ServerCommunicationSystem communication,
                       NewMessageFactory factory,
                       ServerViewController controller,
                       Blockchain blockchain) {
        this.communication = communication;
        this.me = controller.getStaticConf().getProcessId();
        this.factory = factory;
        this.controller = controller;
        this.proofExecutor = Executors.newSingleThreadExecutor();
        this.blockchain = blockchain;
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
    public final void deliver(ChainConsensusMessage msg) {
        if (true) {//executionManager.checkLimits(msg)) {
            logger.debug("Processing msg in view {}", msg.getViewNumber());
            processMessage(msg);
        }
        else {
            logger.debug("Out of context msg in view {}", msg.getViewNumber());
            tomLayer.processOutOfContext();
        }
    }

    public void startConsensus(int cid) {
        this.cid = cid;
        VoteMessage v = factory.createVOTE(blockchain.getCurrentHash(),
                0, cid);
        v.addSignature();
        int[] leader = new int[1];
        leader[0] = executionManager.getCurrentLeader();
        communication.send(leader, v);
        logger.info("I've sent VOTE in cid {} to leader {}", cid, executionManager.getCurrentLeader());
    }

    /**
     * called when a consensus message need to be processed, which processed
     * differently accroding to its type
     * @param msg
     */
    public final void processMessage(ChainConsensusMessage msg) {
        Consensus consensus = executionManager.getConsensus(msg.getEpoch());
        consensus.lock.lock();
        Epoch epoch = consensus.getEpoch(msg.getEpoch(), controller);
        logger.info("message = " + msg.toString());
        switch (msg.getMessageType()) {
            case NewMessageFactory.PROPOSAL:
                startConsensus(msg.getEpoch());
//                proposalReceived(epoch, (ProposalMessage)msg);
                break;
            case NewMessageFactory.SYNC:
                syncReceived(epoch, (SyncMessage)msg);
                break;
            default:
                logger.info("unexpected type of message.");
        }
        consensus.lock.unlock();
    }


    /**
     * called when a PROPOSE message is received, which checking whether it's
     * from current leader, if so, then call executePropose to execute
     * @param epoch
     * @param msg the PROPOSE message
     */
    public void proposalReceived(Epoch epoch, ProposalMessage msg) {
        int cid = epoch.getConsensus().getId();
        logger.info("PROPOSAL received from:{}, for consensus cId:{}",
                msg.getSender(), cid);
        if (checkPROPOSAL(msg)) {
            decide(epoch, msg);
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
        if(msg.getSender() == executionManager.getCurrentLeader()) {
            return true;
        }
        return false;
    }

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

    private boolean checkSYNC(SyncMessage msg) {
        return true;
    }


    /**
     * decide the consensus through message and send this to the client
     * @param msg which to be sent
     */
    private void decide(Epoch epoch, ProposalMessage msg) {
        Consensus consensus = executionManager.getConsensus(msg.getEpoch());
        byte[] value = msg.getData();
        epoch.propValue = value;
        epoch.deserializedPropValue = tomLayer.checkProposedValue(value, true);
        epoch.writeSent();
        epoch.acceptSent();
        epoch.acceptCreated();
        if (epoch.getConsensus().getDecision().firstMessageProposed != null)
            epoch.getConsensus().getDecision().firstMessageProposed.decisionTime = System.nanoTime();
        epoch.getConsensus().decided(epoch, true);
    }

}