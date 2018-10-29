package labohorloges;

/**
 * Protocol used by the master and the clients to communicate by UDP and TCP
 *
 * @author Nathan & Jimmy
 */
public class Protocol {
    protected static final int PORT_SYNC = 4000;
    protected static final int PORT_DELAY_CLIENT = 4001;
    protected static final int PORT_DELAY_SERVER = 4003;
    protected static final String MULTICAST = "232.32.32.32";
    protected static final byte SYNC = 0;
    protected static final byte FOLLOW_UP = 1;
    protected static final byte DELAY_RESPONSE = 2;
    protected static final byte DELAY_REQUEST = 3;
    protected static final int SEND_K = 3000;
}
