package bftsmart.consensus.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.Set;

public class ProposalMessage extends ChainConsensusMessage {

    private byte[] data; // the data that the block contains
    private int prevHash; // the hash value of the referred block
    private Set<VoteMessage> votes; // the set of votes to prove this block valid
    private int leaderID; // identify the current leader
    private byte[] signature; // signed by the current leader

    private int index; // the height of the block (extra)

    public ProposalMessage(byte[] data, int prevHash,
                           Set<VoteMessage> votes, int messageType,
                           int viewNumber, int epoch, int from) {
        super(messageType, viewNumber, epoch, from);

        this.data = data;
        this.prevHash = prevHash;
        this.votes = votes;
        this.leaderID = from;

    }

    public void addSignature() {
        this.signature = null;
    }

    public boolean verifySignature() {
        return true;
    }

    public byte[] getData() {
        return data;
    }

    public Set<VoteMessage> getVotes() {
        return votes;
    }

    public byte[] getSignature() {
        return signature;
    }

    public int getLeaderID() {
        return leaderID;
    }

    public int getPrevHash() {
        return prevHash;
    }

    @Override
    public String toString(){
        return "\ntype = PROPOSAL" +
                "\nviewNumber = " + super.viewNumber +
                "\nepoch = " + super.epoch +
                "\ndata = " + this.data +
                "\nprevHash = " + this.prevHash +
                "\nvotes = " + this.votes +
                "\nleaderID = " + this.leaderID +
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
