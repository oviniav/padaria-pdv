package br.com.padaria.dao;

import br.com.padaria.model.Funcionario;
import br.com.padaria.util.SenhaUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FuncionarioDAO implements CrudDAO<Funcionario> {

    // Códigos de erro padrão do PostgreSQL (SQLState)
    private static final String VIOLA_UNICO = "23505";      // usuário repetido
    private static final String VIOLA_CHAVE_EST = "23503";  // funcionário com vendas

    /**
     * Valida o login. Recebe a senha digitada (texto puro), gera o hash e compara.
     * Retorna o funcionário se estiver certo, ou null se usuário/senha forem inválidos.
     */
    public Funcionario autenticar(String usuario, String senhaDigitada) throws SQLException {
        String sql = "SELECT * FROM funcionario WHERE usuario = ? AND senha = ?";
        try (Connection con = ConexaoDB.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ps.setString(2, SenhaUtil.gerarHash(senhaDigitada));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /** obj.getSenha() deve vir em texto puro: o hash é gerado aqui. */
    @Override
    public void inserir(Funcionario obj) throws SQLException {
        String sql = "INSERT INTO funcionario (nome, cargo, telefone, usuario, senha) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexaoDB.getConexao();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, obj.getNome());
            ps.setString(2, obj.getCargo());
            ps.setString(3, obj.getTelefone());
            ps.setString(4, obj.getUsuario());
            ps.setString(5, SenhaUtil.gerarHash(obj.getSenha()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    obj.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw traduzir(e);
        }
    }

    /** Se a senha vier vazia, mantém a senha atual do funcionário. */
    @Override
    public void atualizar(Funcionario obj) throws SQLException {
        boolean trocaSenha = obj.getSenha() != null && !obj.getSenha().isBlank();
        String sql = "UPDATE funcionario SET nome = ?, cargo = ?, telefone = ?, usuario = ?"
                   + (trocaSenha ? ", senha = ?" : "")
                   + " WHERE id = ?";
        try (Connection con = ConexaoDB.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, obj.getNome());
            ps.setString(i++, obj.getCargo());
            ps.setString(i++, obj.getTelefone());
            ps.setString(i++, obj.getUsuario());
            if (trocaSenha) {
                ps.setString(i++, SenhaUtil.gerarHash(obj.getSenha()));
            }
            ps.setInt(i, obj.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw traduzir(e);
        }
    }

    @Override
    public void excluir(int id) throws SQLException {
        try (Connection con = ConexaoDB.getConexao();
             PreparedStatement ps = con.prepareStatement("DELETE FROM funcionario WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw traduzir(e);
        }
    }

    @Override
    public Funcionario buscarPorId(int id) throws SQLException {
        try (Connection con = ConexaoDB.getConexao();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM funcionario WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    @Override
    public List<Funcionario> listarTodos() throws SQLException {
        List<Funcionario> lista = new ArrayList<>();
        try (Connection con = ConexaoDB.getConexao();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM funcionario ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Converte uma linha do ResultSet em objeto Funcionario. */
    private Funcionario mapear(ResultSet rs) throws SQLException {
        return new Funcionario(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("cargo"),
                rs.getString("telefone"),
                rs.getString("usuario"),
                rs.getString("senha"));
    }

    /** Troca erros técnicos do banco por mensagens que o usuário entende. */
    private SQLException traduzir(SQLException e) {
        if (VIOLA_UNICO.equals(e.getSQLState())) {
            return new SQLException("Já existe um funcionário com esse usuário.", e);
        }
        if (VIOLA_CHAVE_EST.equals(e.getSQLState())) {
            return new SQLException("Este funcionário possui vendas registradas e não pode ser excluído.", e);
        }
        return e;
    }
}
