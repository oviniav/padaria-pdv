package br.com.padaria.model;

/**
 * Unidade em que o produto é vendido.
 * UN = por unidade (quantidade inteira); KG = por peso (até 3 casas decimais).
 */
public enum UnidadeMedida {

    UN("Unidade", 0),
    KG("Quilo", 3);

    private final String descricao;
    private final int casasDecimais;

    UnidadeMedida(String descricao, int casasDecimais) {
        this.descricao = descricao;
        this.casasDecimais = casasDecimais;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getCasasDecimais() {
        return casasDecimais;
    }

    public boolean aceitaFracionado() {
        return casasDecimais > 0;
    }

    @Override
    public String toString() {
        return name() + " - " + descricao;
    }
}
