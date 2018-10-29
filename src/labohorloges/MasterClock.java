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
 * 
 * @author Nathan & Jimmy
 */
public class MasterClock {
    private byte id = 0;    // ID of the package
    private Timer time = new Timer();   // Time that starts with master
    
    /**
     * TimerTask initilized with master class to send messages to slave
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
        
        @Override
        public void run() {
            try {
                // Send the message SYNC to the slave
                byte[] sendSync = {Protocol.SYNC, id};
                DatagramPacket datagram = new DatagramPacket(sendSync, sendSync.length,
                             group, Protocol.PORT_SYNC);
                socketMulticastMaster.send(datagram);
                System.out.println("server sent SYNC with id: " + id);
                // Time transformation into bytes
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                buffer.putLong(System.currentTimeMillis());
                buffer.array();
                
                // Sending the second message FOLLOW_UP to the slave
                byte[] sendFollowUp = new byte[2 + buffer.capacity()];
                sendFollowUp[0] = Protocol.FOLLOW_UP;
                sendFollowUp[1] = id;
                for(int i = 0; i < buffer.capacity(); ++i) {
                    sendFollowUp[i+2] = buffer.get(i);
                }
                
                DatagramPacket datagram2 = new DatagramPacket(sendFollowUp, sendFollowUp.length,
                             group, Protocol.PORT_SYNC);
                socketMulticastMaster.send(datagram2);
                System.out.println("server sent FOLLOW_UP with id: " + id);
                ++id;
            } catch (IOException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };
    
    /**
     * Thread created by master class to send the time to slaves
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
        @Override
        public void run() {
            byte[] dataSlave = new byte[2];
            DatagramPacket slavePacket = new DatagramPacket(dataSlave, dataSlave.length);
            try {
                while(true) {
                    System.out.println("server wait DELAY_REQUEST");
                    socketMaster.receive(slavePacket);
                    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                    buffer.putLong(System.currentTimeMillis());
                    buffer.array();
                    
                    byte[] sendDELAY_RESPONSE = new byte[2 + buffer.capacity()];
                    sendDELAY_RESPONSE[0] = Protocol.DELAY_RESPONSE;
                    sendDELAY_RESPONSE[1] = dataSlave[1];
                    for(int i = 0; i < buffer.capacity(); ++i) {
                        sendDELAY_RESPONSE[i+2] = buffer.get(i);
                    }
                    
                    if(dataSlave[0] == Protocol.DELAY_REQUEST && dataSlave.length == 2) {
                        System.out.println("server received DELAY_REQUEST");
                        DatagramPacket sendToSlave = new DatagramPacket(sendDELAY_RESPONSE,
                                                        sendDELAY_RESPONSE.length,
                                                        slavePacket.getAddress(),
                                                        slavePacket.getPort());
                        socketMaster.send(sendToSlave);
                        System.out.println("server sent DELAY_RESPONSE");
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
        System.out.println("Start thread master");
        time.schedule(this.timeTask, 0, Protocol.SEND_K);
        System.out.println("Start master timer");
    }
    
    public static void main(String[] args) {
        MasterClock master = new MasterClock();
        System.out.println("Start master");
    }
}
