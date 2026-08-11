package com.octopusfile.support.logging;

import java.util.logging.Logger;

/**
 * Ponto único para obtenção de loggers dentro da biblioteca OctopusFile.
 *
 * Utiliza exclusivamente java.util.logging (JUL), disponível no JDK,
 * não exigindo dependências externas como SLF4J, Logback ou Log4j.
 *
 * Os loggers seguem a convenção:
 *
 *     com.octopusfile.<módulo>
 *
 * Exemplos:
 *
 *     LoggerProvider.get("audit")
 *     -> com.octopusfile.audit
 *
 *     LoggerProvider.get("monitoring")
 *     -> com.octopusfile.monitoring
 *
 *     LoggerProvider.get(MyClass.class)
 *     -> nome completo da classe
 */
public final class LoggerProvider {

    private static final String ROOT_LOGGER =
            "com.octopusfile";

    private LoggerProvider() {
        // Impede instanciação.
    }

    /**
     * Obtém um logger associado à classe informada.
     *
     * @param clazz classe que será utilizada como categoria do logger
     * @return logger configurado para a classe
     */
    public static Logger get(Class<?> clazz) {

        if (clazz == null) {
            throw new IllegalArgumentException(
                    "A classe do logger não pode ser nula"
            );
        }

        return Logger.getLogger(
                clazz.getName()
        );
    }

    /**
     * Obtém um logger utilizando um nome de módulo.
     *
     * O prefixo "com.octopusfile." é adicionado automaticamente.
     *
     * @param name nome do módulo ou categoria
     * @return logger correspondente
     */
    public static Logger get(String name) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome do logger não pode ser vazio"
            );
        }

        String loggerName;

        if (name.startsWith(ROOT_LOGGER)) {
            loggerName = name;
        } else {
            loggerName = ROOT_LOGGER + "." + name;
        }

        return Logger.getLogger(loggerName);
    }

    /**
     * Retorna o nome raiz utilizado pela biblioteca.
     *
     * @return nome raiz dos loggers
     */
    public static String rootName() {
        return ROOT_LOGGER;
    }
}
