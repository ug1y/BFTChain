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

public class SyncMessage extends ChainConsensusMessage {

    private ProposalMessage msg; // the block that needs to sync

    /**
     * to avoid EOFException in Serializable
     */
    public SyncMessage(){}

    public SyncMessage(ProposalMessage msg, int viewNumber, int consId, int epoch, int from) {
        super(ChainMessageFactory.SYNC, viewNumber, consId, epoch, from);

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