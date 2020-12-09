package se.miun.dt070a.mqttbroker.response;

import se.miun.dt070a.mqttbroker.MessageType;
import se.miun.dt070a.mqttbroker.Response;
import se.miun.dt070a.mqttbroker.Subscription;
import se.miun.dt070a.mqttbroker.request.SubscribeRequest;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class SubscribeResponse extends Response {

    byte[] packetContent;

    public SubscribeResponse(SubscribeRequest subscribeRequest) {
        super(subscribeRequest.getSocket());

        /* variable length header */
        int packetIdentifier = subscribeRequest.getPacketIdentifier();
        byte packetIdMSB = (byte) (packetIdentifier & 0b1111111100000000);  //mask out msb
        byte packetIdLSB = (byte) (packetIdentifier & 0b11111111);          //mask out lsb
        byte[] packetId = new byte[] {packetIdMSB, packetIdLSB};            // two bytes representing packetId

        /* payload */
        List<Subscription> subs = subscribeRequest.getSubscriptions().toList().blockingGet();

        byte[] responseCodes = new byte[subs.size()];                  //response codes for each topic (subscription)
        for (int i = 0; i < subs.size(); i++) {
            responseCodes[i] = (byte) subs.get(i).getResponseCode();
        }

        byte[] variableLengthHeaderAndPaylod = concatenate(packetId, responseCodes);


        /* header */
        header = new byte[]{(byte) (getMessageTypeAsByte() << 4), (byte) variableLengthHeaderAndPaylod.length};
                                                                            // 10010000    SUBACK
                                                                            // XXXXXXXX    Remaining length


        packetContent = concatenate(header, variableLengthHeaderAndPaylod);     //header
                                                                                //variable length header
                                                                                //payload
    }

    @Override
    public void send() throws IOException {
        socket.getOutputStream().write(packetContent);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.SUBACK;
    }

    @Override
    public byte getMessageTypeAsByte() {
        return (byte) getMessageType().code;
    }
}
