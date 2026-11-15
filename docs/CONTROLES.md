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

---

# Entrega 5E — Diagnóstico de joystick e mapeamentos

Pendência da 5D: um controle físico não foi reconhecido. **Ainda não houve teste com hardware;
nada aqui afirma que o problema foi corrigido.** O diagnóstico agora mostra em que etapa o
controle se perde.

## O que o diagnóstico mostra

```
java -jar target/AventuraIrmaosMiau-1.0-SNAPSHOT.jar --diagnostico-gamepad 20
```

1. **Biblioteca**: GLFW inicializado (versão) ou o motivo da falha.
2. **Joysticks detectados pelo sistema** (`glfwJoystickPresent`): porta, nome, **GUID**, número de
   eixos, botões e direcionais e se há mapeamento de gamepad (`glfwJoystickIsGamepad`).
3. **Gamepads utilizáveis no jogo**: só os que têm mapeamento.
4. **Mapeamentos extras**: arquivo usado, linhas válidas/ignoradas e se o GLFW os aceitou.
5. **Conclusão** com o próximo passo.

| O diagnóstico diz | Significa | O que fazer |
|---|---|---|
| Biblioteca INDISPONIVEL | o GLFW não carregou | ver o motivo; o jogo segue no teclado |
| 0 joysticks detectados | o Windows não entregou o aparelho ao GLFW | cabo/pareamento, `joy.cpl`, Steam Input/DS4Windows segurando o controle, modo XInput/DirectInput do controle |
| detectado, gamepad: NÃO | o aparelho existe, mas o GLFW não sabe o layout dele | fornecer um mapeamento SDL (abaixo) |
| gamepad: SIM | pronto | CONFIGURAÇÕES → CONTROLES → Gamepad |

## Joystick sem mapeamento

O GLFW aceita mapeamentos no formato do **SDL_GameControllerDB** (`glfwUpdateGamepadMappings`):

```
GUID,Nome,a:b0,b:b1,x:b2,y:b3,leftx:a0,lefty:a1,...,platform:Windows,
```

1. Rode o diagnóstico e anote o **GUID** do aparelho.
2. Procure esse GUID no `gamecontrollerdb.txt` do projeto SDL_GameControllerDB, ou gere a linha com
   uma ferramenta de mapeamento SDL. Se o GUID da base for diferente, use a linha trocando o GUID
   pelo mostrado no diagnóstico.
