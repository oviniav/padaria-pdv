# Padaria PDV 🥖

Sistema de **Ponto de Venda (PDV)** para padaria, feito em **Java Swing** com banco de dados **PostgreSQL**.
Trabalho prático da faculdade (UNIP).

Permite cadastrar produtos e funcionários, registrar vendas por **unidade ou por peso (kg)** com baixa automática de estoque, e consultar o histórico de vendas, com **controle de acesso por cargo**.

## ✨ Funcionalidades

- **Login** com senha armazenada como hash SHA-256 (nunca em texto puro)
- **Permissões por cargo:** cada funcionário só vê as telas que o cargo dele permite
- **Venda (PDV):** carrinho com vários itens, venda por unidade ou por quilo (até 3 casas decimais, ex.: 0,350 kg), conferência de estoque e total em tempo real
- **Registro da venda em transação:** cabeçalho, itens e baixa de estoque são gravados juntos; se algo falhar (ex.: estoque insuficiente), tudo é desfeito
- **Preço congelado:** cada item guarda o preço do momento da venda, então mudar o preço depois não altera vendas antigas
- **Produtos:** cadastro, edição e exclusão
- **Funcionários:** cadastro, edição e exclusão (o usuário logado não pode se excluir)
- **Histórico de vendas:** filtro por período e por funcionário, com os itens de cada venda
- **Visual moderno** com FlatLaf

## 👥 Permissões por cargo

| Tela / ação | Gerente | Caixa | Padeiro | Estoquista |
|---|:---:|:---:|:---:|:---:|
| Nova venda | ✅ | ✅ | | |
| Cadastrar/editar produtos | ✅ | | ✅ | ✅ |
| Excluir produtos | ✅ | | | ✅ |
| Histórico de vendas | ✅ | | | |
| Funcionários | ✅ | | | |

As regras ficam centralizadas na classe `Permissao`; as telas só perguntam "pode ou não pode?".

## 🏗️ Arquitetura

Organizado em camadas (Model, DAO e View):

```
src/main/java/br/com/padaria/
├── Padaria.java          # Ponto de entrada (abre a tela de login)
├── ModoTeste.java        # Abre o menu sem login e sem banco, só para testar as telas
├── model/                # Produto, Funcionario, Venda, ItemVenda, UnidadeMedida
├── dao/                  # Acesso ao banco via JDBC (CrudDAO<T> genérico + DAOs)
├── view/                 # Telas Swing: Login, Principal, Venda, Produtos, Funcionarios, Historico
└── util/                 # Permissao, SenhaUtil (hash), Tema (FlatLaf)
```

### Banco de dados

```
funcionario 1───N venda 1───N item_venda N───1 produto
```

- Valores monetários e quantidades em `NUMERIC` no banco e `BigDecimal` no Java, sem erro de arredondamento
- `CHECK` no banco impede preço/estoque negativo e unidade inválida

## 🛠️ Tecnologias

Java 21 · Swing · PostgreSQL · JDBC · Maven · FlatLaf

## ▶️ Como rodar

**Requisitos:** Java 21, Maven e PostgreSQL.

1. **Crie o banco** e rode o script:
   ```bash
   createdb padaria
   psql -d padaria -f banco.sql
   ```
2. **Configure a conexão:** copie `src/main/resources/db.properties.exemplo` para `db.properties` na mesma pasta e coloque a sua senha do PostgreSQL. (O `db.properties` não vai para o GitHub.)
3. **Execute:**
   ```bash
   mvn compile exec:java
   ```
   Ou abra o projeto no NetBeans/IntelliJ e rode a classe `br.com.padaria.Padaria`.

**Usuário de teste:** `admin` / `admin123` (cargo Gerente). Troque a senha antes de usar de verdade.

> Para ver só as telas, sem banco, rode a classe `ModoTeste`.

## 🚧 Melhorias futuras

- [ ] Hash de senha com salt (BCrypt)
- [ ] Relatórios de vendas (por produto, por dia)
- [ ] Impressão de cupom
- [ ] Testes automatizados

## 👤 Autor

**Vinicius Alberto**, estudante de TI na UNIP
[GitHub](https://github.com/oviniav) · [LinkedIn](https://www.linkedin.com/in/vini0209vicente)
