package bftsmart.consensus.messages;

import java.util.Set;

public class ProposalMessage extends ChainConsensusMessage {

    private byte[] data; // the data that the block contains
    private byte[] prevHash; // the hash value of the referred block
    private Set<VoteMessage> votes; // the set of votes to prove this block valid
    private int leaderID; // identify the current leader
    private byte[] signature; // signed by the current leader

    private int index; // the height of the block (extra)

}
