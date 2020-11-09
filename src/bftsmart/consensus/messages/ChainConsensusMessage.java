package bftsmart.consensus.messages;

import bftsmart.communication.SystemMessage;


public abstract class ChainConsensusMessage extends SystemMessage {

    protected int messageType; // message type, including proposal, vote, and sync
    protected int viewNumber; // the view number that this message created in
    protected int epoch; // epoch to which this message belongs to

}




