/* Copyright 2026 Andesk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.farmerbb.taskbar.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Runs a single app-list lookup off the main thread and delivers the result on the UI thread.
 * Call {@link #cancel()} from the owning activity's finish/onDestroy paths.
 */
public final class CancelableAppListLoader<T> {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private Future<?> task;

    public void start(Callable<T> supplier, Consumer<T> onComplete) {
        if(cancelled.get()) return;

        task = executor.submit(() -> {
            final T result;
            try {
                result = supplier.call();
            } catch (Exception e) {
                if(!cancelled.get()) Log.w("Andesk", "Unable to load app list", e);
                return;
            }

            if(!cancelled.get() && !Thread.currentThread().isInterrupted()) {
                mainHandler.post(() -> {
                    if(!cancelled.get()) onComplete.accept(result);
                });
            }
        });
    }

    public void cancel() {
        if(!cancelled.compareAndSet(false, true)) return;

        if(task != null) task.cancel(true);
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
    }
}
