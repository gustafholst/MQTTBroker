package se.miun.dt070a.mqttbroker.request;

import se.miun.dt070a.mqttbroker.error.MalformedMQTTRequestError;
import se.miun.dt070a.mqttbroker.MessageType;
import se.miun.dt070a.mqttbroker.Request;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

public class ConnectRequest extends Request {

    /* not used yet (header flags) */
    private String protocolName;
    private int protocolLevel;
    private int keepAlive;

    /* not used yet (connect flags from variable length header) */
    private boolean userName;
    private boolean password;
    private boolean willRetain;
    private int willQoS;
    private boolean willFlag;

    private String clientId;
    private boolean cleanSession;

    public ConnectRequest(Socket socket) throws IOException {
        super(socket);
    }

    public Optional<String> getClientId() {
        return Optional.ofNullable(clientId);
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    private void parseConnectFlags(int flags) {
        userName = (flags & 128) != 0;   // 10000000
        password = (flags & 64) != 0;    // 01000000
        willRetain = (flags & 32) != 0;  // 00100000
        willQoS = (flags & 24) >> 3;     // 00011000
        willFlag = (flags & 4) != 0;     // 00000100
        cleanSession = (flags & 2) != 0; // 00000010
                                         // 00000001 is reserved (SHOULD be checked for zero and disconnect otherwise)
    }

    public void createFromInputStream() throws IOException {
        if (remaining == 0)   // first two bytes (minimum header) have already been read
            return;

        try {
            int lengthMSB = nextByteIfRemainingElseThrow();
            int lengthLSB = nextByteIfRemainingElseThrow();

            //move significant bit 8 steps to the left
            int variableLengthHeaderLength = (lengthMSB << 8) + lengthLSB;

        /*
        The variable header for the CONNECT Packet consists of four fields in the following order:
         - Protocol Name
         - Protocol Level
         - Connect Flags
         - Keep Alive.
         */
            byte[] bytes = nextNBytesIfRemainingElseThrow(variableLengthHeaderLength);
            this.protocolName = new String(bytes);

            this.protocolLevel = nextByteIfRemainingElseThrow();  //Level 4 is MQTT vs 3

            int connectFlags = nextByteIfRemainingElseThrow();
            parseConnectFlags(connectFlags);

            int keepAliveMSB = nextByteIfRemainingElseThrow();
            int keepAliveLSB = nextByteIfRemainingElseThrow();
            this.keepAlive = (keepAliveMSB << 8) + keepAliveLSB;

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

            byte[] clientIdBuffer = nextNBytesIfRemainingElseThrow(clienIdentifierLength);

            this.clientId = new String(clientIdBuffer);

        } catch (MalformedMQTTRequestError mqttRequestException) {
            System.out.println("Malformed mqtt request");

            // here should instead be set a flag in order to send connect response with response code
        }
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.CONNECT;
    }
}
