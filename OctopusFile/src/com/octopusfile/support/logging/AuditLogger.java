package com.octopusfile.support.logging;

import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registra eventos importantes para auditoria e conformidade:
 *
 * - exclusões;
 * - alterações de permissões;
 * - violações de segurança;
 * - acessos permitidos ou negados.
 *
 * Utiliza exclusivamente java.util.logging (JUL), não dependendo
 * de bibliotecas externas como SLF4J, Logback ou Log4j.
 */
public class AuditLogger {

    private static final String LOGGER_NAME = "com.octopusfile.audit";

    private final Logger logger;

    /**
     * Cria um AuditLogger usando o logger padrão do Java.
     */
    public AuditLogger() {
        this.logger = Logger.getLogger(LOGGER_NAME);
    }

    /**
     * Permite injetar um Logger personalizado.
     *
     * Útil para testes ou aplicações que desejem configurar
     * handlers, formatters e níveis específicos.
     */
    public AuditLogger(Logger logger) {

        if (logger == null) {
            throw new IllegalArgumentException(
                    "Logger não pode ser nulo"
            );
        }

        this.logger = logger;
    }

    /**
     * Registra uma exclusão de arquivo ou diretório.
     *
     * @param path recurso excluído
     * @param actor responsável pela operação
     */
    public void recordDeletion(Path path, String actor) {

        record(
                "DELETE",
                path,
                actor,
                null,
                Level.INFO
        );
    }

    /**
     * Registra uma alteração de permissões.
     *
     * @param path recurso afetado
     * @param actor responsável pela alteração
     * @param newPermissions novas permissões
     */
    public void recordPermissionChange(
            Path path,
            String actor,
            String newPermissions
    ) {

        record(
                "PERMISSION_CHANGE",
                path,
                actor,
                "novo=" + newPermissions,
                Level.INFO
        );
    }

    /**
     * Registra uma violação de segurança.
     *
     * Este evento utiliza WARNING porque representa uma situação
     * potencialmente perigosa.
     *
     * @param path recurso envolvido
     * @param actor responsável pela operação
     * @param reason motivo da violação
     */
    public void recordSecurityViolation(
            Path path,
            String actor,
            String reason
    ) {

        record(
                "SECURITY_VIOLATION",
                path,
                actor,
                "reason=" + reason,
                Level.WARNING
        );
    }

    /**
     * Registra uma tentativa de acesso.
     *
     * @param path recurso acessado
     * @param actor responsável pela operação
     * @param operation operação realizada
     * @param allowed indica se o acesso foi permitido
     */
    public void recordAccess(
            Path path,
            String actor,
            String operation,
            boolean allowed
    ) {

        record(
                allowed ? "ACCESS_GRANTED" : "ACCESS_DENIED",
                path,
                actor,
                "op=" + operation,
                allowed ? Level.INFO : Level.WARNING
        );
    }

    /**
     * Registra um evento genérico de auditoria.
     */
    private void record(
            String event,
            Path path,
            String actor,
            String detail,
            Level level
    ) {

        StringBuilder message = new StringBuilder();

        message.append("[AUDIT]")
                .append(" event=")
                .append(event)
                .append(" path='")
                .append(path)
                .append("' actor='")
                .append(actor)
                .append("'");

        if (detail != null && !detail.isBlank()) {
            message.append(" ")
                    .append(detail);
        }

        message.append(" at=")
                .append(Instant.now());

        logger.log(level, message.toString());
    }
}