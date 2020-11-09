package bftsmart.consensus;

import bftsmart.consensus.messages.ProposalMessage;

import java.util.HashMap;

public class Blockchain {

    private HashMap<Integer, ProposalMessage> blocks; // the blocks written into the chain
    private int currentHeight; // the current height of the blockchain
    private int committedHeight; // the committed height of the blockchain (generally 2 less than current height)
    private byte[] currentBlockHash; // the hash value of the block at the current height

}
