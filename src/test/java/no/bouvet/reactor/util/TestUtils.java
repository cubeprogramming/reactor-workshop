/*
 * Copyright (c) 2016-2021 VMware Inc. or its affiliates, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package no.bouvet.reactor.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.fail;

public class TestUtils {
    private static final Logger log = LoggerFactory.getLogger(TestUtils.class.getName());

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public static void acquireSemaphore(Semaphore semaphore) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public static <T> void waitUntil(String errorMessage, Supplier<Object> errorMessageArg, Predicate<T> predicate, T arg, Duration duration) {
        long endTimeMillis = System.currentTimeMillis() + duration.toMillis();
        while (System.currentTimeMillis() < endTimeMillis) {
            if (predicate.test(arg))
                return;
            TestUtils.sleep(10);
        }
        String message = errorMessageArg == null ? errorMessage : errorMessage + errorMessageArg.get();
        fail(message);
    }

    public static void waitForLatch(String errorPrefix, CountDownLatch latch, Duration duration) throws InterruptedException {
        if (!latch.await(duration.toMillis(), TimeUnit.MILLISECONDS))
            fail(errorPrefix + ", remaining=" + latch.getCount());
    }

    public static void printStackTrace(String threadNamePattern) {
        Thread[] threads = new Thread[1000];
        int count = Thread.currentThread().getThreadGroup().enumerate(threads);
        Pattern pattern = Pattern.compile(threadNamePattern);
        for (int i = 0; i < count && i < threads.length; i++) {
            Thread thread = threads[i];
            if (pattern.matcher(thread.getName()).matches()) {
                StackTraceElement[] stackTrace = thread.getStackTrace();
                log.warn("Stack trace of thread {}: {}", thread.getName(), stackTrace);
            }
        }
    }

    public static void execute(Runnable runnable, long maxTimeMs) throws Exception {
        execute(() -> {
            runnable.run();
            return null;
        }, maxTimeMs);
    }

    public static <T> T execute(Callable<T> callable, long maxTimeMs) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            return executor.submit(callable).get(maxTimeMs, TimeUnit.MILLISECONDS);
        } finally {
            executor.shutdown();
        }
    }
}
