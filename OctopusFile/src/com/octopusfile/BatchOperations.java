package com.octopusfile;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Executa uma mesma operação sobre uma coleção de arquivos/diretórios.
 */
public class BatchOperations {

    public void processBash(List<Path> paths, Consumer<Path> operacao) {
        for (Path path : paths) {
            operacao.accept(path);
        }
    }
}
