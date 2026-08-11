package com.octopusfile.infrastructure.errors;

/**
 * Códigos de erro padronizados usados em toda a biblioteca OctopusFile.
 *
 * Cada código possui:
 * - identificador estável para logs, telemetria e integrações;
 * - mensagem padrão em português;
 *
 * A mensagem padrão pode ser sobrescrita ao lançar a exceção.
 */
public enum ErrorCodes {

    // ============================================================
    // GENÉRICOS - OF-000
    // ============================================================

    UNKNOWN_ERROR(
            "OF-000",
            "Erro desconhecido na operação de arquivo"
    ),

    INVALID_ARGUMENT(
            "OF-001",
            "Argumento inválido fornecido à operação"
    ),

    NULL_ARGUMENT(
            "OF-002",
            "Argumento obrigatório não pode ser nulo"
    ),

    UNSUPPORTED_OPERATION(
            "OF-003",
            "Operação não suportada"
    ),

    INTERNAL_ERROR(
            "OF-004",
            "Erro interno da biblioteca"
    ),

    INVALID_STATE(
            "OF-005",
            "Estado interno inválido para executar a operação"
    ),

    RESOURCE_UNAVAILABLE(
            "OF-006",
            "Recurso necessário está indisponível"
    ),

    LIST_FILE_ERROR(
            "OF-007",
            "Erro ao listar"
    ),

    // ============================================================
    // CAMINHO / ARQUIVO / DIRETÓRIO - OF-100
    // ============================================================

    PATH_NOT_FOUND(
            "OF-100","Caminho não encontrado"
    ),

    FILE_NOT_FOUND(
            "OF-101","Arquivo não encontrado"
    ),

    DIRECTORY_NOT_FOUND(
            "OF-102","Diretório não encontrado"
    ),

    FILE_ALREADY_EXISTS(
            "OF-103","Arquivo já existe no destino"
    ),

    NOT_A_FILE(
            "OF-104","O caminho informado não é um arquivo"
    ),

    NOT_A_DIRECTORY(
            "OF-105","O caminho informado não é um diretório"
    ),

    INVALID_PATH(
            "OF-106","Caminho inválido ou malformado"
    ),

    INVALID_EXTENSION(
            "OF-107","Extensão de arquivo inválida ou não permitida"
    ),

    DIRECTORY_ALREADY_EXISTS(
            "OF-108","O diretório já existe"
    ),

    DIRECTORY_CREATE_ERROR(
            "OF-109","Falha ao criar o diretório"
    ),

    FILE_CREATE_ERROR(
            "OF-110","Falha ao criar o arquivo"
    ),

    PATH_TOO_LONG(
            "OF-111","O caminho excede o limite permitido pelo sistema"
    ),

    INVALID_FILE_NAME(
            "OF-112","Nome de arquivo inválido"
    ),

    INVALID_DIRECTORY_NAME(
            "OF-113","Nome de diretório inválido"
    ),

    FILE_TOO_LARGE(
            "OF-114","O arquivo excede o tamanho máximo permitido"
    ),

    EMPTY_PATH(
            "OF-115","O caminho informado está vazio"
    ),

    SAME_SOURCE_AND_TARGET(
            "OF-116","A origem e o destino não podem ser iguais"
    ),

    TARGET_ALREADY_EXISTS(
            "OF-117","O destino já existe"
    ),

    INVALID_SOURCE(
            "OF-118",
            "A origem informada é inválida"
    ),

    INVALID_TARGET(
            "OF-119",
            "O destino informado é inválido"
    ),


    // ============================================================
    // PERMISSÃO / SEGURANÇA - OF-200
    // ============================================================

    ACCESS_DENIED(
            "OF-200",
            "Acesso negado ao recurso solicitado"
    ),

    PERMISSION_DENIED(
            "OF-201",
            "Permissões insuficientes para a operação"
    ),

    SECURITY_VIOLATION(
            "OF-202",
            "Violação de política de segurança detectada"
    ),

    READ_PERMISSION_DENIED(
            "OF-203",
            "Permissão insuficiente para leitura"
    ),

