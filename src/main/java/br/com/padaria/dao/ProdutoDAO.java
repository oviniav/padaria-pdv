package br.com.padaria.dao;

import br.com.padaria.model.Produto;
import br.com.padaria.model.UnidadeMedida;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO implements CrudDAO<Produto> {

    private static final String VIOLA_CHAVE_EST = "23503"; // produto já usado em vendas

    @Override
    public void inserir(Produto obj) throws SQLException {
        String sql = "INSERT INTO produto (nome, descricao, preco, unidade, estoque) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexaoDB.getConexao();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(ps, obj);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    obj.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void atualizar(Produto obj) throws SQLException {
        String sql = "UPDATE produto SET nome = ?, descricao = ?, preco = ?, unidade = ?, estoque = ? "
                   + "WHERE id = ?";
        try (Connection con = ConexaoDB.getConexao();
             PreparedStatement ps = con.prepareStatement(sql)) {
            preencher(ps, obj);
            ps.setInt(6, obj.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void excluir(int id) throws SQLException {
        try (Connection con = ConexaoDB.getConexao();
             PreparedStatement ps = con.prepareStatement("DELETE FROM produto WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (VIOLA_CHAVE_EST.equals(e.getSQLState())) {
                throw new SQLException("Este produto já foi vendido e não pode ser excluído.", e);
            }
            throw e;
        }
    }

    @Override
    public Produto buscarPorId(int id) throws SQLException {
        try (Connection con = ConexaoDB.getConexao();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM produto WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    @Override
    public List<Produto> listarTodos() throws SQLException {
        List<Produto> lista = new ArrayList<>();
        try (Connection con = ConexaoDB.getConexao();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM produto ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Preenche os 5 primeiros parâmetros (comuns ao INSERT e ao UPDATE). */
    private void preencher(PreparedStatement ps, Produto obj) throws SQLException {
        ps.setString(1, obj.getNome());
        ps.setString(2, obj.getDescricao());
        ps.setBigDecimal(3, obj.getPreco());
        ps.setString(4, obj.getUnidade().name());
        ps.setBigDecimal(5, obj.getEstoque());
    }

    /** Converte uma linha do ResultSet em objeto Produto. */
    private Produto mapear(ResultSet rs) throws SQLException {
        return new Produto(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("descricao"),
                rs.getBigDecimal("preco"),
                UnidadeMedida.valueOf(rs.getString("unidade")),
                rs.getBigDecimal("estoque"));
    }
}
