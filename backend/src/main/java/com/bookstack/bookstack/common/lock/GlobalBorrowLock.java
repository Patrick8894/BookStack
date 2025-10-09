package com.bookstack.bookstack.common.lock;

import org.springframework.stereotype.Component;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class GlobalBorrowLock {
    // fair = true helps avoid starvation under high contention
    private final ReentrantLock lock = new ReentrantLock(true);

    public boolean tryLock(long timeoutMs) throws InterruptedException {
        return lock.tryLock(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    public void unlock() {
        if (lock.isHeldByCurrentThread()) lock.unlock();
    }
}
