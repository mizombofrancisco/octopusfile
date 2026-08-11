package com.octopusfile.support.logging;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registra o ciclo de vida de operações de arquivo:
 *
 * - início;
 * - sucesso;
 * - falha;
 * - operação ignorada.
 *
 * Utiliza exclusivamente java.util.logging (JUL), não dependendo
 * de bibliotecas externas como SLF4J, Logback ou Log4j.
 *
 * Diferente do AuditLogger, este logger é destinado a diagnóstico
 * técnico e troubleshooting. Por isso, seus eventos podem ser
 * desabilitados sem comprometer a trilha de auditoria.
 */
public class OperationLogger {

    private final Logger logger;

    /**
     * Cria um OperationLogger utilizando o logger padrão
     * da biblioteca.
     */
    public OperationLogger() {
        this.logger = LoggerProvider.get(OperationLogger.class);
    }

    /**
     * Permite utilizar um logger personalizado.
     *
     * Útil para testes ou para aplicações que desejem configurar
     * handlers, formatters e níveis específicos.
     *
     * @param logger logger utilizado pela classe
     */
    public OperationLogger(Logger logger) {

        if (logger == null) {
            throw new IllegalArgumentException(
                    "Logger não pode ser nulo"
            );
        }

        this.logger = logger;
    }

    /**
     * Registra o início de uma operação.
     *
     * Utiliza FINE como equivalente aproximado ao DEBUG.
     *
     * @param operation nome da operação
     * @param path caminho envolvido
     */
    public void started(
            String operation,
            Path path
    ) {

        logger.log(
                Level.FINE,
                () -> format(
                        operation,
                        "iniciado em",
                        path,
                        null
                )
        );
    }

    /**
     * Registra uma operação concluída com sucesso.
     *
     * @param operation nome da operação
     * @param path caminho envolvido
     * @param durationMillis duração da operação em milissegundos
     */
    public void succeeded(
            String operation,
            Path path,
            long durationMillis
    ) {

        logger.log(
                Level.INFO,
                () -> String.format(
                        "[%s] concluído em '%s' (%d ms)",
                        safe(operation),
                        path,
                        durationMillis
                )
        );
    }

    /**
     * Registra uma operação que falhou.
     *
     * A exceção original é preservada no registro para facilitar
     * o diagnóstico técnico.
     *
     * @param operation nome da operação
     * @param path caminho envolvido
     * @param error exceção responsável pela falha
     */
    public void failed(
            String operation,
            Path path,
            Throwable error
    ) {

        if (error == null) {

            logger.log(
                    Level.WARNING,
                    () -> String.format(
                            "[%s] falhou em '%s': erro desconhecido",
                            safe(operation),
                            path
                    )
            );

            return;
        }

        logger.log(
                Level.WARNING,
                String.format(
                        "[%s] falhou em '%s': %s",
                        safe(operation),
                        path,
                        error.getMessage()
                ),
                error
        );
    }

    /**
     * Registra uma operação ignorada.
     *
     * Utiliza FINE como equivalente aproximado ao DEBUG.
     *
     * @param operation nome da operação
     * @param path caminho envolvido
     * @param reason motivo pelo qual a operação foi ignorada
     */
    public void skipped(
            String operation,
            Path path,
            String reason
    ) {

        logger.log(
                Level.FINE,
                () -> format(
                        operation,
                        "ignorado em",
                        path,
                        reason
                )
        );
    }

    /**
     * Formata uma mensagem de diagnóstico.
     */
    private String format(
            String operation,
            String action,
            Path path,
            String detail
    ) {

        StringBuilder message = new StringBuilder();

        message.append("[")
                .append(safe(operation))
                .append("] ")
                .append(action)
                .append(" '")
                .append(path)
                .append("'");

        if (detail != null && !detail.isBlank()) {
            message.append(": ")
                    .append(detail);
        }

        return message.toString();
    }

    /**
     * Evita que valores nulos apareçam diretamente nas mensagens.
     */
    private String safe(String value) {

        return value == null || value.isBlank()
                ? "UNKNOWN_OPERATION"
                : value;
    }
}