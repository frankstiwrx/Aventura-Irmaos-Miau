# Arquitetura — Aventura dos Irmãos Miau

> Estado de referência: Entrega 2 (Conteúdo expansível). Descreve o código **como ele é hoje**
> e as direções já aprovadas. Itens marcados *(planejado)* ainda não existem.

## Stack e build

| Item | Valor |
|---|---|
| Linguagem / UI | Java, Swing + Java2D |
| Build | Maven (`pom.xml`), sem dependências de runtime |
| Bytecode | `maven.compiler.release = 23` (compila e roda no JDK 25 Temurin do ambiente) |
| Testes | JUnit 5.10.5 (escopo `test`), Surefire 3.5.6, `java.awt.headless=true` |
| Classe principal | `com.irmaosmiau.main.Main` (POM, manifest do JAR e `nbactions.xml`) |
| Protótipo | `com.irmaosmiau.main.MainPrototipo` — versionado, abre direto o modo dois Miaus |

### Comandos

```bash
mvn test                                  # compila e roda os testes
mvn package                               # gera target/AventuraIrmaosMiau-1.0-SNAPSHOT.jar
java -jar target/AventuraIrmaosMiau-1.0-SNAPSHOT.jar   # abre o menu oficial
```

No NetBeans: **Run Project** usa `nbactions.xml` → `Main`. Para o protótipo,
clique com o botão direito em `MainPrototipo.java` → **Run File**.
**Test Project** (Alt+F6) roda os mesmos testes do `mvn test`.

## Pacotes

```
main ──► ui (MenuPanel, SelecaoPersonagemPanel) ──► entities.TipoPersonagem
  └────► game.GamePanel ──► entities (Branco/Preto), input.InputHandler, combate
combate ──► entities.IrmaoMiau, entities.AtributosLutador
entities ──► somente AWT
editor (MiauStudio) ──► entities.IrmaoMiau, IrmaoMiauBranco, IrmaoMiauPreto
conteudo ──► combate, entities.IrmaoMiau          (Entrega 2; ainda não usado pelo game)
personagens ──► entities                          (Entrega 2; ainda não usado pelo game/ui)
```

| Pacote | Responsabilidade | Classes principais |
|---|---|---|
| `main` | Pontos de entrada e troca de telas | `Main`, `MainPrototipo` |
| `ui` | Menu principal e seleção de personagem | `MenuPanel`, `SelecaoPersonagemPanel` |
| `game` | Loop (Timer 16 ms ≈ 60 FPS), input → personagens → combate, pausa/fim, render, HUD, espelhamento | `GamePanel` |
| `input` | Teclas → flags booleanas por personagem (InputMap/ActionMap) | `InputHandler` |
| `entities` | Corpo, movimento, linhas, fôlego/condicionamento, estados corporais, renderers e arrays de animação | `IrmaoMiau` (base), `IrmaoMiauBranco`, `IrmaoMiauPreto`, `AtributosLutador` |
| `combate` | Ações declarativas, fotografia espacial, interação compartilhada, timeline conjunta | ver abaixo |
| `editor` | MiauStudio: autoria de poses/animações, projeto JSON v2, exportação de arrays | `EditorDePoses` |
| `conteudo` | Leitura JSON sem dependências, conversor MiauStudio v2, catálogos de animações e técnicas | `CarregadorConteudoCombate`, `ConteudoCombate`, `FonteConteudo`, `ConteudoInvalidoException` |
| `conteudo.json` | Parser JSON e acesso tipado com caminho nos erros | `LeitorJson`, `ObjetoJson` |
| `conteudo.miaustudio` | `MIAUSTUDIO_PROJECT` v2 → quadros de 30 inteiros | `ConversorMiauStudio`, `AnimacaoConjuntaMiauStudio` |
| `personagens` | Catálogo de personagens por ID com fábrica | `CatalogoPersonagens`, `DefinicaoPersonagem`, `FabricaLutador`, `ReferenciasVisuais` |

Guia de cadastro: [CONTEUDO.md](CONTEUDO.md).

## Motor de combate (`com.irmaosmiau.combate`)

