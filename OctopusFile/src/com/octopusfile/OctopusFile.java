package com.octopusfile;

import com.octopusfile.infrastructure.errors.ErrorCodes;
import com.octopusfile.infrastructure.errors.OctopusFileException;
import com.octopusfile.infrastructure.compression.ZipCompressionEngine;
import com.octopusfile.infrastructure.hashing.FileHasher;
import com.octopusfile.infrastructure.security.EncryptionEngine;
import com.octopusfile.modules.duplicates.DuplicateFinder;
import com.octopusfile.modules.duplicates.DuplicateGroup;
import com.octopusfile.modules.organization.FileOrganizer;
import com.octopusfile.modules.sync.DirectorySynchronizer;
import com.octopusfile.modules.sync.SyncResult;
import com.octopusfile.modules.trash.TrashEntry;
import com.octopusfile.modules.trash.TrashManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Ponto de entrada público (Fachada) da biblioteca OctopusFile.
 * <p>
 * Esta única classe já é suficiente para o uso do dia a dia da biblioteca —
 * ideal para quem está começando: basta chamar {@code OctopusFile.of()} (ou,
 * para monitoramento, {@code OctopusFile.watch(...)}) e encadear a operação
 * desejada. Nenhum dos métodos abaixo lança exceção checada: qualquer erro
 * de I/O é convertido em {@link OctopusFileException} (RuntimeException).
 */
public class OctopusFile {

    private static OctopusFile instance;

    private final GlobalConfiguration globalConfiguration;
    private final FileManager fileManager;
    private final DirectoryManager directoryManager;
    private final BatchOperations batchOperations;
    private final AsyncOperations asyncOperations;
    private final FileOrganizer fileOrganizer = new FileOrganizer();
    private final TrashManager trashManager;
    private final DuplicateFinder duplicateFinder = new DuplicateFinder();
    private final DirectorySynchronizer directorySynchronizer = new DirectorySynchronizer();
    private final EncryptionEngine encryptionEngine = new EncryptionEngine();
    private final FileHasher fileHasher = new FileHasher();

    private OctopusFile() {
        this.globalConfiguration = new GlobalConfiguration();
        this.fileManager = new FileManager();
        this.directoryManager = new DirectoryManager();
        this.batchOperations = new BatchOperations();
        this.asyncOperations = new AsyncOperations();
        this.trashManager = new TrashManager(
                globalConfiguration.getTrashDirectory(),
                new ZipCompressionEngine(globalConfiguration.getCompressionLevel())
        );
    }

    private static synchronized OctopusFile getInstance() {
        if (instance == null) {
            instance = new OctopusFile();
        }
        return instance;
    }

    /**
     * Ponto de entrada fluente principal.Devolve a instância única da
 biblioteca, pronta para encadear qualquer operação abaixo.
     * @return 
     */
    public static OctopusFile of() {
        return getInstance();
    }

    // ---------------------------------------------------------------
    // Leitura
    // ---------------------------------------------------------------

    /** Inicia uma leitura fluente.
     * @param path
     * @return  */
    public ReadOperation read(String path) {
        return new ReadOperation(Paths.get(path));
    }

    public ReadOperation read(Path path) {
        return new ReadOperation(path);
    }

    // ---------------------------------------------------------------
    // Escrita
    // ---------------------------------------------------------------

    /** Inicia uma escrita fluente.
     * @param path
     * @return  */
    public WriteOperation write(String path) {
        return new WriteOperation(Paths.get(path));
    }

    public WriteOperation write(Path path) {
        return new WriteOperation(path);
    }

    // ---------------------------------------------------------------
    // Cópia / Mover / Apagar / Existência
    // ---------------------------------------------------------------

    /** Inicia uma cópia fluente.
     * @param path
     * @return  */
    public CopyOperation copy(String path) {
        return new CopyOperation(Paths.get(path));
    }

    public CopyOperation copy(Path path) {
        return new CopyOperation(path);
    }

    /** Inicia um "mover/renomear" fluente.
     * @param path
     * @return  */
    public MoveOperation move(String path) {
        return new MoveOperation(Paths.get(path));
    }

    public MoveOperation move(Path path) {
        return new MoveOperation(path);
    }

    /** *  Apaga um arquivo ou diretório vazio.Devolve {@code true} se algo foi removido.
     * @param path
     * @return  */
    public boolean delete(String path) {
        return delete(Paths.get(path));
    }

