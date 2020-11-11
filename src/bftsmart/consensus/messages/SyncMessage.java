package bftsmart.consensus.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class SyncMessage extends ChainConsensusMessage {

    private ProposalMessage msg; // the block that needs to sync

    public SyncMessage(ProposalMessage msg, int messageType, int viewNumber,
                       int epoch, int from) {
        super(messageType, viewNumber, epoch, from);

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
    }

    // Implemented method of the Externalizable interface
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
    }
}