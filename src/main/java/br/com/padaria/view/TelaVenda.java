package br.com.padaria.view;

import br.com.padaria.dao.ProdutoDAO;
import br.com.padaria.dao.VendaDAO;
import br.com.padaria.model.Funcionario;
import br.com.padaria.model.ItemVenda;
import br.com.padaria.model.Produto;
import br.com.padaria.model.UnidadeMedida;
import br.com.padaria.model.Venda;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author vinicius.alberto
 */
public class TelaVenda extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(TelaVenda.class.getName());

    private static final Locale BR = Locale.forLanguageTag("pt-BR");
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(BR);

    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final VendaDAO vendaDAO = new VendaDAO();
    private final Funcionario funcionarioLogado;
    private Venda venda;
    private List<Produto> produtos = new ArrayList<>(); // mesma ordem do combo

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Produto", "Unid", "Qtd/Peso", "Preço", "Subtotal"}, 0) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false; // carrinho só para visualizar
        }
    };

    /**
     * Abre a tela de venda para o funcionário logado.
     */
    public TelaVenda(Funcionario funcionarioLogado) {
        initComponents();
        this.funcionarioLogado = funcionarioLogado;
        this.venda = new Venda(funcionarioLogado);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Padaria - Nova Venda");
        setLocationRelativeTo(null);

        lblOperador.setText("Operador: " + funcionarioLogado.getNome());
        tblItens.setModel(modelo);

        cbProduto.addActionListener(e -> atualizarProdutoSelecionado());
        btnAdicionar.addActionListener(e -> adicionarItem());
        txtQuantidade.addActionListener(e -> adicionarItem()); // Enter no campo também adiciona

        carregarProdutos();
        atualizarCarrinho();
    }

    /** Busca os produtos no banco e coloca no combo só os que têm estoque. */
    private void carregarProdutos() {
        produtos = new ArrayList<>();
        DefaultComboBoxModel<String> itensCombo = new DefaultComboBoxModel<>();
        try {
            for (Produto p : produtoDAO.listarTodos()) {
                if (p.getEstoque().signum() > 0) {
                    produtos.add(p);
                    itensCombo.addElement(p.toString());
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar produtos:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
        cbProduto.setModel(itensCombo);
        atualizarProdutoSelecionado();
    }

    /** Produto escolhido no combo (ou null se a lista estiver vazia). */
    private Produto produtoSelecionado() {
        int i = cbProduto.getSelectedIndex();
        return i >= 0 ? produtos.get(i) : null;
    }

    /** Mostra o preço e troca "Quantidade" por "Peso (kg)" conforme o produto. */
    private void atualizarProdutoSelecionado() {
        Produto p = produtoSelecionado();
        if (p == null) {
            lblPreco.setText("");
            lblQuantidade.setText("Quantidade:");
            return;
        }
        boolean porPeso = p.getUnidade() == UnidadeMedida.KG;
        lblQuantidade.setText(porPeso ? "Peso (kg):" : "Quantidade:");
        lblPreco.setText(MOEDA.format(p.getPreco()) + (porPeso ? " / kg" : " / un"));
        txtQuantidade.setText(porPeso ? "" : "1");
        txtQuantidade.requestFocus();
    }

    /** Valida a quantidade, confere o estoque e coloca o item no carrinho. */
    private void adicionarItem() {
        Produto p = produtoSelecionado();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Selecione um produto.");
            return;
        }

        BigDecimal quantidade;
        try {
            quantidade = new BigDecimal(txtQuantidade.getText().trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida. Exemplos: 2 ou 0,350");
            return;
        }
        if (quantidade.stripTrailingZeros().scale() > 3) {
            JOptionPane.showMessageDialog(this, "Use no máximo 3 casas decimais (ex.: 0,350 kg).");
            return;
        }

        // Soma o que já está no carrinho deste produto antes de conferir o estoque
        BigDecimal jaNoCarrinho = BigDecimal.ZERO;
        for (ItemVenda item : venda.getItens()) {
            if (item.getProduto().getId() == p.getId()) {
                jaNoCarrinho = jaNoCarrinho.add(item.getQuantidade());
            }
        }
        if (!p.temEstoque(jaNoCarrinho.add(quantidade))) {
            BigDecimal disponivel = p.getEstoque().subtract(jaNoCarrinho);
            JOptionPane.showMessageDialog(this, "Estoque insuficiente. Disponível: "
                    + formatarQuantidade(disponivel, p.getUnidade()) + " " + p.getUnidade().name());
            return;
        }

        try {
            venda.adicionarItem(new ItemVenda(p, quantidade)); // ItemVenda valida inteiro x fracionado
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            return;
        }
        atualizarCarrinho();
        atualizarProdutoSelecionado(); // limpa o campo e volta o foco para ele
    }

    /** Redesenha a tabela do carrinho e o total. */
    private void atualizarCarrinho() {
        modelo.setRowCount(0);
        for (ItemVenda item : venda.getItens()) {
            UnidadeMedida un = item.getProduto().getUnidade();
            modelo.addRow(new Object[]{
                item.getProduto().getNome(),
                un.name(),
                formatarQuantidade(item.getQuantidade(), un),
                MOEDA.format(item.getPrecoUnitario()) + (un == UnidadeMedida.KG ? " /kg" : ""),
                MOEDA.format(item.getSubtotal())
            });
        }
        lblTotal.setText(MOEDA.format(venda.getValorTotal()));
    }

    /** KG com 3 casas (0,350); UN como inteiro (2). */
    private String formatarQuantidade(BigDecimal q, UnidadeMedida un) {
        return un == UnidadeMedida.KG
                ? String.format(BR, "%.3f", q)
                : q.stripTrailingZeros().toPlainString();
    }

    private void removerItem() {
        int linha = tblItens.getSelectedRow();
        if (linha < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um item na tabela para remover.");
            return;
        }
        venda.removerItem(linha);
        atualizarCarrinho();
    }

    private void finalizarVenda() {
        if (venda.estaVazia()) {
            JOptionPane.showMessageDialog(this, "Adicione pelo menos um item.");
            return;
        }
        int resposta = JOptionPane.showConfirmDialog(this,
                "Finalizar a venda no valor de " + MOEDA.format(venda.getValorTotal()) + "?",
                "Confirmar venda", JOptionPane.YES_NO_OPTION);
        if (resposta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            vendaDAO.registrar(venda);
            JOptionPane.showMessageDialog(this, "Venda nº " + venda.getId() + " registrada com sucesso!");
            venda = new Venda(funcionarioLogado); // começa uma venda nova
            carregarProdutos();                   // estoque mudou
            atualizarCarrinho();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao registrar a venda:\n" + ex.getMessage()
                    + "\n\nNada foi gravado.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelarVenda() {
        if (!venda.estaVazia()) {
            int resposta = JOptionPane.showConfirmDialog(this,
                    "Descartar os itens desta venda?", "Cancelar venda", JOptionPane.YES_NO_OPTION);
            if (resposta != JOptionPane.YES_OPTION) {
                return;
            }
        }
        dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitulo = new javax.swing.JLabel();
        lblOperador = new javax.swing.JLabel();
        lblProduto = new javax.swing.JLabel();
        cbProduto = new javax.swing.JComboBox<>();
        lblPreco = new javax.swing.JLabel();
        lblQuantidade = new javax.swing.JLabel();
        txtQuantidade = new javax.swing.JTextField();
        btnAdicionar = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblItens = new javax.swing.JTable();
        btnRemover = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        lblTotal = new javax.swing.JLabel();
        btnCancelar = new javax.swing.JButton();
        btnFinalizar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblTitulo.setText("NOVA VENDA");

        lblOperador.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblOperador.setText("Operador : ");

        lblProduto.setText("Produto : ");

        cbProduto.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        lblPreco.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblPreco.setText("R$ 0,00");

        lblQuantidade.setText("Quantidade : ");

        txtQuantidade.setColumns(8);

        btnAdicionar.setText("Adicionar");

        tblItens.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(tblItens);

        btnRemover.setText("Remover Item");
        btnRemover.addActionListener(this::btnRemoverActionPerformed);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("TOTAL : ");

        lblTotal.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblTotal.setText("R$ 0,00");

        btnCancelar.setText("Cancelar");
        btnCancelar.addActionListener(this::btnCancelarActionPerformed);

        btnFinalizar.setText("Finalizar Venda");
        btnFinalizar.addActionListener(this::btnFinalizarActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblTitulo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblOperador)
                        .addGap(2, 2, 2))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblProduto)
                            .addComponent(lblQuantidade))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(btnAdicionar))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtQuantidade, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(cbProduto, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(26, 26, 26)
                                        .addComponent(lblPreco)))
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addGap(236, 236, 236))
            .addGroup(layout.createSequentialGroup()
                .addGap(99, 99, 99)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(74, 74, 74)
                                .addComponent(lblTotal)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnCancelar)
                        .addGap(39, 39, 39)
                        .addComponent(btnFinalizar))
                    .addComponent(btnRemover)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 712, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(151, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTitulo)
                    .addComponent(lblOperador))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(49, 49, 49)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblQuantidade)
                            .addComponent(txtQuantidade, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cbProduto, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblProduto)
                            .addComponent(lblPreco))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnAdicionar)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnRemover)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 68, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(lblTotal)
                    .addComponent(btnCancelar)
                    .addComponent(btnFinalizar))
                .addGap(28, 28, 28))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnRemoverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoverActionPerformed
        removerItem();
    }//GEN-LAST:event_btnRemoverActionPerformed

    private void btnCancelarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarActionPerformed
        cancelarVenda();
    }//GEN-LAST:event_btnCancelarActionPerformed

    private void btnFinalizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFinalizarActionPerformed
        finalizarVenda();
    }//GEN-LAST:event_btnFinalizarActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdicionar;
    private javax.swing.JButton btnCancelar;
    private javax.swing.JButton btnFinalizar;
    private javax.swing.JButton btnRemover;
    private javax.swing.JComboBox<String> cbProduto;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblOperador;
    private javax.swing.JLabel lblPreco;
    private javax.swing.JLabel lblProduto;
    private javax.swing.JLabel lblQuantidade;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JLabel lblTotal;
    private javax.swing.JTable tblItens;
    private javax.swing.JTextField txtQuantidade;
    // End of variables declaration//GEN-END:variables
}
