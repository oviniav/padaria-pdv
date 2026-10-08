package br.com.padaria;

import br.com.padaria.view.TelaLogin;

public class Padaria {

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new TelaLogin().setVisible(true));
    }
}