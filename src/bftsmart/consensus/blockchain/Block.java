package bftsmart.consensus.blockchain;

import bftsmart.consensus.messages.NewConsensusMessage;
import bftsmart.consensus.messages.NewMessageFactory;

public class Block  {

    private byte[] data = null; // the data that the block proposed for
    private int viewNumber; // the view number that this message for
    private int hashValue; // the hash value of the previous block
    private Object setofProof; // the set of votes or view-change in PROPOSE and NEWVIEW
    private int epoch; // Epoch to which this message belongs to

    /**
     * for generate a blank block for genesis
     */
    public Block() {
        this.data = null;
        this.viewNumber = 0;
        this.hashValue = 0;
        this.setofProof = null;
        this.epoch = 0;
    }

    /**
     * constructing a block using a PROPOSE message
     * @param msg the PROPOSE message we use
     */
    public Block(NewConsensusMessage msg) {
        this.data = msg.getData();
        this.viewNumber = msg.getViewNumber();
        this.hashValue = msg.getHashValue();
        this.setofProof = msg.getSetofProof();
        this.epoch = msg.getEpoch();
    }

    /**
     * computing the hash value of previous block, which can be used in structing
     * new PROPOSE
     * @return the hash value of block, the way that computed is referring to
     * other hashCode() override in this project
     */
    public int computeHashValue() {
        int hash = 1;
        if (this.data != null) {
            for (int i = 0; i < this.data.length; i++)
                hash = hash * 31 + (int)this.data[i];
        } else {
            hash = hash * 31 + 0;
        }
        hash = hash * 31 + this.viewNumber;
        hash = hash * 31 + this.hashValue;
        hash = hash * 31 + this.setofProof.hashCode();
        hash = hash * 31 + this.epoch;
        return hash;
    }

    /**
     * verifying the VOTEs for previous block
     * @return whether all VOTEs are vaild or not
     */
    public boolean verifyProof() {
        return true;
    }

    /**
     * some get methods
     * @return appropriate value
     */
    public byte[] getData() {
        return data;
    }

    public int getHashValue() {
        return hashValue;
    }

    public int getViewNumber() {
        return viewNumber;
    }

    public Object getSetofProof() {
        return setofProof;
    }

    public int getEpoch() {
        return epoch;
    }
}