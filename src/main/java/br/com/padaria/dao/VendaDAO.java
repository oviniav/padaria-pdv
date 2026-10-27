package br.com.padaria.dao;

import br.com.padaria.model.Funcionario;
import br.com.padaria.model.ItemVenda;
import br.com.padaria.model.Produto;
import br.com.padaria.model.UnidadeMedida;
import br.com.padaria.model.Venda;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VendaDAO {

    public void registrar(Venda venda) throws SQLException {
        if (venda.estaVazia()) {
            throw new SQLException("A venda não tem itens.");
        }
        String sqlVenda = "INSERT INTO venda (data_hora, id_funcionario, valor_total) "
                + "VALUES (?, ?, ?)";
        String sqlItem = "INSERT INTO item_venda (id_venda, id_produto, quantidade, preco_unitario) "
                + "VALUES (?, ?, ?, ?)";
        String sqlEstoque = "UPDATE produto SET estoque = estoque - ? "
                + "WHERE id = ? AND estoque >= ?";

        try (Connection conn = ConexaoDB.getConexao()) {
            conn.setAutoCommit(false);
            try {
                // 1) Cabeçalho da venda, recuperando o id gerado pelo SERIAL
                int idVenda;
                try (PreparedStatement st = conn.prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS)) {
                    st.setTimestamp(1, Timestamp.valueOf(venda.getDataHora()));
                    st.setInt(2, venda.getFuncionario().getId());
                    st.setBigDecimal(3, venda.getValorTotal());
                    st.executeUpdate();
                    try (ResultSet rs = st.getGeneratedKeys()) {
                        rs.next();
                        idVenda = rs.getInt(1);
                    }
                }
                // 2) Itens e 3) baixa de estoque, item por item
                try (PreparedStatement stItem = conn.prepareStatement(sqlItem);
                        PreparedStatement stEstoque = conn.prepareCall(sqlEstoque)) {

                    for (ItemVenda item : venda.getItens()) {
                        stItem.setInt(1, idVenda);
                        stItem.setInt(2, item.getProduto().getId());
                        stItem.setBigDecimal(3, item.getQuantidade());
                        stItem.setBigDecimal(4, item.getPrecoUnitario());
                        stItem.executeUpdate();

                        stEstoque.setBigDecimal(1, item.getQuantidade());
                        stEstoque.setInt(2, item.getProduto().getId());
                        stEstoque.setBigDecimal(3, item.getQuantidade());
                        if (stEstoque.executeUpdate() == 0) {
                            // nenhuma linha atualizada = estoque insuficiente
                            throw new SQLException("Estoque insuficiente para" + item.getProduto().getNome() + ".");
                        }
                    }
                }
                conn.commit(); // tudo certo? confirma
                venda.setId(idVenda);
            } catch (SQLException e) {
                conn.rollback(); // algo falhou? desfaz tudo
                throw e;
            }
        }
    }

    /**
     * Histórico de vendas do período [inicio, fim], com o funcionário que fez
     * cada venda e os seus itens. idFuncionario = 0 traz de todos.
     */
    public List<Venda> listarHistorico(LocalDate inicio, LocalDate fim, int idFuncionario) throws SQLException {
        String sql = "SELECT v.id, v.data_hora, "
                + "f.id AS f_id, f.nome AS f_nome, f.cargo AS f_cargo, f.telefone AS f_telefone, f.usuario AS f_usuario, "
                + "i.id AS i_id, i.quantidade, i.preco_unitario, "
                + "p.id AS p_id, p.nome AS p_nome, p.descricao, p.preco, p.unidade, p.estoque "
                + "FROM venda v "
                + "JOIN funcionario f ON f.id = v.id_funcionario "
                + "JOIN item_venda i ON i.id_venda = v.id "
                + "JOIN produto p ON p.id = i.id_produto "
                + "WHERE v.data_hora >= ? AND v.data_hora < ? "
                + (idFuncionario > 0 ? "AND v.id_funcionario = ? " : "")
                + "ORDER BY v.data_hora DESC, v.id DESC, i.id";

        // LinkedHashMap mantém a ordem da consulta (mais recentes primeiro)
        Map<Integer, Venda> vendas = new LinkedHashMap<>();
        try (Connection conn = ConexaoDB.getConexao();
                PreparedStatement st = conn.prepareStatement(sql)) {
            st.setTimestamp(1, Timestamp.valueOf(inicio.atStartOfDay()));
            st.setTimestamp(2, Timestamp.valueOf(fim.plusDays(1).atStartOfDay())); // inclui o dia final inteiro
            if (idFuncionario > 0) {
                st.setInt(3, idFuncionario);
            }
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    // cada linha é um item; a venda só é criada na primeira linha dela
                    Venda venda = vendas.get(rs.getInt("id"));
                    if (venda == null) {
                        venda = new Venda(new Funcionario(
                                rs.getInt("f_id"),
                                rs.getString("f_nome"),
                                rs.getString("f_cargo"),
                                rs.getString("f_telefone"),
                                rs.getString("f_usuario"),
                                null)); // hash da senha não é necessário aqui
                        venda.setId(rs.getInt("id"));
                        venda.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());
                        vendas.put(venda.getId(), venda);
                    }
                    Produto produto = new Produto(
                            rs.getInt("p_id"),
                            rs.getString("p_nome"),
                            rs.getString("descricao"),
                            rs.getBigDecimal("preco"),
                            UnidadeMedida.valueOf(rs.getString("unidade")),
                            rs.getBigDecimal("estoque"));
                    ItemVenda item = new ItemVenda();
                    item.setId(rs.getInt("i_id"));
                    item.setProduto(produto);
                    item.setQuantidade(rs.getBigDecimal("quantidade"));
                    item.setPrecoUnitario(rs.getBigDecimal("preco_unitario")); // preço da época da venda
                    venda.adicionarItem(item);
                }
            }
        }
        return new ArrayList<>(vendas.values());
    }

}
