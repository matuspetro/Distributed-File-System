package com.example.extent;

import com.example.LogManager;
import com.google.protobuf.ByteString;
import dfs.extentservice.ExtentServiceGrpc;
import dfs.extentservice.ExtentServiceOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ExtentServer {
    private static final LogManager log = new LogManager("LogExtentServer.txt");
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

        log.log(command);
        log.log(ipAddress);

        if (command.equals("start")) {
            ExtentServiceImpl extentService = new ExtentServiceImpl(args[2]);

            Server server = ServerBuilder.forPort(portNumber)
                    .maxInboundMessageSize(50 * 1024 * 1024)
                    .addService(extentService)
                    .build();

            extentService.setServer(server);

            System.out.println("Server started on port " + portNumber);
            server.start();
            server.awaitTermination();

        } else if (command.equals("stop")) {
            ManagedChannel channel = ManagedChannelBuilder.forAddress(ip, portNumber)
                    .usePlaintext() // Disable SSL for now
                    .disableRetry()
                    .build();

            ExtentServiceGrpc.ExtentServiceBlockingStub stub = ExtentServiceGrpc.newBlockingStub(channel);

            stub.stop(ExtentServiceOuterClass.StopRequest.getDefaultInstance());

            channel.shutdown();
        }
    }

    public static class ExtentServiceImpl extends ExtentServiceGrpc.ExtentServiceImplBase {
        private final FileManager fileManager;
        private Server server;

        ExtentServiceImpl(String path) {
            fileManager = new FileManager(path, log);
        }
        public void setServer(Server server) {
            this.server = server;
        }

        @Override
        public void stop(ExtentServiceOuterClass.StopRequest request, StreamObserver<ExtentServiceOuterClass.StopResponse> responseObserver) {
            log.log("STOP REQUEST");

            ExtentServiceOuterClass.StopResponse stopResponse = ExtentServiceOuterClass.StopResponse.newBuilder().build();
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
        public void get(ExtentServiceOuterClass.GetRequest request, StreamObserver<ExtentServiceOuterClass.GetResponse> responseObserver) {
            log.log("GET REQUEST > " + request.getFileName());

            try {
                byte[] fileData = fileManager.get(request.getFileName());
                ExtentServiceOuterClass.GetResponse getResponse;

                if (fileData != null) {
                    getResponse = ExtentServiceOuterClass.GetResponse.newBuilder()
                            .setFileData(ByteString.copyFrom(fileData))
                            .build();
                } else {
                    getResponse = ExtentServiceOuterClass.GetResponse.newBuilder().build();
                }

                responseObserver.onNext(getResponse);
            } catch (IOException e) {
                log.log("Error during GET: " + e.getMessage());
            } finally {
                responseObserver.onCompleted();
            }
        }


        @Override
        public void put(ExtentServiceOuterClass.PutRequest request, StreamObserver<ExtentServiceOuterClass.PutResponse> responseObserver) {
            log.log("PUT REQUEST > " + request.getFileName() + "  DATA? > " + request.hasFileData());

            byte[] fileData = request.hasFileData() ? request.getFileData().toByteArray() : null;

            try {
                ExtentServiceOuterClass.PutResponse putResponse = ExtentServiceOuterClass.PutResponse.newBuilder()
                        .setSuccess(fileManager.put(request.getFileName(), fileData))
                        .build();
                responseObserver.onNext(putResponse);
            } catch (IOException e) {
                log.log("Error during PUT REQUEST: " + e.getMessage());
            }
            responseObserver.onCompleted();
        }
    }
}
