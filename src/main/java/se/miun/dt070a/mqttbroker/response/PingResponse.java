package se.miun.dt070a.mqttbroker.response;

import se.miun.dt070a.mqttbroker.MessageType;
import se.miun.dt070a.mqttbroker.Response;

import java.net.Socket;

public class PingResponse extends Response {
    public PingResponse(Socket socket) {
        super(socket);
        header = new byte[]{(byte) (getMessageTypeAsByte() << 4), 0 };
                                                 // 11010000    PINGRESP
                                                 // 00000000    Remaining length = 0

    }

    @Override
    public MessageType getMessageType() {
        return MessageType.PINGRESP;
    }

    @Override
    public byte getMessageTypeAsByte() {
        return (byte)getMessageType().code;
    }

}
