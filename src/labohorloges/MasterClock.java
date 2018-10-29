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
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Nathan
 */
public class MasterClock {
    private final int SEND_K = 5000;
    private byte id = 0;
    private Timer time = new Timer();
    
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
                
                // Send the message SYNC to the slave
                byte[] data = {Protocol.SYNC, id++};
                DatagramPacket datagram = new DatagramPacket(data, data.length,
                             group, Protocol.PORT_SYNC);
                socket.send(datagram);
                
                // Time transformation into bytes
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                buffer.putLong(System.currentTimeMillis());
                buffer.array();
                
                // Sending the second message FOLLOW_UP to the slave
                byte[] data2 = {Protocol.FOLLOW_UP, id, buffer.get()};
                
                DatagramPacket datagram2 = new DatagramPacket(data2, data2.length,
                             group, Protocol.PORT_SYNC);
                socket.send(datagram2);
            } catch (IOException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };
    
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
                    
                    long currTime = System.currentTimeMillis();
                    if(dataSlave[0] == Protocol.DELAY_REQUEST && dataSlave.length == 2) {
                        DatagramPacket sendToSlave = new DatagramPacket(dataSlave,
                                                        dataSlave.length,
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
        time.schedule(this.timeTask, 0, this.SEND_K);
        masterThread.start();
    }
    
    public static void main(String[] args) {
        MasterClock master = new MasterClock();
    }
}
