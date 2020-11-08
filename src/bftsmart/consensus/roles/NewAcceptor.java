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
import bftsmart.consensus.blockchain.Chain;
import bftsmart.consensus.blockchain.Block;

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
    private Chain chain;

    /**
     * Creates a new instance of Acceptor.
     *
     * @param communication Replicas communication system
     * @param factory       Message factory for PaW messages
     * @param controller
     */
    public NewAcceptor(ServerCommunicationSystem communication,
                       NewMessageFactory factory, ServerViewController controller, Chain chain) {
        this.communication = communication;
        this.me = controller.getStaticConf().getProcessId();
        this.factory = factory;
        this.controller = controller;
        this.privKey = controller.getStaticConf().getPrivateKey();
        this.proofExecutor = Executors.newSingleThreadExecutor();
        this.chain = chain;
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
            logger.debug("Processing msg in view {}", msg.getViewNumber());
            processMessage(msg);
        }
        else {
            logger.debug("Out of context msg in view {}", msg.getViewNumber());
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
        logger.debug("message = " + msg.toString());
        switch (msg.getMessageType()) {
            case NewMessageFactory.PROPOSE:
//                noConsensus(msg);
                proposeReceived(epoch, msg);
                break;
            case NewMessageFactory.VOTE:
                voteReceived(epoch, msg);
                break;
            case NewMessageFactory.SUBMIT://using for calling leader's acceptor to work
                computeVOTE(epoch, msg);
                break;
        }
        consensus.lock.unlock();
    }

    /**
     * just for usability testing
     * @param epoch
     * @param msg
     */
    public void noConsensus(Epoch epoch, NewConsensusMessage msg) {
        logger.info("jumping!");
        byte[] value = msg.getData();
        epoch.propValue = value;
        epoch.deserializedPropValue = tomLayer.checkProposedValue(value, true);
        epoch.writeSent();
        epoch.acceptSent();
        epoch.acceptCreated();
        logger.debug("deserializedPropValue = {}", epoch.deserializedPropValue);
        decide(epoch);
    }

    public void noConsensus(NewConsensusMessage msg) {
        logger.info("jjjumping!");
        decide(msg);
    }

    /**
     * called when a PROPOSE message is received, which checking whether it's
     * from current leader, if so, then call executePropose to execute
     * @param epoch
     * @param msg the PROPOSE message
     */
    public void proposeReceived(Epoch epoch, NewConsensusMessage msg) {
        int cid = epoch.getConsensus().getId();
        logger.debug("PROPOSE received from:{}, for consensus cId:{}, I am:{}",
                msg.getSender(), cid, me);
        if (msg.getSender() == executionManager.getCurrentLeader()) {
            // Is the replica the leader?
            executePropose(epoch, msg);
        } else {
            logger.debug("Propose received is not from the expected leader");
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
        logger.debug("Executing propose for cId:{}, Epoch Timestamp:{}",
                cid, epoch.getTimestamp());
        if(true){//one propose per epoch
            epoch.propValue = data;
            epoch.propValueHash = tomLayer.computeHash(data);
            epoch.getConsensus().addWritten(data);
            logger.debug("I've written data {} in cid {} with timestamp {}.",
                    data, cid, epoch.getConsensus().getEts());
            epoch.deserializedPropValue = tomLayer.checkProposedValue(data, true);
            if(true) {
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
                Block b = new Block(msg);// generating a block via PROPOSE message
                if(!(this.executionManager.getCurrentLeader() == me)) {
                    chain.addBlock(b);//add a block to the chain locally
                    logger.info("I'm {}, I've just updated BLOCK" + msg.toString() + "to the chain locally.", me);
                }
                int[] leader = new int[1];
                leader[0] = this.executionManager.getCurrentLeader();
                communication.send(leader, factory.createVOTE(msg.getViewNumber(),
                        b.computeHashValue(), cid + 1));// send VOTE to leader's acceptor
                logger.info("Sent VOTE to {} for cid {} with hashvalue {}",
                        this.executionManager.getCurrentLeader(), cid + 1, b.computeHashValue());
                //some usage-unknown marks..
                epoch.writeSent();
                epoch.acceptSent();
                epoch.acceptCreated();
                //some usage-unknown marks..
            }
        }
        if((this.executionManager.getCurrentLeader() == me) &&
                ((String)msg.getSetofProof()).equals("true")){// using for leader's acceptor to add the init block
            //in the first time, and which can coded better than this...
            decide(msg);
            Block b = new Block(msg);
            chain.addBlock(b);//add a previous block to the chain locally
            logger.info("I'm {}, I've just updated BLOCK" + msg.toString() + "to the chain locally.", me);
        }
    }

    /**
     * the procedure when leader receives a VOTE message
     * @param epoch the epoch related to the consensus, which usage is not clear
     * @param msg the VOTE message
     */
    public void voteReceived(Epoch epoch, NewConsensusMessage msg) {
        int cid = epoch.getConsensus().getId();
        logger.debug("VOTE from {} for consensus {}", msg.getSender(), cid);
        epoch.setVote(msg.getSender());//just record the VOTEs
    }

    /**
     * counting VOTEs that the leader has received, if meet some value required,
     * then decide this consensus
     * @param cid consensus number, usage not clear
     * @param epoch the epoch related to the consensus, which usage is not clear
     */
    public void computeVOTE(Epoch epoch, NewConsensusMessage msg) {
        logger.info("I have {} VOTEs , Timestamp:{} ", epoch.countVote(), epoch.getTimestamp());
        if (epoch.countVote() > controller.getQuorum()) {
            msg.setHashValue(chain.getCurrentBlockHash());
            msg.setMessageType(1110);
            msg.setSetofProof(epoch.countVote() + " proofs here.");
            chain.addBlock(new Block(msg));//add the current block to the chain locally
            logger.info("I'm {}, I've just added BLOCK" + msg.toString() + "to the chain locally.", me);
            communication.send(this.controller.getCurrentViewAcceptors(), msg);
            decide(msg);
        }
    }

    /**
     * decide the consensus and send this to the client
     * but now the client won't accept it for its unedit strategy of accepting
     * @param epoch the epoch related to the consensus
     */
    private void decide(Epoch epoch) {
        if (epoch.getConsensus().getDecision().firstMessageProposed != null)
            epoch.getConsensus().getDecision().firstMessageProposed.decisionTime = System.nanoTime();
        epoch.getConsensus().decided(epoch, true);
    }

    /**
     * decide the consensus through message and send this to the client
     * @param msg which to be sent
     */
    private void decide(NewConsensusMessage msg) {
        Consensus consensus = executionManager.getConsensus(msg.getEpoch());
        Epoch epoch = consensus.getEpoch(msg.getEpoch(), controller);
        byte[] value = msg.getData();
        epoch.propValue = value;
        epoch.deserializedPropValue = tomLayer.checkProposedValue(value, true);
        epoch.writeSent();
        epoch.acceptSent();
        epoch.acceptCreated();
        decide(epoch);
    }
}