    WRITE_PERMISSION_DENIED(
            "OF-204",
            "Permissão insuficiente para escrita"
    ),

    EXECUTE_PERMISSION_DENIED(
            "OF-205",
            "Permissão insuficiente para execução"
    ),

    DELETE_PERMISSION_DENIED(
            "OF-206",
            "Permissão insuficiente para exclusão"
    ),

    SANDBOX_VIOLATION(
            "OF-207",
            "A operação está fora dos limites permitidos"
    ),

    UNSAFE_PATH(
            "OF-208",
            "O caminho viola as regras de segurança"
    ),


    // ============================================================
    // ENTRADA / SAÍDA - OF-300
    // ============================================================

    IO_ERROR(
            "OF-300",
            "Erro de entrada/saída durante a operação"
    ),

    READ_ERROR(
            "OF-301",
            "Falha ao ler o arquivo"
    ),

    WRITE_ERROR(
            "OF-302",
            "Falha ao escrever no arquivo"
    ),

    COPY_ERROR(
            "OF-303",
            "Falha ao copiar o arquivo"
    ),

    MOVE_ERROR(
            "OF-304",
            "Falha ao mover o arquivo"
    ),

    DELETE_ERROR(
            "OF-305",
            "Falha ao excluir o arquivo"
    ),

    STREAM_CLOSED(
            "OF-306",
            "Operação em stream já fechado"
    ),

    FLUSH_ERROR(
            "OF-307",
            "Falha ao sincronizar dados no destino"
    ),

    FILE_LOCKED(
            "OF-308",
            "O arquivo está bloqueado por outro processo"
    ),

    DEVICE_ERROR(
            "OF-309",
            "Erro no dispositivo de armazenamento"
    ),

    DISK_FULL(
            "OF-310",
            "Não há espaço suficiente no dispositivo"
    ),

    STORAGE_UNAVAILABLE(
            "OF-311",
            "Dispositivo ou armazenamento indisponível"
    ),

    CORRUPTED_DATA(
            "OF-312",
            "Os dados do arquivo estão corrompidos"
    ),

    CHECKSUM_MISMATCH(
            "OF-313",
            "A verificação de integridade dos dados falhou"
    ),


    // ============================================================
    // CONCORRÊNCIA / ASSÍNCRONO - OF-400
    // ============================================================

    OPERATION_TIMEOUT(
            "OF-400",
            "Operação excedeu o tempo limite"
    ),

    OPERATION_CANCELLED(
            "OF-401",
            "Operação cancelada"
    ),

    TASK_REJECTED(
            "OF-402",
            "Tarefa rejeitada pelo pool de execução"
    ),

    INTERRUPTED(
            "OF-403",
            "Operação interrompida"
    ),

    CONCURRENT_MODIFICATION(
            "OF-404",
            "Recurso foi modificado durante a operação"
    ),

    RESOURCE_BUSY(
            "OF-405",
            "Recurso está ocupado"
    ),

    DEADLOCK_DETECTED(
            "OF-406",
            "Possível deadlock detectado"
    ),

    THREAD_POOL_SHUTDOWN(
            "OF-407",
            "Pool de execução já foi encerrado"
    ),


    // ============================================================
    // MONITORAMENTO - OF-500
    // ============================================================

    WATCH_REGISTRATION_FAILED(
            "OF-500",
            "Falha ao registrar observador de diretório"
    ),

    WATCH_SERVICE_CLOSED(
            "OF-501",
            "Serviço de monitoramento encerrado"
    ),

    WATCH_KEY_INVALID(
            "OF-502",
            "Chave de monitoramento tornou-se inválida"
    ),

    WATCH_DIRECTORY_NOT_FOUND(
            "OF-503",
            "Diretório monitorado não foi encontrado"
    ),

    WATCH_ACCESS_DENIED(
            "OF-504",
            "Acesso negado ao diretório monitorado"
    ),

    WATCH_OVERFLOW(
            "OF-505",
            "O monitor recebeu eventos além da capacidade disponível"
    ),

    WATCH_EVENT_ERROR(
            "OF-506",
            "Erro ao processar evento do sistema de arquivos"
    ),

