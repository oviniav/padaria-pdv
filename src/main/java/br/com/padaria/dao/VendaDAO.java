package br.com.padaria.dao;

import br.com.padaria.model.ItemVenda;
import br.com.padaria.model.Venda;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class VendaDAO {
    
    public void registrar(Venda venda) throws SQLException {
        if(venda.estaVazia()) {
            throw new SQLException("A venda não tem itens.");
        }
        String sqlVenda = "INSERT INTO venda (data_hora, id_funcionario, valor_total)"
                + "VALUES (?, ?, ?)";
        String sqlItem = "INSERT INTO item_venda (id_venda, id_produto, quantidade, preço_unitario)"
                + "VALUES (?, ?, ?)";
        String sqlEstoque = "UPDATE produto SET estoque = estoque - ?"
                + "WHERE id = ? AND estoque >= ?";
        
        try (Connection conn = ConexaoDB.getConexao()) {
            conn.setAutoCommit(false);
            try{
            // 1) Cabeçalho da venda, recuperando o id gerado pelo SERIAL
            int idVenda;
            try (PreparedStatement st = conn.prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS)){
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
                    stItem.setBigDecimal(3,item.getQuantidade());
                    stItem.setBigDecimal(4, item.getPrecoUnitario());
                    stItem.executeUpdate();
                    
                    stEstoque.setBigDecimal(1, item.getQuantidade());
                    stEstoque.setInt(2, item.getProduto().getId());
                    stEstoque.setBigDecimal(3, item.getQuantidade());
                    if(stEstoque.executeUpdate() == 0) {
                        //nenhuma linha atualizada = estoque insuficiente 
                        throw new SQLException("Estoque insuficiente para" + item.getProduto().getNome() + ".");
                    }
                }
            }
            conn.commit(); //tudo certo? confirma
            venda.setId(idVenda);
            } catch (SQLException e) {
                conn.rollback(); //algo falhou? desfaz tudo
                throw e;
            }
        }
    }
    
}
