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

        //...but
        /* On receipt of DISCONNECT the Server:

            MUST discard any Will Message associated with the current connection without publishing it,
            as described in Section 3.1.2.5 [MQTT-3.14.4-3].
            SHOULD close the Network Connection if the Client has not already done so.

             */
        getSocket().close();
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.DISCONNECT;
    }
}
