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
 * Slave class trying to synchronize a local time with a distant server
 * To reach this objective we listen on UDP a first time the server time.
 * Then we calcul the gap with our time.
 * Then we ask by TCP to the server again the time, To calculate the delay.
 * With the local time, the delay, and the gap we can obtain the server time
 * synchronized.
 * 
 * @author Nathan & Jimmy
 */
public class SlaveClock {
    // Master ip adress obtained with the first UDP SYNC message
    private InetAddress masterAdress;
    // Atomic makes long safe thread
    private AtomicLong gapMasterSlave = new AtomicLong();
    private AtomicLong delay = new AtomicLong();
    private byte idRequest; // ID of the current DELAY_REQUEST sent to the server
    private Timer timeSlave = new Timer();
    private DatagramSocket unicastSocket;
    
    /**
     * Thread managing the reception of SYNC and FOLLOW_UP message sent by UDP
     * by the server
     */
    private Thread slaveThread = new Thread(new Runnable() {
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
        
        /**
         * Method managing the reception of the SYNC and FOLLOW_UP messages
         * It also start the scheduled task after the first FOLLOW_UP received.
         */
        @Override
        public void run() {
            try {
                waitFollowUp();
            } catch (IOException ex) {
                Logger.getLogger(SlaveClock.class.getName()).log(Level.SEVERE, null, ex);
            }
            timeSlave.schedule(new TaskSlave(), 0);
            
            while(true) {
                try {
                    waitFollowUp();
                } catch (IOException ex) {
                    Logger.getLogger(SlaveClock.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        /**
         * Method reading the SYNC and FOLLOW_UP UDP messages sent by the distant
         * server.
         * @throws IOException 
         */
        private void waitFollowUp() throws IOException {
            byte[] dataMasterSync = new byte[2];
            DatagramPacket masterPacketSync = new DatagramPacket(dataMasterSync,
                                                            dataMasterSync.length);
            Byte id = null;
            
            // --------------SYNC----------------
            // We can receive a FOLLOW_UP as first message of lose data, then we
            // wait to receive the complete SYNC message from the server
            while(id == null) {
                System.out.println("slave wait SYNC");
                socketMulticastSlave.receive(masterPacketSync);
                if(dataMasterSync[0] == Protocol.SYNC &&
                   dataMasterSync.length == 2) {
                    System.out.println("slave received SYNC");
                    id = dataMasterSync[1];
                    masterAdress = masterPacketSync.getAddress();
                }
            }
            byte[] dataMasterFollow = new byte[2 + Long.BYTES];

            // --------------FOLLOW_UP-------------
            DatagramPacket masterPacketFollow = new DatagramPacket(dataMasterFollow,
                                                            dataMasterFollow.length);
            // Receiving Follow_Up package
            System.out.println("slave wait FOLLOW_UP");
            socketMulticastSlave.receive(masterPacketFollow);
            System.out.println("FOLLOW_UP " + masterPacketFollow.toString());
            // We check that the second message is effectively a  complete FOLLOW_UP
            // to update the gap calcul in a secure way
            if(dataMasterFollow[0] == Protocol.FOLLOW_UP &&
                dataMasterFollow.length == (2 + Long.BYTES) &&
                dataMasterFollow[1] == id) {
                System.out.println("Slave received FOLLOW_UP");
                Arrays.copyOfRange(dataMasterFollow, 2, dataMasterFollow.length);
                long masterTime = ByteBuffer.wrap(dataMasterFollow).getLong();
                long slaveTime = System.currentTimeMillis();

                // Calculate the gap between the slave and master current time.
                gapMasterSlave.set(masterTime - slaveTime);
                System.out.println("Gap: " + gapMasterSlave.get());
            }
        }
    });
    
    /**
     * Task managing the DELAY_REQUEST and DELAY_RESPONSE messages.
     * It is launched at first time after reading a FOLLOW_UP message.
     * And it launch himself the same task after finishing it and waiting between
     * [4k, 60k] millisecond.
     */
    private class TaskSlave extends TimerTask {
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
                DatagramPacket datagramRequest = new DatagramPacket(dataRequest,
                                                        dataRequest.length,
                                                        masterAdress,
                                                        Protocol.PORT_DELAY_SERVER);
                long beforeSend = System.currentTimeMillis();
                System.out.println("slave send DELAY_REQUEST");
                unicastSocket.send(datagramRequest);
                
                // Receiving the second message RESPONSE from the slave
                byte[] dataResponse = new byte[2 + Long.BYTES];
                DatagramPacket datagramResponse = new DatagramPacket(dataResponse,
                                                                dataResponse.length);
                System.out.println("Slave wait DELAY_RESPONSE");
                unicastSocket.receive(datagramResponse);
                if(dataResponse[0] == Protocol.DELAY_RESPONSE &&
                   dataResponse[1] == idRequest) {
                    System.out.println("Slave received DELAY_RESPONSE");
                    Arrays.copyOfRange(dataResponse, 2, dataResponse.length);
                    long masterTimeResponse = ByteBuffer.wrap(dataResponse).getLong();
                    delay.set((masterTimeResponse - beforeSend) / 2);
                    System.out.println("Delay: " + delay.get());
                    System.out.println("Time: " + actualTime());
                }
            } catch (IOException ex) {
                Logger.getLogger(MasterClock.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            timeSlave.schedule(new TaskSlave(), (4 + new Random().nextInt(57))
                                                * Protocol.SEND_K);
        }
    };

    /**
     * Launch the thread who manage the SYNC and FOLLOW_UP messages
     */
    public SlaveClock() throws SocketException {
        this.unicastSocket = new DatagramSocket();
        slaveThread.start();
    }
    
    /**
     * Get the current synchronized time
     */
    public long actualTime() {
        return System.currentTimeMillis() + gapMasterSlave.get() + delay.get();
    }
    
    public static void main(String[] args) throws SocketException {
        SlaveClock slave = new SlaveClock();
    }
}
