package se.miun.dt070a.mqttbroker.response;

import se.miun.dt070a.mqttbroker.MQTTLogger;
import se.miun.dt070a.mqttbroker.MessageType;
import se.miun.dt070a.mqttbroker.Response;

import java.io.IOException;
import java.net.Socket;

public class DisconnectResponse extends Response {
    public DisconnectResponse(Socket socket) {
        super(socket);
    }

    @Override
    public void send() throws IOException {
        // send nothing...but
        try {
            /* On receipt of DISCONNECT the Server:

            MUST discard any Will Message associated with the current connection without publishing it,
            as described in Section 3.1.2.5 [MQTT-3.14.4-3].
            SHOULD close the Network Connection if the Client has not already done so.

             */
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            MQTTLogger.log("An error occurred while closing socket");
        }
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.DISCONNECT;
    }

    @Override
    public byte getMessageTypeAsByte() {
        return 0;  //never used
    }
}
