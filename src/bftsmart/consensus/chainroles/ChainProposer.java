package bftsmart.consensus.chainroles;

import bftsmart.communication.ServerCommunicationSystem;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

///
public class ChainProposer {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private ChainMessageFactory factory; // Factory for PaW messages
    private ServerCommunicationSystem communication; // Replicas comunication system
    private ServerViewController controller;
    private Blockchain blockchain;
    private ExecutionManager executionManager;// Execution manager of consensus's executions
    private TOMLayer tomLayer; // TOM layer
    private ArrayList<byte[]> proposalData;
    private byte[] data;
    private int cid;
    private boolean valueChanged = false;// to avoid that enough VOTEs received but data not changed(quick followers)



    public ChainProposer(ServerCommunicationSystem communication,
                         ChainMessageFactory factory,
                         ServerViewController controller,
                         Blockchain blockchain) {
        this.communication = communication;
        this.factory = factory;
        this.controller = controller;
        this.blockchain = blockchain;
        this.proposalData = new ArrayList<byte[]>();
    }

    public void setExecutionManager(ExecutionManager executionManager) {
        this.executionManager = executionManager;
    }

    public void setTOMLayer(TOMLayer tomLayer) {
        this.tomLayer = tomLayer;
    }

    /**
     * to store the cid and data from the request
     * @param cid
     * @param data
     */
    public void startConsensus(int cid, byte[] data) {
        this.data = data;
        this.cid = cid;
        this.valueChanged = true;
    }

    public final void getProposalValue(int cid, byte[] data) {
        Consensus consensus = executionManager.getConsensus(cid);
        consensus.lock.lock();
        Epoch epoch = consensus.getEpoch(cid, controller);
        logger.debug("data received for consensus {}", cid);
        this.proposalData.add(data);
        if(epoch.countVote() > controller.getQuorum() && this.proposalData.size() > blockchain.getCurrentHeight()) {
            ProposalMessage p = factory.createPROPOSAL(this.proposalData.get(blockchain.getCurrentHeight()), blockchain.getCurrentHash(),
                    epoch.getVotes(), cid, blockchain.getCurrentHeight());
            p.addSignature();
            logger.debug("get enough votes before data received, proposing {}", p);
            communication.send(this.controller.getCurrentViewAcceptors(), p);
        }
        consensus.lock.unlock();
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
        logger.debug("message = " + msg.toString());
        switch (msg.getMessageType()) {
            case ChainMessageFactory.VOTE:
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

    /**
     * if enough VOTEs received, send a PROPOSAL to all
     * @param epoch
     * @param msg
     */
    public void executeVOTE(Epoch epoch, VoteMessage msg) {
        epoch.setVote(msg.getSender(), msg);//record the VOTEs
        if(epoch.countVote() > controller.getQuorum() && this.proposalData.size() > blockchain.getCurrentHeight()) {
            ProposalMessage p = factory.createPROPOSAL(this.proposalData.get(blockchain.getCurrentHeight()), blockchain.getCurrentHash(),
                    epoch.getVotes(), msg.getViewNumber(), blockchain.getCurrentHeight());
            p.addSignature();
            logger.info("get enough votes, proposing {}", p);
            communication.send(this.controller.getCurrentViewAcceptors(), p);
        }
    }

}