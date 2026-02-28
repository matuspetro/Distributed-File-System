package com.example.lock;

import com.example.LogManager;
import dfs.dfsservice.LockCacheServiceGrpc;
import dfs.dfsservice.LockCacheServiceOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Revoker implements Runnable {
    private String lock;
    private LogManager log;

    public Revoker(String lock, LogManager log) {
        this.lock = lock;
        this.log = log;
    }

    @Override
    public void run() {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(lock.split("#")[1].split(":")[0], Integer.parseInt(lock.split("#")[1].split(":")[1]))
                .usePlaintext() // Disable SSL for now
                .build();

        // REVOKE
        log.log("    3. REVOKE ");
        log.log("          > filename: " + lock.split("#")[0]);
        log.log("          > ownerId: " + lock.split("#")[1]);
        log.log("          > sequence:" + lock.split("#")[2]);
        LockCacheServiceGrpc.LockCacheServiceBlockingStub stub = LockCacheServiceGrpc.newBlockingStub(channel);
        try {
            stub.revoke(LockCacheServiceOuterClass.RevokeRequest.newBuilder()
                    .setLockId(lock.split("#")[0]).build());
        } catch (Exception e) {
            log.log("Error during REVOKE: " + e.getMessage());
        } finally {
            channel.shutdown();
        }
    }
}