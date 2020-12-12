package se.miun.dt070a.mqttbroker;

import io.reactivex.rxjava3.core.Observable;

import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Session {

    private final String clientId;
    private Socket socket;
    private boolean newSession = true;

    private Set<Subscription> subscriptions = new HashSet<>();

    public Session(Socket socket) {
        this(socket, UUID.randomUUID().toString());
    }

    public Session(Socket socket, String clientId) {
        this.socket = socket;
        this.clientId = clientId;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public String getClientId() {
        return this.clientId;
    }

    public boolean isNewSession() {
        return newSession;
    }

    public void addSubscription(Subscription subscription) {
        subscriptions.add(subscription);
    }

    public void removeTopic(Subscription subscription) {
        subscriptions.remove(subscription);
    }

    public Observable<Subscription> getSubscriptions() {
        return Observable.fromIterable(subscriptions);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Session) {
            return this.clientId.equals(((Session) other).getClientId());
        }
        return false;
    }

    public void closePreviousSocketAndReplace(Socket newSocket) {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket = newSocket;
        newSession = false;
    }
}
