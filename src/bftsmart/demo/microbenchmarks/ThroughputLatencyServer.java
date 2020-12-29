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
package bftsmart.demo.microbenchmarks;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.CommandsInfo;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import bftsmart.tom.util.Storage;
import bftsmart.tom.util.TOMUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple server that just acknowledge the reception of a request.
 */
public final class ThroughputLatencyServer extends DefaultRecoverable{
    
    private int interval;
    private byte[] reply;
    private float maxTp = -1;
    private boolean context;
    private int signed;
    
    private byte[] state;
    
    private int iterations = 0;
    private long throughputMeasurementStartTime = System.currentTimeMillis();
            
    private Storage totalLatency = null;
    private Storage consensusLatency = null;
    private Storage preConsLatency = null;
    private Storage decisionLatency = null;
    private Storage replyLatency = null;
    private Storage writeLatency = null;
    private Storage acceptLatency = null;
    private Storage proposalLatency = null;
    private Storage voteLatency = null;
    
    private Storage batchSize = null;
    
    private ServiceReplica replica;
    
    private RandomAccessFile randomAccessFile = null;
    private FileChannel channel = null;
    private BufferedWriter bftbw = null;

    public ThroughputLatencyServer(int id, int interval, int replySize, int stateSize, boolean context,  int signed, int write) {

        this.interval = interval;
        this.context = context;
        this.signed = signed;
        
        this.reply = new byte[replySize];
        
        for (int i = 0; i < replySize ;i++)
            reply[i] = (byte) i;
        
        this.state = new byte[stateSize];
        
        for (int i = 0; i < stateSize ;i++)
            state[i] = (byte) i;

        totalLatency = new Storage(interval);
        consensusLatency = new Storage(interval);
        preConsLatency = new Storage(interval);
        decisionLatency = new Storage(interval);
        replyLatency = new Storage(interval);
        writeLatency = new Storage(interval);
        acceptLatency = new Storage(interval);
        proposalLatency = new Storage(interval);
        voteLatency = new Storage(interval);

        batchSize = new Storage(interval);
        
        if (write > 0) {
            
            try {
                bftbw = new BufferedWriter(new FileWriter("result/" + Long.toString(System.nanoTime()) + "_" +
                        id + "_" + interval + "_" + replySize + "_" + stateSize + ".txt", true));
                final File f = File.createTempFile("bft-"+id+"-", Long.toString(System.nanoTime()));
                randomAccessFile = new RandomAccessFile(f, (write > 1 ? "rwd" : "rw"));
                channel = randomAccessFile.getChannel();
                
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    
                    @Override
                    public void run() {
                        
                        f.delete();
                        try {
                            bftbw.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(0);
            }
        }
        replica = new ServiceReplica(id, this, this);
    }
    
    @Override
    public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtxs, boolean fromConsensus) {

        batchSize.store(commands.length);
                
        byte[][] replies = new byte[commands.length][];

        for (int i = 0; i < commands.length; i++) {

            replies[i] = execute(commands[i],msgCtxs[i]);

        }
        
        if (randomAccessFile != null) {
                
            ObjectOutputStream oos = null;
            try {
                CommandsInfo cmd = new CommandsInfo(commands,msgCtxs);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(bos);
                oos.writeObject(cmd);
                oos.flush();
                byte[] bytes = bos.toByteArray();
                oos.close();
                bos.close();
                
                ByteBuffer bb = ByteBuffer.allocate(bytes.length);
                bb.put(bytes);
                bb.flip();
                
                channel.write(bb);
                channel.force(false);
            } catch (IOException ex) {
                Logger.getLogger(ThroughputLatencyServer.class.getName()).log(Level.SEVERE, null, ex);
                
            } finally {
                try {
                    oos.close();
                } catch (IOException ex) {
                    Logger.getLogger(ThroughputLatencyServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
                    
        return replies;
    }
    
    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        return execute(command,msgCtx);
    }
    
    public byte[] execute(byte[] command, MessageContext msgCtx) {
        
        ByteBuffer buffer = ByteBuffer.wrap(command);
        int l = buffer.getInt();
        byte[] request = new byte[l];
        buffer.get(request);
        l = buffer.getInt();
        byte[] signature = new byte[l];
        
        buffer.get(signature);
        Signature eng;
        
        try {
            
            if (signed > 0) {
                
                if (signed == 1) {
                    
                    eng = TOMUtil.getSigEngine();
                    eng.initVerify(replica.getReplicaContext().getStaticConfiguration().getPublicKey());
                } else {
                
                    eng = Signature.getInstance("SHA256withECDSA", "SunEC");
                    Base64.Decoder b64 = Base64.getDecoder();
                    CertificateFactory kf = CertificateFactory.getInstance("X.509");
                
                    byte[] cert = b64.decode(ThroughputLatencyClient.pubKey);
                    InputStream certstream = new ByteArrayInputStream (cert);
                
                    eng.initVerify(kf.generateCertificate(certstream));
                    
                }
                eng.update(request);
                if (!eng.verify(signature)) {
                    
                    System.out.println("Client sent invalid signature!");
                    System.exit(0);
                }
            }
            
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | CertificateException ex) {
            ex.printStackTrace();
            System.exit(0);
        } catch (NoSuchProviderException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        
        boolean readOnly = false;
        
        iterations++;

        if (msgCtx != null && msgCtx.getFirstInBatch() != null) {
            

            readOnly = msgCtx.readOnly;
                    
            msgCtx.getFirstInBatch().executedTime = System.nanoTime();
                        
            totalLatency.store(msgCtx.getFirstInBatch().executedTime - msgCtx.getFirstInBatch().receptionTime);

            if (readOnly == false) {

                consensusLatency.store(msgCtx.getFirstInBatch().decisionTime - msgCtx.getFirstInBatch().consensusStartTime);
                long temp = msgCtx.getFirstInBatch().consensusStartTime - msgCtx.getFirstInBatch().receptionTime;
                preConsLatency.store(temp > 0 ? temp : 0);
                long tmpv = msgCtx.getFirstInBatch().voteSentTime - msgCtx.getFirstInBatch().deliveryTime;
                long tmpp = msgCtx.getFirstInBatch().proposalReceivedTime - msgCtx.getFirstInBatch().deliveryTime;
                long tmpd = msgCtx.getFirstInBatch().decisionTime - msgCtx.getFirstInBatch().deliveryTime;
                long tmpr = msgCtx.getFirstInBatch().requestReplyTime - msgCtx.getFirstInBatch().deliveryTime;
                if(tmpp > 0 && tmpp < 2000000000) {
                    proposalLatency.store(tmpp);
                }
                if(tmpv > 0 && tmpv < 2000000000){
                    voteLatency.store(tmpv);
                }
                if(tmpd > 0 && tmpd < 2000000000){
                    decisionLatency.store(tmpd);
                }
                if(tmpr > 0 && tmpr < 2000000000){
                    replyLatency.store(tmpr);
                }

            } else {
            
           
                consensusLatency.store(0);
                preConsLatency.store(0);
                decisionLatency.store(0);            
                replyLatency.store(0);
                writeLatency.store(0);
                acceptLatency.store(0);
                proposalLatency.store(0);
                voteLatency.store(0);
                
                
            }
            
        } else {
            
            
                consensusLatency.store(0);
                preConsLatency.store(0);
                decisionLatency.store(0);            
                replyLatency.store(0);
                writeLatency.store(0);
                acceptLatency.store(0);
                proposalLatency.store(0);
                voteLatency.store(0);
                
               
        }
        
        float tp = -1;
        if(iterations % interval == 0) {
            if (context) System.out.println("--- (Context)  iterations: "+ iterations + " // regency: " + msgCtx.getRegency() + " // consensus: " + msgCtx.getConsensusId() + " ---");
            
            System.out.println("--- Measurements after "+ iterations+" ops ("+interval+" samples) ---");
            
            tp = (float)(interval*1000/(float)(System.currentTimeMillis()-throughputMeasurementStartTime));
            
            if (tp > maxTp) maxTp = tp;
            
            System.out.println("Throughput = " + tp +" operations/sec (Maximum observed: " + maxTp + " ops/sec)");            
            
//            System.out.println("Total latency = " + totalLatency.getAverage(false) / 1000 + " (+/- "+ (long)totalLatency.getDP(false) / 1000 +") us ");
//            totalLatency.reset();
//            System.out.println("Consensus latency = " + consensusLatency.getAverage(false) / 1000 + " (+/- "+ (long)consensusLatency.getDP(false) / 1000 +") us ");
//            consensusLatency.reset();
//            System.out.println("Pre-consensus latency = " + preConsLatency.getAverage(false) / 1000 + " (+/- "+ (long)preConsLatency.getDP(false) / 1000 +") us ");
//            preConsLatency.reset();
//            System.out.println("Write latency = " + writeLatency.getAverage(false) / 1000 + " (+/- "+ (long)writeLatency.getDP(false) / 1000 +") us ");
//            writeLatency.reset();
//            System.out.println("Accept latency = " + acceptLatency.getAverage(false) / 1000 + " (+/- "+ (long)acceptLatency.getDP(false) / 1000 +") us ");
//            acceptLatency.reset();
            try {
                bftbw.write(Double.toString(voteLatency.getAverage(false) / 1000) + ", ");
                bftbw.write(Double.toString(proposalLatency.getAverage(false) / 1000) + ", ");
                bftbw.write(Double.toString(decisionLatency.getAverage(false) / 1000) + ", ");
                bftbw.write(Double.toString(replyLatency.getAverage(false) / 1000) + ", ");
                bftbw.write(Double.toString(batchSize.getSum() - 2) + "\n");
            }catch (Exception e){}
            System.out.println("Vote latency = " + voteLatency.getAverage(false) / 1000 + " (+/- "+ (long)voteLatency.getDP(false) / 1000 +") us ");
            voteLatency.reset();
            System.out.println("Proposal latency = " + proposalLatency.getAverage(false) / 1000 + " (+/- "+ (long)proposalLatency.getDP(false) / 1000 +") us ");
            proposalLatency.reset();
            System.out.println("Decision latency = " + decisionLatency.getAverage(false) / 1000 + " (+/- "+ (long)decisionLatency.getDP(false) / 1000 +") us ");
            decisionLatency.reset();
            System.out.println("Reply latency = " + replyLatency.getAverage(false) / 1000 + " (+/- "+ (long)replyLatency.getDP(false) / 1000 +") us ");
            replyLatency.reset();
            System.out.println("Batch amount = " + (batchSize.getSum() - 2)+" requests");
            batchSize.reset();
            
            throughputMeasurementStartTime = System.currentTimeMillis();
        }

        return reply;
    }

    public static void main(String[] args){
        if(args.length < 6) {
            System.out.println("Usage: ... ThroughputLatencyServer <processId> <measurement interval> <reply size> <state size> <context?> <nosig | default | ecdsa> [rwd | rw]");
            System.exit(-1);
        }

        int processId = Integer.parseInt(args[0]);
        int interval = Integer.parseInt(args[1]);
        int replySize = Integer.parseInt(args[2]);
        int stateSize = Integer.parseInt(args[3]);
        boolean context = Boolean.parseBoolean(args[4]);
        String signed = args[5];
        String write = args.length > 6 ? args[6] : "";
        
        int s = 0;
        
        if (!signed.equalsIgnoreCase("nosig")) s++;
        if (signed.equalsIgnoreCase("ecdsa")) s++;
        
        if (s == 2 && Security.getProvider("SunEC") == null) {
            
            System.out.println("Option 'ecdsa' requires SunEC provider to be available.");
            System.exit(0);
        }
        
        int w = 0;
        
        if (!write.equalsIgnoreCase("")) w++;
        if (write.equalsIgnoreCase("rwd")) w++;

        new ThroughputLatencyServer(processId,interval,replySize, stateSize, context, s, w);        
    }

    @Override
    public void installSnapshot(byte[] state) {
        //nothing
    }

    @Override
    public byte[] getSnapshot() {
        return this.state;
    }

   
}
