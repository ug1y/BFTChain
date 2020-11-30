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
/**
 * This class work as a factory of messages used in the new protocol.
 */

import java.util.Set;
import java.util.LinkedHashSet;

public class ChainMessageFactory {

    //constants for messages types
    public static final int PROPOSAL    = 1110;
    public static final int VOTE        = 1111;
    public static final int SYNC        = 1112;
    private int from; //Replica ID of the process that send the message

    /**
     * create a message factory
     * @param from Replica ID of the process that send the message
     */
    public ChainMessageFactory(int from) {
        this.from = from;
    }

    /**
     * create a PROPOSAL message
     * @param data the data to propose
     * @param prevHash the hash of previous block
     * @param votes the votes voted for previous block
     * @param viewNumber the view this message in
     * @param epoch the epoch this message in
     * @return a PROPOSAL message
     */
    public ProposalMessage createPROPOSAL(byte[] data, byte[] prevHash, VoteMessage[] votes,
                                          int viewNumber, int consId, int epoch) {
        return new ProposalMessage(data, prevHash, votes, viewNumber, consId, epoch, this.from);
    }

    /**
     * create a VOTE message
     * @param blockHash the hash of the block which is voted for
     * @param viewNumber the view this message in
     * @param epoch the epoch this message in
     * @return a VOTE message
     */
    public VoteMessage createVOTE(byte[] blockHash,int viewNumber, int consId, int epoch) {
        return new VoteMessage(blockHash,viewNumber, consId, epoch, this.from);
    }

    /**
     * create a SYNC message
     * @param msg the proposal message which trigger the voting
     * @param viewNumber the view this message in
     * @param epoch the epoch this message in
     * @param consId the consensus ID this message in
     * @return a SYNC message
     */
    public SyncMessage createSYNC(ProposalMessage msg, int viewNumber, int consId, int epoch) {
        return new SyncMessage(msg, viewNumber, consId, epoch, this.from);
    }
}