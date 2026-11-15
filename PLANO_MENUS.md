# Plano de menus — Aventura dos Irmãos Miau (Microentrega 5L.3)

Planejamento da renovação visual das telas (5L.3). **Status: título, menu principal e seleção em grade implementados na Entrega 5M** (MAPA e TEMPO funcionais; DIFICULDADE "EM BREVE"; transições animadas e retratos grandes desenhados à mão ficam para depois).

Direção artística: o *clima* dos jogos de luta 16-bit (telas de apresentação fortes, retratos grandes,
grade de seleção, letreiros com contorno, cores saturadas, transições rápidas), com **identidade própria
dos Irmãos Miau**. Não reaproveitar layouts, logotipos, fontes, sons, personagens nem elementos visuais
de jogos existentes; tudo é desenhado a partir dos nossos próprios polígonos, cores e cenários.

---

## 1. Auditoria do que existe hoje

| Peça | Classe / arquivo | O que faz hoje |
|---|---|---|
| Janela e fluxo | `main/Main.java` | Cria o `JFrame`; troca a tela inteira com `trocarTela(janela, painel)`. Métodos: `mostrarMenu`, `mostrarSelecao`, `iniciarJogo`, `abrirTreino`, `mostrarSelecaoTreino`, `iniciarTreino`, `mostrarConfiguracoes`, `mostrarControles`, `mostrarPerfis`. Toca `AudioJogo.MUSICA_MENU`. Instala o `NavegadorMenus` (5I). |
| Menu principal | `ui/MenuPanel.java` | Título "AVENTURA DOS IRMÃOS MIAU" + COMEÇAR, TREINO, CONFIGURAÇÕES, PERSONAGENS PERSONALIZADOS. Não tem SAIR. |
| Seleção (partida e treino) | `ui/SelecaoLutadoresPanel.java` | Duas colunas (J1/J2) com seletor IRMÃO + combo oficial/perfis, atributos, INÍCIO (De joelhos/Em pé). Na partida, **mistura quantidade de jogadores com personagem**: botões MIAU BRANCO / MIAU PRETO (= jogar sozinho com a coluna) e OS DOIS MIAUS. No treino: COMEÇAR TREINO. |
| Seleção antiga | `ui/SelecaoPersonagemPanel.java` | Fora do fluxo desde a 4D (pode ser removida numa limpeza futura). |
| Configurações / controles / perfis | `ui/ConfiguracoesPanel`, `ui/ControlesPanel`, `ui/PerfisPanel` | Áudio e cenário; remapeamento teclado/gamepad; editor de perfis personalizados. |
| Estilo comum | `ui/EstiloTelas.java` | Fundo, título, rótulo, botão (cores e fontes das telas). |
| Navegação sem mouse | `ui/NavegadorMenus`, `ui/TelaNavegavel`, `dispositivos/LeitorNavegacao` | D-pad/analógico/setas, A/ENTER confirma, B/ESC volta (botão marcado com `marcarVoltar`), foco com borda amarela, repetição controlada, sem "vazamento" do botão entre telas. |
| Dados dos personagens | `personagens/CatalogoPersonagens`, `DefinicaoPersonagem` (id, nome, estilo, descrição, atributos, visual, fábrica), `EscolhaLutador`, `selecao/OpcoesDeLutadores` | Branco e Preto oficiais + perfis personalizados por base. |
| Partida / treino | `game/GamePanel`, `treino/ConfiguracaoTreino` | Recebem as escolhas (`EscolhaLutador`, `InicioLuta`). |
| Recursos visuais | `assets/sprites/miau-*.png`, renderizadores `IrmaoMiauBranco/Preto`, `cenario/CenarioDojo` | Retratos simples (PNG) e desenho poligonal em tempo real. |
| Música | `audio/musica/menu.wav`, `treino.wav` | Uma faixa por vez (`AudioJogo`). |

**Não existe hoje:** tela de abertura, opção SAIR, modo contra o computador, sistema de graduação/faixa
(no código, "faixa" é só a faixa de dicas do HUD e o desenho da faixa no quimono), personagens bloqueados.

---

## 2. Fluxo proposto

