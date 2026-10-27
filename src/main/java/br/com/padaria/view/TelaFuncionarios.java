/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package br.com.padaria.view;

import br.com.padaria.dao.FuncionarioDAO;
import br.com.padaria.model.Funcionario;
import br.com.padaria.util.Permissao;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author vinicius.alberto
 */
public class TelaFuncionarios extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(TelaFuncionarios.class.getName());

    /**
     * Creates new form TelaFuncionarios
     */
    private final FuncionarioDAO dao = new FuncionarioDAO();
    private final Funcionario funcionarioLogado; //nao pode excluir a propria conta
    private List<Funcionario> funcionarios = List.of();
    private int idSelecionado = 0; //cadastro de um funcionario novo
    
    private final DefaultTableModel modelo = new DefaultTableModel(
        new Object[]{"Cód", "Nome", "Cargo", "Telefone", "Usuario"},0){
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false; //tab so para vizu
            }
     };
    public TelaFuncionarios(Funcionario funcionarioLogado) {
        this.funcionarioLogado = funcionarioLogado;
        initComponents();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Padaria - Funcionarios");
        setLocationRelativeTo(null);
        
        tblFuncionarios.setModel(modelo);
        txtCargo.setToolTipText("Cargos: " + String.join(", ", Permissao.CARGOS));
        txtSenha.setToolTipText("Ao editar, deixe a senha em branco para manter a senha atual");
        
        tblFuncionarios.getSelectionModel().addListSelectionListener(e->{
            if(!e.getValueIsAdjusting()) {
                preencherCampos();
            }
    });
    
    btnNovo.addActionListener(e -> limparCampos());
    btnSalvar.addActionListener(e -> salvar());
    btnExcluir.addActionListener(e -> excluir());
    carregarTabela();
    }
    
    //busca os funcionarios no banco e mostra na tabela
    private void carregarTabela() {
        modelo.setRowCount(0);
        try {
            funcionarios = dao.listarTodos();
            for (Funcionario f : funcionarios) {
                modelo.addRow(new Object[] {
                    f.getId(), f.getNome(), f.getCargo(), f.getTelefone(), f.getUsuario()
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar funcionarios: \n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    //copia o funcionario da linha selecionada para o campo (senha nunca exibe)
    private void preencherCampos(){
        int linha = tblFuncionarios.getSelectedRow();
        if(linha<0) {
            return;
        }
        Funcionario f = funcionarios.get(linha);
        idSelecionado = f.getId();
        txtNome.setText(f.getNome());
        txtCargo.setText(f.getCargo());
        txtTelefone.setText(f.getTelefone());
        txtUsuario.setText(f.getUsuario());
        txtSenha.setText(""); //em branco = mantem a senha atual
    }
    //limpa os campos
    private void limparCampos(){
        idSelecionado = 0;
        txtNome.setText("");
        txtCargo.setText("");
        txtTelefone.setText("");
        txtUsuario.setText("");
        txtSenha.setText("");
        tblFuncionarios.clearSelection();
        txtNome.requestFocus();
    }
    
    //insere ou atualiza
    private void salvar() {
        String nome = txtNome.getText().trim();
        String cargo = txtCargo.getText().trim();
        String usuario = txtUsuario.getText().trim();
        String senha = new String(txtSenha.getPassword());
        
        if (nome.isEmpty() || cargo.isEmpty() || usuario.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha nome, cargo e usuário.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        //o acesso depende do cargo, entao so aceita os cargos conhecidos
        String cargoOk = Permissao.cargoValido(cargo);
        if (cargoOk == null) {
            JOptionPane.showMessageDialog(this, "Cargo inválido. Use um destes: "
                    + String.join(", ", Permissao.CARGOS) + ".", "Atenção", JOptionPane.WARNING_MESSAGE);
            txtCargo.requestFocus();
            return;
        }
        cargo = cargoOk;
        //funcionario novo precisa de senha (na edicao, vazia = mantem a atual)
        if (idSelecionado == 0 && senha.isBlank()) {
            JOptionPane.showMessageDialog(this, "Informe a senha do novo funcionário.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            txtSenha.requestFocus();
            return;
        }
        Funcionario f = new Funcionario(idSelecionado, nome, cargo, txtTelefone.getText().trim(), usuario, senha);
        try {
            if (idSelecionado == 0) {
                dao.inserir(f);
            }else {
                dao.atualizar(f); //senha vazia
            }
            JOptionPane.showMessageDialog(this, "Funcionario salvo com sucesso!");
            carregarTabela();
            limparCampos();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: \n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    //Exclui funcionario
    private void excluir() {
        if(idSelecionado == 0) {
            JOptionPane.showMessageDialog(this, "Selecione um funcionário na tabela.");
            return;
        }
        if (idSelecionado == funcionarioLogado.getId()) {
            JOptionPane.showMessageDialog(this, "Você não pode excluir o próprio usuário.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int resposta = JOptionPane.showConfirmDialog(this, "Excluir o funcionário \"" + txtNome.getText() + "\"?",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION);
        if(resposta != JOptionPane.YES_OPTION) {
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
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitulo = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtNome = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        txtCargo = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        txtTelefone = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txtUsuario = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        txtSenha = new javax.swing.JPasswordField();
        btnNovo = new javax.swing.JButton();
        btnSalvar = new javax.swing.JButton();
        btnExcluir = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblFuncionarios = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        lblTitulo.setText("CADASTRO DE FUNCIONARIOS");

        jLabel2.setText("Nome: ");

        jLabel3.setText("Cargo: ");

        jLabel4.setText("Telefone: ");

        jLabel5.setText("Usuario: ");

        jLabel6.setText("Senha: ");

        btnNovo.setText("Novo");

        btnSalvar.setText("Salvar");

        btnExcluir.setText("Excluir");

        tblFuncionarios.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(tblFuncionarios);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(lblTitulo))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(42, 42, 42)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 470, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(btnNovo)
                                    .addGap(52, 52, 52)
                                    .addComponent(btnSalvar)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(btnExcluir))
                                .addGroup(layout.createSequentialGroup()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel2)
                                        .addComponent(jLabel3)
                                        .addComponent(jLabel4)
                                        .addComponent(jLabel5)
                                        .addComponent(jLabel6))
                                    .addGap(31, 31, 31)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txtCargo, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtNome, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtTelefone, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                            .addComponent(txtSenha, javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(txtUsuario, javax.swing.GroupLayout.Alignment.LEADING))))))))
                .addContainerGap(70, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {txtCargo, txtNome});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {txtTelefone, txtUsuario});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addComponent(lblTitulo)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtNome, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(23, 23, 23)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtCargo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtTelefone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(txtUsuario, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(txtSenha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(28, 28, 28)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnNovo)
                    .addComponent(btnSalvar)
                    .addComponent(btnExcluir))
                .addGap(32, 32, 32)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(37, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
    
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
        Funcionario teste = new Funcionario(0, "Teste", "Gerente", "", "teste", "");
        java.awt.EventQueue.invokeLater(() -> new TelaFuncionarios(teste).setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnExcluir;
    private javax.swing.JButton btnNovo;
    private javax.swing.JButton btnSalvar;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JTable tblFuncionarios;
    private javax.swing.JTextField txtCargo;
    private javax.swing.JTextField txtNome;
    private javax.swing.JPasswordField txtSenha;
    private javax.swing.JTextField txtTelefone;
    private javax.swing.JTextField txtUsuario;
    // End of variables declaration//GEN-END:variables
}
