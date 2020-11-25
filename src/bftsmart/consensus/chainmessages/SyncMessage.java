package bftsmart.consensus.chainmessages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class SyncMessage extends ChainConsensusMessage {

    private ProposalMessage msg; // the block that needs to sync

    /**
     * to avoid EOFException in Serializable
     */
    public SyncMessage(){}

    public SyncMessage(ProposalMessage msg, int viewNumber, int epoch, int from, int id) {
        super(ChainMessageFactory.SYNC, viewNumber, epoch, from, id);

        this.msg = msg;
    }

    @Override
    public String toString(){
        return "\ntype = SYNC" +
                "\nviewNumber = " + super.viewNumber +
                "\nepoch = " + super.epoch +
                "\nmsg = " + this.msg;
    }


    // Implemented method of the Externalizable interface
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);

        out.writeObject(msg);
    }

    // Implemented method of the Externalizable interface
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        msg = (ProposalMessage)in.readObject();
    }
}