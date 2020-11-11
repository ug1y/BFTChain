package bftsmart.consensus.roles;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.consensus.messages.MessageFactory;
import bftsmart.reconfiguration.ServerViewController;

///
import bftsmart.consensus.messages.NewMessageFactory;
import bftsmart.consensus.messages.ProposalMessage;
import bftsmart.consensus.messages.VoteMessage;
import bftsmart.consensus.messages.ChainConsensusMessage;
import bftsmart.consensus.Blockchain;
import bftsmart.tom.core.ExecutionManager;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.TOMUtil;
import java.util.*;
import bftsmart.consensus.Consensus;
import bftsmart.consensus.Epoch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
///
public class NewProposer {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private NewMessageFactory factory; // Factory for PaW messages
    private ServerCommunicationSystem communication; // Replicas comunication system
    private ServerViewController controller;
    private Blockchain blockchain;
    private Boolean firstpropose;
    private ExecutionManager executionManager;// Execution manager of consensus's executions
    private TOMLayer tomLayer; // TOM layer
    private byte[] data;
    private int cid;



    public NewProposer(ServerCommunicationSystem communication,
                       NewMessageFactory factory,
                    ServerViewController controller,
                       Blockchain blockchain) {
        this.communication = communication;
        this.factory = factory;
        this.controller = controller;
        this.blockchain = blockchain;
        this.firstpropose = true;
    }

    public void setExecutionManager(ExecutionManager executionManager) {
        this.executionManager = executionManager;
    }

    public void setTOMLayer(TOMLayer tomLayer) {
        this.tomLayer = tomLayer;
    }

    public void startConsensus(int cid, byte[] data) {
        this.data = data;
        this.cid = cid;
        ProposalMessage p = factory.createPROPOSAL(data, new byte[1], //blockchain.getCurrentHash(),
                null, 0, cid);
        p.addSignature();
        communication.send(this.controller.getCurrentViewOtherAcceptors(), p);
    }

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
            case NewMessageFactory.VOTE:
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
        logger.info("VOTE received from:{}, for consensus cId:{}",
                msg.getSender(), cid);
        if (checkVOTE(msg)) {
            executeVOTE(epoch, msg);
        } else {
            logger.info("VOTE invalid.");
        }
    }

    /**
     * check whether a VOTE message is valid
     * @param msg the VOTE message
     * @return valid(true) or not(false)
     */
    private boolean checkVOTE(VoteMessage msg) {
        return true;
    }

    public void executeVOTE(Epoch epoch, VoteMessage msg) {
        epoch.setVote(msg.getSender(), msg);//record the VOTEs
        if(epoch.countVote() > controller.getQuorum() - 1) {
            ProposalMessage p = factory.createPROPOSAL(data, blockchain.getCurrentHash(),
                    epoch.getVotes(), msg.getViewNumber(), cid);
            p.addSignature();
            communication.send(this.controller.getCurrentViewAcceptors(), p);
        }
    }

}