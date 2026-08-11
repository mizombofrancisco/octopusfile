package com.octopusfile.modules.monitoring;

import com.octopusfile.infrastructure.concurrency.ConcurrencyManager;
import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.ExceptionHandler;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.infrastructure.filesystem.NIO2FileSystem;
import com.octopusfile.support.logging.LoggerProvider;


import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Motor central de monitoramento: mantém UM único
 * {@code java.nio.file.WatchService} do SO (referenciado com nome
 * totalmente qualificado, pois colide com esta classe) e multiplexa
 * eventos de todos os diretórios registrados através dele, usando um
 * único thread de dispatch (do pool "watch" de {@link ConcurrencyManager}).
 *
 * Isso é a Abordagem B discutida: 1 thread para N diretórios, em vez de
 * 1 thread por diretório. O custo é a complexidade de rotear cada
 * {@link WatchKey} de volta para a(s) {@link Registration} corretas —
 * é isso que este arquivo resolve.
 *
 * Monitoramento recursivo: {@code WatchService} do JDK não é recursivo
 * nativamente. Quando uma {@link Registration} pede {@code recursive=true},
 * registramos cada subdiretório individualmente no momento do registro
 * inicial, e voltamos a registrar novos subdiretórios assim que um evento
 * {@code ENTRY_CREATE} de diretório é observado.
 */
public class WatchService implements AutoCloseable {


    /** Uma solicitação de monitoramento feita pelo usuário: uma raiz, recursiva ou não, com seus listeners. */
    static final class Registration {
        final String id = UUID.randomUUID().toString();
        final Path root;
        final boolean recursive;
        final CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList<>();
        volatile boolean cancelled = false;

        Registration(Path root, boolean recursive) {
            this.root = root;
            this.recursive = recursive;
        }
    }

    private java.nio.file.WatchService delegate;
    private NIO2FileSystem fileSystem;
    private ConcurrencyManager concurrencyManager;

    // WatchKey -> diretório físico que ele observa
    private final Map<WatchKey, Path> keyToDirectory = new ConcurrentHashMap<>();
    // diretório físico -> WatchKey ativa (para evitar registro duplicado)
    private final Map<Path, WatchKey> directoryToKey = new ConcurrentHashMap<>();
    // diretório físico -> todas as Registrations que devem receber eventos dele
    private final Map<Path, List<Registration>> directoryToRegistrations = new ConcurrentHashMap<>();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Future<?> dispatchLoop;

    public WatchService(){

    }
    public WatchService(NIO2FileSystem fileSystem, ConcurrencyManager concurrencyManager) {
        this.fileSystem = fileSystem;
        this.concurrencyManager = concurrencyManager;
        // qualquer Path serve para obter o FileSystem correto (normalmente o default)
        this.delegate = fileSystem.newWatchService(Path.of("."));
    }

    /** Registra uma nova raiz de monitoramento e inicia o loop de dispatch se ainda não estiver rodando. */
    Registration register(Path root, boolean recursive, EventListener listener) {
        if (!fileSystem.isDirectory(root)) {
            throw new OctopusFileException(ErrorCodes.DIRECTORY_NOT_FOUND, null, root);
        }

        Registration registration = new Registration(root.toAbsolutePath().normalize(), recursive);
        registration.listeners.add(listener);

        registerDirectory(registration.root, registration);
        if (recursive) {
            for (Path sub : listSubdirectoriesRecursively(registration.root)) {
                registerDirectory(sub, registration);
            }
        }

        ensureDispatchLoopRunning();
        return registration;
    }
    public java.nio.file.WatchService createService(Path path) throws IOException, IOException {
        return path.getFileSystem().newWatchService();
    }

