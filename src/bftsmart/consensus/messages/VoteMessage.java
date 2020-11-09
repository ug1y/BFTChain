package bftsmart.consensus.messages;

public class VoteMessage extends ChainConsensusMessage {

    private byte[] blockHash; // the hash value of voted block
    private int replicaID; // identify the replica
    private byte[] signature; // signed by the replica

}