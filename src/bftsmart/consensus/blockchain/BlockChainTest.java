package bftsmart.consensus.blockchain;

import bftsmart.consensus.messages.NewConsensusMessage;
import bftsmart.consensus.messages.NewMessageFactory;

public class BlockChainTest {
    public void test() {
        Chain c = new Chain();
        NewMessageFactory f = new NewMessageFactory(0);
        byte[] b = new byte[2];
        b[0] = 0x0;
        b[1] = 0xf;
        NewConsensusMessage m1 = f.createPROPOSE(
                b, 1, c.getCurrentBlockHash(), 1, "no votes here"
        );
        System.out.println("m1 = {}" + m1.toString());
        Block b1 = new Block(m1);
        c.addBlock(b1);
        NewConsensusMessage m2 = f.createPROPOSE(
                b, 2, c.getCurrentBlockHash(), 2, "no votes here"
        );
        System.out.println("m2 = {}" + m2.toString());
        Block b2 = new Block(m2);
        c.addBlock(b2);
        NewConsensusMessage m3 = f.createPROPOSE(
                b, 3, c.getCurrentBlockHash(), 3, "no votes here"
        );
        System.out.println("m3 = {}" + m3.toString());
        Block b3 = new Block(m3);
        c.addBlock(b3);
        Block b3_ = c.getToSubmit();
        NewConsensusMessage m3_ = f.getPROPOSE(b3_);
        System.out.println("m3_ = {}" + m3_.toString());
    }
}