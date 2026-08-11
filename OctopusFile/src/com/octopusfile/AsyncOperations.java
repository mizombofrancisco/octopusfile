package com.octopusfile;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Executa operações da biblioteca de forma assíncrona.
 */
public class AsyncOperations {

    public void executeAsync(Runnable task) {
        CompletableFuture.runAsync(task);
    }

    public void  executeAsync(Runnable task, Executor executor) {
        CompletableFuture.runAsync(task, executor);
    }

    public <T> CompletableFuture<T>  executeAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task);
    }
}
