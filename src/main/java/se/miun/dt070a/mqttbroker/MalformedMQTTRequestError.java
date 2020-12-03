package se.miun.dt070a.mqttbroker;

public class MalformedMQTTRequestError extends Throwable {

    public MalformedMQTTRequestError() {}

    public MalformedMQTTRequestError(String message) {
        super(message);
    }
}
