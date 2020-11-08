/**
Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

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
package bftsmart.consensus.roles;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.consensus.messages.MessageFactory;
import bftsmart.reconfiguration.ServerViewController;

///
import bftsmart.consensus.messages.NewMessageFactory;
import bftsmart.consensus.messages.NewConsensusMessage;
import bftsmart.consensus.blockchain.BlockChainTest;
import bftsmart.consensus.blockchain.Chain;
import bftsmart.consensus.blockchain.Block;
import bftsmart.tom.core.ExecutionManager;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.TOMUtil;
import java.util.*;
import bftsmart.consensus.Consensus;
import bftsmart.consensus.Epoch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
///
/**
 * This class represents the proposer role in the consensus protocol.
 **/
public class Proposer {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private NewMessageFactory factory; // Factory for PaW messages
    private ServerCommunicationSystem communication; // Replicas comunication system
    private ServerViewController controller;
    private Chain chain;
    private Boolean firstpropose;
    private ExecutionManager executionManager;// Execution manager of consensus's executions
    private TOMLayer tomLayer; // TOM layer

    /**
     * Creates a new instance of Proposer
     * 
     * @param communication Replicas communication system
     * @param factory Factory for PaW messages
     * @param verifier Proof verifier
     * @param conf TOM configuration
     */
    public Proposer(ServerCommunicationSystem communication, NewMessageFactory factory,
            ServerViewController controller, Chain chain) {
        this.communication = communication;
        this.factory = factory;
        this.controller = controller;
        this.chain = chain;
        this.firstpropose = true;
    }

    public void setExecutionManager(ExecutionManager executionManager) {
        this.executionManager = executionManager;
    }

    public void setTOMLayer(TOMLayer tomLayer) {
        this.tomLayer = tomLayer;
    }

    /**
     * This method is called by the TOMLayer (or any other)
     * to start the consensus instance.
     *
     * @param cid ID for the consensus instance to be started
     * @param value Value to be proposed
     */
    public void startConsensus(int cid, byte[] value) {
        //******* EDUARDO BEGIN **************//
//        communication.send(this.controller.getCurrentViewAcceptors(),
//                factory.createPropose(cid, 0, value));
        //******* EDUARDO END **************//
        ///
        if(firstpropose){// for the first init, just send PROPOSE to all acceptor,
            // "true" means decide directly without counting votes
            NewConsensusMessage smsg = factory.createPROPOSE(value, 0, 0, cid, "true");
            communication.send(this.controller.getCurrentViewAcceptors(), smsg);
            firstpropose = false;
        }else {// for the else consensus, send new PROPOSE to all replicas after counting votes
            NewConsensusMessage pmsg = factory.createPROPOSE(value, 0, 0, cid, "test");
            Consensus consensus = executionManager.getConsensus(pmsg.getEpoch());
            consensus.lock.lock();
            Epoch epoch = consensus.getEpoch(pmsg.getEpoch(), controller);
            countVOTE(epoch, pmsg);
            consensus.lock.unlock();
        }
//        BlockChainTest t = new BlockChainTest();
//        t.test();
        ///
    }

    /**
     * counting VOTEs that the leader has received, if meet some value required,
     * then decide this consensus
     * @param cid consensus number, usage not clear
     * @param epoch the epoch related to the consensus, which usage is not clear
     */
    public void countVOTE(Epoch epoch, NewConsensusMessage msg) {
        int me = this.controller.getStaticConf().getProcessId();
        logger.info("I have {} VOTEs , Timestamp:{} ", epoch.countVote(), epoch.getTimestamp());
        if (epoch.countVote() > controller.getQuorum()) {
            msg.setHashValue(chain.getCurrentBlockHash());
            msg.setSetofProof(epoch.countVote() + " proofs here.");
            chain.addBlock(new Block(msg));//add the current block to the chain locally
            logger.info("I'm {}, I've just added BLOCK" + msg.toString() + "to the chain locally.", me);
            communication.send(this.controller.getCurrentViewAcceptors(), msg);
            decide(msg);
        }
    }
    /**
     * decide the consensus through message and send this to the client
     * @param msg which to be sent
     */
    private void decide(NewConsensusMessage msg) {
        Consensus consensus = executionManager.getConsensus(msg.getEpoch());
        Epoch epoch = consensus.getEpoch(msg.getEpoch(), controller);
        byte[] value = msg.getData();
        epoch.propValue = value;
        epoch.deserializedPropValue = tomLayer.checkProposedValue(value, true);
        epoch.writeSent();
        epoch.acceptSent();
        epoch.acceptCreated();
        decide(epoch);
    }

    /**
     * decide the consensus and send this to the client
     * but now the client won't accept it for its unedit strategy of accepting
     * @param epoch the epoch related to the consensus
     */
    private void decide(Epoch epoch) {
        if (epoch.getConsensus().getDecision().firstMessageProposed != null)
            epoch.getConsensus().getDecision().firstMessageProposed.decisionTime = System.nanoTime();
        epoch.getConsensus().decided(epoch, true);
    }
}
