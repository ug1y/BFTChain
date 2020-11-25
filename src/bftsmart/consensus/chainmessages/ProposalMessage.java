package bftsmart.consensus.chainmessages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.Arrays;
import java.util.Set;

public class ProposalMessage extends ChainConsensusMessage {

    private byte[] data; // the data that the block contains
    private byte[] prevHash; // the hash value of the referred block
    private Set<VoteMessage> votes; // the set of votes to prove this block valid

    private int leaderID; // identify the current leader
    private byte[] signature; // signed by the current leader

    private int index; // the height of the block (extra)

    /**
     * to avoid EOFException in Serializable
     */
    public ProposalMessage(){}

    public ProposalMessage(byte[] data, byte[] prevHash, Set<VoteMessage> votes,
                           int viewNumber, int epoch, int from, int id) {
        super(ChainMessageFactory.PROPOSAL, viewNumber, epoch, from, id);

        this.data = data;
        this.prevHash = prevHash;
        this.votes = votes;
        this.leaderID = from;
    }

    public void addSignature() {
//        this.signature = null;
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

    public byte[] getPrevHash() {
        return prevHash;
    }

    public byte[] getHash() {
        return new byte[1024];
    }

    @Override
    public String toString(){
        return "\ntype = PROPOSAL" +
                "\nviewNumber = " + super.viewNumber +
                "\nepoch = " + super.epoch +
                "\ndata = " + Arrays.toString(this.data) +
                "\nprevHash = " + Arrays.toString(this.prevHash) +
                "\nvotes = " + this.votes +
                "\nleaderID = " + this.leaderID +
                "\nsignature = " + Arrays.toString(this.signature);
    }

    // Implemented method of the Externalizable interface
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

        super.writeExternal(out);

        out.writeInt(leaderID);

        if(data == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(data.length);
            out.write(data);
        }

        if(prevHash == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(prevHash.length);
            out.write(prevHash);
        }

        if(signature == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(signature.length);
            out.write(signature);
        }

        out.writeObject(votes);

    }

    // Implemented method of the Externalizable interface
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        super.readExternal(in);

        leaderID = in.readInt();

        int len = in.readInt();
        if(len != -1) {
            data = new byte[len];
            do{
                len -= in.read(data, data.length-len, len);
            }while(len > 0);
        }

        len = in.readInt();
        if(len != -1) {
            prevHash = new byte[len];
            do{
                len -= in.read(prevHash, prevHash.length-len, len);
            }while(len > 0);
        }

        len = in.readInt();
        if(len != -1) {
            signature = new byte[len];
            do{
                len -= in.read(signature, signature.length-len, len);
            }while(len > 0);
        }

        votes = (Set<VoteMessage>)in.readObject();

    }
}
