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
package bftsmart.demo.counter;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Example replica that implements a BFT replicated service (a counter).
 * If the increment > 0 the counter is incremented, otherwise, the counter
 * value is read.
 * 
 * @author alysson
 */

public final class CounterServer extends DefaultSingleRecoverable  {
    
    private int counter = 0;
    private int iterations = 0;
    ///for benchmark
    private long voteSum = 0;
    private long proposalSum = 0;
    private long decisionSum = 0;
    private long replySum = 0;
    private int voteCount = 0;
    private int proposalCount = 0;
    private int decisionCount = 0;
    private int replyCount = 0;
    ///for benchmark
    private List<Integer> resultList = new ArrayList<Integer>();
    
    public CounterServer(int id) {
    	new ServiceReplica(id, this, this);
    }
            
    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {         
        iterations++;
        System.out.println("(" + iterations + ") Counter current value: " + counter);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(4);
            new DataOutputStream(out).writeInt(counter);
            return out.toByteArray();
        } catch (IOException ex) {
            System.err.println("Invalid request received!");
            return new byte[0];
        }
    }
  
    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
        iterations++;
        try {
            int increment = new DataInputStream(new ByteArrayInputStream(command)).readInt();
            if(resultList.size() > 0) {
                counter = resultList.get(resultList.size() - 1);
            }
            else {
                counter = 0;
            }
            counter += increment;
            if(msgCtx.getConsensusId() == resultList.size()) {
                resultList.add(counter);
            }
            else if(msgCtx.getConsensusId() == resultList.size() - 1) {
                resultList.set(msgCtx.getConsensusId() - 1, counter);
            }
            else {
                System.out.println("Out of Context ConsensusId: " + msgCtx.getConsensusId());
                return new byte[0];
            }
            if(resultList.size() < 3) {
                counter = 0;
            }
            else {
                counter = resultList.get(resultList.size() - 3);
            }
            
            System.out.println("(" + iterations + ") Counter was incremented. Current value = " + counter);
            ///for benchmark
//            System.out.println("time:" + msgCtx.getFirstInBatch().deliveryTime);
//            System.out.println("time:" + msgCtx.getFirstInBatch().voteSentTime);
//            System.out.println("time:" + msgCtx.getFirstInBatch().proposalReceivedTime);
//            System.out.println("time:" + msgCtx.getFirstInBatch().decisionTime);
//            System.out.println("time:" + msgCtx.getFirstInBatch().requestReplyTime);
            long tmpv = msgCtx.getFirstInBatch().voteSentTime - msgCtx.getFirstInBatch().deliveryTime;
            long tmpp = msgCtx.getFirstInBatch().proposalReceivedTime - msgCtx.getFirstInBatch().deliveryTime;
            long tmpd = msgCtx.getFirstInBatch().decisionTime - msgCtx.getFirstInBatch().deliveryTime;
            long tmpr = msgCtx.getFirstInBatch().requestReplyTime - msgCtx.getFirstInBatch().deliveryTime;
            if(tmpp > 0 && tmpp < 2000000000) {
                proposalSum += tmpp;
                proposalCount += 1;
            }
            if(tmpv > 0 && tmpv < 2000000000){
                voteSum += tmpv;
                voteCount += 1;
            }
            if(tmpd > 0 && tmpd < 2000000000){
                decisionSum += tmpd;
                decisionCount += 1;
            }
            if(tmpr > 0 && tmpr < 2000000000){
                replySum += tmpr;
                replyCount += 1;
            }
            if(voteCount > 0){
                System.out.println("average vote latency = " + voteSum / voteCount);
            }
            if(proposalCount > 0) {
                System.out.println("average proposal latency = " + proposalSum / proposalCount);
            }
            if(proposalCount > 0) {
                System.out.println("average decision latency = " + decisionSum / decisionCount);
            }
            if(proposalCount > 0) {
                System.out.println("average reply latency = " + replySum / replyCount);
            }
            ///for benchmark
            ByteArrayOutputStream out = new ByteArrayOutputStream(4);
            new DataOutputStream(out).writeInt(counter);
            return out.toByteArray();
        } catch (IOException ex) {
            System.err.println("Invalid request received!");
            return new byte[0];
        }
    }

    public static void main(String[] args){
        if(args.length < 1) {
            System.out.println("Use: java CounterServer <processId>");
            System.exit(-1);
        }      
        new CounterServer(Integer.parseInt(args[0]));
    }

    
    @SuppressWarnings("unchecked")
    @Override
    public void installSnapshot(byte[] state) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(state);
            ObjectInput in = new ObjectInputStream(bis);
            counter = in.readInt();
            in.close();
            bis.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Error deserializing state: "
                    + e.getMessage());
        }
    }

    @Override
    public byte[] getSnapshot() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeInt(counter);
            out.flush();
            bos.flush();
            out.close();
            bos.close();
            return bos.toByteArray();
        } catch (IOException ioe) {
            System.err.println("[ERROR] Error serializing state: "
                    + ioe.getMessage());
            return "ERROR".getBytes();
        }
    }
}
