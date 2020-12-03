package se.miun.dt070a.mqttbroker;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import se.miun.dt070a.mqttbroker.error.ConnectError;
import se.miun.dt070a.mqttbroker.response.ConnectResponse;
import se.miun.dt070a.mqttbroker.response.PingResponse;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MQTTBroker {

    private boolean acceptConnections = true;

    private ServerSocket serverSocket;

    private Map<Integer, Disposable> disposables;

    private Disposable mainDisposable;

    private Subject<Socket> connections;

    private int socketId = 0;

    //private Map<String, Client> = new HashMap<>();


    private Response handleRequest(Request request) {
        switch (request.getMessageType()) {
            case CONNECT:
                return new ConnectResponse(request.getSocket());
            case PINGREQ:
                return new PingResponse(request.getSocket());
            default:
                return null;

        }
    }

    public void run() throws IOException {
        disposables = new HashMap<>();
        connections = PublishSubject.create();

        Completable.create(emitter -> listenForIncomingConnectionRequests())
                .subscribeOn(Schedulers.single())
                .subscribe();

        mainDisposable = connections
                //.flatMap(s -> Observable.just(s).observeOn(Schedulers.io()))
                .doOnNext(s -> MQTTLogger.log("tcp connection accepted...now waiting for incoming request"))
                .subscribe(this::listenToSocket);
    }

    private void listenForIncomingConnectionRequests() throws IOException {
        serverSocket = new ServerSocket(1883);
            while (acceptConnections) {
                Socket socket = serverSocket.accept();
                Observable.<Socket>create(emitter -> {
                    emitter.onNext(socket);
                }).observeOn(Schedulers.io()).subscribe(connections);
            }
    }

    private void handleError(Throwable error) {
        if (error instanceof ConnectError) {
            // send out last LWT in case needed

//            Socket socket = ((ConnectError) error).getSocket();
//            Disposable d = disposables.get(socket.hashCode());
            //d.dispose();
        }
    }

    private void listenToSocket(Socket socket) {
        Observable.<Optional<Request>>create(emitter -> {
            while (!emitter.isDisposed()) {
                if (socket.isClosed()) {
                    emitter.onError(new ConnectError(socket));
                }
                Optional<Request> request = Request.parseRequest(socket);
                emitter.onNext(request);
            }
        })//TODO handle MalformedMQTTRequestError
                //.subscribeOn(Schedulers.trampoline())
                .doOnSubscribe(d -> storeDisposable(d, socket))
                .doOnError(this::handleError)
                .onErrorComplete(err -> err instanceof ConnectError)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .doOnNext(MQTTLogger::logRequest)
                .map(this::handleRequest)   //response
                .doOnNext(MQTTLogger::logResponse)
                //.doOnDispose(() -> System.out.println("Disposed of connection"))
                .subscribe(Response::send
                        , Throwable::printStackTrace
                        , () -> MQTTLogger.log("Client closed the socket"));
    }

    private void storeDisposable(Disposable d, Socket socket) {
        disposables.put(socket.hashCode(), d);
    }

    public void shutdown() throws IOException {
        acceptConnections = false;
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
