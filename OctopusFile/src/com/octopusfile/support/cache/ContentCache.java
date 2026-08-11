package com.octopusfile.support.cache;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Cache LRU de conteúdo de arquivo em memória (bytes). Existe apenas para
 * arquivos pequenos (config, templates, ícones) reabertos com frequência —
 * NUNCA deve ser usado para arquivos grandes, por isso impõe:
 *  - um limite de tamanho por entrada (maxEntryBytes): arquivos maiores
 *    simplesmente não entram no cache (loader é chamado, mas o resultado
 *    não é retido);
 *  - um limite de entradas totais, com evicção LRU.
 *
 * Thread-safety: sincronizado internamente (uso esperado é baixa
 * concorrência para este tipo de conteúdo "quente" e pequeno).
 */
public class ContentCache {

    private final int maxEntries;
    private final long maxEntryBytes;
    private final Map<Path, byte[]> cache;

    public ContentCache() {
        this(256, 1_048_576L); // 256 entradas, 1 MiB por entrada
    }

    public ContentCache(int maxEntries, long maxEntryBytes) {
        this.maxEntries = maxEntries;
        this.maxEntryBytes = maxEntryBytes;
        this.cache = new LinkedHashMap<>(maxEntries, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Path, byte[]> eldest) {
                return size() > ContentCache.this.maxEntries;
            }
        };
    }

    public synchronized byte[] getOrLoad(Path path, Function<Path, byte[]> loader) {
        byte[] cached = cache.get(path);
        if (cached != null) {
            return cached;
        }
        byte[] content = loader.apply(path);
        if (content.length <= maxEntryBytes) {
            cache.put(path, content);
        }
        return content;
    }

    public synchronized void invalidate(Path path) {
        cache.remove(path);
    }

    public synchronized void invalidateAll() {
        cache.clear();
    }

    public synchronized int size() {
        return cache.size();
    }

    public long maxEntryBytes() {
        return maxEntryBytes;
    }
}
