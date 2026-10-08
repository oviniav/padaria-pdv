package br.com.padaria.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Abre conexões com o PostgreSQL usando os dados de src/main/resources/db.properties.
 */
public final class ConexaoDB {

    private ConexaoDB() {
    }

    public static Connection getConexao() throws SQLException {
        Properties props = new Properties();
        try (InputStream in = ConexaoDB.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new SQLException("Arquivo db.properties não encontrado. "
                        + "Copie o db.properties.exemplo para db.properties e ajuste a senha.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new SQLException("Erro ao ler o db.properties.", e);
        }
        return DriverManager.getConnection(
                props.getProperty("db.url"),
                props.getProperty("db.usuario"),
                props.getProperty("db.senha"));
    }
}
