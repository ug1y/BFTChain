package bftsmart.consensus.messages;

/**
 * This class work as a factory of messages used in the new protocol.
 */
public class NewMessageFactory{

    //constants for messages types
    public static final int PROPOSE     = 1110;
    public static final int VOTE        = 1111;
    public static final int SYNC        = 1112;
//    public static final int VIEWCHANGE  = 1113;
//    public static final int NEWVIEW     = 1114;
//    in wrong place
    private int from; //Replica ID of the process that send the message

    /**
     * create a message factory
     * @param from Replica ID of the process that send the message
     */
    public NewMessageFactory(int from) {
        this.from = from;
    }

    /**
     * Creates a PROPOSE message to be sent by this process
     * @param data The proposal
     * @param viewNumber The view which in
     * @param hashValue The hash value of the previous block
     * @param setofvotes The votes voted for previous block
     * @return A message of PROPOSE type, with proper info
     */
    public NewConsensusMessage createPROPOSE(byte[] data, int viewNumber, int hashValue,
                                             int epoch, Object setofvotes){
        NewConsensusMessage m = new NewConsensusMessage(PROPOSE, viewNumber, epoch, from);
        m.setData(data);
        m.setHashValue(hashValue);
        m.setSetofProof(setofvotes);
        return m;
    }

    /**
     * Creates a VOTE message to be sent by this process
     * @param viewNumber The view which in
     * @param hashValue The hash of the block voted for
     * @return A message of VOTE type, with proper info
     */
    public NewConsensusMessage createVOTE(int viewNumber, int hashValue, int epoch){
        NewConsensusMessage m = new NewConsensusMessage(VOTE, viewNumber, epoch, from);
        m.setHashValue(hashValue);
        return m;
    }

    /**
     * Creates a SYMC message to be sent by this process
     * @param viewNumber The view which in
     * @param newMessage The proposal message which persuading for referred to
     * @return A message of SYNC type, with proper info
     */
    public NewConsensusMessage createSYNC(int viewNumber, Object newMessage, int epoch){
        NewConsensusMessage m = new NewConsensusMessage(SYNC, viewNumber, epoch, from);
        m.setNewMessage(newMessage);
        return m;
    }
//in wrong place
//    /**
//     * Creates a VIEWCHANGE message to be sent by this process
//     * @param viewNumber The new view number (v+1)
//     * @param hashValue The hash value of the last block that this process vote for
//     * @return A message of VIEWCHANGE type, with proper info
//     */
//    public NewConsensusMessage createVIEWCHANGE(int viewNumber, int hashValue){
//        NewConsensusMessage m = new NewConsensusMessage(VIEWCHANGE, viewNumber, from);
//        m.setHashValue(hashValue);
//        return m;
//    }
//
//    /**
//     * Creates a NEWVIEW message to be sent by this process
//     * @param viewNumber The new view number (v+1)
//     * @param setofviewchange The VIEWCHANGE messages in previous view
//     * @return
//     */
//    public NewConsensusMessage createNEWVIEW(int viewNumber, Object setofviewchange){
//        NewConsensusMessage m = new NewConsensusMessage(NEWVIEW, viewNumber, from);
//        m.setSetofProof(setofviewchange);
//        return m;
//    }
}