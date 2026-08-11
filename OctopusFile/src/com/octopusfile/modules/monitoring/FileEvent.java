package com.octopusfile.modules.monitoring;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Evento único de mudança no sistema de arquivos, já traduzido do
 * vocabulário do java.nio ({@code StandardWatchEventKinds}) para algo
 * mais direto de consumir.
 */
public record FileEvent(Path path, Kind kind, Instant timestamp) {

    public enum Kind {
        CREATED,
        MODIFIED,
        DELETED,

        /**
         * O SO descartou eventos por excesso de volume;
         * o estado observado pode estar incompleto.
         */
        OVERFLOW
    }

    /**
     * Cria um evento utilizando o instante atual.
     */
    public static FileEvent now(Path path, Kind kind) {
        return new FileEvent(path, kind, Instant.now());
    }

    /**
     * Getter tradicional para o caminho.
     */
    public Path getPath() {
        return path;
    }

    /**
     * Getter tradicional para o tipo do evento.
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Getter tradicional para o timestamp.
     */
    public Instant getTimestamp() {
        return timestamp;
    }
}