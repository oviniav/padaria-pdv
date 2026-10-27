package br.com.padaria.view;

import br.com.padaria.dao.FuncionarioDAO;
import br.com.padaria.dao.VendaDAO;
import br.com.padaria.model.Funcionario;
import br.com.padaria.model.ItemVenda;
import br.com.padaria.model.UnidadeMedida;
import br.com.padaria.model.Venda;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Histórico (log) das vendas: quando, quem vendeu, total e os itens de cada venda.
 *
 * @author vinicius.alberto
 */
public class TelaHistorico extends javax.swing.JFrame {

    private static final Locale BR = Locale.forLanguageTag("pt-BR");
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(BR);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final VendaDAO vendaDAO = new VendaDAO();
    private List<Venda> vendas = List.of();

    private final JTextField txtInicio = new JTextField(8);
    private final JTextField txtFim = new JTextField(8);
    private final JComboBox<Funcionario> cbFuncionario = new JComboBox<>();
    private final JButton btnFiltrar = new JButton("Filtrar");
    private final JLabel lblResumo = new JLabel(" ");

    private final DefaultTableModel modeloVendas = new DefaultTableModel(
            new Object[]{"Nº", "Data/Hora", "Funcionário", "Usuário", "Itens", "Total"}, 0) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false; // tabela só para visualizar
        }
    };
    private final DefaultTableModel modeloItens = new DefaultTableModel(
            new Object[]{"Produto", "Unid", "Qtd", "Preço", "Subtotal"}, 0) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };
    private final JTable tblVendas = new JTable(modeloVendas);
    private final JTable tblItens = new JTable(modeloItens);

    public TelaHistorico() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Padaria - Histórico de Vendas");
        montarTela();
        setSize(760, 560);
        setLocationRelativeTo(null);

        // padrão: do primeiro dia do mês até hoje
        LocalDate hoje = LocalDate.now();
        txtInicio.setText(hoje.withDayOfMonth(1).format(DATA));
        txtFim.setText(hoje.format(DATA));

        tblVendas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblVendas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarItens();
            }
        });
        btnFiltrar.addActionListener(e -> carregarVendas());
        txtInicio.addActionListener(e -> carregarVendas()); // Enter também filtra
        txtFim.addActionListener(e -> carregarVendas());

        carregarFuncionarios();
        carregarVendas();
    }

    private void montarTela() {
        JLabel lblTitulo = new JLabel("HISTÓRICO DE VENDAS");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.add(new JLabel("De:"));
        filtros.add(txtInicio);
        filtros.add(new JLabel("Até:"));
        filtros.add(txtFim);
        filtros.add(new JLabel("Funcionário:"));
        filtros.add(cbFuncionario);
        filtros.add(btnFiltrar);
        txtInicio.setToolTipText("dd/mm/aaaa");
        txtFim.setToolTipText("dd/mm/aaaa");

        JPanel topo = new JPanel(new BorderLayout());
        topo.add(lblTitulo, BorderLayout.NORTH);
        topo.add(filtros, BorderLayout.CENTER);

        JScrollPane spVendas = new JScrollPane(tblVendas);
        spVendas.setBorder(BorderFactory.createTitledBorder("Vendas"));
        JScrollPane spItens = new JScrollPane(tblItens);
        spItens.setBorder(BorderFactory.createTitledBorder("Itens da venda selecionada"));
        JSplitPane divisao = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spVendas, spItens);
        divisao.setResizeWeight(0.6);

        lblResumo.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel conteudo = new JPanel(new BorderLayout(0, 8));
        conteudo.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        conteudo.add(topo, BorderLayout.NORTH);
        conteudo.add(divisao, BorderLayout.CENTER);
        conteudo.add(lblResumo, BorderLayout.SOUTH);
        setContentPane(conteudo);
    }

    /** Preenche o filtro de funcionários; o primeiro item (id 0) significa "todos". */
    private void carregarFuncionarios() {
        DefaultComboBoxModel<Funcionario> modelo = new DefaultComboBoxModel<>();
        modelo.addElement(new Funcionario(0, "Todos", "", "", "", null));
        try {
            for (Funcionario f : new FuncionarioDAO().listarTodos()) {
                modelo.addElement(f);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar funcionários:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
        cbFuncionario.setModel(modelo);
    }

    private LocalDate lerData(JTextField campo, String nome) {
        try {
            return LocalDate.parse(campo.getText().trim(), DATA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida em \"" + nome + "\". Use dd/mm/aaaa.");
        }
    }

    /** Busca as vendas do período/funcionário e mostra na tabela. */
    private void carregarVendas() {
        modeloVendas.setRowCount(0);
        modeloItens.setRowCount(0);
        vendas = List.of();
        try {
            LocalDate inicio = lerData(txtInicio, "De");
            LocalDate fim = lerData(txtFim, "Até");
            if (fim.isBefore(inicio)) {
                throw new IllegalArgumentException("A data final é anterior à data inicial.");
            }
            Funcionario f = (Funcionario) cbFuncionario.getSelectedItem();
            vendas = vendaDAO.listarHistorico(inicio, fim, f == null ? 0 : f.getId());

            BigDecimal totalPeriodo = BigDecimal.ZERO;
            for (Venda v : vendas) {
                modeloVendas.addRow(new Object[]{
                    v.getId(),
                    v.getDataHora().format(DATA_HORA),
                    v.getFuncionario().getNome(),
                    v.getFuncionario().getUsuario(),
                    v.getItens().size(),
                    MOEDA.format(v.getValorTotal())
                });
                totalPeriodo = totalPeriodo.add(v.getValorTotal());
            }
            lblResumo.setText(vendas.size() + " venda(s)  |  Total do período: " + MOEDA.format(totalPeriodo));
        } catch (IllegalArgumentException ex) {
            lblResumo.setText(" ");
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Atenção", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            lblResumo.setText(" ");
            JOptionPane.showMessageDialog(this, "Erro ao carregar o histórico:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Mostra os itens da venda selecionada. */
    private void mostrarItens() {
        modeloItens.setRowCount(0);
        int linha = tblVendas.getSelectedRow();
        if (linha < 0) {
            return;
        }
        for (ItemVenda item : vendas.get(linha).getItens()) {
            UnidadeMedida un = item.getProduto().getUnidade();
            modeloItens.addRow(new Object[]{
                item.getProduto().getNome(),
                un.name(),
                formatarQuantidade(item.getQuantidade(), un),
                MOEDA.format(item.getPrecoUnitario()) + (un == UnidadeMedida.KG ? " /kg" : ""),
                MOEDA.format(item.getSubtotal())
            });
        }
    }

    /** KG com 3 casas (0,350); UN como inteiro (2). */
    private String formatarQuantidade(BigDecimal q, UnidadeMedida un) {
        return un == UnidadeMedida.KG
                ? String.format(BR, "%.3f", q)
                : q.stripTrailingZeros().toPlainString();
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new TelaHistorico().setVisible(true));
    }
}
