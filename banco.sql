
DROP TABLE IF EXISTS item_venda;
DROP TABLE IF EXISTS venda;
DROP TABLE IF EXISTS produto;
DROP TABLE IF EXISTS funcionario;

CREATE TABLE funcionario (
    id        SERIAL       PRIMARY KEY,
    nome      VARCHAR(100) NOT NULL,
    cargo     VARCHAR(50)  NOT NULL,
    telefone  VARCHAR(20),
    usuario   VARCHAR(30)  NOT NULL UNIQUE,
    senha     VARCHAR(64)  NOT NULL          -- hash 
);

CREATE TABLE produto (
    id         SERIAL        PRIMARY KEY,
    nome       VARCHAR(100)  NOT NULL,
    descricao  VARCHAR(255),
    preco      NUMERIC(10,2) NOT NULL CHECK (preco >= 0),
    unidade    VARCHAR(2)    NOT NULL CHECK (unidade IN ('UN', 'KG')),
    estoque    NUMERIC(10,3) NOT NULL DEFAULT 0 CHECK (estoque >= 0)
);

CREATE TABLE venda (
    id              SERIAL        PRIMARY KEY,
    data_hora       TIMESTAMP     NOT NULL DEFAULT NOW(),
    id_funcionario  INTEGER       NOT NULL REFERENCES funcionario(id),
    valor_total     NUMERIC(10,2) NOT NULL CHECK (valor_total >= 0)
);

CREATE TABLE item_venda (
    id              SERIAL        PRIMARY KEY,
    id_venda        INTEGER       NOT NULL REFERENCES venda(id) ON DELETE CASCADE,
    id_produto      INTEGER       NOT NULL REFERENCES produto(id),
    quantidade      NUMERIC(10,3) NOT NULL CHECK (quantidade > 0),
    preco_unitario  NUMERIC(10,2) NOT NULL CHECK (preco_unitario >= 0)
);

-- ---------------- Dados de teste ----------------

INSERT INTO funcionario (nome, cargo, telefone, usuario, senha) VALUES
('Administrador', 'Gerente', '(16) 99999-0000', 'admin',
 encode(sha256('admin123'::bytea), 'hex')),
('Administrador', 'Gerente', '', 'adm',
 encode(sha256('adm123'::bytea), 'hex'));

INSERT INTO produto (nome, descricao, preco, unidade, estoque) VALUES
('Pão Francês',      'Pão de sal tradicional',     16.90, 'KG', 20.000),
('Pão de Queijo',    'Assado na hora',              49.90, 'KG',  4.000),
('Queijo Muçarela',  'Fatiado',                     54.90, 'KG',  3.500),
('Sonho',            'Recheio de creme',             6.50, 'UN', 30),
('Bolo de Cenoura',  'Com cobertura de chocolate',  35.00, 'UN',  5),
('Café Expresso',    'Xícara 80 ml',                 5.00, 'UN', 100);
