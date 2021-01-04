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
package bftsmart.consensus.chainroles;

import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.consensus.Blockchain;
import bftsmart.consensus.chainmessages.ChainConsensusMessage;
import bftsmart.consensus.chainmessages.ChainMessageFactory;
import bftsmart.consensus.chainmessages.VoteMessage;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.core.ExecutionManager;
import bftsmart.tom.core.TOMLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainAcceptor {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private ChainMessageFactory factory; // Factory for BFTChain messages
    private ServerCommunicationSystem communication; // Replicas comunication system
    private ServerViewController controller;

    private ExecutionManager executionManager; // Execution manager of consensus's executions
    private TOMLayer tomLayer; // TOM layer

    private Blockchain blockchain;

    public ChainAcceptor(ServerCommunicationSystem communication,
                         ChainMessageFactory factory,
                         ServerViewController controller,
                         Blockchain blockchain) {
        this.communication = communication;
        this.factory = factory;
        this.controller = controller;
        this.blockchain = blockchain;
    }

    public void setExecutionManager(ExecutionManager executionManager) {
        this.executionManager = executionManager;
    }

    public void setTOMLayer(TOMLayer tomLayer) {
        this.tomLayer = tomLayer;
    }

    /**
     * called by communication layer to delivery messages, if the message
     * can be execute, then call other method to process, or just storing
     * it as an out of context message
     * @param msg the message delivered by communication layer
     */
    public final void deliver(ChainConsensusMessage msg) {
        if (executionManager.checkLimits(msg)) {
            logger.debug("Processing msg in view {}", msg.getViewNumber());
            processMessage(msg);
        }
        else {
            logger.debug("Out of context msg in view {}", msg.getViewNumber());
            tomLayer.processOutOfContext();
            // using SYNC asking for latest blocks
        }
    }

    /**
     * called when a consensus message need to be processed, which processed
     * differently accroding to its type
     * @param msg process chain message
     */
    public final void processMessage(ChainConsensusMessage msg) {

    }


    /**
     * start a consensus by follower's voting
     * @param cid start consensus with id
     */
    public void startConsensus(int cid) {
        VoteMessage vote = factory.createVOTE(blockchain.getCurrentHash(), 0, cid, 0);

        int[] leader = new int[1];
        leader[0] = executionManager.getCurrentLeader();
        communication.send(leader, vote);
        logger.info("I've sent VOTE in cid {} to leader {}", cid, executionManager.getCurrentLeader());
    }
}
