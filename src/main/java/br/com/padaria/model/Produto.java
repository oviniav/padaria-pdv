package br.com.padaria.model;

import java.math.BigDecimal;

public class Produto {

    private int id;
    private String nome;
    private String descricao;
    private BigDecimal preco;          // preço por unidade ou por quilo
    private UnidadeMedida unidade;
    private BigDecimal estoque;        // UN: inteiro / KG: fracionado

    public Produto() {
        this.preco = BigDecimal.ZERO;
        this.estoque = BigDecimal.ZERO;
        this.unidade = UnidadeMedida.UN;
    }

    public Produto(int id, String nome, String descricao, BigDecimal preco,
                   UnidadeMedida unidade, BigDecimal estoque) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.unidade = unidade;
        this.estoque = estoque;
    }

    /** Verifica se há estoque suficiente para a quantidade pedida. */
    public boolean temEstoque(BigDecimal quantidade) {
        return estoque.compareTo(quantidade) >= 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    public UnidadeMedida getUnidade() { return unidade; }
    public void setUnidade(UnidadeMedida unidade) { this.unidade = unidade; }

    public BigDecimal getEstoque() { return estoque; }
    public void setEstoque(BigDecimal estoque) { this.estoque = estoque; }

    @Override
    public String toString() {
        return nome + " (" + unidade.name() + ")";
    }
}
