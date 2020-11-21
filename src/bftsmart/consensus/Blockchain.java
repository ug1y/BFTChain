package bftsmart.consensus;

import bftsmart.consensus.chainmessages.ProposalMessage;
import bftsmart.tom.util.TOMUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

public class Blockchain {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private Map<byte[], ProposalMessage> blocks; // the blocks written into the chain
    private List<ProposalMessage> chain; // the chain consisting of blocks
    private int currentHeight; // the current height of the blockchain
    private byte[] currentHash;

    public Blockchain() {
        this.blocks = new HashMap<byte[], ProposalMessage>();
        this.chain = new LinkedList<ProposalMessage>();
        this.currentHeight = -1;
    }

    /**
     * add the genesis block into
     */
    public void initBlockchain(){
        ProposalMessage geniusBlock = new ProposalMessage(null,null,null,-1,-1,-1);
        chain.add(geniusBlock);

        byte[] geniusHash = computeBlockHash(geniusBlock);
        blocks.put(geniusHash,geniusBlock);

        this.currentHash = geniusHash;
        this.currentHeight += 1;
        logger.info("I've init the blockchain with a genius block {}", geniusBlock);
    }

    public byte[] computeBlockHash(ProposalMessage msg) {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(bOut).writeObject(msg);
        } catch (IOException ex) {
            logger.error("Failed to serialize message", ex);
        }
        byte[] data = bOut.toByteArray();
        return TOMUtil.computeHash(data);
    }

    public byte[] getCurrentHash(){
        return currentHash;
    }


    public int getCurrentHeight() {
        return currentHeight;
    }


    public List<ProposalMessage> getChain() {
        return chain;
    }


    public ProposalMessage getBlockByHeight(int height) {
        if (height > -1 && height <= currentHeight) {
            return chain.get(height);
        }
        return null;
    }

    public ProposalMessage getBlockByHash(byte[] hash) {
        if (blocks.containsKey(hash)) {
            return blocks.get(hash);
        }
        return null;
    }


    public ProposalMessage appendBlock(ProposalMessage msg) {
        // only check if the proposal extends to the current block
        if (Arrays.equals(msg.getPrevHash(),this.currentHash)) {
            chain.add(msg);
            currentHeight++;

            currentHash = computeBlockHash(msg);
            blocks.put(currentHash,msg);

            // return the committed block
            if(currentHeight >= 3)
                return chain.get(currentHeight-2);
        }
        return null;
    }

    public boolean replaceBlock(ProposalMessage msg) {
        // only check if the proposal at the same height with the current block
        if (Arrays.equals(chain.get(currentHeight).getPrevHash(),msg.getPrevHash())) {
            chain.remove(currentHeight);
            chain.add(msg);

            blocks.remove(currentHash);
            currentHash = computeBlockHash(msg);
            blocks.put(currentHash,msg);
        }
        return false;
    }

    public boolean rollBack() {
        // remove the current block
        if (currentHeight > 0) {
            chain.remove(currentHeight);
            currentHeight--;

            blocks.remove(currentHash);
            currentHash = computeBlockHash(chain.get(currentHeight));

            return true;
        }
        return false;
    }
}
