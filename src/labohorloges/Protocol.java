/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labohorloges;

/**
 *
 * @author Nathan & Jimmy
 */
public class Protocol {
    protected static final int PORT_SYNC = 4000;
    protected static final int PORT_DELAY = 4001;
    protected static final String MULTICAST = "232.32.32.32";
    protected static final byte SYNC = 0;
    protected static final byte FOLLOW_UP = 1;
    protected static final byte DELAY_RESPONSE = 2;
    protected static final byte DELAY_REQUEST = 3;
    protected static final int SEND_K = 3000;
}
