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
import java.util.Arrays;

public class ProposalMessage extends ChainConsensusMessage {

    private byte[] data; // the data that the block contains
    private byte[] prevHash; // the hash value of the referred block
    private VoteMessage[] votes; // the set of votes to prove this block valid

    /**
     * to avoid EOFException in Serializable
     */
    public ProposalMessage(){}

    public ProposalMessage(byte[] data, byte[] prevHash, VoteMessage[] votes,
                           int viewNumber, int consId, int epoch, int from) {
        super(ChainMessageFactory.PROPOSAL, viewNumber, consId, epoch, from);

        this.data = data;
        this.prevHash = prevHash;
        this.votes = votes;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getPrevHash() {
        return prevHash;
    }

    public VoteMessage[] getVotes() {
        return votes;
    }

    @Override
    public String toString(){
        return "\ntype = PROPOSAL" +
                "\nviewNumber = " + super.viewNumber +
                "\nepoch = " + super.epoch +
                "\ndata = " + Arrays.toString(this.data) +
                "\nprevHash = " + Arrays.toString(this.prevHash) +
                "\nvotes = " + Arrays.toString(this.votes);
    }

    // Implemented method of the Externalizable interface
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

        super.writeExternal(out);

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

        if(votes == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(votes.length);
            for(VoteMessage v : votes) {
                out.writeObject(v);
            }
        }

    }

    // Implemented method of the Externalizable interface
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        super.readExternal(in);

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
            votes = new VoteMessage[len];
            for(int i = 0; i < len; i++) {
                votes[i] = (VoteMessage)in.readObject();
            }
        }
    }

}
