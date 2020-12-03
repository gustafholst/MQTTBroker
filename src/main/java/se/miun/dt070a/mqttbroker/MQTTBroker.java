package se.miun.dt070a.mqttbroker;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import se.miun.dt070a.mqttbroker.response.ConnectResponse;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MQTTBroker {

    private boolean stopListening = false;

    private ServerSocket serverSocket;

    private Map<Integer, Disposable> disposables;

    private Disposable mainDisposable;

    private Observable<Socket> connections;

    private int socketId = 0;

    //private Map<String, Client> = new HashMap<>();


    private Response handleRequest(Request request) {
        switch (request.getMessageType()) {
            case CONNECT:
                return new ConnectResponse(request.getSocket());

            default:
                return null;

        }
    }

    public void run() {
        System.out.println("Entering run!");

        disposables = new HashMap<>();

        mainDisposable = listenForIncomingConnectionRequests().observeOn(Schedulers.io())
                .doOnNext(s -> System.out.println("tcp connection accepted...now waiting for incoming request"))
                .doOnSubscribe(d -> System.out.println("subscribed to connections"))
                //.doOnSubscribe(this::storeDisposable)
                .map(Request::parseRequest) //TODO handle MalformedMQTTRequestError
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::handleRequest)   //response

                .doOnDispose(() -> System.out.println("Disposed of connection"))
                .subscribe(Response::send
                        , Throwable::printStackTrace
                        , () -> System.out.println("Connections completed"));


        System.out.println("Exiting run!");
    }

    private Observable<Socket> listenForIncomingConnectionRequests() {
        try {
            serverSocket = new ServerSocket(1883);

            return Observable.create(emitter -> {
                while (!emitter.isDisposed()) {
                    Socket socket = serverSocket.accept();
                    emitter.onNext(socket);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            return Observable.error(new IOError(e));
        }

    }

    private void storeDisposable(Disposable d) {
        disposables.put(socketId++, d);
    }

    public void shutdown() throws IOException {
//        for (Disposable d : disposables.values()) {
//            d.dispose();
//        }
       // disposables.clear();
        mainDisposable.dispose();
        serverSocket.close();
    }

    public static void main(String[] args){
        try {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));


        MQTTBroker broker = new MQTTBroker();
        Disposable disposable = Observable.just(broker)
                .subscribeOn(Schedulers.single())
                .doOnNext(MQTTBroker::run)
                //.doOnDispose(broker::shutdown)
                .doOnSubscribe(d -> System.out.println("Broker is running...press <enter> to stop"))
                .subscribe();

            br.readLine();

            disposable.dispose();
            Schedulers.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        Scanner scanner = new Scanner(System.in);
//
//        MQTTBroker broker = new MQTTBroker();
//
//        broker.start();
//
//        System.out.println("Broker is running...\npress <enter> to stop");
//
//        scanner.nextLine();
//
//        try {
//            broker.shutdown();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("Exiting main!");
    }
}
