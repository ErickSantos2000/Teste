# Otimização: Redução de Logs do Apache Spark

Este documento descreve uma otimização aplicada para reduzir a verbosidade dos logs do Apache Spark na aplicação.

## Problema

A aplicação gerava uma quantidade excessiva de logs de nível `INFO` do Apache Spark durante sua execução. Isso dificultava a visualização dos resultados reais do processamento e a identificação de informações importantes no console.

## Solução

O nível de log do Spark foi ajustado para `WARN` (aviso) no arquivo de configuração do Spring Boot. Com esta configuração, apenas mensagens de aviso, erro e fatal do Spark serão exibidas, tornando a saída do console muito mais limpa e focada nos resultados essenciais.

## Detalhes da Alteração

*   **Arquivo Modificado:** `src/main/resources/application.properties`
*   **Linha Adicionada:**
    ```properties
    logging.level.org.apache.spark=WARN
    ```

## Impacto

Após a aplicação desta otimização, a execução da aplicação exibirá significativamente menos logs do Apache Spark, facilitando a depuração e a leitura da saída do programa. Os resultados do processamento Spark, como tabelas e contagens, continuarão a ser exibidos normalmente.
