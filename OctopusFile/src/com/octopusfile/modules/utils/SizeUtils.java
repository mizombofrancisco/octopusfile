package com.octopusfile.modules.utils;

import java.util.Locale;

/**
 * Funções utilitárias para conversão e formatação de tamanhos de arquivo.
 */
public final class SizeUtils {

    private static final String[] UNIDADES = {"B", "KB", "MB", "GB", "TB", "PB"};

    private SizeUtils() {
        // classe utilitária: não deve ser instanciada
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " " + UNIDADES[0];
        }

        int unidadeIndex = (int) (Math.log(bytes) / Math.log(1024));
        unidadeIndex = Math.min(unidadeIndex, UNIDADES.length - 1);

        double valor = bytes / Math.pow(1024, unidadeIndex);
        return String.format(Locale.ROOT, "%.2f %s", valor, UNIDADES[unidadeIndex]);
    }

    public static long kilobytesToBytes(long kb) {
        return kb * 1024L;
    }

    public static long megabytesToBytes(long mb) {
        return mb * 1024L * 1024L;
    }

    public static long gigabytesToBytes(long gb) {
        return gb * 1024L * 1024L * 1024L;
    }
}
