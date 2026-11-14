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
