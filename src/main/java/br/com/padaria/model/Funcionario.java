package br.com.padaria.model;

public class Funcionario {

    private int id;
    private String nome;
    private String cargo;
    private String telefone;
    private String usuario;
    private String senha; // guarda o HASH, nunca a senha em texto puro

    public Funcionario() {
    }

    public Funcionario(int id, String nome, String cargo, String telefone,
                       String usuario, String senha) {
        this.id = id;
        this.nome = nome;
        this.cargo = cargo;
        this.telefone = telefone;
        this.usuario = usuario;
        this.senha = senha;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    @Override
    public String toString() {
        return nome; // facilita exibir em JComboBox/JLabel
    }
}