    WATCH_THREAD_ERROR(
            "OF-507",
            "Erro na thread de monitoramento"
    ),

    WATCH_ALREADY_RUNNING(
            "OF-508",
            "O monitoramento já está em execução"
    ),

    WATCH_NOT_RUNNING(
            "OF-509",
            "O monitoramento não está em execução"
    ),


    // ============================================================
    // CONFIGURAÇÃO - OF-600
    // ============================================================

    CONFIG_LOAD_ERROR(
            "OF-600",
            "Falha ao carregar configuração"
    ),

    CONFIG_INVALID(
            "OF-601",
            "Configuração inválida"
    ),

    CONFIG_NOT_FOUND(
            "OF-602",
            "Configuração não encontrada"
    ),

    CONFIG_MISSING(
            "OF-603",
            "Configuração obrigatória não informada"
    ),

    CONFIG_PARSE_ERROR(
            "OF-604",
            "Falha ao interpretar configuração"
    ),

    CONFIG_TYPE_ERROR(
            "OF-605",
            "Tipo de configuração inválido"
    ),

    CONFIG_VALUE_INVALID(
            "OF-606",
            "Valor de configuração inválido"
    ),


    // ============================================================
    // CACHE - OF-700
    // ============================================================

    CACHE_ERROR(
            "OF-700",
            "Erro na camada de cache"
    ),

    CACHE_MISS(
            "OF-701",
            "Recurso não encontrado no cache"
    ),

    CACHE_WRITE_ERROR(
            "OF-702",
            "Falha ao armazenar recurso no cache"
    ),

    CACHE_READ_ERROR(
            "OF-703",
            "Falha ao recuperar recurso do cache"
    ),

    CACHE_INVALID(
            "OF-704",
            "Entrada de cache inválida"
    ),

    CACHE_FULL(
            "OF-705",
            "Capacidade máxima do cache atingida"
    ),

    CACHE_EXPIRED(
            "OF-706",
            "Entrada de cache expirada"
    ),


    // ============================================================
    // STREAM / BUFFER - OF-800
    // ============================================================

    BUFFER_ERROR(
            "OF-800",
            "Erro durante operação com buffer"
    ),

    BUFFER_OVERFLOW(
            "OF-801",
            "Buffer excedeu sua capacidade"
    ),

    BUFFER_UNDERFLOW(
            "OF-802",
            "Não existem dados suficientes no buffer"
    ),

    STREAM_OPEN_ERROR(
            "OF-803",
            "Falha ao abrir stream"
    ),

    STREAM_READ_ERROR(
            "OF-804",
            "Falha ao ler dados do stream"
    ),

    STREAM_WRITE_ERROR(
            "OF-805",
            "Falha ao escrever dados no stream"
    ),

    STREAM_CLOSE_ERROR(
            "OF-806",
            "Falha ao fechar stream"
    ),


    // ============================================================
    // OPERAÇÕES EM LOTE / TRANSAÇÕES - OF-900
    // ============================================================

    BATCH_ERROR(
            "OF-900",
            "Erro durante operação em lote"
    ),

    BATCH_PARTIAL_FAILURE(
            "OF-901",
            "A operação em lote foi concluída parcialmente"
    ),

    BATCH_CANCELLED(
            "OF-902",
            "Operação em lote cancelada"
    ),

    TRANSACTION_ERROR(
            "OF-903",
            "Erro durante transação de arquivos"
    ),

    TRANSACTION_ROLLBACK_FAILED(
            "OF-904",
            "Falha ao desfazer alterações da transação"
    ),

    TRANSACTION_INCOMPLETE(
            "OF-905",
            "Transação de arquivos não foi concluída"
    );


    // ============================================================
    // CAMPOS
    // ============================================================

    private final String code;
    private final String defaultMessage;


    // ============================================================
    // CONSTRUTOR
    // ============================================================

    ErrorCodes(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }


    // ============================================================
    // ACESSORES
    // ============================================================

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }


    // ============================================================
    // REPRESENTAÇÃO
    // ============================================================

    @Override
    public String toString() {
        return code + ": " + defaultMessage;
    }
}