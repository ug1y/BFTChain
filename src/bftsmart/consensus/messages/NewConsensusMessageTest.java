package bftsmart.consensus.messages;

public class NewConsensusMessageTest {

    public NewConsensusMessage testRequest() {
        NewMessageFactory f = new NewMessageFactory(0);
        byte[] b = new byte[2];
        b[0] = 0x0;
        b[1] = 0xf;
        NewConsensusMessage p = f.createPROPOSE(b, 2, 11, "no vote here");
        System.out.println("propose message in origin = " + p.toString());
        return p;
    }

    public void testResponse(NewConsensusMessage m) {
        System.out.println("received message = " + m.toString());
        NewMessageFactory f = new NewMessageFactory(1);
        NewConsensusMessage v = f.createVOTE(2, 12);
        System.out.println("vote message in origin = " + v.toString());
    }
    /**
     * to test the NewConsensusMessage class, there are several changes need to be taken,
     * they are in consensus.role.Proposer, consensus.role.Acceptor, communication.MessageHandler
     * and the changes in Proposer do affect the behaviour of program when processing messages of ConsensusMessage class
     * you can find those changes by searching ///, where changes are surrounded.
     */
}