package com.octopusfile.modules.sync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Sincroniza um diretório de origem com um de destino (sincronização
 * unidirecional, tipo "espelho"): copia arquivos novos ou modificados e,
 * opcionalmente, remove do destino tudo que não existe mais na origem.
 */
public class DirectorySynchronizer {

    /**
     * Sincroniza {@code source} → {@code target}.
     *
     * @param source diretório de origem (fonte da verdade)
     * @param target diretório de destino, que passa a espelhar a origem
     * @param mirror se {@code true}, remove do destino qualquer item que não
     *               exista mais na origem; se {@code false}, apenas adiciona/atualiza
     * @return resumo do que foi copiado, atualizado e removido
     * @throws IOException caso a sincronização não possa ser concluída
     */
    public SyncResult sync(Path source, Path target, boolean mirror) throws IOException {
        Files.createDirectories(target);
        SyncResult resultado = new SyncResult();
        Set<Path> vistosNoDestino = new HashSet<>();

        try (Stream<Path> stream = Files.walk(source)) {
            for (Path origem : (Iterable<Path>) stream::iterator) {
                Path relativo = source.relativize(origem);
                if (relativo.toString().isEmpty()) {
                    continue; // é o próprio diretório-raiz
                }

                Path destino = target.resolve(relativo);
                vistosNoDestino.add(destino);

                if (Files.isDirectory(origem)) {
                    Files.createDirectories(destino);
                    continue;
                }

                if (!Files.exists(destino)) {
                    copiar(origem, destino);
                    resultado.addCopied(destino);
                } else if (foiAlterado(origem, destino)) {
                    copiar(origem, destino);
                    resultado.addUpdated(destino);
                }
            }
        }

        if (mirror) {
            removerExtras(target, vistosNoDestino, resultado);
        }

        return resultado;
    }

    private boolean foiAlterado(Path origem, Path destino) throws IOException {
        if (Files.size(origem) != Files.size(destino)) {
            return true;
        }

        FileTime dataOrigem = Files.getLastModifiedTime(origem);
        FileTime dataDestino = Files.getLastModifiedTime(destino);
        return dataOrigem.compareTo(dataDestino) > 0;
    }

    private void copiar(Path origem, Path destino) throws IOException {
        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }
        Files.copy(
                origem,
                destino,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
        );
    }

    private void removerExtras(Path target, Set<Path> vistos, SyncResult resultado) throws IOException {
        try (Stream<Path> stream = Files.walk(target)) {
            List<Path> extras = stream
                    .filter(path -> !path.equals(target))
                    .filter(path -> !vistos.contains(path))
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .toList();

            for (Path extra : extras) {
                if (Files.exists(extra)) {
                    Files.delete(extra);
                    resultado.addDeleted(extra);
                }
            }
        }
    }
}
