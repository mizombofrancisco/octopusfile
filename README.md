 OctopusFile

Biblioteca Java modular e de alto desempenho para gestão completa de arquivos e diretórios, desenvolvida com base no padrão **Facade** e orientada a objetos.

---

## 🏛️ Arquitetura da Biblioteca

O projeto segue uma arquitetura em camadas estruturada para garantir escalabilidade, segurança e facilidade de uso:

*   **API Pública (Fachada):** Classe principal `OctopusFile` que serve como ponto de entrada único para todos os módulos.
*   **Módulos Principais:**
    *   **Leitura:** `FileReader`, `BufferedReader`, `CsvReader`
    *   **Escrita:** `FileWriter`, `BufferedWriter`, `CsvWriter`
    *   **Manipulação:** `FileMover`, `FileCopier`, `FileRenamer`, `FileDeleter`
    *   **Monitoramento:** `FileWatcher`, `FileEvent`, `WatchService`, `EventListener`
    *   **Organização:** `FileOrganizer`, `FileSorter`, `FileGrouper`, `FileCategorizer`
    *   **Filtragem:** `FileFilter`, `NameFilter`, `TypeFilter`, `SizeFilter`
    *   **Utilitários:** `FileUtils`, `PathUtils`, `NameUtils`, `SizeUtils`
*   **Camada de Suporte:** Gestão de Segurança, Validação, Cache, Logging e Configuração.
*   **Infraestrutura:** Integração com `NIO.2 File System`, Concorrência (`Executors`, `CompletableFuture`), Gerenciamento de Recursos e Tratamento de Erros.

---

## 🚀 Tecnologias Utilizadas
*   **Java 8+**
*   **NIO.2 (`java.nio.file`)** para operações de sistema de ficheiros de alta performance.
*   **WatchService** para monitoramento de eventos em tempo real.
*   **Streams & CompletableFuture** para processamento assíncrono.
*   **Padrões de Projeto:** Facade, Builder, Factory, Observer, Strategy, Singleton, Template Method, Chain of Responsibility.

---

## 💻 Uso Rápido (Exemplo)

```java
// Exemplo de leitura e filtragem encadeada
OctopusFile.of()
    .read("dados.csv")
    .filter(new CsvFilter().withDelimiter(";"))
    .forEach(System.out::println);

// Exemplo de monitoramento assíncrono em tempo real
OctopusFile.watch("C:/arquivos")
    .onModify(event -> System.out.println("Arquivo modificado: " + event.getPath()))
    .start();
```

---

## 📂 Estrutura do Repositório
*   `src/com/octopusfile/core/` - Contém a fachada principal e configurações globais.
*   `src/com/octopusfile/modules/` - Contém todos os módulos especializados (Leitura, Escrita, Manipulação, etc.).
*   `docs/` - Diagramas arquiteturais e documentação técnica.

---

## 🛠️ Instalação e Execução (NetBeans)
1. Clone ou descarregue este repositório para o seu computador.
2. Abra o **NetBeans IDE** e importe o projeto como um projeto Java existente.
3. Certifique-se de que o JDK 8 ou superior está configurado no projeto.
4. Utilize a classe principal `OctopusFile` através da fachada para começar a gerir os seus ficheiros.

---
*Desenvolvido sob o ecossistema ACURATECH.*
