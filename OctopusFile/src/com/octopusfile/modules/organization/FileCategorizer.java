package com.octopusfile.modules.organization;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Determina a categoria de um arquivo com base em sua extensão.
 */
public class FileCategorizer {

    public String categorize(Path file) {
        String extension = extractExtension(file);

        return switch (extension) {
            case "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "Imagens";
            case "mp4", "avi", "mkv", "mov" -> "Videos";
            case "mp3", "wav", "flac", "ogg" -> "Audios";
            case "doc", "docx", "pdf", "txt", "odt" -> "Documentos";
            case "xls", "xlsx", "csv" -> "Planilhas";
            case "zip", "rar", "7z", "tar", "gz" -> "Compactados";
            case "" -> "Sem_Categoria";
            default -> "Outros";
        };
    }

    private String extractExtension(Path path) {
        String fileName = path.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        return (index == -1) ? "" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
