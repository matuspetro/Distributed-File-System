package com.example.dfs;

import com.example.LogManager;
import com.google.protobuf.ByteString;
import dfs.dfsservice.DfsServiceGrpc;
import dfs.dfsservice.DfsServiceOuterClass;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class DfsServer {
    private static final LogManager log = new LogManager("LogDfsServer.txt");

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
            ExtentClient extentClient = new ExtentClient(args[2].split(":")[0], Integer.parseInt(args[2].split(":")[1]));
            LockClient lockClient = new LockClient(args[3].split(":")[0], Integer.parseInt(args[3].split(":")[1]), ip+":"+portNumber+ ":dfs");
            LockCacheServiceImpl lockCacheService = new LockCacheServiceImpl(lockClient, log);
            DfsServiceImpl dfsService = new DfsServiceImpl(extentClient, lockClient, lockCacheService);

            Server server = ServerBuilder.forPort(portNumber)
                    .maxInboundMessageSize(50 * 1024 * 1024)
                    .addService(dfsService)
                    .addService(lockCacheService)
                    .build();
            dfsService.setServer(server);

            System.out.println("Server started on port " + portNumber);
            server.start();
            server.awaitTermination();

        } else if (command.equals("stop")) {
            ManagedChannel channel = ManagedChannelBuilder.forAddress(ip, portNumber)
                    .usePlaintext() // Disable SSL for now
                    .build();

            DfsServiceGrpc.DfsServiceBlockingStub stub = DfsServiceGrpc.newBlockingStub(channel);

            stub.stop(DfsServiceOuterClass.StopRequest.getDefaultInstance());

            channel.shutdown();
        }
    }

    static class DfsServiceImpl extends DfsServiceGrpc.DfsServiceImplBase {
        public LockClient lockClient;
        public ExtentClient extentClient;
        public LockCacheServiceImpl lockCacheService;
        private Server server;

        public DfsServiceImpl(ExtentClient ExtentSetup, LockClient LockSetup, LockCacheServiceImpl lockCacheService) {
            this.extentClient = ExtentSetup;
            this.lockClient = LockSetup;
            this.lockCacheService = lockCacheService;
        }

        public void setServer(Server server) {
            this.server = server;
        }

        @Override
        public void stop(DfsServiceOuterClass.StopRequest request, StreamObserver<DfsServiceOuterClass.StopResponse> responseObserver) {
            log.log("STOP REQUEST");

            DfsServiceOuterClass.StopResponse stopResponse = DfsServiceOuterClass.StopResponse.newBuilder().build();

            responseObserver.onNext(stopResponse);
            responseObserver.onCompleted();

            extentClient.stopChannel();
            lockClient.stopChannel();

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
        public void dir(DfsServiceOuterClass.DirRequest request, StreamObserver<DfsServiceOuterClass.DirResponse> responseObserver) {
            log.log("DIR REQUEST " + request.getDirectoryName());

            byte[] result = extentClient.getExtentServer(request.getDirectoryName());

            DfsServiceOuterClass.DirResponse dirResponse;

            if (result != null) {
                String resultString = new String(result, StandardCharsets.UTF_8);
                String[] resultArray = resultString.split("\n");

                DfsServiceOuterClass.DirResponse.Builder dirResponseBuilder = DfsServiceOuterClass.DirResponse.newBuilder()
                        .setSuccess(true);
                for (String dir : resultArray) {
                    dirResponseBuilder.addDirList(dir);
                }
                dirResponse = dirResponseBuilder.build();

            } else {
                dirResponse = DfsServiceOuterClass.DirResponse.newBuilder()
                        .setSuccess(false).build();
            }
            responseObserver.onNext(dirResponse);
            responseObserver.onCompleted();
        }

        @Override
        public void mkdir(DfsServiceOuterClass.MkdirRequest request, StreamObserver<DfsServiceOuterClass.MkdirResponse> responseObserver) {
            log.log("MKDIR REQUEST " + request.getDirectoryName());

            boolean result = extentClient.putExtentServer(request.getDirectoryName(),new byte[1]);

            DfsServiceOuterClass.MkdirResponse mkdirResponse = DfsServiceOuterClass.MkdirResponse.newBuilder()
                    .setSuccess(result).build();
            responseObserver.onNext(mkdirResponse);
            responseObserver.onCompleted();
        }

        @Override
        public void rmdir(DfsServiceOuterClass.RmdirRequest request, StreamObserver<DfsServiceOuterClass.RmdirResponse> responseObserver) {
            log.log("RMDIR REQUEST " + request.getDirectoryName());

            boolean result = extentClient.putExtentServer(request.getDirectoryName(), null);

            DfsServiceOuterClass.RmdirResponse rmdirResponse = DfsServiceOuterClass.RmdirResponse.newBuilder()
                    .setSuccess(result).build();
            responseObserver.onNext(rmdirResponse);
            responseObserver.onCompleted();

        }

        @Override
        public void get(DfsServiceOuterClass.GetRequest request, StreamObserver<DfsServiceOuterClass.GetResponse> responseObserver) {
            log.log("GET REQUEST " + request.getFileName());

            byte[] result = extentClient.getExtentServer(request.getFileName());

            DfsServiceOuterClass.GetResponse getResponse;
            if (result == null){
                getResponse = DfsServiceOuterClass.GetResponse.newBuilder()
                        .build();
            }else{
                getResponse = DfsServiceOuterClass.GetResponse.newBuilder()
                        .setFileData(ByteString.copyFrom(result)).build();
            }
            responseObserver.onNext(getResponse);
            responseObserver.onCompleted();

        }

        @Override
        public void put(DfsServiceOuterClass.PutRequest request, StreamObserver<DfsServiceOuterClass.PutResponse> responseObserver) {
            log.log("PUT REQUEST " + request.getFileName());

            lockCacheService.setLock(request.getFileName());

            log.log("PUT REQUEST LOCK ACQUIRED" + request.getFileName());

            boolean result = extentClient.putExtentServer(request.getFileName(), request.getFileData().toByteArray());
            log.log("PUT REQUEST LOCK ACQUIRED" + result);
            lockCacheService.removeLock(request.getFileName());

            DfsServiceOuterClass.PutResponse putResponse = DfsServiceOuterClass.PutResponse.newBuilder()
                .setSuccess(result).build();
            responseObserver.onNext(putResponse);
            responseObserver.onCompleted();
        }

        @Override
        public void delete(DfsServiceOuterClass.DeleteRequest request, StreamObserver<DfsServiceOuterClass.DeleteResponse> responseObserver) {
            log.log("DELETE REQUEST " + request.getFileName());

            lockCacheService.setLock(request.getFileName());

            log.log("DELETE REQUEST LOCK ACQUIRED" + request.getFileName());

            boolean result = extentClient.putExtentServer(request.getFileName(), null);
            lockCacheService.removeLock(request.getFileName());

            DfsServiceOuterClass.DeleteResponse deleteResponse = DfsServiceOuterClass.DeleteResponse.newBuilder()
                    .setSuccess(result).build();
            responseObserver.onNext(deleteResponse);
            responseObserver.onCompleted();
        }
    }
}