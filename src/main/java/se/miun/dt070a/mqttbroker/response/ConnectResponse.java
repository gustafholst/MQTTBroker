package se.miun.dt070a.mqttbroker.response;

import se.miun.dt070a.mqttbroker.MessageType;
import se.miun.dt070a.mqttbroker.Response;
import se.miun.dt070a.mqttbroker.Session;

public class ConnectResponse extends Response {

    public ConnectResponse(Session session) {
        super(session.getSocket());
        byte sessionFlag = (byte) (session.isNewSession() ? 0b0 : 0b1);

        header = new byte[]{0b100000, 0b10, sessionFlag, 0b0};  // 00100000    CONNACK
                                                                // 00000010    Remaining length = 2
                                                                // 0000000X    session bit
                                                                // 00000000    return code success
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.CONNACK;
    }

    @Override
    public byte getMessageTypeAsByte() {
        return (byte) getMessageType().code;
    }
}
