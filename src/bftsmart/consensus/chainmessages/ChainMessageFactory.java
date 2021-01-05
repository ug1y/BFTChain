package bftsmart.consensus.chainmessages;

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
    public ProposalMessage createPROPOSAL(byte[] data, byte[] prevHash,
                                          int viewNumber, int consId, int epoch) {
        return new ProposalMessage(data, prevHash, viewNumber, consId, epoch, this.from);
    }

    /**
     * create a VOTE message
     * @param blockHeight the hash of the block which is voted for
     * @param viewNumber the view this message in
     * @param epoch the epoch this message in
     * @return a VOTE message
     */
    public VoteMessage createVOTE(int blockHeight,int viewNumber, int consId, int epoch) {
        return new VoteMessage(blockHeight, viewNumber, consId, epoch, this.from);
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
