package com.octopusfile;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.modules.manipulation.FileCopier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Operação de cópia fluente, devolvida por {@link OctopusFile#copy(String)}.
 * <pre>{@code
 * OctopusFile.of().copy("original.txt").to("backup/original.txt");
 * }</pre>
 */
public class CopyOperation {

    private final Path source;
    private final FileCopier fileCopier = new FileCopier();

    CopyOperation(Path source) {
        this.source = source;
    }

    public void to(String destino) {
        to(Paths.get(destino));
    }

    public void to(Path destino) {
        try {
            if (destino.getParent() != null) {
                Files.createDirectories(destino.getParent());
            }
            fileCopier.copy(source, destino);
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.valueOf("Erro ao copiar " + source + " para " + destino), e);
        }
    }
}
