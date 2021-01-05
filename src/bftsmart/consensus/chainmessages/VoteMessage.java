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

public class VoteMessage extends ChainConsensusMessage {

    private int blockHeight; // the hash value of voted block
    private int replicaID; // identify the replica
    private Object signature; // signed by the replica

    /**
     * to avoid EOFException in Serializable
     */
    public VoteMessage(){}

    public VoteMessage(int blockHeight, int viewNumber, int consId, int epoch, int from) {
        super(ChainMessageFactory.VOTE, viewNumber, consId, epoch, from);

        this.blockHeight = blockHeight;
        this.replicaID = from;
        this.signature = 0;
    }

    public void setSignature(Object signature) {
        this.signature = signature;
    }

    public int getBlockHeight() {
        return blockHeight;
    }

    public int getReplicaID() { return replicaID; }

    public Object getSignature() { return signature; }

    @Override
    public String toString(){
        return "\ntype = VOTE" +
                "\nviewNumber = " + super.viewNumber +
                "\nepoch = " + super.epoch +
                "\nblockHeight = " + this.blockHeight +
                "\nreplicaID = " + this.replicaID +
                "\nsignature = " + this.signature;
    }

    // Implemented method of the Externalizable interface
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

        super.writeExternal(out);

        out.writeInt(replicaID);

        out.writeInt(blockHeight);
//        if(blockHash == null) {
//            out.writeInt(-1);
//        } else {
//            out.writeInt(blockHash.length);
//            out.write(blockHash);
//        }

        if(signature == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeObject(signature);
        }
    }

    // Implemented method of the Externalizable interface
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        super.readExternal(in);

        replicaID = in.readInt();

        blockHeight = in.readInt();
//        int len = in.readInt();
//        if(len != -1) {
//            blockHash = new byte[len];
//            do{
//                len -= in.read(blockHash, blockHash.length-len, len);
//            }while(len > 0);
//        }

        boolean asSignature = in.readBoolean();
        if (asSignature) {
            signature = in.readObject();
        }
    }

}