| Classe | Papel |
|---|---|
| `AcaoCombate` | Dados imutáveis e validados de uma técnica (alcance, linhas, orientação, custo, pesos, id da animação, duração, disponibilidade, descrição). Construtor original + `AcaoCombate.builder(...)`. |
| `DisponibilidadeAcao` | `JOGAVEL` ou `PENDENTE` (pendente nunca executa). |
| `RegistroAcoes` | Catálogo por ID normalizado. `new RegistroAcoes()` = legado (só `pegada_1`); `RegistroAcoes.vazio()` para conteúdo carregado. |
| `ContextoCombate` | Fotografia de um instante atacante→defensor: distância normalizada pela escala, linhas, frente/costas, estados, fôlego, atributos. Não altera nada. |
| `SistemaCombate` | Coordena: requisitos, custo, interação persistente, manutenção espacial, timeline. |
| `EstadoInteracaoCombate` | Estado da **relação** entre os dois (LIVRE, PEGADA_1..4). |
| `AnimacaoCombateAtiva` | Relógio único (frame/duração/progresso) compartilhado por aplicador e receptor. |
| `AnimacaoConjuntaQuadros` | Quadros pareados de 30 valores (formato MiauStudio) + conversão progresso→índice; duração total e distribuição temporal opcionais. |
| `RegistroAnimacoesCombate` | Catálogo de animações conjuntas por ID; rejeita duplicados. `new` = legado (`pegada_1_entrada` em Java); `vazio()` para conteúdo. |

`SistemaCombate` aceita os dois registros por injeção
(`new SistemaCombate(a, b, acoes, animacoes)`); os construtores antigos continuam
usando os registros legados. O `GamePanel` ainda usa o construtor antigo.

### Ordem de um tick no modo dois Miaus (`GamePanel.atualizar`)

1. `atualizarMiauBranco()` / `atualizarMiauPreto()` — se bloqueado por animação, só atualiza o corpo parado.
2. `atualizarOrientacaoDosMiaus()` — vira cada um para o outro.
3. `sistemaCombate.atualizar()` — nova fotografia, manutenção da interação, avança a timeline.
4. `atualizarEntradasCombate()` — borda de subida da tecla **H** → `tentarAcao(branco, preto, "pegada_1")`.
5. `verificarFimDeJogo()` e `repaint()`.

### Renderização da pose de combate

`SistemaCombate` envia o **mesmo índice** de quadro aos dois via
`IrmaoMiau.aplicarPoseCombateExterna(pose, dx, dy)`. Em `desenhar()`, Branco e Preto
verificam essa pose **antes** de qualquer outra animação. O X físico não muda; só o
deslocamento visual. O espelhamento é feito pelo `GamePanel` (`desenharPersonagemOrientado`),
o que inverte automaticamente os deslocamentos quando os lados se trocam.

## Fragilidades conhecidas (aceitas nesta entrega)

1. `EstadoInteracaoCombate` é enum fixo → nova técnica exige editar o enum.
2. ~~Registro de animações não injetável~~ — resolvido na Entrega 2. Ainda existem duas fontes da PEGADA_1 (arrays legados e JSON), mantidas iguais pelos testes de paridade; o jogo usa a legada.
3. O combate depende da classe concreta `IrmaoMiau`, não de uma interface.
4. Após a entrada da PEGADA_1, mudanças corporais (deitar, pular, sprawl) **não** rompem a pegada e a pose externa continua sobrepondo o desenho. Decisão aprovada: romper — implementar na Entrega 3 (Motor).
5. `GamePanel` concentra loop, input, HUD e menus; atualização triplicada por personagem.
6. Pesos e `dificuldade` da `AcaoCombate` ainda não influenciam: sucesso é determinístico.
7. ~~Arrays colados à mão~~ — o JSON do MiauStudio agora é importado diretamente (`ConversorMiauStudio`). O editor continua com seu próprio parser e exportação Java.

## Direções aprovadas *(planejado)*

- **Entrega 2 — Conteúdo:** *(concluída)* ver [CONTEUDO.md](CONTEUDO.md).
- **Entrega 3 — Motor:** ligar o `GamePanel` a `CarregadorConteudoCombate.carregarPadrao()`; estado de interação por ID, política de interação (entrada bloqueia ambos; depois, movimento/mudança corporal rompe até haver movimentação conjunta), efeitos genéricos, disputa com RNG injetável, comandos por lutador desacoplados do teclado.
- **Entrega 4 — Produto:** construtores alternativos de Branco/Preto para atributos customizados (preservando os atuais), saves JSON versionados, áudio WAV com fallback silencioso, menus ampliados.
- **Entrega 5 — Apresentação:** cenário plugável, HUD de pegada, modo treino, empacotamento final.
