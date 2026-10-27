package br.com.padaria.view;

import br.com.padaria.dao.ProdutoDAO;
import br.com.padaria.model.Funcionario;
import br.com.padaria.model.Produto;
import br.com.padaria.model.UnidadeMedida;
import br.com.padaria.util.Permissao;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author vinicius.alberto
 */
public class TelaProdutos extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(TelaProdutos.class.getName());

    private final ProdutoDAO dao = new ProdutoDAO();
    private List<Produto> produtos = List.of();
    private int idSelecionado = 0; // 0 = cadastrando um produto novo

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Cód", "Nome", "Preço", "Unid", "Estoque"}, 0) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false; // tabela só para visualizar
        }
    };

    /**
     * Creates new form TelaProdutos
     */
    public TelaProdutos(Funcionario funcionarioLogado) {
        initComponents();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Padaria - Produtos");
        setLocationRelativeTo(null);

        cbUnidade.setModel(new DefaultComboBoxModel<>(new String[]{"UN", "KG"}));
        tblProdutos.setModel(modelo);

        // Clicou numa linha da tabela -> preenche os campos para editar
        tblProdutos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                preencherCampos();
            }
        });

        // O botão Novo usa o evento criado pelo Design (btnNovoActionPerformed)
        btnSalvar.addActionListener(e -> salvar());
        btnExcluir.addActionListener(e -> excluir());

        // Excluir depende do cargo (ex.: Padeiro edita, mas não exclui)
        btnExcluir.setEnabled(Permissao.podeExcluirProdutos(funcionarioLogado));

        carregarTabela();
    }

    /** Busca os produtos no banco e mostra na tabela. */
    private void carregarTabela() {
        modelo.setRowCount(0);
        try {
            produtos = dao.listarTodos();
            for (Produto p : produtos) {
                modelo.addRow(new Object[]{
                    p.getId(),
                    p.getNome(),
                    p.getPreco(),
                    p.getUnidade().name(),
                    p.getEstoque()
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar produtos:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Copia o produto da linha selecionada para os campos. */
    private void preencherCampos() {
        int linha = tblProdutos.getSelectedRow();
        if (linha < 0) {
            return;
        }
        Produto p = produtos.get(linha);
        idSelecionado = p.getId();
        txtNome.setText(p.getNome());
        txtDescricao.setText(p.getDescricao());
        txtPreco.setText(p.getPreco().toPlainString().replace('.', ','));
        cbUnidade.setSelectedItem(p.getUnidade().name());
        txtEstoque.setText(p.getEstoque().stripTrailingZeros().toPlainString().replace('.', ','));
    }

    /** Limpa os campos para cadastrar um produto novo. */
    private void limparCampos() {
        idSelecionado = 0;
        txtNome.setText("");
        txtDescricao.setText("");
        txtPreco.setText("");
        txtEstoque.setText("");
        cbUnidade.setSelectedIndex(0);
        tblProdutos.clearSelection();
        txtNome.requestFocus();
    }

    /** Converte "16,90" ou "16.90" em BigDecimal. */
    private BigDecimal lerNumero(String texto, String campo) {
        try {
            return new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido no campo " + campo + ".");
        }
    }

    /** Insere (se for novo) ou atualiza (se veio da tabela). */
    private void salvar() {
        try {
            String nome = txtNome.getText().trim();
            if (nome.isEmpty()) {
                throw new IllegalArgumentException("Informe o nome do produto.");
            }
            BigDecimal preco = lerNumero(txtPreco.getText(), "Preço");
            BigDecimal estoque = lerNumero(txtEstoque.getText(), "Estoque");
            if (preco.signum() < 0 || estoque.signum() < 0) {
                throw new IllegalArgumentException("Preço e estoque não podem ser negativos.");
            }
            UnidadeMedida unidade = UnidadeMedida.valueOf((String) cbUnidade.getSelectedItem());
            if (!unidade.aceitaFracionado() && estoque.stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException("Produto vendido por unidade: o estoque deve ser inteiro.");
            }

            Produto p = new Produto(idSelecionado, nome, txtDescricao.getText().trim(),
                    preco, unidade, estoque);

            if (idSelecionado == 0) {
                dao.inserir(p);
            } else {
                dao.atualizar(p);
            }
            JOptionPane.showMessageDialog(this, "Produto salvo com sucesso!");
            carregarTabela();
            limparCampos();

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Atenção", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Exclui o produto selecionado, após confirmação. */
    private void excluir() {
        if (idSelecionado == 0) {
            JOptionPane.showMessageDialog(this, "Selecione um produto na tabela.");
            return;
        }
        int resposta = JOptionPane.showConfirmDialog(this,
                "Excluir o produto \"" + txtNome.getText() + "\"?",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION);
        if (resposta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            dao.excluir(idSelecionado);
            carregarTabela();
            limparCampos();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao excluir:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        lblTitulo = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtNome = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        txtDescricao = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        txtPreco = new javax.swing.JTextField();
        btnNovo = new javax.swing.JButton();
        btnSalvar = new javax.swing.JButton();
        btnExcluir = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        txtEstoque = new javax.swing.JTextField();
        cbUnidade = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblProdutos = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        lblTitulo.setText("Cadastro de Produtos");

        jLabel2.setText("Nome: ");

        txtNome.addActionListener(this::txtNomeActionPerformed);

        jLabel1.setText("Descrição: ");

        jLabel3.setText("Preço: ");

        btnNovo.setText("Novo");
        btnNovo.addActionListener(this::btnNovoActionPerformed);

        btnSalvar.setText("Salvar");

        btnExcluir.setText("Excluir");

        jLabel8.setText("Unidade: ");

        jLabel9.setText("Estoque: ");

        cbUnidade.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        tblProdutos.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(tblProdutos);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(lblTitulo)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel1)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel9))
                                .addGap(28, 28, 28)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                            .addComponent(txtEstoque, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 183, Short.MAX_VALUE)
                                            .addComponent(txtPreco, javax.swing.GroupLayout.Alignment.LEADING))
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addComponent(txtDescricao)
                                    .addComponent(txtNome)))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(33, 33, 33)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel8)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(btnNovo)
                                        .addGap(47, 47, 47)
                                        .addComponent(btnSalvar)
                                        .addGap(53, 53, 53)
                                        .addComponent(btnExcluir)))
                                .addGap(18, 18, 18)
                                .addComponent(cbUnidade, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 586, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(92, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel2, jLabel3});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(lblTitulo)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(txtNome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(txtDescricao, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(txtPreco, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(cbUnidade, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(2, 2, 2)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(txtEstoque, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(38, 38, 38)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnNovo)
                    .addComponent(btnSalvar)
                    .addComponent(btnExcluir))
                .addGap(34, 34, 34)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 217, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(22, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>                        

    private void txtNomeActionPerformed(java.awt.event.ActionEvent evt) {                                        
        // TODO add your handling code here:
    }                                       

    private void btnNovoActionPerformed(java.awt.event.ActionEvent evt) {                                        
        limparCampos();
    }                                       

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        Funcionario teste = new Funcionario(0, "Teste", Permissao.GERENTE, "", "teste", "");
        java.awt.EventQueue.invokeLater(() -> new TelaProdutos(teste).setVisible(true));
    }

    // Variables declaration - do not modify                     
    private javax.swing.JButton btnExcluir;
    private javax.swing.JButton btnNovo;
    private javax.swing.JButton btnSalvar;
    private javax.swing.JComboBox<String> cbUnidade;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JTable tblProdutos;
    private javax.swing.JTextField txtDescricao;
    private javax.swing.JTextField txtEstoque;
    private javax.swing.JTextField txtNome;
    private javax.swing.JTextField txtPreco;
                    
}