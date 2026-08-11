package com.octopusfile.modules.monitoring;

/**
 * Callback invocado para cada evento de sistema de arquivos observado.
 * Implementações devem ser rápidas — o dispatch roda em um único thread
 * compartilhado (veja {@link WatchService}), então um listener lento
 * atrasa a entrega de eventos para TODOS os diretórios monitorados.
 * Para processamento pesado, o listener deve delegar a um executor próprio.
 */
@FunctionalInterface
public interface EventListener {
    void onEvent(FileEvent event);
}
