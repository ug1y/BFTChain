package bftsmart.consensus.chainmessages;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class VoteMessage extends ChainConsensusMessage {

    private byte[] blockHash; // the hash value of voted block
    private int replicaID; // identify the replica
    private byte[] signature; // signed by the replica

    /**
     * to avoid EOFException in Serializable
     */
    public VoteMessage(){}

    public VoteMessage(byte[] blockHash, int viewNumber, int epoch, int from) {
        super(ChainMessageFactory.VOTE, viewNumber, epoch, from);

        this.blockHash = blockHash;
        this.replicaID = from;
    }

    public void addSignature() {
//        this.signature = null;
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

        out.writeInt(replicaID);

        if(blockHash == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(blockHash.length);
            out.write(blockHash);
        }

        if(signature == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(signature.length);
            out.write(signature);
        }
    }

    // Implemented method of the Externalizable interface
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        super.readExternal(in);

        replicaID = in.readInt();

        int len = in.readInt();
        if(len != -1) {
            blockHash = new byte[len];
            do{
                len -= in.read(blockHash, blockHash.length-len, len);
            }while(len > 0);
        }

        len = in.readInt();
        if(len != -1) {
            signature = new byte[len];
            do{
                len -= in.read(signature, signature.length-len, len);
            }while(len > 0);
        }
    }
}