    void unregister(Registration registration) {
        registration.cancelled = true;
        directoryToRegistrations.values().forEach(list -> list.remove(registration));
        // Não cancela a WatchKey do diretório se outra Registration ainda depende dela.
        directoryToRegistrations.entrySet().removeIf(e -> {
            if (e.getValue().isEmpty()) {
                WatchKey key = directoryToKey.remove(e.getKey());
                if (key != null) {
                    key.cancel();
                    keyToDirectory.remove(key);
                }
                return true;
            }
            return false;
        });
    }

    private void registerDirectory(Path dir, Registration registration) {
        directoryToRegistrations.computeIfAbsent(dir, d -> new CopyOnWriteArrayList<>()).add(registration);

        if (directoryToKey.containsKey(dir)) {
            return; // já observado fisicamente (outra registration cobre o mesmo diretório)
        }

        WatchKey key = ExceptionHandler.run(dir, () -> dir.register(
                delegate,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
        ));
        directoryToKey.put(dir, key);
        keyToDirectory.put(key, dir);
    }

    private List<Path> listSubdirectoriesRecursively(Path root) {
        List<Path> subdirs = new ArrayList<>();
        for (Path p : fileSystem.walk(root)) {
            if (!p.equals(root) && fileSystem.isDirectory(p)) {
                subdirs.add(p);
            }
        }
        return subdirs;
    }

    private void ensureDispatchLoopRunning() {
        if (running.compareAndSet(false, true)) {
            dispatchLoop = concurrencyManager.submitWatch(this::dispatchLoop);
        }
    }

    private void dispatchLoop() {

        while (running.get()) {
            WatchKey key;
            try {
                key = delegate.take(); // bloqueia até haver eventos
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (java.nio.file.ClosedWatchServiceException e) {
                break;
            }

            Path dir = keyToDirectory.get(key);
            if (dir == null) {
                key.cancel();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                handleEvent(dir, event);
            }

            boolean stillValid = key.reset();
            if (!stillValid) {
                keyToDirectory.remove(key);
                directoryToKey.remove(dir);
                directoryToRegistrations.remove(dir);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleEvent(Path dir, WatchEvent<?> rawEvent) {
        WatchEvent.Kind<?> kind = rawEvent.kind();

        if (kind == StandardWatchEventKinds.OVERFLOW) {
            dispatchToAll(dir, FileEvent.now(dir, FileEvent.Kind.OVERFLOW));
            return;
        }

        WatchEvent<Path> event = (WatchEvent<Path>) rawEvent;
        Path childName = event.context();
        Path fullPath = dir.resolve(childName);

        FileEvent.Kind translatedKind;
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            translatedKind = FileEvent.Kind.CREATED;
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            translatedKind = FileEvent.Kind.DELETED;
        } else {
            translatedKind = FileEvent.Kind.MODIFIED;
        }

        dispatchToAll(dir, new FileEvent(fullPath, translatedKind, java.time.Instant.now()));

        // Registro recursivo dinâmico: um novo subdiretório apareceu sob uma raiz recursiva.
        if (translatedKind == FileEvent.Kind.CREATED && fileSystem.isDirectory(fullPath)) {
            for (Registration registration : directoryToRegistrations.getOrDefault(dir, List.of())) {
                if (registration.recursive && !registration.cancelled) {
                    registerDirectory(fullPath, registration);
                    for (Path sub : listSubdirectoriesRecursively(fullPath)) {
                        registerDirectory(sub, registration);
                    }
                }
            }
        }
    }

    private void dispatchToAll(Path dir, FileEvent event) {
        for (Registration registration : directoryToRegistrations.getOrDefault(dir, List.of())) {
            if (registration.cancelled) {
                continue;
            }
            for (EventListener listener : registration.listeners) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                }
            }
        }
    }

    public int watchedDirectoryCount() {
        return directoryToKey.size();
    }

    @Override
    public void close() {
        running.set(false);
        if (dispatchLoop != null) {
            dispatchLoop.cancel(true);
        }
        try {
            delegate.close();
        } catch (java.io.IOException e) {
            throw ExceptionHandler.translate(e);
        }
    }
}
