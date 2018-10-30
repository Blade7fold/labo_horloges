/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labohorloges;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class master that will send to the slaves the current time.
 * It send the time by two way.
 * regularly by UDP on a multicast group.
 * And on slave request by TCP.
 * 
 * @author Nathan & Jimmy
 */
public class MasterClock {
    private byte id = 0;    // ID of the current Sync
    private Timer time = new Timer();
    
    /**
     * TimerTask initilized with master class to send SYNC and FOLLOW_UP regularly
     */
    private TimerTask timeTask = new TimerTask() {
        MulticastSocket socketMulticastMaster;
        InetAddress group;
        {
            try {
                this.group = InetAddress.getByName(Protocol.MULTICAST);
                this.socketMulticastMaster = new MulticastSocket();
            } catch (UnknownHostException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        /**
         * Task runned each k millisecond to send SYNC and FOLLOW_UP to slaves
         */
        @Override
        public void run() {
            try {
                // ----------------SYNC---------------
                // Send the message SYNC to the slave
                byte[] sendSync = {Protocol.SYNC, id};
                DatagramPacket datagram = new DatagramPacket(sendSync,
                                                            sendSync.length,
                                                            group,
                                                            Protocol.PORT_SYNC);
                socketMulticastMaster.send(datagram);
                System.out.println("Server sent SYNC with id: " + id);
                // Transform current time into bytes
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                buffer.putLong(System.currentTimeMillis());
                buffer.array();

                // --------------FOLLOW_UP------------
                // Build FOLLOW_UP message
                byte[] sendFollowUp = new byte[2 + buffer.capacity()];
                sendFollowUp[0] = Protocol.FOLLOW_UP;
                sendFollowUp[1] = id;
                for(int i = 0; i < buffer.capacity(); ++i) {
                    sendFollowUp[i+2] = buffer.get(i);
                }
                
                // Send the FOLLOW_UP message
                DatagramPacket datagram2 = new DatagramPacket(sendFollowUp,
                                                        sendFollowUp.length,
                                                        group, Protocol.PORT_SYNC);
                socketMulticastMaster.send(datagram2);
                System.out.println("Server sent FOLLOW_UP with id: " + id);
                ++id;
            } catch (IOException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };
    
    /**
     * Thread managing slaves DELAY_REQUEST requests
     * respond to each slave with DELAY_RESPONSE and the current time
     */
    private Thread masterThread = new Thread(new Runnable() {
        DatagramSocket socketMaster;
        {
            try {
                socketMaster = new DatagramSocket(Protocol.PORT_DELAY_SERVER);
            } catch (SocketException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        /**
         * Infinite method reading the DELAY_REQUEST by TCP and 
         * sending back to each with DELAY_RESPONSE and the current time.
         */
        @Override
        public void run() {
            byte[] dataSlave = new byte[2];
            DatagramPacket slavePacket = new DatagramPacket(dataSlave,
                                                            dataSlave.length);
            try {
                while(true) {
                    // ---------------DELAY_REQUEST----------------
                    System.out.println("Server wait DELAY_REQUEST");
                    socketMaster.receive(slavePacket);
                    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                    buffer.putLong(System.currentTimeMillis());
                    buffer.array();
                    
                    // ---------------DELAY_RESPONSE---------------
                    // Build the DELAY_RESPONSE message
                    byte[] sendDELAY_RESPONSE = new byte[2 + buffer.capacity()];
                    sendDELAY_RESPONSE[0] = Protocol.DELAY_RESPONSE;
                    sendDELAY_RESPONSE[1] = dataSlave[1];
                    for(int i = 0; i < buffer.capacity(); ++i) {
                        sendDELAY_RESPONSE[i+2] = buffer.get(i);
                    }
                    
                    // Check if the DELAY_REQUEST is correct and send back
                    // the DELAY_RESPONSE
                    if(dataSlave[0] == Protocol.DELAY_REQUEST &&
                                                dataSlave.length == 2) {
                        System.out.println("Server received DELAY_REQUEST");
                        DatagramPacket sendToSlave = new DatagramPacket(sendDELAY_RESPONSE,
                                                        sendDELAY_RESPONSE.length,
                                                        slavePacket.getAddress(),
                                                        slavePacket.getPort());
                        socketMaster.send(sendToSlave);
                        System.out.println("Server sent DELAY_RESPONSE");
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    });

    /**
     * Constructor of class MasterClock
     */
    public MasterClock() {
        masterThread.start();
        time.schedule(this.timeTask, 0, Protocol.SEND_K);
    }
    
    public static void main(String[] args) {
        MasterClock master = new MasterClock();
    }
}
