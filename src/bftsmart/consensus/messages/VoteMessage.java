package bftsmart.consensus.messages;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class VoteMessage extends ChainConsensusMessage {

    private byte[] blockHash; // the hash value of voted block
    private int replicaID; // identify the replica
    private byte[] signature; // signed by the replica

    public VoteMessage(byte[] blockHash,int messageType,
                       int viewNumber, int epoch, int from) {
        super(messageType, viewNumber, epoch, from);

        this.blockHash = blockHash;
        this.replicaID = from;
    }

    public void addSignature() {
        this.signature = null;
    }

    @Override
    public String toString(){
        return "\ntype = VOTE" +
                "\nviewNumber = " + super.viewNumber +
                "\nepoch = " + super.epoch +
                "\nblockHash = " + this.blockHash +
                "\nreplicaID = " + this.replicaID +
                "\nsignature = " + this.signature;
    }

    // Implemented method of the Externalizable interface
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
    }

    // Implemented method of the Externalizable interface
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
    }
}