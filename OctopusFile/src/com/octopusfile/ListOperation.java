package com.octopusfile;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.modules.filtering.FileFilter;
import com.octopusfile.modules.organization.FileSorter;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Operação de listagem fluente, devolvida por {@link OctopusFile#list(String)}.
 * <pre>{@code
 * OctopusFile.of().list("C:/downloads")
 *     .filter(new TypeFilter("pdf"))
 *     .forEach(System.out::println);
 * }</pre>
 */
public class ListOperation {

    private enum SortOrder {
        NONE,
        NAME,
        SIZE,
        DATE
    }

    private final Path directory;
    private final FileSorter fileSorter = new FileSorter();

    private FileFilter filter;
    private SortOrder sortOrder = SortOrder.NONE;

    ListOperation(Path directory) {
        this.directory = directory;
    }

    public ListOperation filter(FileFilter filter) {
        this.filter = filter;
        return this;
    }

    public ListOperation sortByName() {
        this.sortOrder = SortOrder.NAME;
        return this;
    }

    public ListOperation sortBySize() {
        this.sortOrder = SortOrder.SIZE;
        return this;
    }

    public ListOperation sortByLastModified() {
        this.sortOrder = SortOrder.DATE;
        return this;
    }

    /** Coleta os arquivos que atendem ao filtro (e ordenação, se definida) em uma lista. */
    public List<Path> collect() throws OctopusFileException {
        try {
            List<Path> result = sortOrder == SortOrder.NONE
                    ? listWithoutSorting()
                    : listSorted();

            return result;
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.LIST_FILE_ERROR, e);
        }
    }

    /** Percorre cada arquivo que atende ao filtro (e ordenação, se definida).
     * @param action */
    public void forEach(Consumer<Path> action) {
        collect().forEach(action);
    }

    private List<Path> listWithoutSorting() throws IOException {
        List<Path> result = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)
                        && (filter == null || filter.accept(file))) {
                    result.add(file);
                }
            }
        }

        return result;
    }

    private List<Path> listSorted() throws IOException {
        List<Path> allFiles = switch (sortOrder) {
            case NAME -> fileSorter.sortByName(directory);
            case SIZE -> fileSorter.sortBySize(directory);
            case DATE -> fileSorter.sortByLastModified(directory);
            case NONE -> listWithoutSorting();
        };

        if (filter == null) {
            return allFiles;
        }

        List<Path> filteredFiles = new ArrayList<>();

        for (Path file : allFiles) {
            if (filter.accept(file)) {
                filteredFiles.add(file);
            }
        }

        return filteredFiles;
    }
}