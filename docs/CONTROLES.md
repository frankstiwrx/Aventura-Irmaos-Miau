# Controles — Entregas 5C e 5D

Controles configuráveis e base para ações contextuais. Nenhuma técnica nova; H, T, U, Y e o
arrasto funcionam exatamente como na 5B (mesmos custos, ordem e resultados).

## Fluxo

```
tecla física (KeyStroke)          MapaControles: slot + AcaoJogador -> TeclaFisica
   │  InputMap/ActionMap
   ▼
InputHandler.isAtiva(slot, acao)  estado pressionado/solto + TravaComandos (3A)
   │
   ▼
GamePanel                         CONTINUA: lida a cada tick (andar, correr, agachar, modificador)
   │                              TOQUE: DetectorToques / campos xxxAnterior (uma vez por pressionamento)
   ▼
AcoesContextuais / executarAcaoCombate -> SistemaCombate (tentarAcao, tentarDesequilibrio, ...)
```

O motor (`combate`) não conhece nenhuma tecla. O `InputHandler` continua sendo o único leitor do
teclado e mantém os nomes de comando de sempre (`brancoPegada1Pressionada`, `pretoRompimentoSolta`...),
de modo que a `TravaComandos` e os testes antigos não mudaram.

## Ações lógicas (`controles.AcaoJogador`)

| Grupo | Ação | Acionamento | J1 (padrão) | J2 (padrão) |
|---|---|---|---|---|
| MOVIMENTO | `MOVER_ESQUERDA` / `MOVER_DIREITA` / `MOVER_CIMA` / `MOVER_BAIXO` | contínua | A / D / W / S | ← / → / ↑ / ↓ |
| | `PULAR` | toque | ESPAÇO | N |
| | `PULO_CURTO` | toque | V | B |
| | `AGACHAR` | contínua | C | M |
| | `CORRER` | contínua | Z | K |
| | `ROLAR` | toque | R | O |
| | `DEITAR_LEVANTAR` | toque | F | J |
| | `RESPIRAR` | contínua | X | L |
| COMBATE | `INICIAR_PEGADA` | toque | H | — |
| | `TENTAR_DESEQUILIBRIO` | toque | T | — |
| | `REFORCAR_PEGADA` | toque | Y | — |
| | `MODO_DESLOCAMENTO_CONTROLADO` | modificador | SHIFT | — |
| DEFESA | `SPRAWL` | toque | G | I |
| | `ROMPIMENTO_TECNICO` | toque | — | U |
| SISTEMA (fixas) | pausa, debug, cenário, reiniciar treino, música, efeitos | — | ESC, F3, F4, F5, F9, F10 | |

"—" significa que o slot ainda não usa a ação (hoje só o Jogador 1 aplica pegada e só o Jogador 2
rompe). O Jogador 1 também controla o único lutador do modo individual (Branco **ou** Preto) e o
jogador do treino.

## Regras do mapeamento (`MapaControles`)

- **Uma tecla, uma ação, nos dois jogadores juntos.** Os dois dividem o teclado; uma tecla nunca é
  ambígua. Tentar usar uma tecla ocupada na tela pergunta se deve **trocar** as duas ações.
- **Reservadas:** ESC e F1–F12 (sistema). TAB não é suportada (o Swing usa para trocar o foco).
- **Toda ação usada pelo slot precisa de tecla** para poder salvar.
- **Modificador lógico:** o arrasto usa "modo controle + esquerda/direita" configurados. O modo
  controle pode ser SHIFT, CTRL, ALT ou qualquer tecla livre (ex.: Q). Cada tecla é registrada com
  todas as combinações de SHIFT/CTRL/ALT, então nada para de funcionar com um modificador segurado.
- **Foco:** quando a janela perde o foco, todas as teclas são soltas (`InputHandler.liberarTodas`),
  para nenhum lutador ficar andando sozinho.
- Um mapa com conflito nunca chega ao jogo: `PreferenciasControles` valida ao carregar e ao salvar e
  o `InputHandler` cai nos padrões se receber um.

## Persistência (`controles.json`)

Arquivo próprio, ao lado de `configuracoes.json` (que não mudou de formato):

