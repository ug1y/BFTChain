package bftsmart.consensus.messages;

public class SyncMessage extends ChainConsensusMessage {

    private ProposalMessage msg; // the block that needs to sync

}