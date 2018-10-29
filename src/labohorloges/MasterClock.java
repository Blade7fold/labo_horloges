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
        MulticastSocket socket;
        InetAddress group;
        {
            try {
                this.group = InetAddress.getByName(Protocol.MULTICAST);
                this.socket = new MulticastSocket();
            } catch (UnknownHostException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        @Override
        public void run() {
            try {
                System.out.println("Send SYNC");
                // Send the message SYNC to the slave
                byte[] data = {Protocol.SYNC, id};
                System.out.println("Data " + Arrays.toString(data));
                DatagramPacket datagram = new DatagramPacket(data, data.length,
                             group, Protocol.PORT_SYNC);
                socket.send(datagram);
                System.out.println("Datagram " + datagram.toString());
                // Time transformation into bytes
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                buffer.putLong(System.currentTimeMillis());
                buffer.array();
                
                System.out.println("Buffer " + buffer.toString());
                // Sending the second message FOLLOW_UP to the slave
                System.out.println("Send FOLLOW_UP");
                byte[] data2 = {Protocol.FOLLOW_UP, id++, buffer.get()};
                System.out.println("Data2 " + Arrays.toString(data2));
                
                DatagramPacket datagram2 = new DatagramPacket(data2, data2.length,
                             group, Protocol.PORT_SYNC);
                socket.send(datagram2);
                System.out.println("Datagram2 " + datagram2.toString());
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
                socketMaster = new DatagramSocket(Protocol.PORT_DELAY);
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
                    socketMaster.receive(slavePacket);
                    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                    buffer.putLong(System.currentTimeMillis());
                    buffer.array();
                    
                    byte[] dataMaster = {Protocol.DELAY_RESPONSE, id, buffer.get()};
                    
                    if(dataSlave[0] == Protocol.DELAY_REQUEST && dataSlave.length == 2) {
                        DatagramPacket sendToSlave = new DatagramPacket(dataMaster,
                                                        dataMaster.length,
                                                        slavePacket.getAddress(),
                                                        slavePacket.getPort());
                        socketMaster.send(sendToSlave);
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