```json
{
  "tipo": "CONTROLES_IRMAOS_MIAU",
  "versaoEsquema": 1,
  "jogadores": {
    "jogador1": { "MOVER_ESQUERDA": "A", "INICIAR_PEGADA": "H", "MODO_DESLOCAMENTO_CONTROLADO": "SHIFT", ... },
    "jogador2": { "MOVER_ESQUERDA": "LEFT", "ROMPIMENTO_TECNICO": "U", ... }
  }
}
```

| Situação | Comportamento |
|---|---|
| Sem arquivo | Padrões; nada é criado até SALVAR |
| JSON corrompido, tipo/versão errados, ação/jogador/tecla desconhecidos, conflito, tecla reservada | Padrões + aviso ao abrir o jogo; o arquivo **fica intacto** e só vira `controles.json.corrompido-<data>` quando o jogador salvar |
| Ação ausente (arquivo de versão anterior) | Recebe a tecla padrão se estiver livre; senão fica **sem tecla**, com aviso, e a tela CONTROLES pede uma tecla antes de salvar |
| Falha de disco ao salvar | Mensagem na tela; os controles anteriores continuam valendo |

## Tela CONFIGURAÇÕES → CONTROLES

- Botões JOGADOR 1 / JOGADOR 2; lista com rolagem agrupada em MOVIMENTO, COMBATE, DEFESA.
- Clicar na tecla de uma ação → "pressione uma tecla…" → a tecla apertada passa a valer.
  ESC cancela. Reservadas e não suportadas são recusadas com mensagem.
- **Durante a captura todas as teclas são consumidas pela tela** (`KeyEventDispatcher`): nenhum
  botão reage a ESPAÇO/ENTER e nenhum comando de jogo roda (não há partida aberta nas telas de menu).
- RESTAURAR PADRÕES (os dois jogadores), SALVAR e VOLTAR (pergunta salvar/descartar/cancelar).
- Os controles salvos valem a partir da próxima partida.

## Ajuda ao jogador

- **Faixa de dicas do HUD** (uma linha, contextual): mostra só as ações úteis agora, com as teclas
  configuradas. Livre: `H pegar`; aplicador: `T desequilibrar · Y reforçar · SHIFT+A/D arrastar`;
  receptor: `U romper`. O modo individual mostra a mesma faixa de antes.
- **Pausa → CONTROLES:** todas as ações de cada jogador em campo, linha "AGORA" com as ações de
  combate do estado atual e os atalhos fixos. ESC ou VOLTAR fecha (o jogo continua pausado).

## Como adicionar uma ação lógica

1. Nova constante em `AcaoJogador`: sufixo do comando, rótulo, verbo, grupo, acionamento e a tecla
   padrão de cada slot (`null` = o slot não usa). A tela CONTROLES, a ajuda, a validação e o
   `controles.json` passam a conhecê-la automaticamente.
2. Escolha uma tecla padrão **livre nos dois slots** (o teste `padraoIdenticoAosControlesAnteriores`
   e `MapaControles.problemas()` acusam repetição).
3. Leia a ação no `GamePanel` com `input.isAtiva(slot, acao)`: contínua direto; toque com
   `DetectorToques`; se for de combate, inclua em `AcoesContextuais.ACOES_DE_COMBATE` e trate em
   `executarAcaoCombate`.
4. Compatibilidade: arquivos antigos não têm a ação e recebem a tecla padrão (ou ficam sem tecla
   com aviso). **Não renomeie** nomes de constantes já gravados; para renomear ou remover uma ação,
   aumente `RepositorioControles.VERSAO_ESQUEMA` e converta o nome antigo ao ler.

## Plano de migração para ações contextuais (entrega futura)

Hoje cada técnica tem uma ação: H → `INICIAR_PEGADA`, T → `TENTAR_DESEQUILIBRIO`, U →
`ROMPIMENTO_TECNICO`, Y → `REFORCAR_PEGADA`. Os pontos de extensão já existem:

- `PapelCombate.de(sistema, lutador)`: LIVRE, ENTRANDO, APLICADOR ou RECEPTOR;
- `AcoesContextuais.disponiveis(papel, slot)`: o que faz sentido no estado atual;
- `GamePanel.executarAcaoCombate(slot, acao)`: único ponto que traduz ação → `SistemaCombate`.

Passos previstos:

1. Criar a ação genérica `EXECUTAR_TECNICA` (e, se fizer falta, `TECNICA_ALTERNATIVA`) com tecla
   padrão livre, sem remover H/T/U/Y.
