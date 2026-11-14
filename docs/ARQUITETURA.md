# Arquitetura — Aventura dos Irmãos Miau

> Estado de referência: Entrega 4A (cenários, HUD, áudio e modo treino). Descreve o código **como ele é hoje**
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
| `input` | Único leitor do teclado: teclas configuradas → estado das ações lógicas por slot (InputMap/ActionMap), trava de bloqueio | `InputHandler`, `TravaComandos` |
| `controles` (5C) | Ações lógicas, teclas suportadas, mapa configurável dos dois slots, conflitos, papel no combate e ações contextuais; ver [CONTROLES.md](CONTROLES.md) | `AcaoJogador`, `SlotJogador`, `TeclaFisica`, `MapaControles`, `PapelCombate`, `AcoesContextuais`, `DetectorToques` |
| `entities` | Corpo, movimento, linhas, fôlego/condicionamento, estados corporais, renderers e arrays de animação | `IrmaoMiau` (base), `IrmaoMiauBranco`, `IrmaoMiauPreto`, `AtributosLutador` |
| `combate` | Ações declarativas, fotografia espacial, interação compartilhada, timeline conjunta | ver abaixo |
| `editor` | MiauStudio: autoria de poses/animações, projeto JSON v2, exportação de arrays | `EditorDePoses` |
| `conteudo` | Leitura JSON sem dependências, conversor MiauStudio v2, catálogos de animações e técnicas | `CarregadorConteudoCombate`, `ConteudoCombate`, `FonteConteudo`, `ConteudoInvalidoException` |
| `conteudo.json` | Parser JSON e acesso tipado com caminho nos erros | `LeitorJson`, `ObjetoJson` |
| `conteudo.miaustudio` | `MIAUSTUDIO_PROJECT` v2 → quadros de 30 inteiros | `ConversorMiauStudio`, `AnimacaoConjuntaMiauStudio` |
| `cenario` | Cenários de arena (fundo, chão, primeiro plano) por ID | `Cenario`, `CenarioDojo`, `CenarioOriginal`, `RegistroCenarios` |
| `hud` | HUD do jogador (painéis, estado da interação, feedback) | `HudCombate`, `DadosLutadorHud`, `FeedbackCombate` |
| `audio` | Música em loop e efeitos WAV do classpath; silencioso se faltar arquivo ou som | `ServicoAudio`, `AudioJogo`, `CarregadorSom`, `SaidaAudio`, `SaidaJavaSound` |
| `treino` | Sessão de treino independente de Swing (jogador x parceiro) | `SessaoTreino`, `ConfiguracaoTreino`, `ComportamentoParceiro` |
| `dispositivos` (5D) | Gamepad: provedores (GLFW, simulado, ausente), estado, esquema experimental de botões, analógico, fonte de ações por slot e sessão de dispositivos da partida; ver [CONTROLES.md](CONTROLES.md) | `ProvedorGamepads`, `ServicoGamepads`, `MapaGamepad`, `FonteGamepad`, `SessaoDispositivos`, `glfw.ProvedorGamepadGlfw` |
| `posicional` (5E) | Posição da dupla, cima/baixo, controle e transições em tempo real (simulador; no jogo só em pé); ver [COMBATE_POSICIONAL.md](COMBATE_POSICIONAL.md) | `MotorPosicional`, `EstadoPosicional`, `CatalogoTransicoes`, `LeituraPosicionalEmPe`, `RepertorioPosicional` |
| `animacao` (5E) | Catálogo, perfis e controlador de animações; JSON individuais; root motion; ver [ANIMACOES.md](ANIMACOES.md) | `ControladorAnimacoes`, `CatalogoAnimacoes`, `PerfilAnimacoes`, `CarregadorAnimacaoJson`, `ReprodutorPoseJson`, `SincronizadorPassos` |
| `preferencias` | Ponto único das preferências em execução: aplica no áudio, salva, F4/F9/F10 (4C); controles (5C) | `PreferenciasJogo`, `PreferenciasControles` |
| `selecao` | Opções de lutador por slot: oficial + perfis da base (4D) | `OpcoesDeLutadores` |
| `combate` (5A) | Disputas técnicas determinísticas e ações durante a pegada; ver [COMBATE_AVANCADO.md](COMBATE_AVANCADO.md) | `ResolvedorDisputa`, `TipoDisputa`, `ResultadoAcaoTecnica` |
| `persistencia` | Configurações e perfis em JSON versionado, gravação segura (Entrega 4B, ainda não ligada à interface; ver [PERSISTENCIA.md](PERSISTENCIA.md)) | `RepositorioConfiguracoes`, `ConfiguracoesJogo`, `RepositorioPerfis`, `ServicoPerfis`, `ArmazenamentoJson` |
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
usando os registros legados (compatibilidade explícita e referência de regressão).

