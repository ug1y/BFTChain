package bftsmart.consensus;

import bftsmart.consensus.messages.ProposalMessage;

import java.util.List;
import java.util.Map;

public class Blockchain {

    private Map<Byte[], ProposalMessage> blocks; // the blocks written into the chain
    private List<ProposalMessage> chain; // the chain consisting of blocks
    private int currentHeight; // the current height of the blockchain
    private int committedHeight; // the committed height of the blockchain (generally 2 less than current height)

}