2. Dar ao lutador uma lista de **técnicas equipadas** (ids do catálogo de técnicas, por perfil).
3. `AcoesContextuais` passa a escolher, entre as equipadas, a primeira autorizada pelo papel e pelo
   estado (alcance, orientação, firmeza, fôlego, recarga): ex. LIVRE → uma pegada; APLICADOR →
   desequilíbrio/queda; RECEPTOR → rompimento/defesa.
4. `executarAcaoCombate` despacha a técnica escolhida para o `SistemaCombate` existente.
5. Só depois de validado no jogo: H/T/U/Y viram atalhos opcionais (ou somem com migração do
   `controles.json`, aumentando a versão do esquema).

## Limitações atuais

- **Teclado compartilhado:** muitos teclados não registram várias teclas ao mesmo tempo
  (*ghosting*), principalmente com os dois jogadores segurando várias teclas.
- O Jogador 2 ainda não aplica pegada e o Jogador 1 não rompe (regra do motor, não do teclado).
- Teclas fora da lista suportada (Ç, acentos e algumas teclas de layouts não americanos) são recusadas.
- Mudanças nos controles valem na próxima partida (uma partida em andamento mantém o mapa com que abriu).
- Gamepad: suporte inicial na 5D (abaixo), com esquema de botões **experimental**.

---

# Entrega 5D — Gamepad (suporte inicial)

> **Situação honesta:** a leitura física foi integrada (GLFW) e verificada **neste PC sem nenhum
> controle conectado**: a biblioteca carrega, inicia, lista 0 gamepads e encerra limpa, inclusive
> pelo JAR. **Nenhum controle Xbox ou PlayStation real foi testado.** Toda a lógica de jogo com
> gamepad foi testada com um gamepad **simulado**. Ver "O que falta validar com hardware".

## Arquitetura

```
dispositivo físico ─► provedor ─────────────► estado ──────────► ações lógicas ───► slot ──► motor
                     (ProvedorGamepads)      (EstadoGamepad)    (FonteGamepad)      (InputHandler)
  GLFW, thread própria  ProvedorGamepadGlfw   imutável, por tick  MapaGamepad +      teclado OU gamepad
  testes                ProvedorGamepadSimulado                   LeitorAnalogico +  → GamePanel → SistemaCombate
  sem suporte           ProvedorGamepadsAusente                   TravaComandos
```

- **Teclado intocado:** o `InputHandler` continua lendo o teclado como na 5C. Ele só ganhou
  `conectarFonte(slot, FonteAcoes)` e `setTecladoAtivo(slot, ...)`. `isAtiva(slot, ação)` = teclado
  do slot (se ativo) **ou** a fonte do slot. Todos os getters antigos (`isPegada1`, `isPretoRompimento`…)
  passam por aí, então o `GamePanel` e as regras de combate não mudaram.
- **Mesmas regras para os dois:** a `FonteGamepad` calcula as bordas (apertou/soltou) a cada tick e
  usa a mesma `TravaComandos` do teclado (bloqueio da entrada da PEGADA_1). Toque × contínua continua
  no `GamePanel` (`DetectorToques` / campos `xxxAnterior`), igual para teclado e gamepad.
- **Sem Robot:** nada simula teclas; o gamepad produz `AcaoJogador` diretamente.
- `SessaoDispositivos` (uma por partida) liga gamepads aos slots e cuida de conexão, desconexão,
  foco e do botão de pausa.

## Biblioteca: LWJGL 3.4.3 (GLFW 3.5.1)

| Item | Detalhe |
|---|---|
| Artefatos Maven | `org.lwjgl:lwjgl` e `org.lwjgl:lwjgl-glfw` 3.4.3 + classificador `natives-windows` |
| Licença | BSD-3-Clause (LWJGL e GLFW zlib/libpng) — compatível com distribuição do jogo |
| Por quê | Mantida, no Maven Central, nativos prontos; o GLFW traz o **layout padrão de gamepad** com o banco de mapeamentos SDL embutido (Xbox/XInput, DualShock 4/DualSense e outros); detecta conexão/desconexão durante o jogo; USB e Bluetooth são indiferentes para ele (o Windows apresenta o controle do mesmo jeito) |
| Descartadas | JInput (sem manutenção, sem conexão a quente); SDL via JNA (DLL separada, mais empacotamento); driver/HID próprio (proibido pelo escopo) |
| Tamanho | JAR do jogo: ~1,2 MB → ~3,0 MB |
| Nativos | Só **Windows x64** no JAR. Em Linux/macOS o jogo abre normalmente, só com teclado |
| JDK 25 | Backend de memória FFM do LWJGL (definido em `ServicoGamepads`) e `Enable-Native-Access: ALL-UNNAMED` no manifesto: **nenhum aviso** do JDK ao rodar `java -jar` |
| DLLs | O LWJGL extrai `glfw.dll`/`lwjgl.dll` para a pasta temporária do sistema (`%TEMP%\lwjgl_<usuário>`); nos testes, para `target/lwjgl-nativos` |
| macOS | Não suportado nesta versão (o GLFW exige a thread principal do processo) |