    public boolean delete(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new OctopusFileException(
                    ErrorCodes.valueOf("Erro ao apagar " + path),
                    e
            );
        }
    }

    /** Verifica se um arquivo ou diretório existe.
     * @param path
     * @return  */
    public boolean exists(String path) {
        return Files.exists(Paths.get(path));
    }

    public boolean exists(Path path) {
        return Files.exists(path);
    }

    // ---------------------------------------------------------------
    // Diretórios e listagem
    // ---------------------------------------------------------------

    /** Cria um diretório (e os pais necessários), se ainda não existir.
     * @param path */
    public void createDirectory(String path) {
        createDirectory(Paths.get(path));
    }

    public void createDirectory(Path path) throws OctopusFileException {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new OctopusFileException(
                    ErrorCodes.DIRECTORY_CREATE_ERROR,
                    e
            );
        }
    }

    /** Inicia uma listagem fluente.
     * @param directory
     * @return  */
    public ListOperation list(String directory) {
        return new ListOperation(Paths.get(directory));
    }

    public ListOperation list(Path directory) {
        return new ListOperation(directory);
    }

    /** Organiza os arquivos de um diretório em subpastas, agrupados por tipo/categoria.
     * @param directory */
    public void organize(String directory) {
        organize(Paths.get(directory));
    }

    public void organize(Path directory) {
        try {
            fileOrganizer.organize(directory);
        } catch (IOException e) {
            throw new OctopusFileException(
                    ErrorCodes.valueOf("Erro ao organizar " + directory),
                    e
            );
        }
    }

    // ---------------------------------------------------------------
    // Backup
    // ---------------------------------------------------------------

    /**
     * Inicia um backup fluente de {@code path}.O conteúdo é comprimido
 (ZIP) em modo streaming, sem sobrecarregar a memória mesmo para
 diretórios grandes.<pre>{@code
     * OctopusFile.of().backup("relatorios/").to("backups/");
     * }</pre>
     * @param path
     * @return 
     */
    public BackupOperation backup(String path) {
        return backup(Paths.get(path));
    }

    public BackupOperation backup(Path path) {
        return new BackupOperation(path);
    }

    // ---------------------------------------------------------------
    // Lixeira (Trash)
    // ---------------------------------------------------------------

    /**
     * Move um arquivo ou diretório para a lixeira ao invés de apagá-lo
     * definitivamente: o conteúdo é comprimido e guardado em
     * {@link GlobalConfiguration#getTrashDirectory()}, podendo ser
     * restaurado depois com {@link #restoreFromTrash(String)}.
     *
     * @param path
     * @return os metadados do item na lixeira; guarde {@code getId()} para restaurar depois
     */
    public TrashEntry moveToTrash(String path) {
        return moveToTrash(Paths.get(path));
    }

    public TrashEntry moveToTrash(Path path) {
        try {
            return trashManager.moveToTrash(path);
        } catch (IOException e) {
            throw new OctopusFileException(
                    ErrorCodes.valueOf("Erro ao mover " + path + " para a lixeira"),
                    e
            );
        }
    }

    /** Restaura um item da lixeira para o seu caminho original.
     * @param id */
    public void restoreFromTrash(String id) {
        try {
            trashManager.restore(id);
        } catch (IOException e) {
            throw new OctopusFileException(
                    ErrorCodes.valueOf("Erro ao restaurar item da lixeira: " + id),
                    e
            );
        }
    }

    /** Restaura um item da lixeira para um caminho alternativo.
     * @param id
     * @param destino */
    public void restoreFromTrash(String id, String destino) {
        try {
            trashManager.restore(id, Paths.get(destino));
        } catch (IOException e) {
            throw new OctopusFileException(
                    ErrorCodes.valueOf("Erro ao restaurar item da lixeira: " + id),
                    e
            );
        }
    }

    /** Lista todos os itens atualmente na lixeira.
     * @return  */
    public List<TrashEntry> listTrash() {
        try {
            return trashManager.list();
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.valueOf("Erro ao listar a lixeira"), e);
        }
    }

    /** Apaga definitivamente um item da lixeira (sem possibilidade de restauração).
     * @param id */
    public void purgeFromTrash(String id) {
        try {
            trashManager.purge(id);
        } catch (IOException e) {
            throw new OctopusFileException(
                    ErrorCodes.valueOf("Erro ao apagar definitivamente o item da lixeira: " + id),
                    e
            );
        }
    }

    /** Esvazia a lixeira, apagando definitivamente todos os itens. */
    public void emptyTrash() {
        try {
            trashManager.empty();
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.valueOf("Erro ao esvaziar a lixeira"), e);
        }
    }

    // ---------------------------------------------------------------
    // Arquivos duplicados
    // ---------------------------------------------------------------

    /** Localiza grupos de arquivos com conteúdo idêntico dentro de um diretório.
     * @param directory
     * @return  */
    public List<DuplicateGroup> findDuplicates(String directory) {
        return findDuplicates(Paths.get(directory));
    }

    public List<DuplicateGroup> findDuplicates(Path directory) {
        try {
            return duplicateFinder.find(directory);
        } catch (IOException e) {
            throw new OctopusFileException(
                    ErrorCodes.valueOf("Erro ao procurar duplicados em " + directory),
                    e
            );
        }
    }

    // ---------------------------------------------------------------
    // Sincronização de diretórios
    // ---------------------------------------------------------------

    /** Sincroniza {@code source} → {@code target}, copiando apenas o que é novo ou mudou.
     * @param source
     * @param target
     * @return  */
    public SyncResult sync(String source, String target) {
        return sync(Paths.get(source), Paths.get(target), false);
    }

    /** Sincroniza {@code source} → {@code target}; com {@code mirror=true}, também remove do destino o que não existe mais na origem.
     * @param source
     * @param target
     * @param mirror
     * @return  */
    public SyncResult sync(String source, String target, boolean mirror) {
        return sync(Paths.get(source), Paths.get(target), mirror);
    }

    public SyncResult sync(Path source, Path target, boolean mirror) {
        try {
            return directorySynchronizer.sync(source, target, mirror);
        } catch (IOException e) {
            throw new OctopusFileException(
                    ErrorCodes.valueOf("Erro ao sincronizar " + source + " com " + target),
                    e
            );
        }
    }

    // ---------------------------------------------------------------
    // Integridade e segurança
    // ---------------------------------------------------------------

    /** Calcula o hash SHA-256 de um arquivo (streaming, sem carregá-lo por completo em memória).
     * @param path
     * @return  */
    public String hash(String path) {
        return hash(Paths.get(path));
    }

    public String hash(Path path) {
        try {
            return fileHasher.hash(path);
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.valueOf("Erro ao calcular hash de " + path), e);
        }
    }

    /** Cifra um arquivo com AES-256-GCM usando a senha informada.
     * @param source
     * @param destination
     * @param password */
    public void encrypt(String source, String destination, char[] password) {
        encrypt(Paths.get(source), Paths.get(destination), password);
    }

    public void encrypt(Path source, Path destination, char[] password) {
        try {
            encryptionEngine.encrypt(source, destination, password);
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.valueOf("Erro ao cifrar " + source), e);
        }
    }

    /** Decifra um arquivo gerado por {@link #encrypt} usando a senha informada.
     * @param source
     * @param destination
     * @param password */
    public void decrypt(String source, String destination, char[] password) {
        decrypt(Paths.get(source), Paths.get(destination), password);
    }

    public void decrypt(Path source, Path destination, char[] password) {
        try {
            encryptionEngine.decrypt(source, destination, password);
        } catch (IOException e) {
            throw new OctopusFileException(ErrorCodes.valueOf("Erro ao decifrar " + source), e);
        }
    }

    // ---------------------------------------------------------------
    // Monitoramento
    // ---------------------------------------------------------------

    /**
     * Inicia uma operação de monitoramento fluente sobre o diretório informado.Estático porque monitorar não depende de nenhum estado prévio da fachada.
     * @param path
     * @return 
     */
    public static WatchOperation watch(String path) {
        return new WatchOperation(Paths.get(path));
    }

    public static WatchOperation watch(Path path) {
        return new WatchOperation(path);
    }

    // ---------------------------------------------------------------
    // Acesso direto aos gerenciadores internos (uso avançado/não-fluente)
    // ---------------------------------------------------------------

    public GlobalConfiguration getGlobalConfiguration() {
        return globalConfiguration;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public DirectoryManager getDirectoryManager() {
        return directoryManager;
    }

    public BatchOperations getBatchOperations() {
        return batchOperations;
    }

    public AsyncOperations getAsyncOperations() {
        return asyncOperations;
    }

    public TrashManager getTrashManager() {
        return trashManager;
    }
}