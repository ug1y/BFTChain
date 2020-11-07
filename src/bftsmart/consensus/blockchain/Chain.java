package bftsmart.consensus.blockchain;

import java.util.*;

public class Chain{
    private HashMap<Integer, Block> BlockMap;
    private int Height;
    private int CurrentBlockHash;

    /**
     * initialize a chain with a blank genesis block
     */
    public Chain(){
        this.BlockMap = new HashMap<Integer, Block>();
        this.Height = 0;
        this.BlockMap.put(0, new Block());
        this.CurrentBlockHash = 0;
    }

    /**
     * add a block to the chain, and get the block should be submitted if exist
     * @param block the block added
     * @return the block 2 previous if exist, or return null
     */
    public Block addBlock(Block block) {
        this.CurrentBlockHash = block.computeHashValue();
        this.BlockMap.put(this.CurrentBlockHash, block);
        this.Height += 1;
        if(this.Height > 2)
            return getToSubmit();
        return null;
    }

    /**
     * get the block which should be submit now
     * @return the 2 previous block from current
     */
    public Block getToSubmit() {
        Block PreviousBlock =
        this.BlockMap.get(
                this.BlockMap.get(
                        this.BlockMap.get(
                                this.CurrentBlockHash
                        ).getHashValue()
                ).getHashValue()
        );
        return PreviousBlock;
    }

    /**
     * some get methods
     * @return appropriate value
     */
    public int getHeight() {
        return Height;
    }

    public int getCurrentBlockHash() {
        return CurrentBlockHash;
    }
}