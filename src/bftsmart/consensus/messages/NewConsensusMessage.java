package bftsmart.consensus.messages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import bftsmart.communication.SystemMessage;

/**
 * This class represents a message used in the new protocol.
 */
public class NewConsensusMessage extends SystemMessage {

    private int messageType; // message type
    private int viewNumber; // the view number that this message for
    private int hashValue; // the hash value of the previous block
    private Object setofProof; // the set of votes or view-change in PROPOSE and NEWVIEW
    private byte[] data = null; // the data that the block proposed for
    private Object newMessage; // the message that SYNC proposing for
    /**
     * here we use Object instead of NewConsensusMessage for setofProof and newMessage for
     * cannot converting Object into NewConsensusMessage in the Override version of readExternal
     * at the invocation of in.readObject()
     */

    /**
     * not used, or just for testing
     */
    public NewConsensusMessage(){}

    /**
     * generate a semi-finished message for all types of messages, set other params if needed
     * @param messageType In NewMessageFactory
     * @param viewNumber The view this message in or for
     * @param from The replica who send this
     */
    public NewConsensusMessage(int messageType, int viewNumber, int from){

        super(from);

        this.messageType = messageType;
        this.viewNumber = viewNumber;

    }

    // Implemented method of the Externalizable interface
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

        super.writeExternal(out);

        out.writeInt(messageType);
        out.writeInt(viewNumber);

        if(hashValue == 0) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeInt(hashValue);
        }

        if(setofProof == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeObject(setofProof);
        }

        if(data == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(data.length);
            out.write(data);
        }

        if(newMessage == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeObject(newMessage);
        }
    }

    // Implemented method of the Externalizable interface
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        super.readExternal(in);

        messageType = in.readInt();
        viewNumber = in.readInt();

        Boolean toRead = in.readBoolean();
        if(toRead)
            hashValue = in.readInt();

        toRead = in.readBoolean();
        if(toRead)
            setofProof = in.readObject();

        int len = in.readInt();
        if(len != -1) {
            data = new byte[len];
            do{
                len -= in.read(data, data.length-len, len);
            }while(len > 0);
        }

        toRead = in.readBoolean();
        if(toRead)
            newMessage = in.readObject();
    }

    /**
     * set hashvalue if needed(for type PROPOSE, VOTE, VIEW-CHANGE)
     * @param hashValue The hash value of block
     */
    public void setHashValue(int hashValue) {
        this.hashValue = hashValue;
    }

    /**
     * set the set of proof in needed(for type PROPOSE, NEW-VIEW)
     * @param setofProof The set of needed messages
     */
    public void setSetofProof(Object setofProof) {
        this.setofProof = setofProof;
    }

    /**
     * set data if needed(for type PROPOSE)
     * @param data The data that proposed
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * set newmessage if needed(for type SYNC)
     * @param newMessage The block that persuading for
     */
    public void setNewMessage(Object newMessage) {
        this.newMessage = newMessage;
    }

    public int getMessageType() {
        return messageType;
    }

    public int getViewNumber() {
        return viewNumber;
    }

    public int getHashValue() {
        return hashValue;
    }

    public Object getSetofProof() {
        return setofProof;
    }

    public byte[] getData() {
        return data;
    }

    public Object getNewMessage() {
        return newMessage;
    }

    /**
     * Returns this message type as a verbose string
     * @return message type
     */
    public String getMessageVerboseType() {
        if (messageType == NewMessageFactory.PROPOSE)
            return "PROPOSE";
        else if (messageType == NewMessageFactory.VOTE)
            return "VOTE";
        else if (messageType == NewMessageFactory.SYNC)
            return "SYNC";
        else if (messageType == NewMessageFactory.VIEWCHANGE)
            return "VIEWCHANGE";
        else if (messageType == NewMessageFactory.NEWVIEW)
            return "NEWVIEW";
        else
            return "NOT DEFINED";
    }

    @Override
    public String toString() {
        return "type=" + getMessageType() + ",\nview number=" + getViewNumber() +
                ",\nhash value=" + getHashValue() + ",\nset of proof=[" +
                getSetofProof() +"],\ndata=" + getData() + ",\nnew message=" +
                getNewMessage() + ".\n";
    }
}