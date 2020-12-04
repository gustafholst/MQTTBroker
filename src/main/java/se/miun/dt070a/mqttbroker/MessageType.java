package se.miun.dt070a.mqttbroker;

import se.miun.dt070a.mqttbroker.error.UnknownMessageTypeError;

import java.util.Arrays;
import java.util.Optional;

public enum MessageType {

    CONNECT(1),
    CONNACK(2),
    PUBLISH(3),
    PUBACK(4),
    PUBREC(5),
    PUBREL(6),
    PUBCOMP(7),
    SUBSCRIBE(8),
    SUBACK(9),
    UNSCUBSCRIBE(10),
    UNSUBACK(11),
    PINGREQ(12),
    PINGRESP(13),
    DISCONNECT(14);

    public int code;

    /* Converts the first byte of MQTT header to a MessageType
            Flags:
            |7|6|5|4|3|2|1|0
            |  type |d|QoS|r
         */
    public static MessageType headerFlagsToMessageType(int flags) throws UnknownMessageTypeError {
        Optional<MessageType> type = Arrays.stream(values()).filter(v -> (flags >> 4) == v.code).findFirst();
        return type.orElseThrow(UnknownMessageTypeError::new);
    }

    MessageType(int code) {
        this.code = code;
    }

}
