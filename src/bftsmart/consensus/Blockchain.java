package bftsmart.consensus;

import bftsmart.consensus.messages.ProposalMessage;

import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.HashMap;

public class Blockchain {

    private Map<byte[], ProposalMessage> blocks; // the blocks written into the chain
    private List<ProposalMessage> chain; // the chain consisting of blocks
    private int currentHeight; // the current height of the blockchain
    private int committedHeight; // the committed height of the blockchain (generally 2 less than current height)
    private byte[] currentHash;

    public Blockchain() {
        this.blocks = new HashMap<byte[], ProposalMessage>();
        this.chain = new LinkedList<ProposalMessage>();
        this.currentHeight = -1;
        this.committedHeight = -1;
    }

    /**
     * add the genesis block into
     */
    public void initBlockchain(){
        addBlock(new ProposalMessage());
    }

    public byte[] getCurrentHash(){
        return currentHash;
    }

    /**
     * add an exist PROPOSAL message into the blockchain
     * @param msg
     */
    public void addBlock(ProposalMessage msg) {
        blocks.put(msg.getHash(), msg);
        chain.add(msg);
        this.currentHeight += 1;
        this.currentHash = msg.getHash();
        if(currentHeight >= 3){
            committedHeight += 1;
        }
    }

    public int getCurrentHeight() {
        return currentHeight;
    }
}
