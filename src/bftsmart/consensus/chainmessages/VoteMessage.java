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
import java.security.PublicKey;
import java.util.Arrays;
import bftsmart.tom.util.TOMUtil;

public class VoteMessage extends ChainConsensusMessage {

    private byte[] blockHash; // the hash value of voted block
    private int replicaID; // identify the replica
    private byte[] signature; // signed by the replica

    /**
     * to avoid EOFException in Serializable
     */
    public VoteMessage(){}

    public VoteMessage(byte[] blockHash, int viewNumber, int consId, int epoch, int from) {
        super(ChainMessageFactory.VOTE, viewNumber, consId, epoch, from);

        this.blockHash = blockHash;
        this.replicaID = from;
    }

    public void addSignature(byte[] signature) {
        this.signature = signature;
    }

    public byte[] getSignature(){
        return this.signature;
    }

    public boolean verifySignature(PublicKey pubKey) {
        byte[] signature = this.signature;
        this.signature = null;
        byte[] b = TOMUtil.computeHash(this.getBytes());
        boolean ret = TOMUtil.verifySignature(pubKey, b, signature);
        this.signature = signature;
        return ret;
    }

    public byte[] getBlockHash() {
        return blockHash;
    }

    @Override
    public String toString(){
        return "\ntype = VOTE" +
                "\nviewNumber = " + super.viewNumber +
                "\nepoch = " + super.epoch +
                "\nblockHash = " + Arrays.toString(this.blockHash) +
                "\nreplicaID = " + this.replicaID +
                "\nsignature = " + Arrays.toString(this.signature);
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