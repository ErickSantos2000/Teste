# Relatório de Correção de Erros da Aplicação

Este documento detalha os erros encontrados na aplicação e os passos executados para corrigi-los.

## Visão Geral

A aplicação Spring Boot com Apache Spark não estava compilando devido a uma série de erros de configuração, dependências e código. Os seguintes arquivos foram modificados para corrigir os problemas:

*   `pom.xml`
*   `src/main/java/com/example/demo/DemoApplication.java`
*   `src/main/resources/dados.csv` (novo arquivo)

---

### Erro 1: `java.lang.IllegalAccessError`

*   **Problema:** A aplicação falhava ao iniciar com um erro de acesso ilegal. Isso ocorria porque o Apache Spark tentava acessar uma API interna do Java (`sun.nio.ch.DirectBuffer`) que não é mais permitida por padrão a partir do Java 9.

*   **Solução:** Foi necessário configurar o plugin de testes do Maven (`maven-surefire-plugin`) para adicionar um argumento à JVM que permite esse acesso específico.

*   **Arquivo Alterado:** `pom.xml`
    *   Adicionada a seguinte configuração dentro da seção `<plugins>`:
    ```xml
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
            <argLine>--add-opens java.base/sun.nio.ch=ALL-UNNAMED</argLine>
        </configuration>
    </plugin>
    ```

### Erro 2: `Non-parseable POM` (Erro de sintaxe no `pom.xml`)

*   **Problema:** Durante uma das correções, a tag de fechamento `</dependencies>` foi removida acidentalmente, o que tornou o arquivo `pom.xml` inválido.

*   **Solução:** A tag `</dependencies>` foi reinserida no local correto, antes da tag `<build>`.

*   **Arquivo Alterado:** `pom.xml`

### Erro 3: `java.lang.NoClassDefFoundError: javax.servlet.http.HttpServlet`

*   **Problema:** Após corrigir o primeiro erro, a aplicação falhou novamente por não encontrar a classe `HttpServlet`. Isso aconteceu porque o Spring Boot 3 usa o novo pacote `jakarta.servlet`, mas a versão do Spark utilizada ainda depende do pacote antigo `javax.servlet`.

*   **Solução:** Adicionamos uma dependência que provê a API `javax.servlet` para que o Spark pudesse encontrar a classe necessária.

*   **Arquivo Alterado:** `pom.xml`
    *   Adicionada a seguinte dependência:
    ```xml
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
        <version>4.0.1</version>
        <scope>provided</scope>
    </dependency>
    ```

### Erro 4: `org.apache.spark.sql.AnalysisException: [PATH_NOT_FOUND]`

*   **Problema:** A aplicação tentava ler um arquivo CSV de um caminho absoluto e fixo no código (`/home/erick/dados.csv`), o que causa falhas se o arquivo não existir ou se a aplicação for executada em outra máquina. Além disso, o caminho do projeto continha caracteres especiais que não eram tratados corretamente.

*   **Solução:** A abordagem foi alterada para tornar o projeto autossuficiente e robusto.
    1.  **Criação de arquivo de dados:** Um arquivo `dados.csv` de exemplo foi criado na pasta de recursos do projeto.
    2.  **Leitura a partir do Classpath:** O código foi modificado para carregar o arquivo a partir dos recursos do projeto, em vez de um caminho fixo.
    3.  **Tratamento de Caracteres Especiais:** O método de obtenção do caminho do arquivo foi melhorado para lidar corretamente com espaços e caracteres especiais no caminho do projeto.

*   **Arquivos Alterados:**
    *   **`src/main/resources/dados.csv` (Novo)**:
        ```csv
        id,nome
        1,Mundo
        ```
    *   **`src/main/java/com/example/demo/DemoApplication.java`**:
        *   A linha que definia o caminho do arquivo foi alterada de:
        ```java
        String caminhoArquivo = "/home/erick/dados.csv";
        ```
        *   Para:
        ```java
        java.net.URL resource = getClass().getClassLoader().getResource("dados.csv");
        if (resource == null) {
            throw new IllegalArgumentException("Arquivo não encontrado: dados.csv");
        }
        String caminhoArquivo = new java.io.File(resource.toURI()).getAbsolutePath();
        ```

---

Após essas correções, a aplicação passou a compilar e executar os testes com sucesso.

---

## Erros ao Executar a Aplicação Empacotada (`java -jar`)

Depois que a aplicação foi compilada com sucesso, novos erros surgiram ao tentar executá-la com o comando `java -jar target/demo-0.0.1-SNAPSHOT.jar`.

### Erro 5: `java.lang.IllegalAccessError` (em `java -jar`)

*   **Problema:** O mesmo erro `IllegalAccessError` que ocorreu durante os testes do Maven reapareceu. Isso acontece porque a configuração `<argLine>` no `pom.xml` só é aplicada quando o Maven executa os testes, não quando o JAR é executado diretamente.

*   **Solução:** O argumento `--add-opens` precisa ser passado diretamente para o comando `java`.

*   **Comando Corrigido:**
    ```bash
    java --add-opens java.base/sun.nio.ch=ALL-UNNAMED -jar target/demo-0.0.1-SNAPSHOT.jar
    ```

### Erro 6: `java.lang.IllegalArgumentException: URI is not hierarchical`

*   **Problema:** Mesmo com o comando `java` corrigido, a aplicação falhou com este erro. Isso ocorre porque o método `new File(resource.toURI())` não funciona quando o recurso (`dados.csv`) está dentro de um arquivo JAR, pois o caminho não é mais um caminho de arquivo padrão do sistema.

*   **Solução:** O código foi modificado para extrair o `dados.csv` de dentro do JAR para um arquivo temporário no sistema. O caminho deste arquivo temporário é então passado para o Spark.

*   **Arquivo Alterado:** `src/main/java/com/example/demo/DemoApplication.java`
    *   O bloco de leitura de arquivo foi reescrito para usar `getResourceAsStream` e criar um arquivo temporário. O bloco `finally` garante que a sessão Spark seja sempre encerrada.
    ```java
    try (java.io.InputStream inputStream = getClass().getClassLoader().getResourceAsStream("dados.csv")) {
        if (inputStream == null) {
            throw new IllegalArgumentException("Arquivo não encontrado: dados.csv");
        }
        java.io.File tempFile = java.io.File.createTempFile("dados", ".csv");
        tempFile.deleteOnExit();
        try (java.io.OutputStream outputStream = new java.io.FileOutputStream(tempFile)) {
            inputStream.transferTo(outputStream);
        }
        String caminhoArquivo = tempFile.getAbsolutePath();

        // Lê o arquivo CSV usando Spark
        Dataset<Row> dados = spark.read()
                .option("header", "true") // se tiver cabeçalho
                .option("inferSchema", "true") // tenta inferir tipo das colunas
                .csv(caminhoArquivo);

        // Mostra as primeiras 5 linhas
        dados.show(5);

        // Conta o número total de registros
        long totalRegistros = dados.count();
        System.out.println("Total de registros: " + totalRegistros);
    } finally {
        // Encerra a sessão Spark
        spark.stop();
    }
    ```
# Teste
# Teste
