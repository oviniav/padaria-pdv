package br.com.padaria;

import br.com.padaria.view.TelaLogin;
import br.com.padaria.util.Tema;

public class Padaria {

    public static void main(String[] args) {
        Tema.aplicar();
        java.awt.EventQueue.invokeLater(() -> new TelaLogin().setVisible(true));
    }
}