`ProvedorGamepadGlfw` é a **única** classe que usa LWJGL e é carregada por reflexão: sem os jars,
sem as DLLs, em sistema não suportado, se o GLFW não iniciar, ou com
`-Dirmaosmiau.gamepad=desligado`, o jogo usa `ProvedorGamepadsAusente` e segue só com o teclado. O
motivo aparece em CONFIGURAÇÕES → CONTROLES.

**Thread:** todo o GLFW roda na thread `irmaosmiau-gamepad` (daemon): `glfwInit`, `glfwPollEvents`,
`glfwGetGamepadState` a ~120 Hz e `glfwTerminate`. Ela publica um mapa **imutável** a cada leitura; o
jogo (thread do Swing) só lê esse mapa, nunca espera. Começa depois que a janela existe
(`Main`) e é encerrada por um *shutdown hook* ao fechar o jogo.

**Empacotamento:** `maven-shade-plugin` gera um JAR único (`java -jar` continua bastando) com
`Multi-Release: true`; `META-INF/INDEX.LIST` e `module-info` dos jars do LWJGL são removidos. O
`original-…jar` ao lado é o JAR sem dependências (gerado pelo shade; não é usado).

## Esquema de botões (EXPERIMENTAL — `MapaGamepad.experimental()`)

Nomes neutros de marca: SUL = Xbox A / PS Cruz, LESTE = B / Bola, OESTE = X / Quadrado, NORTE = Y / Triângulo.

| Botão | Jogador 1 | Jogador 2 |
|---|---|---|
| Analógico esquerdo / D-pad | mover (5 linhas) | mover |
| SUL (A/Cruz) | pular | pular |
| OESTE (X/Quadrado) | iniciar pegada | — |
| LESTE (B/Bola) | desequilibrar | romper a pegada |
| NORTE (Y/Triângulo) | reforçar | — |
| LB/L1 (segurado) + analógico ←/→ | arrasto conjunto | — |
| RB/R1 | correr | correr |
| LT/L2 | agachar | agachar |
| RT/R2 | sprawl | sprawl |
| L3 / R3 | rolar / deitar-levantar | idem |
| Back/Share | respirar | respirar |
| Start/Options | pausa / continuar (fecha a ajuda) | idem |
| — | pulo curto **sem botão** | idem |

- Tudo num único lugar; trocar o esquema = editar `MapaGamepad.experimental()`. Não é remapeável
  pela tela nesta entrega (decisão de game design pendente).
- LESTE é o **embrião do botão contextual**: o mesmo botão vira a ação do papel de cada slot.
- O gamepad produz só ações lógicas; nenhum botão chama técnica diretamente.

### Analógico

- **Zona morta radial** 0,25 (ruído de repouso não move); o resto é **normalizado** para 0..1.
- **Histerese:** liga em 0,50 e só desliga abaixo de 0,35 (sem liga-desliga no limiar).
- O analógico apenas "segura" `MOVER_ESQUERDA/DIREITA/CIMA/BAIXO`, exatamente como as teclas:
  **velocidade, fórmulas e as cinco linhas não mudaram**. Diagonal = duas direções (como duas teclas).
- D-pad e analógico juntos somam numa única ação (sem movimento duplicado).
- Gatilhos contam como apertados a partir de 0,5.

## Associação dispositivo ↔ jogador

CONFIGURAÇÕES → CONTROLES → escolha o jogador → **Dispositivo: Teclado / Gamepad** → SALVAR.
A linha ao lado mostra os gamepads conectados agora (atualiza a cada segundo) ou o motivo de o
gamepad estar indisponível.

