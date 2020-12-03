package se.miun.dt070a.mqttbroker;

import se.miun.dt070a.mqttbroker.request.ConnectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Optional;

public abstract class Request {

    private final Socket socket;
    protected int remaining = 0;

    protected Request(Socket socket) throws IOException {
        this.socket = socket;

        // second byte of the header
        remaining = socket.getInputStream().read();
    }

    public Socket getSocket() {
        return socket;
    }

    private InputStream getInputStream() throws IOException {
        return this.socket.getInputStream();
    }

    protected int nextByteIfRemainingElseThrow() throws IOException, MalformedMQTTRequestError {
        if (remaining-- <= 0) {
            throw new MalformedMQTTRequestError();
        }
        return getInputStream().read();
    }

    protected byte[] nextNBytesIfRemainingElseThrow(int n) throws IOException, MalformedMQTTRequestError {
        remaining -= n;
        if (remaining < 0) {
            throw new MalformedMQTTRequestError();
        }

        return getInputStream().readNBytes(n);
    }

    public abstract void createFromInputStream() throws IOException;

    public abstract MessageType getMessageType();

    /*
        Factory method for creating the corresponding Request instances.
     */
    public static Optional<Request> parseRequest(Socket socket) throws IOException, MalformedMQTTRequestError {
        //first byte of the header   |0|1|2|3|4|5|6|7
        //                           |  type |d|QoS|r
        int flags = socket.getInputStream().read();

        Optional<Request> request;

        try {
            MessageType type = MessageType.headerFlagsToMessageType(flags);

            switch (type) {
                case CONNECT:
                    request = Optional.of(new ConnectRequest(socket)); break;
                default:
                    request = Optional.empty();  //should not happen (UnknownMessageType is thrown)
            }


        } catch (UnknownMessageTypeError unknownMessageTypeError) {
            throw new MalformedMQTTRequestError("Unknown request type");
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
