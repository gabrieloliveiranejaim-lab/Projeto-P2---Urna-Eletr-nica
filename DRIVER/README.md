# Pasta `driver`

Aqui fica o **driver do SQLite para Java** (o arquivo `.jar` do projeto *sqlite-jdbc*).
Ele ensina o Java a conversar com o banco SQLite. Sem ele, o programa avisa que
não conseguiu abrir o banco de dados.

## Como colocar

1. Baixe o `sqlite-jdbc` (arquivo `.jar`) na página oficial do projeto:
   https://github.com/xerial/sqlite-jdbc (seção de downloads/releases) ou no Maven Central.
2. Copie o `.jar` para **esta pasta**. Pode manter o nome original, com a versão.
3. Se ao rodar aparecer um erro com a palavra `slf4j`, baixe também o `slf4j-api`
   (`.jar`) e coloque aqui também.

Os comandos do `README.md` usam `driver/*`, então todo `.jar` desta pasta entra no
classpath automaticamente.
