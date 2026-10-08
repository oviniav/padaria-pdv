package br.com.padaria.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ItemVenda {

    private int id;
    private Produto produto;
    private BigDecimal quantidade;
    private BigDecimal precoUnitario; // preço "congelado" no momento da venda

    public ItemVenda() {
    }

    public ItemVenda(Produto produto, BigDecimal quantidade) {
        validarQuantidade(produto, quantidade);
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPreco();
    }

    /** Regras: quantidade > 0 e, se o produto é por unidade, sem casas decimais. */
    private static void validarQuantidade(Produto produto, BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        if (!produto.getUnidade().aceitaFracionado()
                && quantidade.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                    produto.getNome() + " é vendido por unidade: informe um número inteiro.");
        }
    }

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(quantidade).setScale(2, RoundingMode.HALF_UP);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }

    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }
}
