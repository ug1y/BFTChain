package bftsmart.consensus;

import bftsmart.consensus.messages.ProposalMessage;

import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.HashMap;

public class Blockchain {

    private Map<Byte[], ProposalMessage> blocks; // the blocks written into the chain
    private List<ProposalMessage> chain; // the chain consisting of blocks
    private int currentHeight; // the current height of the blockchain
    private int committedHeight; // the committed height of the blockchain (generally 2 less than current height)

    public Blockchain() {
        this.blocks = new HashMap<Byte[], ProposalMessage>();
        this.chain = new LinkedList<ProposalMessage>();
        this.currentHeight = 0;
        this.committedHeight = 0;
    }

    public int getCurrentHash(){
        return 0;
    }

    //no concrete methods due to not knowing the usage of Map and List
}
