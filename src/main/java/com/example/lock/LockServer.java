package com.example.lock;

import com.example.LogManager;
import dfs.lockservice.LockServiceGrpc;
import dfs.lockservice.LockServiceOuterClass;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LockServer {
    private static LogManager log = new LogManager("LogLockServer.txt");
    private static final List<String> lockList = new ArrayList<>();
    private static final List<String> retryList = new ArrayList<>();

    public static void main(String[] args) throws IOException, InterruptedException {
        String command = args[0];
        String ipAddress = args[1];
        int portNumber;
        String ip = "localhost";

        log.log(command);
        log.log(ipAddress);

        if (ipAddress.contains(":")) {
            ip = ipAddress.split(":")[0];
            portNumber = Integer.parseInt(ipAddress.split(":")[1]);
        } else {
            portNumber = Integer.parseInt(ipAddress);
        }

        log.log(ip);
        log.log(String.valueOf(portNumber));

        if (command.equals("start")) {
            LockServiceImpl lockService = new LockServiceImpl();

            Server server = ServerBuilder.forPort(portNumber)
                    .addService(lockService)
                    .build();
            lockService.setServer(server);

            System.out.println("Server started on port "+ portNumber);
            server.start();
            server.awaitTermination();

        } else if (command.equals("stop")) {
            ManagedChannel channel = ManagedChannelBuilder.forAddress(ip, portNumber)
                    .usePlaintext() // Disable SSL for now
                    .build();

            LockServiceGrpc.LockServiceBlockingStub stub = LockServiceGrpc.newBlockingStub(channel);

            stub.stop(LockServiceOuterClass.StopRequest.getDefaultInstance());

            channel.shutdown();
        }
    }

    public static class LockServiceImpl extends LockServiceGrpc.LockServiceImplBase {

        private Server server;

        public void setServer(Server server) {
            this.server = server;
        }

        @Override
        public void stop(LockServiceOuterClass.StopRequest request, StreamObserver<LockServiceOuterClass.StopResponse> responseObserver) {
            log.log("STOP REQUEST");

            LockServiceOuterClass.StopResponse stopResponse = LockServiceOuterClass.StopResponse.newBuilder().build();
            responseObserver.onNext(stopResponse);
            responseObserver.onCompleted();

            try {
                server.shutdown();
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void acquire(LockServiceOuterClass.AcquireRequest request, StreamObserver<LockServiceOuterClass.AcquireResponse> responseObserver) {
            log.log("");
            log.log("ACQUIRE REQUEST ");
            log.log("          > filename: " + request.getLockId());
            log.log("          > ownerId: " + request.getOwnerId());
            log.log("          > sequence:" + request.getSequence());

            LockServiceOuterClass.AcquireResponse acquireResponse;

            for (String lock : lockList) {
                if (lock.split("#")[0].equals(request.getLockId())) {
                    log.log("    2. acquired FALSE");
                    acquireResponse = LockServiceOuterClass.AcquireResponse.newBuilder()
                            .setSuccess(false).build();
                    responseObserver.onNext(acquireResponse);
                    responseObserver.onCompleted();

                    retryList.add(request.getLockId() + "#" + request.getOwnerId() + "#" + request.getSequence());
                    Thread releaser = new Thread(new Revoker(lock, log));
                    releaser.start();
                    return;
                }
            }
            log.log("    1. acquired TRUE");
            log.log(" ");
            lockList.add(request.getLockId() + "#" + request.getOwnerId() + "#" + request.getSequence());
            acquireResponse = LockServiceOuterClass.AcquireResponse.newBuilder()
                        .setSuccess(true).build();
            responseObserver.onNext(acquireResponse);
            responseObserver.onCompleted();
        }

        @Override
        public void release(LockServiceOuterClass.ReleaseRequest request, StreamObserver<LockServiceOuterClass.ReleaseResponse> responseObserver) {
            log.log("    4. RELEASE REQUEST");
            log.log("          > filename: " + request.getLockId());
            log.log("          > ownerId: " + request.getOwnerId());

            Iterator<String> iterator = lockList.iterator();
            while (iterator.hasNext()) {
                String lock = iterator.next();
                if (lock.split("#")[0].equals(request.getLockId()) && lock.split("#")[1].equals(request.getOwnerId())) {
                    iterator.remove();
                    log.log("          > file was released!!!");
                }
            }

            LockServiceOuterClass.ReleaseResponse releaseResponse = LockServiceOuterClass.ReleaseResponse.newBuilder()
                    .build();
            responseObserver.onNext(releaseResponse);
            responseObserver.onCompleted();

            for (String retry : retryList) {
                if (retry.split("#")[0].equals(request.getLockId())) {
                    Thread retryer = new Thread(new Retryer(retry, log));
                    retryer.start();
                }
                retryList.remove(retry);
            }
        }
    }
}
