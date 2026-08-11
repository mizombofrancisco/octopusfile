package com.octopusfile.modules.duplicates;

import com.octopusfile.infrastructure.hashing.FileHasher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Localiza arquivos duplicados (mesmo conteúdo, mesmo com nomes diferentes)
 * dentro de um diretório.
 * <p>
 * Estratégia em duas fases para economizar CPU/E-S: primeiro agrupa por
 * tamanho (barato), e só calcula o hash — a parte cara — para arquivos que já
 * têm pelo menos outro do mesmo tamanho.
 */
public class DuplicateFinder {

    private final FileHasher fileHasher;

    public DuplicateFinder() {
        this(new FileHasher());
    }

    public DuplicateFinder(FileHasher fileHasher) {
        this.fileHasher = fileHasher;
    }

    /**
     * Varre {@code directory} recursivamente e devolve os grupos de arquivos
     * com conteúdo idêntico.
     */
    public List<DuplicateGroup> find(Path directory) throws IOException {
        Map<Long, List<Path>> porTamanho = new HashMap<>();

        try (Stream<Path> stream = Files.walk(directory)) {
            for (Path path : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator) {
                porTamanho.computeIfAbsent(Files.size(path), tamanho -> new ArrayList<>()).add(path);
            }
        }

        Map<String, List<Path>> porHash = new HashMap<>();
        Map<String, Long> tamanhoPorHash = new HashMap<>();

        for (Map.Entry<Long, List<Path>> grupo : porTamanho.entrySet()) {
            List<Path> candidatos = grupo.getValue();
            if (candidatos.size() < 2) {
                continue; // tamanho único não pode ter duplicata
            }

            for (Path path : candidatos) {
                String hash = fileHasher.hash(path);
                porHash.computeIfAbsent(hash, h -> new ArrayList<>()).add(path);
                tamanhoPorHash.put(hash, grupo.getKey());
            }
        }

        List<DuplicateGroup> resultado = new ArrayList<>();
        for (Map.Entry<String, List<Path>> entry : porHash.entrySet()) {
            if (entry.getValue().size() > 1) {
                resultado.add(new DuplicateGroup(
                        entry.getKey(),
                        entry.getValue(),
                        tamanhoPorHash.get(entry.getKey())
                ));
            }
        }

        return resultado;
    }
}
