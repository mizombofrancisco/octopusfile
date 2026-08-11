package com.octopusfile.modules.backup;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Agenda backups automáticos e periódicos de um arquivo/diretório, usando
 * uma única thread daemon dedicada (não impede a JVM de encerrar).
 * <pre>{@code
 * BackupScheduler scheduler = new BackupScheduler();
 * scheduler.schedule(
 *         Paths.get("dados/"), Paths.get("backups/"),
 *         1, TimeUnit.HOURS,
 *         metadata -> System.out.println("Backup criado: " + metadata));
 * // ...
 * scheduler.stop();
 * }</pre>
 */
public class BackupScheduler implements AutoCloseable {

    private final BackupService backupService;
    private final ScheduledExecutorService executor;

    public BackupScheduler() {
        this(new BackupService());
    }

    public BackupScheduler(BackupService backupService) {
        this.backupService = backupService;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "OctopusFile-BackupScheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Agenda backups periódicos de {@code source} para {@code backupDirectory}.
     *
     * @param source          arquivo ou diretório a ser copiado
     * @param backupDirectory diretório onde cada backup comprimido será salvo
     * @param period          intervalo entre execuções
     * @param unit            unidade do intervalo
     * @param onBackupCreated callback chamado a cada backup concluído com sucesso (pode ser {@code null})
     */
    public void schedule(
            Path source,
            Path backupDirectory,
            long period,
            TimeUnit unit,
            Consumer<BackupMetadata> onBackupCreated
    ) {
        executor.scheduleAtFixedRate(() -> {
            try {
                BackupMetadata metadata = backupService.backup(source, backupDirectory);
                if (onBackupCreated != null) {
                    onBackupCreated.accept(metadata);
                }
            } catch (Exception e) {
                // Uma falha isolada não deve cancelar os próximos agendamentos.
                System.err.println("[OctopusFile] Falha ao executar backup agendado: " + e.getMessage());
            }
        }, period, period, unit);
    }

    /** Para todos os agendamentos e libera a thread. */
    public void stop() {
        executor.shutdown();
    }

    @Override
    public void close() {
        stop();
    }
}
