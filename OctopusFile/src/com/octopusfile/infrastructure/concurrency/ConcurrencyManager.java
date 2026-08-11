package com.octopusfile.infrastructure.concurrency;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gerencia os pools de threads usados por toda a biblioteca:
 *  - um pool de I/O ligado (bound) para operações de leitura/escrita/cópia,
 *    dimensionado para trabalho bloqueante de disco/rede;
 *  - um pool leve para tarefas de monitoramento (FileWatcher) que ficam
 *    a maior parte do tempo bloqueadas em WatchService.take();
 *  - um scheduler para tarefas periódicas (limpeza de cache, flush de logs).
 *
 * Uso: singleton compartilhado via OctopusFile, mas também instanciável
 * isoladamente para testes.
 */
public class ConcurrencyManager implements AutoCloseable {

    private static final int DEFAULT_IO_THREADS = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
    private static final int DEFAULT_WATCH_THREADS = 2;

    private final ExecutorService ioPool;
    private final ExecutorService watchPool;
    private final ScheduledExecutorService scheduler;
    private volatile boolean shutdown = false;

    public ConcurrencyManager() {
        this(DEFAULT_IO_THREADS, DEFAULT_WATCH_THREADS);
    }

    public ConcurrencyManager(int ioThreads, int watchThreads) {
        AtomicInteger ioCounter = new AtomicInteger();
        this.ioPool = Executors.newFixedThreadPool(ioThreads, r -> {
            Thread t = new Thread(r, "octopusfile-io-" + ioCounter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });

        AtomicInteger watchCounter = new AtomicInteger();
        this.watchPool = Executors.newFixedThreadPool(watchThreads, r -> {
            Thread t = new Thread(r, "octopusfile-watch-" + watchCounter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "octopusfile-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public <T> CompletableFuture<T> submitIo(Callable<T> task) {
        checkNotShutdown();
        CompletableFuture<T> future = new CompletableFuture<>();
        ioPool.submit(() -> {
            try {
                future.complete(task.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public CompletableFuture<Void> submitIo(Runnable task) {
        return submitIo(() -> {
            task.run();
            return null;
        });
    }

    /** Executa uma tarefa de longa duração (ex.: loop de FileWatcher) em background. */
    public Future<?> submitWatch(Runnable task) {
        checkNotShutdown();
        return watchPool.submit(task);
    }

    public void scheduleAtFixedRate(Runnable task, long initialDelayMs, long periodMs) {
        checkNotShutdown();
        scheduler.scheduleAtFixedRate(task, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Executa em lote (fan-out) e aguarda todos com timeout, agregando falhas.
     * Usado por OperacoesEmLote para paralelizar cópias/movimentações em massa.
     */
    public <T> List<T> invokeAllIo(List<Callable<T>> tasks, long timeout, TimeUnit unit) throws OctopusFileException {
        checkNotShutdown();
        try {
            List<Future<T>> futures = ioPool.invokeAll(tasks, timeout, unit);
            List<T> results = new java.util.ArrayList<>(futures.size());
            for (Future<T> f : futures) {
                if (f.isCancelled()) {
                    throw new OctopusFileException(ErrorCodes.OPERATION_TIMEOUT,null);
                }
                results.add(f.get());
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OctopusFileException(ErrorCodes.INTERRUPTED, null, e);
        } catch (ExecutionException e) {
            throw new OctopusFileException(ErrorCodes.UNKNOWN_ERROR, e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e.getCause());
        }
    }

    public <T> T await(CompletableFuture<T> future, long timeout, TimeUnit unit) {
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new OctopusFileException(ErrorCodes.OPERATION_TIMEOUT, null, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OctopusFileException(ErrorCodes.INTERRUPTED, null, e);
        } catch (ExecutionException e) {
            throw new OctopusFileException(ErrorCodes.UNKNOWN_ERROR, e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e.getCause());
        }
    }

    private void checkNotShutdown() {
        if (shutdown) {
            throw new OctopusFileException(ErrorCodes.TASK_REJECTED, "ConcurrencyManager já foi encerrado");
        }
    }

    @Override
    public void close() {
        shutdown = true;
        shutdownPool(ioPool);
        shutdownPool(watchPool);
        shutdownPool(scheduler);
    }

    private void shutdownPool(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
