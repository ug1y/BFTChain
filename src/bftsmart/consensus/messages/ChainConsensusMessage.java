package bftsmart.consensus.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import bftsmart.communication.SystemMessage;


public abstract class ChainConsensusMessage extends SystemMessage {

    protected int messageType; // message type, including proposal, vote, and sync
    protected int viewNumber; // the view number that this message created in
    protected int epoch; // epoch to which this message belongs to

    /**
     * to avoid EOFException in Serializable
     */
    public ChainConsensusMessage(){}

    public ChainConsensusMessage(int messageType, int viewNumber, int epoch,
                                 int from) {
        super(from);

        this.messageType = messageType;
        this.viewNumber = viewNumber;
        this.epoch = epoch;
    }

    public int getEpoch() {
        return epoch;
    }

    public int getViewNumber() {
        return viewNumber;
    }

    public int getMessageType() {
        return messageType;
    }

    @Override
    public String toString(){
        return "\ntype = " + this.messageType +
                "\nviewNumber = " + this.viewNumber +
                "\nepoch = " + this.epoch +
                "\nfrom = " + super.getSender();
    }

    // Implemented method of the Externalizable interface
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

        super.writeExternal(out);

        out.writeInt(messageType);
        out.writeInt(viewNumber);
        out.writeInt(epoch);
    }

    // Implemented method of the Externalizable interface
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        super.readExternal(in);

        messageType = in.readInt();
        viewNumber = in.readInt();
        epoch = in.readInt();
    }
}




