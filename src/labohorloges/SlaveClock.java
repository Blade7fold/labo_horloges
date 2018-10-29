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
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nathan & Jimmy
 */
public class SlaveClock {
    private InetAddress slaveAddress;
    private long gapMasterSlave;
    private long delay;
    private byte idRequest;
    private Timer timeSlave = new Timer();
    
    private Thread slaveThread = new Thread(new Runnable() {
        DatagramSocket socketSlave;
        {
            try {
                socketSlave = new DatagramSocket(Protocol.PORT_DELAY);
            } catch (SocketException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        MulticastSocket socket;
        InetAddress group;
        {
            try {
                this.group = InetAddress.getByName(Protocol.MULTICAST);
                this.socket = new MulticastSocket();
                socket.joinGroup(group);
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
                System.out.println("Wait FOLLOW_UP");
                socketSlave.receive(masterPacketSync);
                if(dataMasterSync[0] == Protocol.SYNC && dataMasterSync.length == 2) {
                    System.out.println("Received FOLLOW_UP");
                    id = dataMasterSync[1];
                    slaveAddress = masterPacketSync.getAddress();
                }
            }
            
            // Calculating the gap between the slave and the master
            byte[] dataMasterFollow = new byte[2 + Long.BYTES];
            DatagramPacket masterPacketFollow = new DatagramPacket(dataMasterFollow, dataMasterFollow.length);
            // Receiving Follow_Up package
            socketSlave.receive(masterPacketFollow);
            System.out.println("FOLLOW_UP " + masterPacketFollow.toString());
            if(dataMasterFollow[0] == Protocol.FOLLOW_UP &&
                dataMasterFollow.length == (2 + Long.BYTES) &&
                dataMasterFollow[1] == id) {
                Arrays.copyOfRange(dataMasterFollow, 2, dataMasterFollow.length);
                long masterTime = ByteBuffer.wrap(dataMasterFollow).getLong();
                long slaveTime = System.currentTimeMillis();
                gapMasterSlave = masterTime - slaveTime;
                System.out.println("Gap: " + gapMasterSlave);
            }
        }
    });
    
    private TimerTask taskSlave = new TimerTask() {
        DatagramSocket unicastSocket;
        InetAddress group;
        {
            try {
                this.group = InetAddress.getByName(Protocol.MULTICAST);
                this.unicastSocket = new DatagramSocket();
            } catch (UnknownHostException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
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
                             group, Protocol.PORT_DELAY);
                long beforeSend = System.currentTimeMillis();
                unicastSocket.send(datagramRequest);
                
                // Receiving the second message RESPONSE from the slave
                byte[] dataResponse = new byte[2 + Long.BYTES];
                DatagramPacket datagramResponse = new DatagramPacket(dataResponse, dataResponse.length,
                             group, Protocol.PORT_DELAY);
                unicastSocket.receive(datagramResponse);
                if(dataResponse[0] == Protocol.DELAY_RESPONSE && dataResponse[1] == idRequest) {
                    Arrays.copyOfRange(dataResponse, 2, dataResponse.length);
                    long masterTimeResponse = ByteBuffer.wrap(dataResponse).getLong();
                    delay = (masterTimeResponse - beforeSend) / 2;
                }
            } catch (IOException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };

    public SlaveClock() {
        slaveThread.start();
        System.out.println("Start thread slave");
        timeSlave.schedule(taskSlave, 0, Protocol.SEND_K);
        System.out.println("Start slave timer");
    }
    
    public long actualTime() {
        return System.currentTimeMillis() + gapMasterSlave + delay;
    }
    
    public static void main(String[] args) {
        System.out.println("Main Slave");
        SlaveClock slave = new SlaveClock();
        System.out.println("Start slave");
    }
}
