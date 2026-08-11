package com.octopusfile.infrastructure.resources;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Rastreia recursos {@link AutoCloseable} abertos pela biblioteca (streams,
 * WatchServices, canais) para garantir que nada vaze quando o usuário
 * esquece de fechar algo, ou quando OctopusFile.shutdown() é chamado.
 *
 * Não substitui try-with-resources no código do usuário — é uma rede de
 * segurança e um ponto de observabilidade (quantos recursos estão abertos).
 */
public class ResourceManager implements AutoCloseable {

    private final Set<AutoCloseable> openResources =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final CopyOnWriteArrayList<AutoCloseable> orderedResources = new CopyOnWriteArrayList<>();
    private final Object lock = new Object();

    /** Registra um recurso para rastreamento. Retorna o próprio recurso para uso fluente. */
    public <T extends AutoCloseable> T track(T resource) {
        if (resource == null) {
            return null;
        }
        synchronized (lock) {
            openResources.add(resource);
            orderedResources.add(resource);
        }
        return resource;
    }

    /** Fecha e remove um recurso específico do rastreamento. */
    public void release(AutoCloseable resource) {
        if (resource == null) {
            return;
        }
        synchronized (lock) {
            if (!openResources.remove(resource)) {
                return; // já liberado ou nunca rastreado
            }
            orderedResources.remove(resource);
        }
        closeQuietly(resource);
    }

    public int openResourceCount() {
        synchronized (lock) {
            return openResources.size();
        }
    }

    /** Fecha todos os recursos abertos, coletando falhas sem interromper o processo. */
    @Override
    public void close() {
        java.util.List<AutoCloseable> toClose;
        synchronized (lock) {
            toClose = new java.util.ArrayList<>(orderedResources);
            openResources.clear();
            orderedResources.clear();
        }

        Exception first = null;
        for (AutoCloseable resource : toClose) {
            try {
                resource.close();
            } catch (Exception e) {
                if (first == null) {
                    first = e;
                }
            }
        }
        if (first != null) {
            throw new OctopusFileException(ErrorCodes.IO_ERROR, "Falha ao liberar um ou mais recursos", first);
        }
    }

    private void closeQuietly(AutoCloseable resource) {
        try {
            resource.close();
        } catch (Exception e) {
            throw new OctopusFileException(ErrorCodes.IO_ERROR, "Falha ao fechar recurso", e);
        }
    }
}