```
ABERTURA ──(qualquer botão / ENTER / A)──► MENU PRINCIPAL
                                             │
     ┌─────────────┬─────────────┬───────────┼──────────────┬────────┐
     ▼             ▼             ▼           ▼              ▼        ▼
   JOGAR         TREINO     PERSONAGENS  CONFIGURAÇÕES     SAIR   (B/ESC: nada)
     │             │             │           │
     ▼             ▼             ▼           ▼
 1 JOGADOR*    SELEÇÃO (modo   PERFIS     CONFIGURAÇÕES → CONTROLES
 2 JOGADORES   treino: jogador (atual)    (atuais)
     │         + parceiro)
     ▼             │
 SELEÇÃO (J1/J2)   ▼
     │          TREINO
     ▼
   LUTA ──(pausa: menu principal / trocar personagem / tentar novamente — já existem)
```

\* **1 JOGADOR** = modo contra o computador, **ainda não existe**. Proposta: mostrar a opção desabilitada
com a etiqueta "EM BREVE" (não selecionável; o navegador já pula componentes desabilitados) — ou
simplesmente não mostrar. Recomendação: **não mostrar** até existir IA, para não oferecer algo que não
funciona. O "jogar sozinho" atual (um lutador sem adversário) é um modo de teste, que pode ir para dentro
de TREINO como "Livre (sem parceiro)", se ainda for útil.

B/ESC volta uma tela em todo o fluxo; no menu principal não sai do jogo (sair só por SAIR, com confirmação).

---

## 3. Esboços (800 × 450)

### 3.1 Abertura

```
┌──────────────────────────────────────────────────────────────┐
│  céu de fim de tarde em faixas (gradiente em 4-5 tons)       │
│        ┌────────────────────────────────────────┐            │
│        │   AVENTURA DOS IRMÃOS MIAU              │ ← letreiro │
│        │   (letras grossas com contorno e sombra)│   original │
│        └────────────────────────────────────────┘            │
│   [BRANCO]  em guarda, de frente          [PRETO] em guarda   │
│   (renderizador real, escala 1.2,         (espelhado)         │
│    respiração/idle já existentes)                             │
│  ════════ tatame do dojo / bandeirolas do torneio ═════════   │
│              APERTE START / ENTER  (pisca a cada 0,5 s)       │
│  v5L                                        © Irmãos Miau     │
└──────────────────────────────────────────────────────────────┘
```

- Cenário: reaproveitar o desenho do `CenarioDojo` (tatame, parede) com uma camada de "torneio"
  (bandeirolas e faixas de cor desenhadas em código). Nada de imagens externas.
- Personagens: os próprios `IrmaoMiauBranco`/`IrmaoMiauPreto` desenhados pelo renderizador (idle),
  frente a frente.
- Transição: letreiro desce em ~0,4 s; "APERTE START" pisca; ao confirmar, um *wipe* horizontal curto
  (0,2 s) para o menu. Música: a do menu (`menu.wav`), sem sobreposição.

### 3.2 Menu principal

```
┌──────────────────────────────────────────────────────────────┐
│   AVENTURA DOS IRMÃOS MIAU                (letreiro menor)    │
│                                                              │
│   ▶ JOGAR                         [Branco e Preto pequenos,   │
│     TREINO                         lado a lado, idle, à       │
│     PERSONAGENS                    direita da lista]          │
│     CONFIGURAÇÕES                                             │
│     SAIR                                                      │
│                                                              │
│  A/ENTER confirmar · B/ESC voltar · ↑↓ escolher               │
└──────────────────────────────────────────────────────────────┘
```

- JOGAR abre um submenu na própria tela: **2 JOGADORES** (e "1 JOGADOR" só quando existir IA).
- PERSONAGENS = a tela de perfis atual (`PerfisPanel`), renomeada no menu.
- Foco: seta ▶ + a borda amarela já existente do `NavegadorMenus`.

### 3.3 Seleção de personagens (futura)

