package se.miun.dt070a.mqttbroker.request;

import se.miun.dt070a.mqttbroker.MalformedMQTTRequestError;
import se.miun.dt070a.mqttbroker.MessageType;
import se.miun.dt070a.mqttbroker.Request;

import java.io.IOException;
import java.net.Socket;

public class ConnectRequest extends Request {

    private boolean userName;
    private boolean password;
    private boolean willRetain;
    private int willQoS;
    private boolean willFlag;
    private boolean cleanSession;

    public ConnectRequest(Socket socket) throws IOException {
        super(socket);
    }

    private void parseConnectFlags(int flags) {
        userName = (flags & 128) != 0;   // 10000000
        password = (flags & 64) != 0;    // 01000000
        willRetain = (flags & 32) != 0;  // 00100000
        willQoS = (flags & 24) >> 3;     // 00011000
        willFlag = (flags & 4) != 0;     // 00000100
        cleanSession = (flags & 2) != 0; // 00000010
                                         // 00000001 is reserved
    }

    public void createFromInputStream() throws IOException {

        System.out.println("remaining: " + remaining);

        if (remaining == 0)
            return;

        try {
            int lengthMSB = nextByteIfRemainingElseThrow();
            int lengthLSB = nextByteIfRemainingElseThrow();

            //move significant bit 8 steps to the left
            int variableLengthHeaderLength = (lengthMSB << 8) + lengthLSB;
            System.out.println("Variable length header length: " + variableLengthHeaderLength);

        /*
        The variable header for the CONNECT Packet consists of four fields in the following order:
         - Protocol Name
         - Protocol Level
         - Connect Flags
         - Keep Alive.
         */
            byte[] bytes = nextNBytesIfRemainingElseThrow(variableLengthHeaderLength);
            System.out.println("Protocol name: " + new String(bytes));

            int protocolLevel = nextByteIfRemainingElseThrow();  //Level 4 is MQTT vs 3
            System.out.println("Protocol level: " + protocolLevel);

            int connectFlags = nextByteIfRemainingElseThrow();
            System.out.println("connect flags: " + Integer.toBinaryString(connectFlags));

            int keepAliveMSB = nextByteIfRemainingElseThrow();
            int keepAliveLSB = nextByteIfRemainingElseThrow();
            int keepAlive = (keepAliveMSB << 8) + keepAliveLSB;
            System.out.println("Keep alive (seconds): " + keepAlive);

        /*
        The payload of the CONNECT Packet contains one or more length-prefixed fields, whose presence
        is determined by the flags in the variable header. These fields,
        if present, MUST appear in the order
         - Client Identifier
         - Will Topic
         - Will Message
         - User Name
         - Password
        */

            int clientIdentifierLengthMSB = nextByteIfRemainingElseThrow();
            int clientIdentifierLengthLSB = nextByteIfRemainingElseThrow();
            int clienIdentifierLength = (clientIdentifierLengthMSB << 8) + clientIdentifierLengthLSB;
            System.out.println("client id length: " + clienIdentifierLength);

            byte[] clientIdBuffer = nextNBytesIfRemainingElseThrow(clienIdentifierLength);
            System.out.println("client id: " + new String(clientIdBuffer));

        } catch (MalformedMQTTRequestError mqttRequestException) {
            System.out.println("Malformed mqtt request");
        }

    }

    @Override
    public MessageType getMessageType() {
        return MessageType.CONNECT;
    }
}
