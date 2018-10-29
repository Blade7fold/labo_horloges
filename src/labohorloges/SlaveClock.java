/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labohorloges;

import java.io.IOException;
import java.util.Arrays;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nathan & Jimmy
 */
public class SlaveClock {
    private InetAddress masterAdress;
    private AtomicLong gapMasterSlave = new AtomicLong();
    private AtomicLong delay = new AtomicLong();
    private byte idRequest;
    private Timer timeSlave = new Timer();
    
    private Thread slaveThread = new Thread(new Runnable() {
        DatagramSocket socketSlave;
        {
            try {
                socketSlave = new DatagramSocket();
            } catch (SocketException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        MulticastSocket socketMulticastSlave;
        InetAddress group;
        {
            try {
                this.group = InetAddress.getByName(Protocol.MULTICAST);
                this.socketMulticastSlave = new MulticastSocket(Protocol.PORT_SYNC);
                socketMulticastSlave.joinGroup(group);
            } catch (UnknownHostException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        @Override
        public void run() {
            try {
                waitFollowUp();
            } catch (IOException ex) {
                Logger.getLogger(SlaveClock.class.getName()).log(Level.SEVERE, null, ex);
            }
            timeSlave.schedule(taskSlave, 0);
            
            while(true) {
                try {
                    waitFollowUp();
                } catch (IOException ex) {
                    Logger.getLogger(SlaveClock.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        /**
         * 
         * @throws IOException 
         */
        private void waitFollowUp() throws IOException {
            byte[] dataMasterSync = new byte[2];
            DatagramPacket masterPacketSync = new DatagramPacket(dataMasterSync, dataMasterSync.length);
            Byte id = null;
            
            // Treating the case if we receive SYNC
            while(id == null) {
                System.out.println("slave wait SYNC");
                socketMulticastSlave.receive(masterPacketSync);
                if(dataMasterSync[0] == Protocol.SYNC && dataMasterSync.length == 2) {
                    System.out.println("slave received SYNC");
                    id = dataMasterSync[1];
                    masterAdress = masterPacketSync.getAddress();
                }
            }
            
            // Calculating the gap between the slave and the master
            byte[] dataMasterFollow = new byte[2 + Long.BYTES];
            DatagramPacket masterPacketFollow = new DatagramPacket(dataMasterFollow, dataMasterFollow.length);
            // Receiving Follow_Up package
            System.out.println("slave wait FOLLOW_UP");
            socketMulticastSlave.receive(masterPacketFollow);
            System.out.println("FOLLOW_UP " + masterPacketFollow.toString());
            if(dataMasterFollow[0] == Protocol.FOLLOW_UP &&
                dataMasterFollow.length == (2 + Long.BYTES) &&
                dataMasterFollow[1] == id) {
                System.out.println("slave received FOLLOW_UP");
                Arrays.copyOfRange(dataMasterFollow, 2, dataMasterFollow.length);
                long masterTime = ByteBuffer.wrap(dataMasterFollow).getLong();
                long slaveTime = System.currentTimeMillis();
                gapMasterSlave.set(masterTime - slaveTime);
                System.out.println("Gap: " + gapMasterSlave.get());
            }
        }
    });
    
    private TimerTask taskSlave = new TimerTask() {
        DatagramSocket unicastSocket;
        {
            try {
                this.unicastSocket = new DatagramSocket(Protocol.PORT_DELAY_CLIENT);
            } catch (IOException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        @Override
        public void run() {
            try {
                // Send the message REQUEST to the master
                byte[] dataRequest = {Protocol.DELAY_REQUEST, idRequest};
                DatagramPacket datagramRequest = new DatagramPacket(dataRequest, dataRequest.length,
                             masterAdress, Protocol.PORT_DELAY_SERVER);
                long beforeSend = System.currentTimeMillis();
                System.out.println("slave send DELAY_REQUEST");
                unicastSocket.send(datagramRequest);
                
                // Receiving the second message RESPONSE from the slave
                byte[] dataResponse = new byte[2 + Long.BYTES];
                DatagramPacket datagramResponse = new DatagramPacket(dataResponse, dataResponse.length);
                System.out.println("slave wait DELAY_RESPONSE");
                unicastSocket.receive(datagramResponse);
                if(dataResponse[0] == Protocol.DELAY_RESPONSE && dataResponse[1] == idRequest) {
                    System.out.println("slave received DELAY_RESPONSE");
                    Arrays.copyOfRange(dataResponse, 2, dataResponse.length);
                    long masterTimeResponse = ByteBuffer.wrap(dataResponse).getLong();
                    delay.set((masterTimeResponse - beforeSend) / 2);
                    System.out.println("delay: " + delay.get());
                    System.out.println("Time: " + actualTime());
                }
            } catch (IOException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            timeSlave.schedule(taskSlave, (4 + new Random().nextInt(57)) * Protocol.SEND_K);
        }
    };

    public SlaveClock() {
        slaveThread.start();
    }
    
    public long actualTime() {
        return ((System.currentTimeMillis() + gapMasterSlave.get() + delay.get()) / 1000 / 360 / 24 / 365);
    }
    
    public static void main(String[] args) {
        SlaveClock slave = new SlaveClock();
    }
}
