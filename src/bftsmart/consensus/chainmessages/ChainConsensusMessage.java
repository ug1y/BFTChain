/*
Copyright (c) 2020 Hao Yin, Zhibo Xing

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package bftsmart.consensus.chainmessages;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import bftsmart.communication.SystemMessage;


public abstract class ChainConsensusMessage extends SystemMessage {

    protected int msgType; // message type, including proposal, vote, and sync
    protected int viewNumber; // the view number that this message created in
    protected int consId; // consensus ID for this message
    protected int epoch; // epoch to which this message belongs to

    /**
     * to avoid EOFException in Serializable
     */
    public ChainConsensusMessage(){}

    /**
     * Creates a consensus message. Used by the message factory to create a chain consensus message
     * @param msgType This should be NewMessageFactory.PROPOSAL or NewMessageFactory.VOTE or NewMessageFactory.SYNC
     * @param viewNumber The view number of the block proposed in
     * @param epoch Epoch timestamp
     * @param from This should be this process ID
     */
    public ChainConsensusMessage(int msgType, int viewNumber, int consId, int epoch, int from) {
        super(from);

        this.msgType = msgType;
        this.viewNumber = viewNumber;
        this.consId = consId;
        this.epoch = epoch;
    }

    public int getEpoch() {
        return epoch;
    }

    public int getViewNumber() {
        return viewNumber;
    }

    public int getMsgType() {
        return msgType;
    }

    public int getConsId() {
        return consId;
    }

    @Override
    public String toString(){
        return "type = " + this.msgType + ", viewNumber = " + this.viewNumber + "consId = " + this.consId +
                ", epoch = " + this.epoch + ", from = " + super.getSender();
    }

    // Implemented method of the Externalizable interface
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

        super.writeExternal(out);

        out.writeInt(msgType);
        out.writeInt(viewNumber);
        out.writeInt(consId);
        out.writeInt(epoch);
    }

    // Implemented method of the Externalizable interface
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        super.readExternal(in);

        msgType = in.readInt();
        viewNumber = in.readInt();
        consId = in.readInt();
        epoch = in.readInt();
    }
}




