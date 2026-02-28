package com.example.dfs;

import dfs.extentservice.ExtentServiceGrpc;
import dfs.extentservice.ExtentServiceOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ExtentClient {
    private final ManagedChannel channel;

    public ExtentClient(String ipAddress, int portNumber){
        channel = ManagedChannelBuilder.forAddress(ipAddress, portNumber)
                .maxInboundMessageSize(50 * 1024 * 1024)
                .usePlaintext() // Disable SSL for now
                .build();
    }

    public void stopChannel(){
        channel.shutdown();
    }

    public byte[] getExtentServer(String Name) {
        ExtentServiceGrpc.ExtentServiceBlockingStub stub = ExtentServiceGrpc.newBlockingStub(channel);

        ExtentServiceOuterClass.GetResponse response = stub.get(ExtentServiceOuterClass.GetRequest.newBuilder()
                .setFileName(Name)
                .build());

        return response.hasFileData() ? response.getFileData().toByteArray() : null;
    }

    public boolean putExtentServer(String Name, byte[] content) {
        ExtentServiceGrpc.ExtentServiceBlockingStub stub = ExtentServiceGrpc.newBlockingStub(channel);
        ExtentServiceOuterClass.PutResponse response;
        if (content != null) {
            response = stub.put(ExtentServiceOuterClass.PutRequest.newBuilder()
                    .setFileName(Name).setFileData(com.google.protobuf.ByteString.copyFrom(content)).build());
        }else{
            response = stub.put(ExtentServiceOuterClass.PutRequest.newBuilder()
                    .setFileName(Name).build());
        }
        return response.getSuccess();
    }
}
