package se.miun.dt070a.mqttbroker;

import se.miun.dt070a.mqttbroker.MessageType;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

public class MQTTBroker {

    public static class MalformedMQTTRequestException extends Throwable {

        public MalformedMQTTRequestException() {}

        public MalformedMQTTRequestException(String message) {
            super(message);
        }
    }

    public static abstract class Request {

        private InputStream inputStream;
        public int remaining = 0;

        protected Request(InputStream inputStream) throws IOException {
            this.inputStream = inputStream;
            remaining = inputStream.read();
        }

        protected int nextByteIfRemainingElseThrow() throws IOException, MalformedMQTTRequestException {
            if (remaining-- <= 0) {
                throw new MalformedMQTTRequestException();
            }
            return inputStream.read();
        }

        protected byte[] nextNBytesIfRemainingElseThrow(int n) throws IOException, MalformedMQTTRequestException {
            remaining -= n;
            if (remaining < 0) {
                throw new MalformedMQTTRequestException();
            }

            return inputStream.readNBytes(n);
        }

        public abstract void createFromInputStream() throws IOException;

        public static Optional<Request> parseRequest(InputStream inputStream) throws IOException, MalformedMQTTRequestException {
            //first byte of the header   |0|1|2|3|4|5|6|7
            //                           |  type |d|QoS|r
            int flags = inputStream.read();

            Optional<Request> request;

            try {
                MessageType type = MessageType.headerFlagsToMessageType(flags);

                switch (type) {
                    case CONNECT:
                        request = Optional.of(new ConnectRequest(inputStream)); break;
                    default:
                        request = Optional.empty();  //should not happen (UnknownMessageType is thrown)
                }


            } catch (UnknownMessageTypeError unknownMessageTypeError) {
                throw new MalformedMQTTRequestException("Unknown request type");
            }

            request.ifPresent(r -> {
                try {
                    r.createFromInputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            return request;
        }
    }


    public static class ConnectRequest extends Request {

        private boolean userName;
        private boolean password;
        private boolean willRetain;
        private int willQoS;
        private boolean willFlag;
        private boolean cleanSession;

        public ConnectRequest(InputStream inputStream) throws IOException {
            super(inputStream);
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

            } catch (MalformedMQTTRequestException mqttRequestException) {
                System.out.println("Malformed mqtt request");
            }

        }
    }

    public static void main(String[] args) {

        try {
            ServerSocket serverSocket = new ServerSocket(1883);

            System.out.println("Broker listening...");

            Socket socket = serverSocket.accept();

            System.out.println("tcp connection accepted...now waiting for incoming request");

            InputStream in = socket.getInputStream();

//            byte[] buffer = new byte[32];
//            in.read(buffer);
//
//            for (byte b : buffer) {
//                System.out.println(Integer.toBinaryString(b));
//            }

            try {
                Optional<Request> newRequest = Request.parseRequest(in);
            } catch (MalformedMQTTRequestException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
