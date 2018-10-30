package labohorloges;

/**
 * Protocol used by the master and the clients to communicate by UDP and TCP
 *
 * @author Nathan & Jimmy
 */
public class Protocol {
    protected static final int PORT_SYNC = 4000;    // Port for UDP
    protected static final int PORT_DELAY_CLIENT = 4001; // Port for TCP Client
    protected static final int PORT_DELAY_SERVER = 4003; // Port for TCP Server
    protected static final String MULTICAST = "232.32.32.32"; // Multicast IP Adress
    protected static final byte SYNC = 0;           // Byte to verify that the message is a SYNC
    protected static final byte FOLLOW_UP = 1;      // Byte to verify that the message is a FOLLOW_UP
    protected static final byte DELAY_RESPONSE = 2; // Byte to verify that the message is a DELAY_RESPONSE
    protected static final byte DELAY_REQUEST = 3;  // Byte to verify that the message is a DELAY_REQUEST
    protected static final int SEND_K = 300;       // Time to wait between messages
}
