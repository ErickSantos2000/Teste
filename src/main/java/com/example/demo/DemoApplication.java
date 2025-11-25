package com.example.demo;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Cria a sessão Spark
        SparkSession spark = SparkSession.builder()
                .appName("Exemplo Spark com Spring Boot")
                .master("local[*]")

                // 1. DESABILITA A INTERFACE WEB (UI): Evita conflito de portas com o Spring Boot Tomcat.
                .config("spark.ui.enabled", "false")

                // 2. DESABILITA O SISTEMA DE MÉTRICAS: Essencial para resolver o erro 'javax/servlet/http/HttpServlet'.
                .config("spark.metrics.enabled", "false")

                // 3. CONFIGURAÇÃO AGRESSIVA DE MÉTRICAS: Reforça a desativação de Servlets de Métricas.
                .config("spark.metrics.conf.master.sink.servlet.class", "")

                .getOrCreate();

        System.out.println("SparkSession criada com sucesso!");

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
    }
}