```
┌──────────────────────────────────────────────────────────────┐
│  ESCOLHA SEU MIAU                     INÍCIO: [De joelhos ◀▶] │
│ ┌───────────────┐   ┌──┬──┬──┬──┐   ┌───────────────┐        │
│ │  J1           │   │BR│PR│🔒│🔒│   │           J2  │        │
│ │  prévia grande│   ├──┼──┼──┼──┤   │ prévia grande │        │
│ │  (renderizador│   │🔒│🔒│🔒│🔒│   │ (espelhada)   │        │
│ │   real, idle) │   └──┴──┴──┴──┘   │               │        │
│ │ MIAU BRANCO   │   cursor J1 = moldura vermelha      │        │
│ │ Passagem de   │   cursor J2 = moldura azul          │        │
│ │ guarda        │   (os dois no mesmo retrato = moldura│       │
│ │ FAIXA: [◀ ▶]* │    dividida em duas cores)          │        │
│ │ ▮▮▮▮▯ técnica │                                     │        │
│ │ ▮▮▮▯▯ força   │                                     │        │
│ │ PERFIL: [◀ ▶] │                                     │        │
│ │   [PRONTO]    │                         [PRONTO]    │        │
│ └───────────────┘                    └───────────────┘        │
│  B/ESC voltar · A confirmar · ◀▶ mover na grade              │
└──────────────────────────────────────────────────────────────┘
```

- **Grade de retratos:** 2 desbloqueados (Branco, Preto) + espaços futuros com cadeado e silhueta escura
  ("???"). Os espaços bloqueados são **só visuais**: não há personagens cadastrados para eles. A grade lê o
  `CatalogoPersonagens`; os cadeados preenchem o resto até o tamanho da grade.
- **Retratos:** desenhados pelo próprio renderizador (cabeça e tronco recortados) ou os PNG atuais; nada
  externo.
- **Prévia grande:** o lutador real em idle, nas cores do personagem/perfil escolhido.
- **Informações:** nome, estilo (`DefinicaoPersonagem.getEstilo()`), atributos em barras (as de hoje).
- **PERFIL:** escolhe oficial ou perfil personalizado daquele irmão (o combo atual), com ◀▶.
- **FAIXA (graduação)\*:** só aparece para personagens que suportarem; hoje **nenhum suporta** (não existe
  sistema de graduação). O espaço fica reservado no layout e escondido até existir a regra. Não inventar
  efeitos de faixa sem regra definida.
- **J1 e J2 independentes:** cada um move o seu cursor (J1: WASD/gamepad do J1; J2: setas/gamepad do J2)
  e confirma com PRONTO; personagens iguais permitidos. Quando os dois estão prontos, a luta começa.
  No treino: as colunas viram JOGADOR e PARCEIRO (o J1 escolhe os dois, como hoje).
- **INÍCIO (De joelhos/Em pé):** mantido (padrão De joelhos), no topo.

---

## 4. Componentes reaproveitáveis

| Componente | Uso no plano |
|---|---|
| `Main.trocarTela` e os métodos `mostrar*` | Fluxo novo é só uma reordenação: `mostrarAbertura` → `mostrarMenu` → `mostrarJogar` … |
| `NavegadorMenus` + `TelaNavegavel` + `marcarVoltar` | Navegação por teclado/gamepad, foco visível, voltar. A seleção com dois cursores precisa de uma leitura **por slot** (ver riscos). |
| `EstiloTelas` | Cores/fontes base; acrescentar o "letreiro" (texto com contorno e sombra). |
| `SelecaoLutadoresPanel.Slot`, `OpcoesDeLutadores`, `EscolhaLutador` | Dados de cada lado (irmão, perfil, atributos); a nova tela troca só a apresentação. |
| `CatalogoPersonagens` / `DefinicaoPersonagem` | Lista de personagens da grade, nome, estilo, atributos. |
| Renderizadores `IrmaoMiauBranco/Preto` + animações (idle, `AnimadorPartida`) | Prévias e retratos sem imagens novas. |
| `CenarioDojo` | Fundo da abertura e do menu. |
| `AudioJogo` | Música do menu/abertura (uma por vez). |
| `InicioLuta`, `ConfiguracaoTreino`, `GamePanel` | Sem mudança: recebem as mesmas escolhas. |

---

## 5. Arquivos a alterar (quando implementar)

- **Novos:** `ui/AberturaPanel.java`, `ui/MenuJogarPanel.java` (ou submenu dentro do `MenuPanel`),
  `ui/SelecaoGradePanel.java` (nova seleção), `ui/Letreiro.java` (desenho do título retrô).
