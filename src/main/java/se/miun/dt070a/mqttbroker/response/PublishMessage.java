package se.miun.dt070a.mqttbroker.response;

import se.miun.dt070a.mqttbroker.MessageType;
import se.miun.dt070a.mqttbroker.Response;
import se.miun.dt070a.mqttbroker.request.PublishRequest;

import java.io.IOException;
import java.net.Socket;

public class PublishMessage extends Response {

    private final byte[] packetContent;

    public PublishMessage(PublishRequest request) {
        super(request.getSocket());
        String payloadString = request.getPayload();

        byte[] topic = request.getTopic().getBytes();
        byte topicLengthMSB = (byte) (topic.length & 0b1111111100000000);  //mask out msb
        byte topicLengthLSB = (byte) (topic.length & 0b11111111);          //mask out lsb

        int topicLengthNumberOfBytes = 2;
        int remainingLength = topicLengthNumberOfBytes + topic.length + payloadString.length();

        header = new byte[]{(byte) (getMessageTypeAsByte() << 4) , (byte) remainingLength};
                                                                        // 0011XXXX    PUBLISH
                                                                        // 00000000    Remaining length = 0

                                                                        // XXXXXXXX    variable length header (topic)
                                                                        // XXXXXXXX    payload   (message)
        byte[] variableLengthHeader = concatenate(new byte[]{topicLengthMSB, topicLengthLSB}, topic);
        byte[] payLoadBytes = payloadString.getBytes();

        packetContent = concatenate(concatenate(header, variableLengthHeader), payLoadBytes);
    }

    @Override
    public void send() throws IOException {
        if (!socket.isClosed())
            socket.getOutputStream().write(packetContent);
    }

    public void sendToSocket(Socket receivingSocket) throws IOException {
        receivingSocket.getOutputStream().write(packetContent);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.PUBLISH;
    }

    @Override
    public byte getMessageTypeAsByte() {
        return (byte)getMessageType().code;
    }
}
