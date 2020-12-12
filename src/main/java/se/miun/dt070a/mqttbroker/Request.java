package se.miun.dt070a.mqttbroker;

import se.miun.dt070a.mqttbroker.error.ConnectError;
import se.miun.dt070a.mqttbroker.error.MalformedMQTTRequestError;
import se.miun.dt070a.mqttbroker.error.UnknownMessageTypeError;
import se.miun.dt070a.mqttbroker.request.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
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

    public abstract void createFromInputStream() throws IOException, MalformedMQTTRequestError;

    public abstract MessageType getMessageType();

    /*
        Factory method for creating the corresponding Request instances.
     */
    public static Optional<Request> parseRequest(Socket socket) throws IOException, MalformedMQTTRequestError, ConnectError {

        Optional<Request> request;

        try {
            //read first byte of the header
            int flags = socket.getInputStream().read();

            MessageType type = MessageType.headerFlagsToMessageType(flags);

            switch (type) {
                case CONNECT:
                    request = Optional.of(new ConnectRequest(socket)); break;
                case PINGREQ:
                    request = Optional.of(new PingRequest(socket)); break;
                case DISCONNECT:
                    request = Optional.of(new DisconnectRequest(socket)); break;
                case PUBLISH:
                    request = Optional.of(new PublishRequest(socket, flags)); break;
                case SUBSCRIBE:
                    request = Optional.of(new SubscribeRequest(socket, flags)); break;
                case UNSCUBSCRIBE:
                    request = Optional.of(new UnsubscribeRequest(socket)); break;
                default:
                    request = Optional.empty();  //should not happen (UnknownMessageType is thrown)
            }

        } catch (UnknownMessageTypeError unknownMessageTypeError) {
            throw new MalformedMQTTRequestError("Unknown request type");
        } catch (SocketException socketException) {
            throw new ConnectError(socket);
        }

        if (request.isPresent()) {
            request.get().createFromInputStream();
        }

        return request;
    }
}
