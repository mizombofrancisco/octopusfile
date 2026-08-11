package com.octopusfile;

import com.octopusfile.modules.monitoring.EventListener;
import com.octopusfile.modules.monitoring.FileEvent;
import com.octopusfile.modules.monitoring.FileWatcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Operação de monitoramento fluente, devolvida por
 * {@link OctopusFile#watch(String)}.
 * <pre>{@code
 * OctopusFile.watch("C:/arquivos")
 *     .filterExtension(".csv")
 *     .onModify(event -> System.out.println(event.getPath()))
 *     .start();
 * }</pre>
 */
public class WatchOperation implements AutoCloseable {

    private final FileWatcher fileWatcher;

    private Consumer<FileEvent> onCreate = e -> {
    };
    private Consumer<FileEvent> onModify = e -> {
    };
    private Consumer<FileEvent> onDelete = e -> {
    };
    private Consumer<FileEvent> onOverflow = e -> {
    };
    private Consumer<FileEvent> onAny = e -> {
    };

    // Novo: Filtro opcional por predicado (ex: extensões de arquivos específicas)
    private Predicate<Path> fileFilter = path -> true;

    private Thread watchThread;
    private volatile boolean running = false; // Controlo de estado atómico/seguro

    WatchOperation(Path path) {
        this.fileWatcher = new FileWatcher(path);
        this.fileWatcher.addListener(dispatchListener());
    }

    private EventListener dispatchListener() {
        return event -> {
            Path path = event.getPath();

            // Aplica o filtro personalizado (se definido, ex: ignorar .tmp ou focar em .pdf)
            if (path != null && !fileFilter.test(path)) {
                return;
            }

            switch (event.getKind()) {
                case CREATED ->
                    onCreate.accept(event);
                case MODIFIED ->
                    onModify.accept(event);
                case DELETED ->
                    onDelete.accept(event);
                case OVERFLOW ->
                    onOverflow.accept(event);
            }

            onAny.accept(event);
        };
    }

    public WatchOperation onCreate(Consumer<FileEvent> callback) {
        this.onCreate = callback;
        return this;
    }

    public WatchOperation onModify(Consumer<FileEvent> callback) {
        this.onModify = callback;
        return this;
    }

    public WatchOperation onDelete(Consumer<FileEvent> callback) {
        this.onDelete = callback;
        return this;
    }

    /**
     * Callback disparado para qualquer tipo de evento (criação, modificação ou
     * remoção).
     *
     * @param callback
     * @return
     */
    public WatchOperation onEvent(Consumer<FileEvent> callback) {
        this.onAny = callback;
        return this;
    }

    // =========================================================================
    // NOVOS MÉTODOS ADICIONADOS PARA EXPANSÃO FUTURA
    // =========================================================================
    /**
     * Permite tratar eventos de *Overflow* do sistema operativo (quando a fila
     * de eventos estoure).
     *
     * @param callback
     * @return
     */
    public WatchOperation onOverflow(Consumer<FileEvent> callback) {
        this.onOverflow = callback;
        return this;
    }

    /**
     * Filtra os eventos de monitoramento para aceitar apenas arquivos com uma
     * extensão específica.Exemplo: .filterExtension(".txt") ou
     * .filterExtension(".pdf")
     *
     * @param extension
     * @return
     */
    public WatchOperation filterExtension(String extension) {
        String ext = extension.startsWith(".") ? extension.toLowerCase() : "." + extension.toLowerCase();
        this.fileFilter = path -> path.toString().toLowerCase().endsWith(ext);
        return this;
    }

    /**
     * Permite definir um filtro personalizado customizado via Predicate.
     *
     * @param customFilter
     * @return
     */
    public WatchOperation filter(Predicate<Path> customFilter) {
        this.fileFilter = customFilter;
        return this;
    }

    /**
     * Verifica se o watcher está atualmente em execução.
     *
     * @return
     */
    public boolean isRunning() {
        return running && watchThread != null && watchThread.isAlive();
    }

    // =========================================================================
    /**
     * Inicia o monitoramento em uma thread separada, sem bloquear quem chamou.
     *
     * @return
     */
    public WatchOperation start() {
        if (isRunning()) {
            return this; // Evita iniciar duplicado
        }

        running = true;
        watchThread = new Thread(() -> {
            try {
                fileWatcher.watch();
            } catch (IOException e) {
                if (running) {
                    throw new RuntimeException("Erro ao iniciar monitoramento", e);
                }
            } finally {
                running = false;
            }
        }, "OctopusFile-Watcher-Thread");

        watchThread.setDaemon(true);
        watchThread.start();
        return this;
    }

    /**
     * Para o monitoramento iniciado por {@link #start()}.
     */
    public void stop() {
        running = false;
        fileWatcher.stop();
        if (watchThread != null) {
            watchThread.interrupt();
        }
    }

    @Override
    public void close() {
        stop();
    }
}