**Entrega 3A:** o `GamePanel` cria o combate com
`CarregadorConteudoCombate.carregarParaJogo().criarSistema(branco, preto)`: a PEGADA_1 do jogo vem de
`/conteudo/miaustudio/PEGADA_1.json` (o HUD de debug mostra `FONTE ANIM`). Se um recurso faltar ou
estiver inválido, o jogo mostra o erro (console + janela) e não abre a luta; não há troca silenciosa
para os arrays legados.

### Política de interação (Entrega 3A)

`SistemaCombate` delega a decisão de manter/encerrar a interação a uma `PoliticaInteracao`
(padrão: `PoliticaInteracaoPadrao`; injetável no construtor de 5 argumentos). Quando a entrada termina,
o sistema tira um retrato (`EstadoCorporalObservado`) dos dois corpos; a cada tick a política compara o
corpo atual com esse retrato. O motivo do último encerramento fica em `getUltimoMotivoEncerramento()`
(HUD: `ROMPIMENTO`). Detalhes em [CONTRATOS.md](CONTRATOS.md#c8--política-de-interação-entrega-3a).

`InputHandler` tem uma `TravaComandos`: o `GamePanel` a aciona a cada frame bloqueado, e as teclas
seguradas nesse período só voltam a valer depois de soltas.

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
4. ~~Mudanças corporais não rompiam a pegada~~ — resolvido na Entrega 3A (`PoliticaInteracao`).
5. `GamePanel` concentra loop, input, HUD e menus; atualização triplicada por personagem.
6. Pesos e `dificuldade` da `AcaoCombate` ainda não influenciam: sucesso é determinístico.
7. ~~Arrays colados à mão~~ — o JSON do MiauStudio agora é importado diretamente (`ConversorMiauStudio`). O editor continua com seu próprio parser e exportação Java.

## Direções aprovadas *(planejado)*

- **Entrega 2 — Conteúdo:** *(concluída)* ver [CONTEUDO.md](CONTEUDO.md).
- **Entrega 3A:** *(concluída)* conteúdo JSON no jogo e política de interação.
- **Entrega 3 (restante) — Motor:** estado de interação por ID, efeitos genéricos, disputa com RNG injetável, comandos por lutador desacoplados do teclado.
- **Entrega 4 — Produto:** construtores alternativos de Branco/Preto para atributos customizados (preservando os atuais), saves JSON versionados, áudio WAV com fallback silencioso, menus ampliados.
- **Entrega 5 — Apresentação:** cenário plugável, HUD de pegada, modo treino, empacotamento final.

## Requisitos futuros

Ver [REQUISITOS_FUTUROS.md](REQUISITOS_FUTUROS.md): lógica independente da animação, níveis de prontidão,
reuso de animações por papel e evolução visual dos personagens.

## Apresentação (Entrega 4A)

### Ordem de desenho do `GamePanel`

1. `cenario.desenharFundo` → 2. `cenario.desenharChao` → 3. personagens (mesma ordem e espelhamento
de antes) → 4. `cenario.desenharPrimeiroPlano` → 5. `HudCombate` → 6. debug técnico (F3) → 7. escurecer de pausa/fim.

O cenário só desenha: posições, linhas e colisões não mudaram. No dojo, cada uma das 5 linhas é uma faixa
de tatame centrada nos pés (y = 326 + 20·linha, medido nos renderers atuais).

### Modos

| Modo | Como abre | Lutadores | Cenário | Música |
|---|---|---|---|---|
| Menu | `Main` | — | imagem do menu | `menu` |
| Branco / Preto | Começar → seleção | 1 | dojo | `combate` (ainda sem arquivo: silêncio) |
| Os dois | Começar → seleção | 2 jogadores | dojo | `combate` |
| **Treino** | Menu → TREINO | Branco (jogador) x Preto (parceiro passivo) | dojo | `treino` |
| Protótipo | `MainPrototipo` | 2 jogadores, debug ligado | dojo | `combate` |

No treino o `GamePanel` usa uma `SessaoTreino`: o parceiro é atualizado pelo `ComportamentoParceiro.PASSIVO`
(as teclas do segundo jogador são ignoradas) e F5 recria os dois lutadores nas posições iniciais.

### Atalhos (teclas reservadas, nunca atribuíveis aos controles — 5C)

| Tecla | Ação |
|---|---|
| ESC | Pausa (menu: continuar, trocar de Miau, menu principal) |
| F3 | Mostra/esconde o debug técnico do motor |
| F4 | Próximo cenário (dojo ↔ original) |
| F5 | Treino: reinicia as posições |
| F9 | Música liga/desliga |
| F10 | Efeitos liga/desliga |

### Correção incluída

O `GamePanel` agora para o próprio `Timer` em `removeNotify()`. Antes, ao voltar ao menu ou usar
"Tentar novamente", o painel antigo continuava rodando o loop em segundo plano.

## Configurações e perfis (Entrega 4C)

- **Menu:** COMEÇAR, TREINO, **CONFIGURAÇÕES**, **PERSONAGENS PERSONALIZADOS**.
- **`PreferenciasJogo`** é a única porta para mudar áudio/cenário em execução. O `Main` carrega
  `configuracoes.json` ao iniciar (sem criar nada) e instala a instância persistente; a tela de
  configurações e os atalhos F4/F9/F10 do `GamePanel` usam a mesma instância, então tela, áudio e
  arquivo nunca divergem. Sem instalação (testes, `MainPrototipo`) ela fica **em memória** e não grava.
- **Telas** (`ui/ConfiguracoesPanel`, `ui/PerfisPanel`) recebem serviços por construtor e um `Dialogos`
  (JOptionPane no jogo, respostas programadas nos testes headless).
- **Perfis jogáveis (4D):** ver seção abaixo.

## Perfis jogáveis (Entrega 4D)

Caminho dos atributos até a instância:

```
perfil JSON --ServicoPerfis--> PerfilLutador --OpcoesDeLutadores--> EscolhaLutador
  --(GamePanel / SessaoTreino)--> DefinicaoPersonagem.criarLutadorPersonalizado
  --> new IrmaoMiauBranco(x, y, escala, atributos.copiar(), nome)   (mesmo renderer/animações)
  --> IrmaoMiau.atributos  --> fôlego/condicionamento máximos, velocidade, recuperação
```

- Branco e Preto ganharam construtores com `AtributosLutador` (e nome de exibição). O construtor
  antigo `(x, y, escala)` delega a eles com `atributosOficiais()`, valores idênticos aos de antes.
- `DefinicaoPersonagem` aceita uma `FabricaLutadorPersonalizado`; `EscolhaLutador` representa
  oficial ou perfil (fotografia: jogar não grava nada e excluir o arquivo depois não quebra a partida).
- **Slots com controles fixos:** slot Branco (WASD/H) = Branco oficial ou perfis do Branco; slot Preto
  (setas) = Preto oficial ou perfis do Preto. No treino: jogador = slot Branco, parceiro passivo = slot Preto.
  O `GamePanel` recusa perfil no slot errado.
- **Atributos usados hoje pelo motor:** condicionamento e fôlego (máximos e consumo), velocidade
  (`3.2 + velocidade/100 × 1.6`) e recuperação (taxas de recuperação). Força, agilidade, técnica e peso
  acompanham a instância, mas nenhuma fórmula os usa ainda (nada foi inventado).
- `SelecaoLutadoresPanel` substitui no fluxo a `SelecaoPersonagemPanel` (mantida no projeto, sem alterações).

## Controles configuráveis (Entrega 5C)

Tecla física → `AcaoJogador` (via `MapaControles`) → `InputHandler.isAtiva(slot, ação)` →
`GamePanel` (contínua, toque ou modificador) → sistema existente. O motor não conhece teclas.

- `controles.json` separado de `configuracoes.json` (tipo `CONTROLES_IRMAOS_MIAU`, versão 1),
  carregado por `Main.prepararControles()` e instalado em `PreferenciasControles.global()`;
  cada `GamePanel` lê o mapa ao ser criado.
- CONFIGURAÇÕES → CONTROLES (`ui.ControlesPanel`): remapear, trocar em conflito, restaurar, salvar.
- Pausa → CONTROLES (`game.PainelAjudaControles`): ajuda com as teclas reais; a faixa de dicas do
  HUD (`hud.AjudaControles`) é gerada das teclas e mostra só as ações úteis no papel atual.
- Ações de combate despachadas por `GamePanel.executarAcaoCombate` na ordem
  `AcoesContextuais.ACOES_DE_COMBATE` (pegada, T, Y, U), iguais à 5B.
- Janela sem foco solta todas as teclas.

Detalhes, regras de conflito, como adicionar ações e o plano das ações contextuais: [CONTROLES.md](CONTROLES.md).

## Gamepad (Entrega 5D)

Dispositivo → `ProvedorGamepads` → `EstadoGamepad` (imutável) → `FonteGamepad` (ações lógicas,
mesma `TravaComandos`) → `InputHandler` (teclado **ou** gamepad por slot) → `GamePanel` → motor.

- Única dependência de produção: LWJGL 3.4.3 (GLFW), BSD-3; JAR único via `maven-shade-plugin`.
- `ProvedorGamepadGlfw` carregado por reflexão; qualquer falha = só teclado.
- Leitura numa thread própria; o jogo só lê o último estado publicado (nada bloqueia o Swing).
- `GamePanel.atualizar()` chama `SessaoDispositivos.atualizar()` no início de cada tick, inclusive
  pausado (Start = pausa); foco perdido/ganho chega à sessão pelo `WindowFocusListener` da 5C.
- `dispositivos.json` próprio; nenhum formato anterior mudou.

## Fundação 5E: posições e animações

- `posicional` não duplica o `SistemaCombate`: em pé ele é **lido** dele (`LeituraPosicionalEmPe`);
  o chão existe só no simulador (o `MotorPosicional` em modo JOGO recusa transições para o chão).
- `animacao` decide a animação por estado (`ControladorAnimacoes`) sem tocar no movimento. O jogo
  continua desenhando pelo código legado; duas animações (movimento no chão do Branco e do Preto)
  já existem em JSON com paridade pixel a pixel.
- `GamePanel`, renderizadores, arrays e `SistemaCombate` não mudaram. `IrmaoMiau` ganhou só dois
  getters de leitura (`estaAndando`, `estaEmTaunt`).
- Diagnóstico de joystick ampliado (presente × mapeado) e mapeamentos SDL extras, somente leitura.

## Gamepad remapeável e comandos contextuais (Entrega 5F)

- `MapaGamepad` agora é **editável por slot** (esquema CLASSICO ou CONTEXTUAL + botões), salvo em
  `gamepad.json` (`PreferenciasGamepad`). O Clássico padrão é idêntico à 5D.
- Contextual: `FonteGamepad` produz `EntradaContextual`; o `ResolvedorComandosContextuais`
  (independente de aparelho) gera no máximo um `PosicaoComando` por toque. O `RepertorioContextual`
  diz o que a posição faz no papel atual, e o `GamePanel` executa pelo mesmo `executarAcaoCombate`
  das teclas. Transições de chão (5E) aparecem reservadas.
- HUD: `hud.PainelDpadContextual` (cruz compacta por jogador). Tela: `ControlesPanel` em modo
  "Editar: Gamepad".

## Animações no runtime e ergonomia do gamepad (Entrega 5G)

- `animacao.AnimadorPartida` liga o `ControladorAnimacoes` à partida (uma chamada por tick lógico no
  `GamePanel`). Animações JSON do perfil são desenhadas pelo renderizador do próprio personagem
  (canal de pose da PEGADA_1); o resto continua legado. Detalhes em [ANIMACOES.md](ANIMACOES.md).
- `PerfisAnimacoes.global()`/`paraJogo` (catálogo tolerante a JSON ausente/inválido) e
  `registrarClasse` (classe → personagem, sem `instanceof` no runtime).
- Gamepad: padrões novos (R3 rola, L3 deita, A pula, modificador + A = pulo alto) e o
  `ComandoGamepad.MODIFICADOR_PULO_ALTO`, decidido no toque pela `FonteGamepad`.

## Analógico direito e arrasto automático (Entrega 5H)

- `dispositivos.LeitorAnalogicoTecnicas`: analógico direito → direção de técnica (zona morta,
  histerese, diagonais, uma por inclinação). A `FonteGamepad` junta essa direção ao D-pad e entrega ao
  mesmo `ResolvedorComandosContextuais`.
- `CamadaComando`: 1 (RS), 2 (LT), 3 (LB), 4 (LT + LB, avançada e reservada).
- `MapaGamepad.acoesAtivas` (Contextual): andar para os lados também emite o "modo controle". O
  `GamePanel` (regra da 5B) só arrasta o aplicador de uma pegada estabelecida.

## Papéis independentes do slot (Entrega 5I)

- `AcaoJogador`: os dois slots têm tecla padrão para todas as ações; `usadaAntesDa5I` guarda o uso
  antigo só para reconhecer arquivos salvos. `MapaGamepad.padrao5H/padrao5G/padraoAnterior`
  reconstroem os padrões antigos para a migração do `RepositorioGamepad`.
- `GamePanel.atualizarEntradasCombate`: coleta os toques dos dois slots, resolve o botão
  compartilhado pelo papel (`resolverBotaoCompartilhado`) e descarta a segunda pegada do tick.
  O arrasto existe para os dois slots (`deslocandoConjuntoJ1/J2`).
- `GamePanel.criarDoSlot` não confere mais a base: qualquer irmão em qualquer slot.
- `SelecaoLutadoresPanel.Slot`: seletor de irmão que repovoa o combo com `OpcoesDeLutadores.paraBase`.

## Navegação de menus (Entrega 5I, complemento)

- `dispositivos.LeitorNavegacao`: um gamepad → comandos de menu (sem Swing; zona morta,
  histerese, repetição, armar só depois do repouso).
- `ui.NavegadorMenus`: instalado uma vez na janela pelo `Main`; lê todos os gamepads conectados
  (timer de 16 ms) e as setas/ENTER/ESC, e age na tela atual ou no diálogo aberto. Telas
  `TelaNavegavel` dizem quando ele pode agir (`GamePanel`: só pausado/fim; `ControlesPanel`: nunca
  durante a captura). O "voltar" de cada tela é o botão marcado com `NavegadorMenus.marcarVoltar`.
- Ao sair da pausa, `GamePanel.sincronizarBordasDeToque` impede que o toque que fechou o menu
  vire ação.
