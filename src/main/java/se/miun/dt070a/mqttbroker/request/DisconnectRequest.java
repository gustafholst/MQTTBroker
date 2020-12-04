package se.miun.dt070a.mqttbroker.request;

import se.miun.dt070a.mqttbroker.MessageType;
import se.miun.dt070a.mqttbroker.Request;

import java.io.IOException;
import java.net.Socket;

public class DisconnectRequest extends Request {

    public DisconnectRequest(Socket socket) throws IOException {
        super(socket);
    }

    @Override
    public void createFromInputStream() throws IOException {
        //ignore
        //both bytes of the header are already read and no payload should be present
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.DISCONNECT;
    }
}
