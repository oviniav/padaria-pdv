package br.com.padaria.dao;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato comum de CRUD. Cada DAO (FuncionarioDAO, ProdutoDAO...)
 * implementa esta interface para o seu tipo T.
 */
public interface CrudDAO<T> {

    void inserir(T obj) throws SQLException;

    void atualizar(T obj) throws SQLException;

    void excluir(int id) throws SQLException;

    T buscarPorId(int id) throws SQLException;

    List<T> listarTodos() throws SQLException;
}
