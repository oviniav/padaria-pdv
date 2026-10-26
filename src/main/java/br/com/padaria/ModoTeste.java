package br.com.padaria;

import br.com.padaria.model.Funcionario;
import br.com.padaria.util.Tema;
import br.com.padaria.view.TelaPrincipal;

// SÓ PARA TESTE: abre o menu sem login e sem banco.
public class ModoTeste {

    public static void main(String[] args) {
        Tema.aplicar();
        Funcionario teste = new Funcionario(0, "Usuário Teste", "Gerente", "", "teste", "");
        java.awt.EventQueue.invokeLater(() -> new TelaPrincipal(teste).setVisible(true));
    }
}