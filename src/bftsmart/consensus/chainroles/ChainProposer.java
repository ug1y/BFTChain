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
    private ArrayList<Decision> decs;
    private byte[] data;


    public ChainProposer(ServerCommunicationSystem communication,
                         ChainMessageFactory factory,
                         ServerViewController controller,
                         Blockchain blockchain) {
        this.communication = communication;
        this.factory = factory;
        this.controller = controller;
        this.blockchain = blockchain;
        this.decs = new ArrayList<Decision>();
    }

    public void setExecutionManager(ExecutionManager executionManager) {
        this.executionManager = executionManager;
    }

    public void setTOMLayer(TOMLayer tomLayer) {
        this.tomLayer = tomLayer;
    }

    public final void getProposalValue(int cid, Decision dec) {
        Consensus consensus = executionManager.getConsensus(cid);
        consensus.lock.lock();
        Epoch epoch = consensus.getEpoch(cid, controller);
        logger.info("data received for consensus {}", cid);
        this.decs.add(dec);
        if(epoch.countVote() > controller.getQuorum() && this.decs.size() > blockchain.getCurrentHeight()) {
            int viewNumber = ((VoteMessage)epoch.getVotes().toArray()[0]).getViewNumber();
            ProposalMessage p = factory.createPROPOSAL(this.data, blockchain.getCurrentHash(),
                    epoch.getVotes(), viewNumber, blockchain.getCurrentHeight());
            p.addSignature();
            logger.info("get enough votes, proposing");
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
            logger.debug("Processing msg in number {}", msg.getEpoch());
            processMessage(msg);
        }
        else {
            logger.debug("Out of context msg in number {}", msg.getEpoch());
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
        if(epoch.countVote() > controller.getQuorum()){
            byte[] data = tomLayer.createPropose(this.decs.get(blockchain.getCurrentHeight()));
            this.data = data;
            if(this.decs.size() > blockchain.getCurrentHeight()) {
                logger.info("get enough votes");
                ProposalMessage p = factory.createPROPOSAL(this.data, blockchain.getCurrentHash(),
                        epoch.getVotes(), msg.getViewNumber(), blockchain.getCurrentHeight());
                p.addSignature();
                logger.info("proposing");
                communication.send(this.controller.getCurrentViewAcceptors(), p);
            }
        }
    }

}