- **Alterar:** `main/Main.java` (fluxo: abertura, JOGAR → 2 JOGADORES, SAIR, renomear PERSONAGENS),
  `ui/MenuPanel.java` (opções novas e SAIR), `ui/EstiloTelas.java` (letreiro e cores).
- **Manter até a troca final:** `ui/SelecaoLutadoresPanel.java` (fica como fallback até a nova seleção
  ser validada; depois o modo "jogar sozinho" sai dele).
- **Testes:** `MenuPanelTest`, `SelecaoLutadoresPanelTest`, `SelecaoIrmaos5ITest`, `InicioLuta5JTest`,
  `NavegadorMenusTest` precisarão acompanhar as telas novas.
- **Docs:** `CONTROLES.md` (navegação), `REGRESSAO.md` (roteiro das telas).

---

## 6. Plano em entregas pequenas

| Entrega | Escopo | Risco |
|---|---|---|
| **M1 — Menu principal** | JOGAR → 2 JOGADORES; TREINO; PERSONAGENS (perfis); CONFIGURAÇÕES; SAIR com confirmação. A seleção atual continua sendo usada, sem os botões "jogar sozinho". | Baixo |
| **M2 — Abertura** | `AberturaPanel` com letreiro, irmãos em idle, dojo/torneio, "APERTE START", transição curta. | Baixo |
| **M3 — Letreiro e estilo** | `Letreiro` + `EstiloTelas` aplicados ao menu, à seleção e às configurações (só visual). | Baixo |
| **M4 — Seleção em grade (um cursor)** | Grade (Branco, Preto, cadeados), prévia grande, nome/estilo/atributos, PERFIL ◀▶, INÍCIO. O J1 escolhe os dois lados em sequência (como o treino hoje). | Médio |
| **M5 — Dois cursores** | J1 e J2 escolhendo ao mesmo tempo, cada um pelo seu dispositivo; PRONTO dos dois. | Médio-alto (entrada por slot nos menus) |
| **M6 — Faixa** | Só quando existir a regra de graduação: seletor de faixa e efeito definido. | Depende da regra |
| **M7 — 1 JOGADOR** | Só quando existir IA. | Depende da IA |

Cada etapa com `mvn clean verify`, validação no NetBeans e no Xbox, e a tela anterior como fallback até
a aprovação.

---

## 7. Riscos para a seleção e a navegação atuais

1. **Navegação de dois cursores:** o `NavegadorMenus` lê *qualquer* gamepad e move *um* foco. Para J1 e J2
   ao mesmo tempo, a seleção precisa ler a entrada **por slot** (teclado do J1/J2 e gamepad de cada
   slot, como a `SessaoDispositivos` faz na luta). Fazer isso só dentro da tela de seleção, sem mudar o
   navegador das outras telas.
2. **Associação de dispositivos:** quem está no teclado e quem está no gamepad é definido em
   CONFIGURAÇÕES; a seleção deve respeitar a mesma associação (hoje: J1 Xbox, J2 teclado nos testes do
   usuário) e não criar regra nova.
3. **Botão que confirma não pode vazar** para a tela seguinte (já resolvido pelo `LeitorNavegacao`;
   manter o "desarmar" em toda troca de tela, inclusive abertura → menu).
4. **Perfis personalizados:** a grade mostra os irmãos; os perfis entram como variações (PERFIL ◀▶), não como
   retratos novos, para não poluir a grade nem simular personagens.
5. **Treino:** hoje usa a mesma tela com outro modo; a nova seleção precisa manter jogador/parceiro e o F5.
6. **Testes de UI existentes** dependem dos botões atuais (MIAU BRANCO/PRETO/OS DOIS MIAUS); a troca deve
   ser feita com a tela antiga ainda disponível e os testes migrados com justificativa.
7. **Cadeados sem personagens:** não cadastrar personagens falsos no catálogo; os espaços bloqueados são
   puramente visuais e não podem ser selecionados.
8. **Escala/performance:** desenhar os lutadores na abertura e na seleção usa o mesmo renderizador da luta;
   manter um único timer de animação por tela e parar ao sair (como o `GamePanel` faz).
