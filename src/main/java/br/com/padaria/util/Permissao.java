package br.com.padaria.util;

import br.com.padaria.model.Funcionario;

/**
 * Regras de acesso por cargo. Para mudar o que um cargo pode fazer,
 * muda-se só aqui: as telas apenas perguntam "pode ou não pode?".
 *
 * O Gerente é o superusuário: pode tudo, sem precisar aparecer em cada regra.
 * Os demais cargos só podem o que estiver listado no método correspondente.
 */
public final class Permissao {

    public static final String GERENTE = "Gerente";
    public static final String CAIXA = "Caixa";
    public static final String PADEIRO = "Padeiro";
    public static final String ESTOQUISTA = "Estoquista";

    /** Lista fixa de cargos, usada no combo da tela de funcionários. */
    public static final String[] CARGOS = { GERENTE, CAIXA, PADEIRO, ESTOQUISTA };

    private Permissao() {
    }

    /** Superusuário: o Gerente tem acesso a tudo. */
    public static boolean isGerente(Funcionario f) {
        return f != null && f.getCargo() != null && GERENTE.equalsIgnoreCase(f.getCargo().trim());
    }

    /**
     * true se o funcionário for Gerente ou se o cargo dele for um dos
     * informados (ignora maiúsculas/espaços).
     */
    private static boolean temCargo(Funcionario f, String... cargos) {
        if (isGerente(f)) {
            return true;
        }
        if (f == null || f.getCargo() == null) {
            return false;
        }
        String cargo = f.getCargo().trim();
        for (String c : cargos) {
            if (c.equalsIgnoreCase(cargo)) {
                return true;
            }
        }
        return false;
    }

    /** Devolve o cargo escrito como na lista CARGOS, ou null se não for um cargo válido. */
    public static String cargoValido(String cargo) {
        if (cargo != null) {
            for (String c : CARGOS) {
                if (c.equalsIgnoreCase(cargo.trim())) {
                    return c;
                }
            }
        }
        return null;
    }

    public static boolean podeVender(Funcionario f) {
        return temCargo(f, CAIXA);
    }

    public static boolean podeEditarProdutos(Funcionario f) {
        return temCargo(f, PADEIRO, ESTOQUISTA);
    }

    public static boolean podeExcluirProdutos(Funcionario f) {
        return temCargo(f, ESTOQUISTA);
    }

    public static boolean podeVerHistorico(Funcionario f) {
        return temCargo(f); // só o Gerente
    }

    public static boolean podeGerenciarFuncionarios(Funcionario f) {
        return temCargo(f); // só o Gerente
    }
}