3. Salve as linhas num arquivo e indique-o de **uma** destas formas:
   - `-Dirmaosmiau.gamepad.mapeamentos=C:\caminho\meus_mapeamentos.txt`, ou
   - `gamecontrollerdb.txt` na pasta de dados (`%USERPROFILE%\.aventura-irmaos-miau\`).
4. Rode o diagnóstico de novo: o item 4 diz quantas linhas valeram e se o GLFW aceitou; o aparelho
   deve passar a "gamepad: SIM".

Regras de segurança:
- O jogo só **lê** esse arquivo; nunca cria nem altera.
- Linhas mal formadas não chegam ao GLFW e aparecem no diagnóstico com o número da linha.
- Arquivo ilegível ou sem linhas válidas é ignorado; a leitura de gamepad e o teclado continuam
  funcionando.

GLFW e SDL foram mantidos; nenhum driver HID novo. Só vale trocar a biblioteca se o diagnóstico com
o aparelho real mostrar que o Windows o entrega e mesmo assim o GLFW não o enxerga.

Observação: no Git Bash os acentos do diagnóstico podem aparecer trocados (página de código do
console). No Prompt de Comando, no PowerShell e no NetBeans aparecem normalmente.

---

# Entrega 5F — Remapeamento do gamepad e comandos contextuais

Contexto: o Xbox Wireless Controller foi validado fisicamente pelo desenvolvedor (XInput). O
joystick USB genérico sem mapeamento continua pendente (não faz parte desta entrega).

## Remapear botões do gamepad

CONFIGURAÇÕES → CONTROLES → escolha o jogador → **Editar: Gamepad**.

- A lista mostra cada ação (e, no esquema Contextual, as 4 direções de técnica e os 2 modificadores)
  com o botão atual. Clique e **aperte o botão real** no controle.
- Vale qualquer botão, direcional ou gatilho. Gatilho conta a partir de 0,5 e só solta abaixo de
  0,35 (histerese). O que já estava apertado quando a captura começou é ignorado até ser solto.
- **ESC** ou **Start** cancelam; **Backspace** deixa a ação **sem botão** (por exemplo, para tirar
  o "correr" do RB defeituoso). Start/Options e Guide são reservados.
- Botão já usado no mesmo jogador: a tela pergunta se deve **trocar**. Nenhum botão é obrigatório.
- Durante a captura o teclado fica preso na tela: nenhuma tecla aciona botões do menu. Não há
  partida aberta, então nada de combate dispara.
- RESTAURAR PADRÕES volta teclado, dispositivos e gamepad (os dois no Clássico); SALVAR grava;
  VOLTAR pergunta se deve salvar. Cada jogador tem os seus botões e o seu esquema.
- Arquivo próprio **`gamepad.json`** (tipo `MAPEAMENTO_GAMEPAD_IRMAOS_MIAU`, versão 1). Não é o
  `gamecontrollerdb.txt` (SDL), que só serve para o GLFW **reconhecer** joysticks genéricos.

## Esquemas

| | Clássico (5D) — padrão | Contextual (experimental) |
|---|---|---|
| Mover | D-pad e analógico | **só o analógico** (o D-pad vira técnicas) |
| A/Cruz | pular | pular |
| B/Bola | desequilibrar (J1) / romper (J2) | **agachar** |
| X/Quadrado | pegada (J1) | correr |
| Y/Triângulo | reforçar (J1) | rolar |
| LB/L1 | arrasto (com o analógico) | arrasto (com o analógico) **e** modificador da camada 3 |
| LT/L2 | agachar | **modificador da camada 2** |
| RT/R2 | sprawl | sprawl |
| RB/R1 | correr | **livre** |
| L3 / R3 | rolar / deitar | deitar / pulo curto |
| Back | respirar | respirar |
| D-pad | mover | **técnicas por contexto** |
| Start | pausa | pausa |

Escolher o esquema troca os botões do jogador pelos padrões daquele esquema (depois é só remapear).
Nada é migrado sozinho: quem já usava a 5D continua no Clássico.

## As 12 posições (Contextual)

| Camada | Combinação | Prioridade |
|---|---|---|
| 3 | LT + LB + direção | 1ª |
| 2 | LT + direção | 2ª |
| 1 | direção sozinha (LB sozinho também cai aqui) | 3ª |

Regras do `ResolvedorComandosContextuais`:
- Um toque produz **um** comando: dispara quando a direção acaba de ser apertada e é a única
  segurada. Diagonais ou uma segunda direção com outra segurada não disparam nada.
- Segurar não repete. Trocar LT/LB com a direção segurada não dispara outra coisa: é preciso soltar
  e tocar de novo.
- Pausa, perda de foco, desconexão e reconexão limpam o comando pendente; nada dispara até o D-pad
  ser solto.
- LT segurado suspende o arrasto do LB (LT+LB = camada 3). LB sozinho + analógico continua sendo o
  arrasto contínuo da 5B.

### O que cada posição faz hoje (`RepertorioContextual`)

| Contexto | Camada 1 (executa de verdade) | Camadas 2 e 3 (reservadas, "*") |
|---|---|---|
| Livre (em pé) | ↑ PEGAR (PEGADA_1) | — |
| Pegada, aplicador | ↑ REFORÇO · → DESEQUILIBRAR | LT+↑ QUEDA* · LT+LB+↑ GUARDA* (puxar) |
| Pegada, receptor | ↓ ROMPER | LT+LB+↑ GUARDA* (puxar) |
| Entrando (30 quadros) | nada | nada |

- As ações executáveis passam pelo **mesmo** caminho das teclas H/T/Y/U
  (`GamePanel.executarAcaoCombate`): custos, recargas, alcance, bloqueios e mensagens são os de sempre.
- Reservadas vêm do `RepertorioPosicional` (5E). Ao serem apertadas, avisam
  "QUEDA: ainda não disponível no jogo" e não mudam nada: posições de chão não são ativadas.
- Posições vazias avisam "nada em …". Nenhuma técnica foi inventada para preencher posições.
- O Jogador 2 ainda não aplica pegada (regra do motor), então livre ele não tem nada no D-pad.

### Indicador no HUD

Uma cruz pequena sob o painel de cada jogador que está no Contextual (y 66–128, 138 px), com a
camada segurada ("J1 · LT + D-pad") e o que cada direção faz agora:
- branco = disponível;
- cinza = existe, mas não dá agora (recarga, alcance, firmeza cheia);
- "*" = reservada;
- "·" = vazia.

A faixa de dicas mostra, por exemplo, "D-pad→ desequilibrar · D-pad↑ reforçar". A ajuda da pausa
lista as 3 camadas com os botões configurados.

## Teclado

O sistema contextual recebe `EntradaContextual` (4 direções + 2 modificadores), sem saber de onde
vem. Nesta entrega só o gamepad a produz; o teclado continua com H, T, U, Y e o arrasto, sem
nenhuma tecla nova. Para o teclado usar as 12 posições no futuro: 4 ações de direção + 2
modificadores no `MapaControles` (6 teclas, nunca 12), alimentando o mesmo resolvedor.

## Limitações (5F)

- Remapeamento só para gamepads reconhecidos (com mapeamento GLFW); o joystick genérico continua
  dependendo de um mapeamento SDL (5E).
- Os menus ainda não são navegáveis pelo gamepad.
- Camadas 2 e 3 só têm transições reservadas até as posições de chão ganharem visual e lógica.

---

# Entrega 5G — Ergonomia do gamepad

| Botão | Antes (5F) | Agora (5G), Clássico e Contextual |
|---|---|---|
| A/Cruz | pulo alto (Clássico) / pulo (Contextual) | **pulo normal** (`pularCurto`, custo 3,5) |
| RB + A | — | **pulo alto** (o `pular` que já existia, custo 8) |
| R3 | deitar/levantar (Clássico) / pulo curto (Contextual) | **rolamento** (com o analógico inclinado) |
| L3 | rolamento (Clássico) / deitar (Contextual) | **deitar/levantar** |
| Y/Triângulo (Contextual) | rolar | livre |

- O jogo já tinha os dois pulos: `pular()` (alto) e `pularCurto()`. Nada de física nova.
- **Modificador do pulo alto** é um item da tela CONTROLES → Editar: Gamepad (grupo "PULO ALTO"),
  nos dois esquemas. Pode ser qualquer botão livre; conflitos com as camadas do D-pad (LT/LB) são
  detectados como os demais. Sem modificador, A é sempre o pulo normal. "Pular" (alto) também pode
  ganhar um botão próprio.
- O tipo de pulo é decidido **no instante em que o A é apertado** e vale até soltar. RB + A dá **só**
  o pulo alto; apertar ou soltar o RB no meio não dispara outro pulo; segurar não repete.
- No Clássico o RB continua sendo "correr" quando segurado (dividir o RB entre correr e pulo alto é
  permitido: RB + A = pulo alto correndo). Para deixar o RB totalmente livre, tire o botão dos dois.
- Numa troca, se o botão antigo é dividido (RB) e o outro item não pode dividi-lo, esse item fica
  **sem botão** e a tela avisa. O mapa nunca fica inválido.
- **Preferências salvas:** um `gamepad.json` igual ao padrão antigo (nunca personalizado) passa para
  os padrões novos **com aviso** ao abrir o jogo. Um personalizado é mantido como está, com aviso de
  que os novos padrões estão em RESTAURAR PADRÕES. O arquivo só muda ao salvar.
- `MapaGamepad.experimental()` continua sendo o layout histórico da 5D (diagnóstico e testes antigos).
- Teclado sem mudança: ESPAÇO pula alto, V pula curto.

---

# Entrega 5H — Analógico direito, camadas e arrasto automático (Contextual)

O esquema **Clássico não mudou**. Tudo abaixo vale só para jogadores no **Contextual**.

## Técnicas no analógico direito (RS)

O polegar esquerdo fica no analógico esquerdo, que move; o direito escolhe as técnicas. Os dois
funcionam ao mesmo tempo. O D-pad continua como alternativa e é remapeável como na 5F. RS e D-pad
alimentam o **mesmo** `ResolvedorComandosContextuais` e o mesmo `RepertorioContextual`.

`LeitorAnalogicoTecnicas`:

| Parâmetro | Valor | Efeito |
|---|---|---|
| Zona morta radial | 0,25 | ruído de repouso não conta |
| Ativar | 0,60 (normalizado) | só inclinação intencional |
| Voltar ao neutro | 0,30 (normalizado) | histerese: precisa voltar ao centro para armar de novo |
| Abertura de cada direção | ±30° do eixo | perto da diagonal (faixa de 30°) nada é escolhido até assentar num eixo |

- **Uma técnica por inclinação.** Segurar não repete. Girar para outra direção sem voltar ao centro
  não troca a direção.
- **R3:** o clique do analógico direito desarma a leitura até ele voltar ao neutro, então a
  oscilação do clique nunca vira técnica. R3 continua sendo o rolamento, na direção do analógico
  **esquerdo**.
- Pausa, perda de foco, desconexão, reconexão e reinício desarmam (precisa voltar ao neutro).
- RS e D-pad juntos em direções diferentes não disparam nada.

## Camadas (5H)

| Combinação | Camada | Conteúdo hoje |
|---|---|---|
| RS (ou D-pad) | 1 | pegar · reforçar · desequilibrar · romper (existentes) |
| LT + RS | 2 | reservadas da 5E (ex.: QUEDA*) |
| LB + RS | 3 | reservadas da 5E (ex.: puxar GUARDA*) |
| LT + LB + RS | avançada (4) | vazia (reservada) |

Prioridade: os dois modificadores > um modificador > nenhum. Uma combinação nunca dispara ação de
outra camada. Na 5F a camada 3 era LT + LB e o LB sozinho caía na camada 1; isso mudou a pedido.

## Arrasto automático

No Contextual, **andar para os lados com o analógico esquerdo** vale como "modo controle" (antes:
segurar LB). O `GamePanel` só arrasta quando o jogador é o **aplicador** de uma pegada **estabelecida**,
exatamente a regra da 5B. Por isso:
- velocidade reduzida (1 px/tick), limites do dojo, linhas e orientação preservados, e o desgaste
  extra de firmeza do arrasto (5B, nenhum custo novo);
- o **receptor** andando continua rompendo a pegada (movimento/alcance), nunca arrasta o aplicador;
- mudar de linha (cima/baixo) durante a pegada continua rompendo (regra 3A);
- sem pegada, ou depois do rompimento, o analógico volta a ser caminhada normal na hora;
- J1 e J2 têm cada um a sua fonte; hoje só o J1 aplica pegada (regra do motor).

O LB fica livre para a camada 3. O "modo controle" continua remapeável: um botão atribuído a ele
também funciona. No Clássico, nada mudou: LB + analógico arrasta e andar sem LB rompe.

## Padrões e preferências salvas

- Contextual 5H: igual ao da 5G, mas sem "modo controle" no LB (o arrasto é automático).
- Um `gamepad.json` igual a um padrão antigo (5F ou 5G) e nunca personalizado passa ao padrão da 5H
  **com aviso**. Personalizados ficam como estão; o analógico direito vale para eles também.

## Planejamento: defesa alta e baixa (não implementado)

Usos atuais do **Y**:
- teclado: tecla Y = reforçar a pegada (J1), remapeável;
- gamepad Clássico: Y/Triângulo = reforçar a pegada (J1);
- gamepad Contextual: Y/Triângulo **livre** desde a 5G (o reforço está no RS ↑ quando aplicador).

Proposta para quando existirem ataques altos e baixos reais:
- Contextual: **Y segurado = defesa alta**; **LT + Y segurado = defesa baixa**. O LT só muda a
  camada quando combinado com RS/D-pad, então LT + Y fica livre.
- Seriam duas ações contínuas novas (`DEFESA_ALTA`, `DEFESA_BAIXA`) com tipo de acionamento
  CONTÍNUA, resolvidas pelo motor contra o tipo do golpe recebido.
- No Clássico o Y continua sendo o reforço; a defesa lá precisará de outro botão, a decidir.
- Nada foi implementado: não existe defesa só visual, nem dano, hitbox ou colisão de golpes.

---

# Entrega 5I — Branco e Preto jogáveis, papéis independentes do slot

## Auditoria: o que o Preto podia fazer (até a 5H) e agora

| Capacidade | Até a 5H (slot J2 / Preto) | 5I |
|---|---|---|
| Andar, correr, agachar, pular, pulo curto, rolar, deitar, respirar, sprawl | sim | sim (igual) |
| Iniciar PEGADA_1 | **não** (sem tecla, sem botão; `slot.usa` = false) | sim: END / X-Quadrado (Clássico) / RS ↑ (Contextual) |
| Desequilibrar, reforçar | **não** | sim, quando for o aplicador: PAGE_DOWN, PAGE_UP / B, Y / RS |
| Arrastar (modo controle) | **não** (`atualizarMiauPreto` não tinha arrasto) | sim, quando for o aplicador: DELETE + setas / LB + direção / automático no Contextual |
| Romper a pegada | sim (U) | sim (U) — e agora o J1 também rompe (tecla 5 / B) |
| Treino controlando o Preto | **não**: o treino abria com o Branco; o slot do jogador recusava o Preto | sim: escolha na tela do treino |
| Preto no slot do J1 / Branco no do J2 | **não** (`criarDoSlot` lançava erro) | sim, qualquer combinação (inclusive o mesmo irmão nos dois) |

O motor (`SistemaCombate`) já era genérico: `tentarAcao(atacante, defensor, "pegada_1")` e as
ações técnicas conferem o papel real. A limitação estava só na entrada (`AcaoJogador` sem tecla
para o J2, `slot.usa`), no `GamePanel` (arrasto só no J1) e na seleção.

## Slot, identidade e papel

- **Slot** (J1 / J2): de onde vêm as teclas/gamepad. Não muda durante a luta.
- **Identidade** (Branco passador / Preto guardeiro, oficial ou perfil): escolhida na seleção.
  Não muda atributos, poses nem renderizadores.
- **Papel** (livre / entrando / aplicador / receptor): vem do `SistemaCombate` a cada tick
  (`PapelCombate.de`). Quem toca a pegada primeiro e está no alcance vira o aplicador.

O aplicador pode reforçar, desequilibrar e arrastar; o receptor pode romper. As ações de um papel
que o lutador não tem só mostram o aviso de sempre ("só com a pegada estabelecida").

## Teclas novas (padrão; todas remapeáveis em CONTROLES)

| Ação | J1 | J2 |
|---|---|---|
| Iniciar pegada | H | **END** |
| Desequilibrar | T | **PAGE DOWN** |
| Reforçar | Y | **PAGE UP** |
| Modo controle (arrastar) | SHIFT | **DELETE** |
| Romper | **5** | U |

Gamepad: os dois slots usam a mesma tabela. Clássico: X/Quadrado pega, Y reforça, **B
desequilibra quando aplicador e rompe quando receptor**, LB + direção arrasta. Contextual:
RS conforme o papel (a cruz já mostrava só o que vale) e arrasto automático (5H) para J1 e J2.

`controles.json` e `gamepad.json` salvos antes: as teclas que faltam recebem o padrão (como desde a
5C). Um `gamepad.json` igual a um padrão antigo (5F/5G/5H) passa ao padrão da 5I com aviso; um
personalizado fica como está, com aviso se o Clássico não tiver botão para a pegada.

## Entradas simultâneas (determinístico)

1. Os toques do tick são lidos para os dois slots e despachados na ordem
   pegar → desequilibrar → reforçar → romper, J1 antes do J2 em cada ação (a ordem da 5B).
2. **Pegada ao mesmo tempo:** vale a do J1; a do J2 é descartada sem aviso, porque a interação
   já começou nesse tick. Nunca há duas pegadas.
3. **Desequilibrar e romper no mesmo toque** (o B do Clássico): vale a do papel real —
   receptor rompe, aplicador desequilibra, livre fica com o desequilíbrio (que só avisa).

## PEGADA_1 com papéis invertidos

Nada mudou nos JSONs, nos arrays nem no renderizador. O `SistemaCombate` já entrega os quadros
de aplicador ao aplicador e os de receptor ao receptor, cada um com o seu deslocamento, e o
`GamePanel` espelha quem está virado para a esquerda. Teste: Branco aplicando da esquerda vs Branco
aplicando da direita (J2) → mesma distância entre a dupla e silhuetas espelhadas (IoU > 0,95) em
cinco quadros da entrada e na pegada estabelecida; a dupla não é simétrica (controle < 0,85).
Duração, custo, firmeza e alcance são os mesmos para os dois slots (testado).

## Treino

A tela do treino sempre aparece (antes abria direto com o Branco quando não havia perfis). A
coluna JOGADOR escolhe o irmão controlado; trocar o irmão do jogador troca o parceiro para o
outro irmão (o parceiro pode ser trocado depois). O jogador usa os controles do J1 e faz tudo,
inclusive pegada e arrasto; o parceiro continua passivo, sem IA. F5 reinicia com o mesmo irmão.

## Seleção

Cada coluna é um slot (JOGADOR 1 / JOGADOR 2) com um seletor **IRMÃO** (Miau Branco / Miau
Preto) e o combo de oficial + perfis daquele irmão. O padrão continua Branco no J1 e Preto no J2.
Os botões de jogar sozinho mostram o irmão escolhido na coluna. No gamepad: ↑↓ escolhe o campo,
←→ troca o valor, A confirma.

---

# Entrega 5I (complemento) — Menus sem mouse

## Navegação (todas as telas e a pausa)

| | Gamepad | Teclado |
|---|---|---|
| Mover o foco | D-pad ou analógico esquerdo | setas |
| Mudar valor (combo, slider) | ← → | ← → |
| Confirmar | A | ENTER (ESPAÇO como antes) |
| Voltar / cancelar | B | ESC (na partida o ESC continua sendo a pausa) |
| Pausar / continuar | Start | ESC |

- Foco visível: borda amarela no botão/campo em foco. O mouse continua funcionando.
- Analógico: zona morta 0,5 e volta ao neutro abaixo de 0,3; segurar repete depois de 400 ms,
  um passo a cada 140 ms.
- Sem disparo acidental: a cada troca de tela (e ao abrir/fechar a pausa) o gamepad precisa voltar
  ao repouso antes do próximo comando. Não depende do RB.
- Diálogos (salvar / descartar / cancelar): navegáveis; B fecha = cancelar.
- Na tela de controles, durante a captura de tecla/botão, o navegador não age.
- Na pausa: foco inicial em CONTINUAR; B = continuar. Ao sair da pausa, nenhum toque que ainda
  esteja segurado (A, pulo, pegada...) dispara; movimento segurado continua.

## Ficou para a próxima entrega (dividido de propósito)

- Pausa completa com **Configurar controles** e **Configurações** dentro da partida (exige
  reaplicar o mapa de teclas e do gamepad com a partida viva; hoje o `InputHandler` e o
  `MapaGamepad` são fixados na criação do `GamePanel`).
- **Músicas originais** por tela (menu, configurações, treino, combate, seleção) e **SFX de
  interface** (navegar, confirmar, voltar, escolher irmão, indisponível, abrir/fechar pausa). Nada
  foi improvisado como música. A infraestrutura atual (`AudioJogo.tocarMusica`, uma faixa por
  vez, volume das configurações, arquivo ausente = silêncio) continua a base.
- Fundo animado do menu: nada foi bloqueado (o navegador só mexe em foco e cliques).

---

# Entrega 5J — De joelhos

- Na seleção (partida e treino) há o campo **INÍCIO: De joelhos / Em pé**. Padrão: **De joelhos**. A
  escolha fica lembrada até fechar o jogo (não é gravada em arquivo). Navegável por mouse, setas e
  D-pad/analógico (←→ trocam o valor).
- **De joelhos:** os dois começam ajoelhados, frente a frente, na mesma linha, a 84 px (dentro do alcance
  da PEGADA_1), simétricos em relação ao centro, sem pegada. F5 e TENTAR NOVAMENTE mantêm.
- Ajoelhado: andar = deslocamento lento (18% da velocidade normal), sem levantar. Pegar, reforçar,
  desequilibrar, romper e arrastar funcionam como em pé (mesmas regras e teclas).
- Comandos incompatíveis são ignorados de joelhos: pulo, pulo curto, rolamento, sprawl, correr e agachar.
- **DEITAR/LEVANTAR (F / J / L3) levanta.** Ainda não há comando para voltar a ajoelhar (só o início).
  Levantar no meio de uma pegada a encerra (motivo "deitar ou levantar", como deitar).
- Como em pé, a caminhada normal não tem borda própria (o limite da arena vale para o arrasto).
- F3 (HUD técnico) mostra a postura dos dois, aplicador e receptor, a variante da animação, e se os
  passos do arrasto/reação ao desequilíbrio (fallback) estão ativos.

---

# Entrega 5K — Posturas por F/L3 e fôlego

## Comando de postura (DEITAR_LEVANTAR: F no J1, J no J2, L3 no Xbox; remapeável)

**Clique simples** — uma transição por clique, no ciclo:

EM PÉ → AJOELHADO → DEITADO → AJOELHADO → EM PÉ

De joelhos, o lutador lembra de onde veio (`IrmaoMiau.isCicloSubindo`): veio de em pé → o próximo clique
deita; veio de deitado → o próximo clique levanta. No início de joelhos conta como "veio de em pé".

**Duplo clique** (dois toques em até 250 ms = 16 ticks lógicos):

| Postura | Duplo clique | Resultado |
|---|---|---|
| Em pé | F/L3 + F/L3 | deitado direto |
| Ajoelhado (descendo ou subindo) | F/L3 + F/L3 | em pé direto |
| Deitado | F/L3 + F/L3 | em pé direto |

- Bordas reais: segurar não repete. O 2º toque dentro da janela vira **uma** ação rápida (um custo, uma
  animação); o simples pendente não executa. Um 3º toque começa uma nova contagem.
- **Compromisso de ergonomia:** o clique simples só acontece quando a janela fecha (~250 ms depois do
  toque), porque até lá ele ainda pode virar duplo. O duplo responde na hora do 2º toque. A janela é uma
  constante (`ReconhecedorCliqueDuplo.JANELA_MS_PADRAO`); vale testar no Xbox se 250 ms está confortável.
- Pausa, perda de foco e F5 descartam o clique pendente. O botão usado para sair do menu não vira postura.
- **Na pegada (aplicador ou receptor) a postura não muda** (aviso "POSTURA SÓ FORA DA PEGADA"): nada de fuga
  gratuita. Pulo, rolamento, sprawl e a entrada de uma técnica também bloqueiam.
- Sem fôlego para a transição: nada acontece (nem parte dela) e aparece "SEM FÔLEGO".

## Início da luta

O padrão continua **de joelhos** (5J). O campo INÍCIO da seleção foi mantido (De joelhos / Em pé): ele
não atrapalha o fluxo e preserva o início em pé para treinos, testes e estilos futuros.

## Fôlego (valores; todos em `entities.CustosFolego`, exceto os de combate que já existiam)

| Item | Antes (5J) | 5K | Observação |
|---|---|---|---|
| Caminhar em pé | **recuperava** enquanto andava | ~0,10/s (10 s ≈ 1 ponto) e não recupera | cobrado pela distância real |
| Andar de joelhos | recuperava | ~0,045/s (fator 2,5 por distância) | metade do custo de andar em pé |
| Rastejar deitado | recuperava | ~0,035/s (fator 5 por distância) | |
| Correr | 0,12/tick | igual | sem custo de caminhada somado |
| Pulo alto / curto / rolamento / sprawl | 8 / 3,5 / 6 / 4 | igual | o rolamento não conta como caminhada |
| Em pé → joelhos / joelhos → deitado | — | 0,5 / 0,5 | |
| Deitado → joelhos / joelhos → em pé | — | 0,8 / 1,2 | |
| Rápida: em pé → deitado | — | 2,5 | lento: 1,0 |
| Rápida: deitado → em pé | — | 4,0 | lento: 2,0 |
| Rápida: joelhos → em pé | — | 2,0 | lento: 1,2 |
| Pegada / reforço / desequilíbrio / rompimento | 3 / 3 / 4 / 3 | igual | não duplicado |
| Arrasto (só o aplicador) | só firmeza | + 0,0025 por px real (~0,16/s) | receptor não paga; dupla parada não paga |
| Recuperação parado | 0,008 + recuperação × 0,0002 por tick (~1,1/s) | igual | não recupera no tick de esforço |

- Parado em qualquer postura recupera; nenhuma postura estática gasta.
- Alternar posturas sempre gasta (não existe recuperação "artificial").
- Fôlego nunca negativo; recupera até o máximo de cada irmão (Branco e Preto continuam diferentes).
- Ninguém fica preso sem fôlego: parado sempre recupera e os custos de postura são pequenos.

## Desequilíbrio

A consequência mecânica já existia (5B) e foi mantida: com **sucesso**, o receptor fica DESEQUILIBRADO por
120 ticks (~2 s) com velocidade a 60%; o aplicador paga 4. Quem perde a disputa não paga fôlego (regra
5B, preservada). Tentativa inválida ou resistida não aplica nada. O HUD (F3) mostra a consequência
("DESEQUILIBRADO: velocidade 60% (N ticks)") e a reação visual da 5J só aparece com o efeito do motor.
Futuro: a mesma janela de instabilidade pode reduzir a defesa contra quedas e raspagens.

## Atalho de desenvolvimento

- **F6** alterna as animações de pegada entre AUTORAL (padrão) e EXPERIMENTAL; vale a partir da próxima
  pegada e fica até fechar o jogo. Também por `-Dirmaosmiau.animacoes.pegada=experimental`.
- **F3** mostra postura e direção do ciclo, clique pendente/duplo, transição ativa, fôlego atual/máximo,
  último custo, firmeza, consequência do desequilíbrio, arrasto, variante e origem da animação.

---

# Entrega 5L — Defesa de pegadas e quatro pegadas

## Causa da fuga gratuita (corrigida)

A `PoliticaInteracaoPadrao` (3A/5B) encerra a pegada estabelecida quando o corpo de qualquer um dos dois
muda: X ou Y (MOVIMENTO), pulo, rolamento, sprawl, agachar, deitar/levantar. O `GamePanel` deixava o
**receptor** andar, mudar de linha, pular, rolar, agachar e fazer sprawl normalmente. Então bastava ao
receptor andar 1 px: o X mudava e a política encerrava por MOVIMENTO. (A postura já estava bloqueada na 5K.)

## Regra nova (receptor de pegada estabelecida)

- Fica **preso**: não anda, não muda de linha, não pula, não rola, não agacha, não faz sprawl, não muda de
  postura. Os comandos não soltam nada.
- Apertar uma direção vira **resistência**: a firmeza cai mais rápido (+0,12 por tick, além do desgaste
  normal de 0,10) e o receptor gasta 0,004 de fôlego por tick (~0,25/s). Sem fôlego, forçar não conta (e
  quem força sem fôlego também não recupera). O aplicador pode reforçar.
- Saídas legítimas: **ROMPER** (disputa de atributos + firmeza; resistido = a pegada fica, sucesso =
  encerra), firmeza chegando a zero (SEM_CONTROLE), e o próprio aplicador soltar (andar sem o modo controle,
  pular etc., como antes). Forçar reduz a firmeza, o que ajuda o ROMPER seguinte.
- O aplicador continua arrastando (Clássico com modificador, Contextual automático).
- ROMPER **não é instantâneo**: depende da disputa e tem recarga de 45 ticks. O minijogo de precisão fica
  para o futuro; o ponto de troca é `SistemaCombate.tentarRompimentoTecnico`.

## Evasão ANTES do contato

Quando alguém tenta uma pegada e o defensor está:
- **abaixando a tempo** — o comando de agachar (C / M / LT), de pé ou **de joelhos**, nos primeiros
  12 ticks (~190 ms) depois de apertado. Segurar mais tempo não protege. Custa 1,5 de fôlego a quem evita;
- **rolando** ou **no ar** (pulo) — sem custo extra (o rolamento e o pulo já custam);

então a pegada é **EVITADA** (resultado DEFENDIDO): nada começa, o atacante paga metade do custo da
pegada e aparece "EVITADA". Depois do contato (entrada ou pegada estabelecida) nada disso funciona.

## Quatro pegadas

| | Pegada | Teclado J1 / J2 | Xbox (livre) | Alcance | Fôlego | Firmeza inicial | Reforço | Romper | Prepara | Especialista |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Gola e manga | H / END | RS ↑ (e X no Clássico) | 125 | 3,0 | 100 | ×1,0 | ×1,0 | queda | — (igual à 5K) |
| 2 | Cruzada | 6 / NUM 7 | RS → | 115 | 3,5 | 100 | ×0,8 | ×0,85 (difícil) | queda | Branco |
| 3 | Duas mangas | 7 / NUM 8 | RS ↓ | 135 | 2,5 | 85 | ×1,2 | ×1,15 (fácil) | guarda | Preto |
| 4 | Gola e nuca | 8 / NUM 9 | RS ← | 110 | 4,0 | 95 | ×1,0 | ×0,9 | queda | Branco |

- Especialista: rompimento 10% mais difícil e reforço 10% maior para quem domina a pegada. Atributos
  dos personagens não mudam.
- "Romper": a disputa do ROMPER vê a firmeza dividida pelo fator (×1,15 = parece mais fraca).
- Alcances: no início de joelhos (84 px = 120 u) a 1 e a 3 já alcançam; a 2 e a 4 exigem chegar mais perto.
- **Comandos:** livre, o RS escolhe a pegada nos **dois esquemas** (no Clássico o RS só faz isso). Com
  pegada, o RS do Contextual volta a reforçar (↑), desequilibrar (→) e romper (↓ do receptor); no Clássico
  continuam os botões. LT, LB e LT+LB continuam reservados. D-pad no Contextual = as mesmas 4 pegadas.
  Teclado: as novas teclas são remapeáveis em CONTROLES.
- Pegadas simultâneas: continua valendo a do J1 (regra 5I, agora para as quatro).

---

# Entrega 5L.1 — HUD do J2 e balanceamento

## HUD de comandos do J2

A cruz de comandos (RS) do J2 aparece no lado direito também quando o J2 está no **teclado**, com o mesmo
desenho, as mesmas setas, o mesmo "RS" e os mesmos nomes do J1 (livre: PEGAR / CRUZADA / MANGAS / NUCA;
na pegada: REFORÇO / DESEQ. / ROMPER pelo papel). Nada muda no J1, no Clássico nem no treino. A entrada
não mudou: o J2 continua podendo usar um gamepad.

## Balanceamento (valores em `SistemaCombate`)

| Parâmetro | Antes (5L) | Depois (5L.1) |
|---|---|---|
| Custo de iniciar pegada | 3 / 3,5 / 2,5 / 4 | igual |
| Custo de reforçar | 3 | **2** |
| Ganho do reforço | +25 | **+30** (recarga 60 ticks, igual) |
| Custo de desequilibrar | 4 | **3** |
| Custo de romper | 3 | **2,5** |
| Desgaste natural da firmeza | 0,10/tick (~16,7 s) | **0,06/tick (~27 s)** |
| Desgaste extra do receptor forçando | 0,12/tick | **0,08/tick** (forçando sem parar: ~12 s) |
| Insistência (novo) | — | **+8% no ataque por tentativa fracassada seguida** da mesma ação, na mesma pegada, até +32%; zera no sucesso e no fim da pegada |
| Recuperação de fôlego | ~1,1/s parado | igual |
| Fórmula da disputa, atributos, especialidades | — | iguais |

**Causa da dificuldade:** a disputa é determinística. Com os oficiais, o Preto **nunca** desequilibrava o
Branco (em nenhuma firmeza) e só rompia a pegada do Branco com firmeza abaixo de ~40; repetir a tentativa dava
sempre o mesmo resultado. A insistência resolve a causa sem sorteio: quem perde uma vez consegue em poucas
tentativas (cada uma custa fôlego e recarga), e ninguém rompe ou desequilibra "de graça" na primeira se for
mais fraco naquilo. O Branco continua desequilibrando o Preto de primeira (diferença de atributos mantida).

## Proposta: arrasto multidirecional (próxima entrega; nada mudou agora)

- Hoje: 5 linhas de profundidade (`IrmaoMiau.moverVertical`, `ESPACO_ENTRE_LINHAS`). A pegada acaba ao
  mudar de linha porque a política compara o Y com o retrato do estabelecimento (MOVIMENTO) e limita a
  diferença de linhas (LINHAS). `SistemaCombate.deslocarPegadaControlada` só move X (os dois o mesmo dx) e
  translada os retratos só em X (`EstadoCorporalObservado.transladado(dx)`).
- Menor mudança segura: `deslocarPegadaControlada(aplicador, dx, dy)` movendo os dois pelo mesmo passo de
  linha (mesma regra de borda de linha 1..5 para os dois, senão nada se move), `transladado(dx, dy)` nos
  retratos, a mesma velocidade lenta e o mesmo custo por distância (incluindo |dy|). A `CamadaPoseCombate`
  já avança os passos pela distância: basta somar |Δy|. Diagonal = dx e dy no mesmo tick.

## Telas de menu (Entrega 5M)

| Tela | J1 | J2 | Gamepad |
|---|---|---|---|
| Título | ENTER / ESPAÇO | ENTER / ESPAÇO | A ou START (só depois de soltar) |
| Menu principal | setas, ENTER, ESC | — | D-pad/analógico, A, B (qualquer controle) |
| Seleção (grade) | W A S D, ESPAÇO ou H confirma, Q cancela | setas, ENTER ou END confirma, BACKSPACE cancela | cada slot associado a GAMEPAD usa o seu controle (D-pad, A, B) |

Na seleção, ESC volta ao menu. Cada tecla age uma vez por aperto. Com o J1 pronto, ↑/↓ escolhem
MAPA / TEMPO / LUTAR e ←/→ mudam o valor; só o J1 configura a partida.

Treino (5M.3): a mesma grade, com um cursor só: W A S D ou setas, ESPAÇO/H/ENTER/END confirmam, Q ou
BACKSPACE voltam, e qualquer gamepad conectado guia o J1. ↑/↓ escolhem MAPA / PARCEIRO / INÍCIO / TREINAR!.

Arrasto multidirecional (5N): com a pegada estabelecida, o aplicador segura o modo controle (J1 SHIFT, J2
DELETE; gamepad Clássico LB; Contextual automático pelo analógico) e usa as quatro direções ou as diagonais:
a dupla anda junta para os lados e entre as linhas 1–5.

## Famílias de técnicas, defesas e acrobacias (Entrega 5O)

Mapeamento real (padrão; os dois esquemas, salvo indicação):

| Botão | Clássico | Contextual |
|---|---|---|
| RS sozinho | 4 pegadas (↑ gola e manga, → cruzada, ↓ duas mangas, ← gola e nuca) | idem (+ D-pad e camadas LT/LB) |
| **RT + RS** | **defesas**: ↑ guarda alta, ← guarda média, → guarda baixa, ↓ sprawl | idem |
| **RB + RS** | **quedas e transições** (reservado: o HUD mostra "EM BREVE", nada executa) | idem |
| RB | corrida (segurado) + modificador do pulo alto (RB + A) | modificador do pulo alto |
| R3 + direção | rolamento técnico (sem mudança) | idem |
| **Y + direção do movimento** | acrobacia (livre); com a pegada, Y continua sendo o reforço | acrobacia |
| B | desequilibrar / romper | agachamento suave |
| LB | modo controle (arrasto) | camada 3 |

Mudança de propósito: o RT sozinho era o sprawl; agora o RT é o modificador das defesas e o sprawl é
**RT + RS ↓** (no teclado continua G / I). Um `gamepad.json` nunca personalizado da 5N é atualizado sozinho.

Prioridade: **RT vence RB**; com um modificador, a direção nunca pega. Guardas ficam enquanto a direção
estiver segurada; sprawl e quedas saem uma vez por inclinação (volta ao neutro para repetir). Soltar o RT
com o RS ainda inclinado não vira pegada.

Teclado (equivalentes lógicos — ALT foi evitado: no Windows abre o menu da janela, e o AltGr do ABNT2 é
Ctrl+Alt): J1 **3** = defesas, **4** = quedas, **9** + WASD = acrobacia; J2 **INSERT**, **HOME**,
**NUM 5** + setas. A direção da técnica são as teclas de pegada (H/6/7/8 e END/NUM 7/8/9).

Acrobacias (direção relativa ao corpo): para onde olha = mortal para a frente; para trás = mortal para
trás; fundo/frente da tela = estrela para a esquerda/direita do corpo.


# Entrega 5O.1 — R3 contextual, RT respira, Select = RB e teclado-controle do J2

O que mudou em relação à 5O (o resto continua igual):

| Comando | 5O | 5O.1 |
|---|---|---|
| R3 + direção (J1 teclado: R + direção) | rolamento técnico (só em pé) | **em pé: acrobacia** · **de joelhos: rolamento** · **deitado: rolamento mais lento** |
| Y + direção (J1 teclado: 9) | acrobacia | **nada** (Y livre; no Clássico o Y continua sendo o **reforço** da pegada) |
| RT sozinho (J1: 3; J2: P) | nada | **respirar** (sustentado; a mesma mecânica de antes) |
| RT + RS | guardas e sprawl | igual (a defesa vence a respiração) |
| Select / View | respirar | **RB lógico** (pulo alto com A, quedas com o RS, corrida no Clássico) |
| Start | pausa | pausa (sem mudança) |
| Guarda segurada | o lutador ficava parado | anda a **50%** (`DefesaCorporal.MULTIPLICADOR_VELOCIDADE`), sem corrida |

## Teclado do J2 = um controle (`controles.LayoutTecladoControle`)

O J2 no teclado não usa mais "teclas soltas": cada tecla faz o papel de um botão do Xbox e passa pelo
**mesmo caminho do gamepad** (`TecladoControle` → `FonteGamepad` → `MapaGamepad` do J2, esquema Clássico
ou Contextual). Teclado e Xbox fazem exatamente a mesma coisa.

| Xbox | Teclado J2 |
|---|---|
| Analógico esquerdo ↑ ↓ ← → | W S A D |
| Analógico direito ↑ ↓ ← → | setas ↑ ↓ ← → |
| A / B / X / Y | K / L / J / I |
| L3 / R3 | Z / M |
| D-pad ↑ → ↓ ← | T / H / G / F |
| RB / RT / LB / LT | O / P / E / Q |
| Start / Select | ENTER / SHIFT |

- **P** = RT: sozinho respira; com uma seta = defesas. **O** ou **SHIFT** = RB lógico.
- **ENTER** só **abre** a pausa; com a pausa aberta, o ENTER confirma o item do menu (não alterna duas vezes).
- Os dois SHIFTs valem como Select (o Swing não separa esquerdo/direito nos atalhos).
- Voltar às teclas livres configuráveis do J2 (as de antes): `-Dirmaosmiau.teclado.j2=livre`.

## Teclado do J1 (colisões resolvidas com a menor mudança)

O J2 passou a usar W A S D, setas, K L J I, Z M, T H G F, O P E Q, ENTER e SHIFT. Só as teclas do J1 que
batiam nessas mudaram:

| Ação do J1 | 5O | 5O.1 |
|---|---|---|
| Mover ↑ ← ↓ → | W A S D | **NUM 8 / NUM 4 / NUM 5 / NUM 6** |
| Correr | Z | **NUM 0** |
| Deitar / levantar | F | **1** |
| Pegada 1 | H | **2** |
| Desequilibrar | T | **0** |
| Modo controle (arrasto) | SHIFT | **CTRL** |
| Sprawl direto | G | **-** (o sprawl também sai em 3 + 7, RT + baixo) |
| Acrobacia | 9 | (removida: R + direção em pé) |

Sem mudança: ESPAÇO, V, C, R, X, Y, 3, 4, 5, 6, 7, 8. Nas teclas livres do J2 (modo `livre`), a pegada 3
passou de NUM 8 para NUM 2. Regra nova: nenhuma tecla do J1 pode ser uma tecla do teclado-controle do J2
(`ProblemaControle.Tipo.TECLA_DO_CONTROLE`; a tela de CONTROLES recusa). Um `controles.json` nunca
personalizado da 5O é atualizado sozinho; num J1 personalizado, só a ação que usava uma tecla do J2 fica
sem tecla, com aviso. O `gamepad.json` padrão da 5O também é atualizado (Select livre); a ação ACROBACIA
salva em arquivos antigos é ignorada.

## Famílias por modificadores (`controles.ResolvedorModificadores`)

| Combinação | Família | Situação |
|---|---|---|
| (nenhum) + RS | pegadas | ativa |
| RT + RS | defesas | ativa (prioridade 1) |
| RB (ou Select) + RS | quedas e transições | ativa, técnicas reservadas ("EM BREVE") |
| LT + RS, LB + RS | — | reservadas (LT/LB continuam agachar/arrasto e as camadas) |
| RT + LB, RT + LT, LT + LB | — | reservadas |
| RT + LT + LB | — | reservada especial (três botões nunca são o padrão) |

Vale a combinação **ativa** mais específica contida no que está segurado; empate = prioridade. As
reservadas nunca disparam nada e não mudam as ativas (RT + LB continua sendo defesas).

## Custos e velocidades (5O.1)

| Movimento | Fôlego | Tempo |
|---|---|---|
| Rolamento técnico (joelhos ou deitado) | **4,0** (antes 6,0) | 22 ticks de joelhos; 37 deitado (×0,6 de velocidade, mesma distância) |
| Estrela | 5,0 | 30 ticks |
| Mortal para a frente / trás | 7,0 | 32 / 34 ticks |
| Sprawl | 4,0 + recarga de 30 ticks | 26 ticks |
| Guarda | 0,3 para entrar, 0,15 para trocar; não recupera | andando: caminhada × 1,0 (a metade por segundo) |


# Entrega 5O.2 — Contextual como padrão, J2 rompendo e arrastando, respiração por postura

## Esquema de controles × versão das animações

- **Esquema de controles** (CONFIGURAÇÕES → CONTROLES → Gamepad, por jogador): **Clássico** ou
  **Contextual (experimental)**. Vale para o Xbox **e** para o teclado-controle do J2 (que lê o mesmo mapa
  do slot). Desde a 5O.2 o padrão é o **Contextual** nos dois jogadores (`MapaGamepad.ESQUEMA_PADRAO`).
- **Versão das animações** (F7): ORIGINAL × EXPERIMENTAL 5O do rolamento e do sprawl. Não tem nada a ver
  com o esquema de controles.
- O F3 mostra `ESQUEMA J1 … J2 …` (CONTEXTUAL, CLASSICO ou TECLAS para as teclas livres).
- As teclas livres do J1 não têm esquema: são ações diretas (5 rompe, CTRL + direção arrasta).

Migração: um slot salvo no **Clássico com os botões padrão** e **sem** a marca `esquemaEscolhido` (arquivos
de antes da 5O.2) é tratado como "nunca escolhido" e passa para o Contextual, com aviso. Salvar em
CONTROLES grava `"esquemaEscolhido": true`, e a partir daí a escolha é respeitada. Um Clássico
personalizado (botões trocados) nunca é alterado.

## Causa real dos dois defeitos do J2

1. **Esquema errado (causa principal).** `MapaGamepad.padrao()` punha os dois slots no **Clássico** e o
   teclado-controle do J2 usa o mapa do J2. No Clássico:
   - o RS só escolhe pegada com o lutador **livre** (`consumirPegadaClassica`); agarrado, RS ↓ (seta ↓)
     não fazia nada — romper era o **B (L)**;
   - o arrasto exige **segurar o LB (E)** junto com o LS; o arrasto automático só existe no Contextual.
   O J1, no Xbox já configurado no Contextual, rompia e arrastava; o J2 não. Por isso "acontecia com os dois
   Miaus": era o slot, não o personagem.
2. **Arrasto diagonal recuperava fôlego (valia para os dois jogadores).** Na diagonal cada eixo anda 0,707 px
   por tick; nos ticks em que a fração ainda não completava 1 px ninguém andava, e o aplicador ganhava a
   recuperação passiva de fôlego. Agora esses ticks contam como esforço (`SistemaCombate`, sem custo e sem
   recuperação).

Nada no arrasto usa índice fixo de jogador: o motor usa o aplicador real (`getAplicadorInteracao`) e cada
slot lê o próprio dispositivo. Os testes cobrem J1→J2 e J2→J1, com os personagens trocados de slot,
teclado-controle e Xbox simulado.

## J2 no Contextual (teclado-controle)

| Tecla | Botão | No Contextual |
|---|---|---|
| WASD | LS | andar; sendo o aplicador, **arrasta** (automático, 8 direções) |
| setas | RS | livre: as 4 pegadas · aplicador: ↑ reforço, → desequilíbrio · agarrado: **↓ romper** |
| K | A | pulo (O/SHIFT + K = pulo alto) |
| L | B | agachamento suave |
| J | X | correr (nunca pega) |
| I | Y | livre |
| Z / M | L3 / R3 | postura / mobilidade (R3 + WASD) |
| T H G F | D-pad | técnicas pelas camadas (não anda) |
| O / SHIFT | RB / Select | modificador de quedas, pulo alto |
| P | RT | sozinho respira; + seta = defesas |
| E / Q | LB / LT | camadas 3 e 2 |
| ENTER | Start | abre a pausa |

## Respiração por postura (`CustosFolego`)

| Postura | Recuperação (× em pé) | Velocidade andando enquanto respira (× normal) |
|---|---|---|
| Em pé | 1,0 (15× a recuperação parada, como antes) | 0,15 (como antes) |
| De joelhos | **0,7** | **0,08** (andar de joelhos é 0,18) |
| Deitado | **0,4** | **0,03** (rastejar é 0,07) |

- A velocidade respirando é **uma** penalidade (fração da velocidade normal), não multiplicada pela da
  postura.
- Respirar não muda a postura; não respira no ar, rolando, no sprawl, numa acrobacia nem no meio de uma
  transição. RT + direção = defesa (sem respirar). Deitado, RT + direção não vira guarda (solo) e não respira.
- De joelhos e deitado o peito e a cabeça sobem e descem 1–2 px (só no desenho).


## Fôlego durante a pegada (Entrega 5O.3)

Enquanto existir uma pegada envolvendo o lutador — **aplicando ou sofrendo**, da entrada até o fim do
vínculo — ele **não recupera** fôlego nem condicionamento: nem respirando (RT ou a tecla de respirar) nem
parado. Vale durante arrasto, reforço, desequilíbrio, resistência e rompimento. O consumo normal continua
(arrasto, resistência, técnicas). Segurar o RT agarrado não recupera nem cobra nada a mais, e não quebra a
pegada. Ao acabar a pegada, a recuperação volta pelas regras normais (respiração por postura da 5O.2: em pé
100%, de joelhos 70%, deitado 40%).

Implementação: o `SistemaCombate` marca `IrmaoMiau.setVinculadoEmPegada` nos dois lutadores no início da
interação e desmarca no fim (e num combate novo); as quatro funções de recuperação do `IrmaoMiau` param
enquanto a marca está ligada. Não depende de tecla, esquema nem índice de jogador.
