package se.miun.dt070a.mqttbroker.response;

import se.miun.dt070a.mqttbroker.MessageType;
import se.miun.dt070a.mqttbroker.Response;
import se.miun.dt070a.mqttbroker.Subscription;
import se.miun.dt070a.mqttbroker.request.UnsubscribeRequest;

import java.io.IOException;
import java.util.List;

public class UnscubscribeResponse extends Response {

    byte[] packetContent;

    public UnscubscribeResponse(UnsubscribeRequest unscubscribeRequest) {
        super(unscubscribeRequest.getSocket());

        /* variable length header */
        int packetIdentifier = unscubscribeRequest.getPacketIdentifier();
        byte packetIdMSB = (byte) (packetIdentifier & 0b1111111100000000);  //mask out msb
        byte packetIdLSB = (byte) (packetIdentifier & 0b11111111);          //mask out lsb
        byte[] packetId = new byte[] {packetIdMSB, packetIdLSB};            // two bytes representing packetId

        /* header */
        header = new byte[]{(byte) (getMessageTypeAsByte() << 4), (byte) packetId.length};
        // 10100000    UNSUBACK
        // XXXXXXXX    Remaining length


        packetContent = concatenate(header, packetId);     //header
                                                           //variable length header
                                                           //payload
    }

    @Override
    public void send() throws IOException {
        socket.getOutputStream().write(packetContent);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.UNSUBACK;
    }

    @Override
    public byte getMessageTypeAsByte() {
        return (byte) getMessageType().code;
    }
}
