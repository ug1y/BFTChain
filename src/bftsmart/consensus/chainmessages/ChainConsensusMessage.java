package bftsmart.consensus.chainmessages;

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

    /**
     * Creates a consensus message. Used by the message factory to create a chain consensus message
     * @param messageType This should be NewMessageFactory.PROPOSAL or NewMessageFactory.VOTE or NewMessageFactory.SYNC
     * @param viewNumber The view number of the block proposed in
     * @param epoch Epoch timestamp
     * @param from This should be this process ID
     */
    public ChainConsensusMessage(int messageType, int viewNumber, int epoch, int from) {
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
        return "type = " + this.messageType + ", viewNumber = " + this.viewNumber +
                ", epoch = " + this.epoch + ", from = " + super.getSender();
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