Regras (`SessaoDispositivos`):

1. Padrão: os dois no teclado — exatamente como antes da 5D.
2. Na partida, os slots com GAMEPAD são resolvidos em ordem (J1, depois J2); cada um recebe o
   **primeiro gamepad conectado que não está com o outro**. Um gamepad nunca controla os dois.
3. Enquanto um gamepad controla o slot, **o teclado daquele slot fica desligado** (dois dispositivos
   não agem no mesmo jogador). As teclas do outro slot e as personalizações da 5C não mudam.
4. Sem gamepad livre: o slot usa as teclas dele e o HUD avisa ("J2: nenhum gamepad livre · usando o
   teclado"). Partida só com teclado sempre funciona.
5. **Desconectou:** todas as ações do gamepad são soltas na hora, o teclado do slot volta e o HUD
   avisa ("J1: GAMEPAD DESCONECTADO · teclado ativo").
6. **Reconectou:** o slot volta a usar o gamepad (prefere o mesmo aparelho, pela GUID); botões já
   segurados nesse momento ficam travados até serem soltos ("J1: GAMEPAD CONECTADO · …").
7. **Janela sem foco:** gamepad ignorado (o GLFW leria mesmo sem foco); na volta, o que estiver
   segurado fica travado até ser solto. O teclado é solto como na 5C.
8. Treino e modo individual: só o Jogador 1 existe; o parceiro continua passivo (nem o gamepad do J2
   faz ele defender).
9. Pausa: Start alterna a pausa e fecha a ajuda; com o jogo pausado, nada se move. Os botões do menu
   de pausa ainda são escolhidos com mouse/teclado.

### `dispositivos.json`

```json
{ "tipo": "DISPOSITIVOS_IRMAOS_MIAU", "versaoEsquema": 1,
  "jogadores": { "jogador1": "TECLADO", "jogador2": "GAMEPAD" } }
```

Guarda só a escolha (qual gamepad físico é decidido na partida), então um controle desconectado
nunca invalida o arquivo. Mesmas regras de segurança dos outros arquivos: nada é criado ao abrir o
jogo; arquivo inválido = todos no teclado + aviso, e o arquivo só vira `.corrompido-…` ao salvar.
`configuracoes.json`, `controles.json` e os perfis **não mudaram de formato**.

## Testes sem hardware

- `ProvedorGamepadSimulado`: conecta, desconecta e define botões/eixos na hora. Os testes do
  `GamePanel` real (`GamepadGamePanelTest`) instalam um com `ServicoGamepads.instalarGlobal(...)`.
- O teste do provedor físico (`SessaoDispositivosTest.provedorFisicoIniciaEEncerraSemHardwareNemExcecao`)
  inicia o GLFW de verdade e só exige que inicie/encerre sem erro — passa com ou sem controle.
- `mvn verify` roda também `JarExecutavelIT`: confere o manifesto e as DLLs no JAR e executa
  `java -jar … --diagnostico-gamepad 0` a partir do JAR final.

## Diagnóstico com um controle de verdade

```
java -jar target/AventuraIrmaosMiau-1.0-SNAPSHOT.jar --diagnostico-gamepad 20
```

Mostra se a leitura iniciou, os gamepads (nome e GUID) e, por 20 s, os botões apertados e as ações
que produzem para J1 e J2. Não abre o jogo nem grava nada.

## O que falta validar com hardware

- Um controle Xbox (XInput) e um PlayStation (DualShock 4 / DualSense), por USB e por Bluetooth:
  aparecem no diagnóstico? Os botões batem com os rótulos?
- Controles genéricos: se o GLFW não tiver mapeamento para eles, **não aparecem** (só controles com
  layout de gamepad são aceitos nesta entrega).
- Conectar e desconectar durante a partida; dois controles ao mesmo tempo.
- Sensação do analógico (zona morta 0,25 e limiares 0,50/0,35) e das cinco linhas.

## Decisões de game design pendentes

- O esquema definitivo de botões (e se o J2 também aplicará pegadas).
- Um botão contextual de técnica (`EXECUTAR_TECNICA`) no lugar de botões por técnica.
- Remapeamento de botões pela tela e navegação dos menus pelo gamepad.
- Pulo curto sem botão; analógico direito sem uso.
- Vibração.
