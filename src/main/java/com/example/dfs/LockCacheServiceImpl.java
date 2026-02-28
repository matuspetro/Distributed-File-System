package com.example.dfs;

import com.example.LogManager;
import dfs.dfsservice.LockCacheServiceGrpc;
import dfs.dfsservice.LockCacheServiceOuterClass;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

public class LockCacheServiceImpl extends LockCacheServiceGrpc.LockCacheServiceImplBase {
    private static final List<String> cacheList = new ArrayList<>();
    private final LockClient lockClient;
    private static final Object lock = new Object();
    private static boolean retry;
    private static long sequence = 0;
    public LogManager log;

    public LockCacheServiceImpl(LockClient lockClient, LogManager log){
        this.lockClient = lockClient;
        this.log = log;
    }
    @Override
    public void revoke(LockCacheServiceOuterClass.RevokeRequest request, StreamObserver<LockCacheServiceOuterClass.RevokeResponse> responseObserver) {
        log.log("  - REVOKE REQUEST " + request.getLockId());

        synchronized (lock) {
            try {
                while (!cacheList.contains(request.getLockId() + "#0")) {
                    log.log("    - Waiting for revoke signal...");
                    log.log("     - Current cacheList: " + String.join(", ", cacheList));
                    lock.wait();
                }
            } catch (InterruptedException e) {
                log.log("    - Interrupted while waiting for revoke: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        log.log("    - Remove from cacheList");
        lockClient.releaseLockServer(request.getLockId());
        cacheList.remove(request.getLockId() + "#0");
        LockCacheServiceOuterClass.RevokeResponse response = LockCacheServiceOuterClass.RevokeResponse.newBuilder()
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void retry(LockCacheServiceOuterClass.RetryRequest request, StreamObserver<LockCacheServiceOuterClass.RetryResponse> responseObserver) {
        log.log("  - RETRY REQUEST   " + request.getLockId() + "  " + request.getSequence());
        log.log("    - ocakavane " + sequence);
        if (request.getSequence() == sequence){
            synchronized (lock) {
                retry = true;
                lock.notifyAll();
            }
        }
        LockCacheServiceOuterClass.RetryResponse response = LockCacheServiceOuterClass.RetryResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void setLock(String fileName) {
        log.log("  - SetLock " + fileName);
        if (cacheList.contains(fileName + "#0") || cacheList.contains(fileName + "#1")) {
            log.log("    - cachelist contains");
            while (cacheList.contains(fileName + "#1")) {}
            cacheList.remove(fileName + "#0");
            cacheList.add(fileName + "#1");
            log.log("    - cachelist lock");
        } else {
            log.log("    - cachelist not contains");
            do {
                if (lockClient.acquireLockServer(fileName, sequence += 1)) {
                    log.log("    - lockserver acquired");
                    cacheList.add(fileName + "#1");
                } else {
                    log.log("    - lockserver not acquired");
                    synchronized (lock) {
                        retry = false;
                        try {
                            while (!retry) {
                                log.log("    - Waiting for retry signal...");
                                lock.wait();
                            }
                        } catch (InterruptedException e) {
                            log.log("    - Interrupted while waiting for retry: " + e.getMessage());
                            Thread.currentThread().interrupt();
                        }
                    }
                    log.log("  - RETRY, try again");
                }
            } while (!cacheList.contains(fileName + "#1"));
        }
    }
    public void removeLock(String fileName) {
        log.log("  - RemoveLock " + fileName);
        log.log("     - Pred cacheList: " + String.join(", ", cacheList));
        synchronized (lock) {
            cacheList.remove(fileName+"#1");
            cacheList.add(fileName+"#0");
            lock.notifyAll();
        }
        log.log("     - Po cacheList: " + String.join(", ", cacheList));
    }
}