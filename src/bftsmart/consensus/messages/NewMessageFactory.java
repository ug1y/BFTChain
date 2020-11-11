package bftsmart.consensus.messages;
/**
 * This class work as a factory of messages used in the new protocol.
 */
import java.util.Set;

public class NewMessageFactory{

    //constants for messages types
    public static final int PROPOSAL    = 1110;
    public static final int VOTE        = 1111;
    public static final int SYNC        = 1112;
    private int from; //Replica ID of the process that send the message

    /**
     * create a message factory
     * @param from Replica ID of the process that send the message
     */
    public NewMessageFactory(int from) {
        this.from = from;
    }

    /**
     * create a PROPOSAL message
     * @param data the data to propose
     * @param prevHash the hash of previous block
     * @param votes the votes voted for previous block
     * @param signature the signature of current leader for this message
     * @param viewNumber the view this message in
     * @param epoch the epoch this message in
     * @return a PROPOSAL message
     */
    public ProposalMessage createPROPOSAL(byte[] data, int prevHash,
                                          Set<VoteMessage> votes,
                                          int viewNumber, int epoch) {
        return new ProposalMessage(data, prevHash, votes, PROPOSAL,
                viewNumber, epoch, this.from);
    }

    /**
     * create a VOTE message
     * @param blockHash the hash of the block which is voted for
     * @param signature the signature of this replica for this message
     * @param viewNumber the view this message in
     * @param epoch the epoch this message in
     * @return a VOTE message
     */
    public VoteMessage createVOTE(int blockHash,int viewNumber,
                                  int epoch) {
        return new VoteMessage(blockHash,VOTE,viewNumber, epoch,
                this.from);
    }

    /**
     * create a SYNC message
     * @param msg the proposal message which trigger the voting
     * @param viewNumber the view this message in
     * @param epoch the epoch this message in
     * @return a SYNC message
     */
    public SyncMessage createSYNC(ProposalMessage msg, int viewNumber, int epoch) {
        return new SyncMessage(msg, SYNC, viewNumber, epoch, this.from);
    }
}