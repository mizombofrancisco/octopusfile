package com.octopusfile.modules.monitoring;

import com.octopusfile.infrastructure.concurrency.ConcurrencyManager;
import com.octopusfile.infrastructure.filesystem.NIO2FileSystem;

import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.List;

/**
 * API pública de monitoramento de arquivos. Por baixo, todas as instâncias
 * de {@code FileWatcher} criadas a partir do mesmo {@link ConcurrencyManager}
 * compartilham o mesmo {@link WatchService} (motor único, Abordagem B) —
 * isso é intencional e transparente para quem consome a fachada.
 *
 * Uso típico:
 * <pre>
 *   FileWatcher watcher = new FileWatcher();
 *   FileWatcher.Subscription sub = watcher.watch(dir, true, event -> {
 *       System.out.println(event.kind() + ": " + event.path());
 *   });
 *   // ...
 *   sub.cancel(); // para de observar apenas essa raiz
 *   watcher.close(); // encerra o motor compartilhado
 * </pre>
 */
public class FileWatcher implements AutoCloseable {

    /** Alça de cancelamento para uma chamada individual de {@link #watch}. */
    public static final class Subscription {
        private final WatchService engine;
        private final WatchService.Registration registration;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        private Subscription(WatchService engine, WatchService.Registration registration) {
            this.engine = engine;
            this.registration = registration;
        }

        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                engine.unregister(registration);
            }
        }
    }

    private static final ConcurrentHashMap<ConcurrencyManager, WatchService> SHARED_ENGINES = new ConcurrentHashMap<>();

    private WatchService engine;
    private ConcurrencyManager concurrencyManager;
    private boolean ownsConcurrencyManager;

    private Path path;
    private final WatchService watchServiceFactory = new WatchService();
    private final List<EventListener> listeners = new ArrayList<>();
    private volatile boolean rodando = false;



    public FileWatcher() {
        this(new NIO2FileSystem(), new ConcurrencyManager(), true);
    }

    public FileWatcher(NIO2FileSystem fileSystem, ConcurrencyManager concurrencyManager) {
        this(fileSystem, concurrencyManager, false);
    }

    private FileWatcher(NIO2FileSystem fileSystem, ConcurrencyManager concurrencyManager, boolean ownsConcurrencyManager) {
        this.concurrencyManager = concurrencyManager;
        this.ownsConcurrencyManager = ownsConcurrencyManager;
        this.engine = SHARED_ENGINES.computeIfAbsent(concurrencyManager, cm -> new WatchService(fileSystem, cm));
    }

    public FileWatcher(Path path) {
        this.path = path;
    }

    public void addListener(EventListener listener) {
        listeners.add(listener);
    }

    public void watch() throws IOException {
        java.nio.file.WatchService watchService = watchServiceFactory.createService(path);
        path.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
        );

        rodando = true;
        while (rodando) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {

                WatchEvent.Kind<?> nioKind = event.kind();

                if (nioKind == StandardWatchEventKinds.OVERFLOW) {
                    FileEvent fileEvent = FileEvent.now(
                            path,
                            FileEvent.Kind.OVERFLOW
                    );

                    for (EventListener listener : listeners) {
                        listener.onEvent(fileEvent);
                    }

                    continue;
                }

                Path changed = path.resolve((Path) event.context());

                FileEvent.Kind kind;

                if (nioKind == StandardWatchEventKinds.ENTRY_CREATE) {
                    kind = FileEvent.Kind.CREATED;

                } else if (nioKind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    kind = FileEvent.Kind.MODIFIED;

                } else if (nioKind == StandardWatchEventKinds.ENTRY_DELETE) {
                    kind = FileEvent.Kind.DELETED;

                } else {
                    continue;
                }

                FileEvent fileEvent = FileEvent.now(changed, kind);

                for (EventListener listener : listeners) {
                    listener.onEvent(fileEvent);
                }
            }

            if (!key.reset()) {
                break;
            }
        }
    }

    public void stop() {
        rodando = false;
    }

    /** Passa a observar {@code directory}, opcionalmente de forma recursiva, notificando {@code listener}. */
    public Subscription watch(Path directory, boolean recursive, EventListener listener) {
        WatchService.Registration registration = engine.register(directory, recursive, listener);
        return new Subscription(engine, registration);
    }

    public Subscription watch(Path directory, EventListener listener) {
        return watch(directory, false, listener);
    }

    public int watchedDirectoryCount() {
        return engine.watchedDirectoryCount();
    }

    /**
     * Encerra o motor de monitoramento compartilhado associado a este
     * ConcurrencyManager. Se este FileWatcher foi criado com o construtor
     * sem argumentos (dono exclusivo do ConcurrencyManager), também
     * encerra os pools de threads associados.
     */
    @Override
    public void close() {
        engine.close();
        SHARED_ENGINES.remove(concurrencyManager);
        if (ownsConcurrencyManager) {
            concurrencyManager.close();
        }
    }
}
