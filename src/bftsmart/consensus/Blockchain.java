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
    private byte[] currentHash; // the hash value of the block at the current height

    public Blockchain() {
        this.blocks = new HashMap<byte[], ProposalMessage>();
        this.chain = new LinkedList<ProposalMessage>();
        this.currentHeight = -1;
    }

    public Map<byte[], ProposalMessage> getBlocks() { return blocks; }

    public List<ProposalMessage> getChain() {
        return chain;
    }

    public int getCurrentHeight() {
        return currentHeight;
    }

    public byte[] getCurrentHash(){
        return currentHash;
    }

    /**
     * add the genesis block into
     */
    public void initBlockchain(){
        ProposalMessage geniusBlock = new ProposalMessage(null,null,-1,-1,-1, -1);
        chain.add(geniusBlock);

        byte[] geniusHash = computeBlockHash(geniusBlock);
        blocks.put(geniusHash,geniusBlock);

        this.currentHash = geniusHash;
        this.currentHeight += 1;
        logger.debug("I've init the blockchain with a genius block {}", geniusBlock);
    }

    public byte[] computeBlockHash(ProposalMessage msg) {
        return TOMUtil.computeHash(TOMUtil.getBytes(msg));
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
