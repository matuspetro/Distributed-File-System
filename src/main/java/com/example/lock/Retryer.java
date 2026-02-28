package com.example.lock;

import com.example.LogManager;
import dfs.dfsservice.LockCacheServiceGrpc;
import dfs.dfsservice.LockCacheServiceOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Retryer implements Runnable {
    private String retry;
    private LogManager log;

    public Retryer(String retry, LogManager log) {
        this.retry = retry;
        this.log = log;
    }

    @Override
    public void run() {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(retry.split("#")[1].split(":")[0], Integer.parseInt(retry.split("#")[1].split(":")[1]))
                .usePlaintext() // Disable SSL for now
                .build();

        // RETRY
        log.log("    5. RETRY ");
        log.log("          > filename: " + retry.split("#")[0]);
        log.log("          > ownerId: " + retry.split("#")[1]);
        log.log("          > sequence:" + retry.split("#")[2]);
        log.log(" ");

        LockCacheServiceGrpc.LockCacheServiceBlockingStub stub = LockCacheServiceGrpc.newBlockingStub(channel);
        try {
            stub.retry(LockCacheServiceOuterClass.RetryRequest.newBuilder()
                    .setLockId(retry.split("#")[0]).setSequence(Long.parseLong(retry.split("#")[2])).build());
        } catch (Exception e) {
            log.log("Error during RETRY: " + e.getMessage());
        } finally {
            channel.shutdown();
        }
    }
}