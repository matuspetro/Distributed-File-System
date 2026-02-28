package com.example.dfs;

import dfs.lockservice.LockServiceGrpc;
import dfs.lockservice.LockServiceOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class LockClient {
    private final ManagedChannel channel;
    private final String ownerId;

    public LockClient(String ipAddress, int portNumber, String ownerId){
        this.ownerId = ownerId;
        channel = ManagedChannelBuilder.forAddress(ipAddress, portNumber)
                .usePlaintext() // Disable SSL for now
                .build();
    }

    public void stopChannel(){
        channel.shutdown();
    }

    public boolean acquireLockServer(String lockId, long sequence) {
        LockServiceGrpc.LockServiceBlockingStub stub = LockServiceGrpc.newBlockingStub(channel);

        LockServiceOuterClass.AcquireResponse response = stub.acquire(LockServiceOuterClass.AcquireRequest.newBuilder()
                .setLockId(lockId).setOwnerId(ownerId).setSequence(sequence).build());

        return response.getSuccess();
    }

    public void releaseLockServer(String lockId) {
        LockServiceGrpc.LockServiceBlockingStub stub = LockServiceGrpc.newBlockingStub(channel);

        LockServiceOuterClass.ReleaseResponse response = stub.release(LockServiceOuterClass.ReleaseRequest.newBuilder()
                .setLockId(lockId).setOwnerId(ownerId).build());
    }
}
