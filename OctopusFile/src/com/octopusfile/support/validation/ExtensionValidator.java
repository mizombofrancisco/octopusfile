package com.octopusfile.support.validation;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Valida extensões de arquivo contra allowlist/denylist configuráveis.
 * Usado principalmente por SecureFileHandler e pelos módulos de escrita
 * para bloquear tipos perigosos (ex.: .exe, .bat) em contextos de upload.
 */
public class ExtensionValidator {

    private final Set<String> allowedExtensions; // null = todas permitidas, exceto denylist
    private final Set<String> deniedExtensions;

    public ExtensionValidator() {
        this(null, defaultDenylist());
    }

    public ExtensionValidator(Set<String> allowedExtensions, Set<String> deniedExtensions) {
        this.allowedExtensions = normalize(allowedExtensions);
        this.deniedExtensions = normalize(deniedExtensions);
    }

    private static Set<String> defaultDenylist() {
        return Set.of("exe", "bat", "cmd", "sh", "msi", "dll", "com", "scr", "vbs", "ps1");
    }

    private static Set<String> normalize(Set<String> extensions) {
        if (extensions == null) {
            return null;
        }
        return extensions.stream()
                .map(e -> e.toLowerCase(Locale.ROOT).replaceFirst("^\\.", ""))
                .collect(Collectors.toUnmodifiableSet());
    }

    public String extensionOf(Path path) {
        String name = path.getFileName() != null ? path.getFileName().toString() : "";
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public boolean isAllowed(Path path) {
        String ext = extensionOf(path);
        if (deniedExtensions != null && deniedExtensions.contains(ext)) {
            return false;
        }
        if (allowedExtensions != null) {
            return allowedExtensions.contains(ext);
        }
        return true;
    }

    /** Lança OctopusFileException se a extensão não for permitida. */
    public void validate(Path path) {
        if (!isAllowed(path)) {
            throw new OctopusFileException(
                    ErrorCodes.INVALID_EXTENSION,
                    "Extensão '." + extensionOf(path) + "' não é permitida",
                    path
            );
        }
    }
}
