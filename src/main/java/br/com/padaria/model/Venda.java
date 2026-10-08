package br.com.padaria.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Venda {

    private int id;
    private LocalDateTime dataHora;
    private Funcionario funcionario;
    private final List<ItemVenda> itens = new ArrayList<>();

    public Venda() {
        this.dataHora = LocalDateTime.now();
    }

    public Venda(Funcionario funcionario) {
        this();
        this.funcionario = funcionario;
    }

    public void adicionarItem(ItemVenda item) {
        itens.add(item);
    }

    public void removerItem(int indice) {
        itens.remove(indice);
    }

    public BigDecimal getValorTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemVenda item : itens) {
            total = total.add(item.getSubtotal());
        }
        return total;
    }

    public boolean estaVazia() {
        return itens.isEmpty();
    }

    /** Lista somente leitura: itens entram/saem apenas pelos métodos acima. */
    public List<ItemVenda> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public Funcionario getFuncionario() { return funcionario; }
    public void setFuncionario(Funcionario funcionario) { this.funcionario = funcionario; }
}
