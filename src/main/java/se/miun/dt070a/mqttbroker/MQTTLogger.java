package se.miun.dt070a.mqttbroker;

public class MQTTLogger {

    public static void logRequest(Request request) {
        log("Received " + request.getMessageType().name());
    }

    public static void logResponse(Response response) {
        log("Sending " + response.getMessageType().name());
    }

    public static void log(String message) {
        System.out.println(message + " on thread " + Thread.currentThread().getName());
    }
}
