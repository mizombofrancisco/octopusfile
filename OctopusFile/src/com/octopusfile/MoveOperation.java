package com.octopusfile;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.modules.manipulation.FileMover;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Operação de mover/renomear fluente, devolvida por {@link OctopusFile#move(String)}.
 * <pre>{@code
 * OctopusFile.of().move("rascunho.txt").to("final/relatorio.txt");
 * }</pre>
 */
public class MoveOperation {

    private final Path source;
    private final FileMover fileMover = new FileMover();

    MoveOperation(Path source) {
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
            fileMover.move(source, destino);
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.valueOf("Erro ao mover " + source + " para " + destino), e);
        }
    }
}
