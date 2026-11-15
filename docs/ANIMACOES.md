# Animações — Entregas 5E e 5G

## Camadas

```
identidade      ──► PerfisAnimacoes (por id do CatalogoPersonagens: miau_branco, miau_preto)
movimento       ──► IrmaoMiau / GamePanel / SistemaCombate          (não sabem de animação)
estado          ──► ControladorAnimacoes.estadoVisual(lutador, combate) ─► EstadoVisual
animação        ──► PerfilAnimacoes.resolver(estado) ─► DefinicaoAnimacao (CatalogoAnimacoes)
dados visuais   ──► LEGADA_CODIGO (renderizador atual) | JSON_INDIVIDUAL | MIAUSTUDIO_CONJUNTA
```

- `EstadoVisual`: PARADO, TAUNT, CAMINHADA, CORRIDA, AGACHADO, PULO, ROLAMENTO, SPRAWL, DEITADO,
  MOVIMENTO_CHAO e os conjuntos PEGADA_ENTRADA, PEGADA_ESTABELECIDA, ARRASTO_CONJUNTO.
- `ControladorAnimacoes` é o **ponto central** para saber qual animação um lutador usa. Ele só
  lê: nunca move, nunca muda estado nem pose. A prioridade é a mesma do renderizador atual.
- `CatalogoAnimacoes.padrao()` registra todas as animações atuais do Branco e do Preto como
  **legadas** (fallback), as duas animações JSON migradas e as conjuntas (PEGADA_1 e arrasto).
- `PerfilAnimacoes` usa composição: um perfil pode ter um **base** de mesmo rig e substituir só
  o que quiser. A resolução vai própria → base → nada; nada = o renderizador legado desenha.
- Perfis personalizados (4D) usam o perfil do personagem base: mudam atributos, não animações.

## O que foi migrado para JSON

| Animação | Situação | Prova |
|---|---|---|
| Movimento no chão do **Branco** | **JSON com paridade** (`conteudo/animacoes/individuais/branco_movimento_chao.json`) | dados = array legado; quadro no tempo = conta legada; **pixel a pixel igual** nos 5 quadros, olhando para os dois lados |
| Movimento no chão do **Preto** | **JSON com paridade** (`preto_movimento_chao.json`) | idem |
| Caminhada (Branco/Preto) | **legada** (adaptador no catálogo) | procedural: senoide `getOscilacaoPasso` (amplitude 10) aplicada a cada articulação em `desenharEmPe`; não há quadros para converter sem inventar poses |
| Corrida | **legada** | mesma senoide com amplitude 15 |
| Arrasto conjunto (5B) | **legado** | não existe animação: a pose final da PEGADA_1 é transladada inteira |
| Parado/idle, agachado, pulo | legadas | procedurais |
| Taunt, rolamento, sprawl | legadas | arrays MiauStudio, mas o rolamento/sprawl são **interpolados por progresso** e o taunt mistura cabeça própria com corpo procedural; converter exige o mesmo cuidado de paridade (próximo passo natural do taunt) |
| PEGADA_1 | conjunta (inalterada) | `PEGADA_1.json` + `RegistroAnimacoesCombate`; catálogo só referencia |

Os arrays originais e os renderizadores **não foram alterados**. Os JSON foram **gerados a partir
dos próprios arrays** (leitura por reflexão), não digitados à mão.

### Como o JSON é desenhado

`ReprodutorPoseJson` usa o canal de pose externa que Branco e Preto já têm (o mesmo da PEGADA_1).
Ele chama `desenharPoseMiauStudio` do próprio personagem com o deslocamento visual do arquivo. Não
sobrescreve a pose do combate e só limpa a pose que ele mesmo aplicou.

**No jogo, os perfis padrão continuam apontando para as versões legadas.** Ligar o JSON é trocar
uma linha do perfil (`comAnimacao(MOVIMENTO_CHAO, "branco.movimento_chao.json", …)`) e fazer o
`GamePanel`/renderizador consultar o `ControladorAnimacoes` a cada quadro. Isso foi deixado para
uma entrega com validação visual no NetBeans, para não arriscar a 5E.

## Formato `ANIMACAO_INDIVIDUAL_IRMAOS_MIAU` (versão 1)

```json
{
  "tipo": "ANIMACAO_INDIVIDUAL_IRMAOS_MIAU", "versaoEsquema": 1,
  "id": "branco.movimento_chao", "estado": "MOVIMENTO_CHAO", "rig": "miau_v1", "uso": "INDIVIDUAL",
  "loop": true, "duracaoQuadroMs": 150, "sequencia": [0, 1, 2, 3, 4, 3, 2, 1],
  "deslocamentoVisual": { "x": 0, "y": 70 }, "espelhamento": "LADO_DO_LUTADOR",
  "origem": "IrmaoMiauBranco.MOVIMENTO_CHAO_MIAUSTUDIO (...)",
  "quadros": [ { "pose": [30 inteiros do MiauStudio] }, ... ]
}
```

- `rig`: `miau_v1` = pose de 30 valores do MiauStudio (Branco e Preto). Outros rigs são recusados.
- `espelhamento`: `LADO_DO_LUTADOR` (o `GamePanel` espelha quem olha para a esquerda, como hoje) ou `NUNCA`.
- `duracaoQuadroMs` mantém a fidelidade com o legado (que conta milissegundos);
  `indiceQuadroNoTick(ticks, 1000/60)` permite contar pelo loop lógico.
- Validação: tipo, versão, campos conhecidos, 30 valores por pose, sequência dentro dos quadros,
  estado não conjunto, `uso = INDIVIDUAL`. Pontos de referência e eventos ficam para quando houver
  uso real.
- **Não altera** o `PEGADA_1.json` nem o formato de projeto do MiauStudio.

## Um personagem novo trocando só a caminhada

```java
PerfilAnimacoes boxeador = PerfilAnimacoes.baseadoEm("gato_boxeador", perfis.buscar("miau_branco").get())
        .comAnimacao(EstadoVisual.CAMINHADA, "boxeador.caminhada", catalogo);
// CORRIDA, PULO, ... continuam vindo do Branco (herdadas); CAMINHADA é a própria.
```

Validado em teste com um perfil **hipotético** (não é lutador nem perfil registrado no jogo). Um
rig diferente é recusado ao trocar a animação. Modalidades (boxe, judô, karatê, sumô) serão
composição de perfil de animações + repertório de técnicas + capacidades, sem subclasses por
modalidade.

## Animações conjuntas e root motion

- PEGADA_1: 30 quadros, pose final, posições relativas, espelhamento, firmeza e profundidade
  **não mudaram** (o catálogo só referencia o `RegistroAnimacoesCombate`).
- **Movimento físico** ≠ **movimento visual**. `SincronizadorPassos` é o contrato para uma futura
  animação de arrasto: o quadro é escolhido pela **distância física percorrida** (o deslocamento
  5B, 1 px/tick), não pelo tempo. Parado, os pés não andam; o arrasto mais rápido acelera os passos.
- Sem animação de arrasto cadastrada, o fallback é a pose final transladada (5B), testado com
  as poses aprovadas da PEGADA_1.

---

# Entrega 5G — Animações JSON ativas nas partidas

## O que roda pelo JSON agora

| Animação | Fonte no jogo | Prova |
|---|---|---|
| Movimento no chão do **Branco** (rastejar) | **JSON** `branco_movimento_chao.json` | teste de integração no `GamePanel` real: a pose desenhada é o quadro do JSON, pixel a pixel igual ao legado |
| Movimento no chão do **Preto** | **JSON** `preto_movimento_chao.json` | idem |
| Deitado parado | legado (é o quadro 1 do mesmo array/JSON) | sem salto ao começar/parar |
| Caminhada, corrida, idle, taunt, agachado, pulo | legado (procedural) | inalterado |
| Rolamento, sprawl | legado (arrays interpolados) | inalterado |
| PEGADA_1 e arrasto | conjunta do MiauStudio (inalterada) | poses aprovadas conferidas em teste |

## Como funciona (`animacao.AnimadorPartida`)

Uma chamada por **tick lógico** no `GamePanel.atualizar()`, depois do movimento e do combate:

1. `ControladorAnimacoes.estadoVisual` lê o lutador e o combate (só leitura).
2. O perfil do personagem (`PerfisAnimacoes.global()`, por classe → id de personagem) devolve a
   animação daquele estado.
3. **JSON** → o quadro sai do tempo da própria animação (150 ms por quadro, sequência ping-pong
   `0,1,2,3,4,3,2,1`), contado em ticks desde que o estado começou (16 ms por tick). A pose vai para
   o renderizador do próprio Branco/Preto pelo canal de pose da PEGADA_1
   (`ReprodutorPoseJson` → `desenharPoseMiauStudio`, deslocamento Y 70 como no legado).
4. **Legado**, ou JSON ausente/inválido → solta a pose e o código antigo desenha.

Garantias:
- **Não move ninguém.** x, y e linha seguem as regras do jogo; teste compara o trajeto
  tick a tick com e sem JSON.
- **Nunca passa por cima do combate.** Se o `SistemaCombate` tem a pose (PEGADA_1/arrasto), o
  animador não aplica nem limpa nada.
- **Começo e fim coerentes.** Começar a rastejar mostra o 1º quadro, igual ao deitado parado. Parar,
  levantar, entrar em pegada ou perder o foco soltam a pose na hora.
- **Pausa.** O relógio lógico para, e o quadro congela e continua de onde estava.
- **Mudança de direção.** A mesma animação continua (o espelhamento é do `GamePanel`, como antes).
- **Reinício do treino (F5).** Os lutadores novos começam do zero e os antigos são esquecidos.

## Fallback e diagnóstico

`PerfisAnimacoes.global()` carrega o catálogo do jogo **uma vez** (`CatalogoAnimacoes.paraJogo`).
JSON ausente ou inválido não entra no catálogo; o perfil fica com a animação legada daquele estado e
o motivo vai **uma única vez** para o `System.err`, por exemplo:
`[animacoes] /conteudo/animacoes/individuais/branco_movimento_chao.json ausente: ... usa o desenho legado.`
Nada é logado por quadro nem por partida.

## Inspeção (testes)

`GamePanel.getAnimador().origem(lutador)` → `OrigemVisual(fonte, idAnimacao, quadro, estado)`.
Os testes `AnimacoesRuntimeGamePanelTest` usam isso para provar que o quadro desenhado vem do JSON
e **falham** se a ligação for removida (sem o animador no `GamePanel`, ou com o perfil sem o JSON).

## Personagem futuro

O runtime não testa "Branco"/"Preto": `PerfisAnimacoes.registrarClasse(Classe, id)` + um perfil
(`baseadoEm` + `comAnimacao`) bastam. Testado com uma subclasse **só de teste** e uma animação JSON
própria de rig `miau_v1`.

## Próximas evoluções sugeridas

1. Taunt (arrays do MiauStudio, tempo fixo) pelo mesmo caminho, com teste de paridade.
2. Rolamento e sprawl: estender o formato com "quadro por progresso" (são interpolados por progresso,
   não por tempo) antes de migrar.
3. Um canal de pose de **animação** separado do de combate, quando houver animações que convivam com
   a pegada.
4. Caminhada e corrida próprias em JSON (com `SincronizadorPassos`, por distância) para personagens
   novos; Branco e Preto continuam procedurais.

---

# Entrega 5J — Variantes por identidade e papel, postura de joelhos e camadas da pegada

## Três coisas diferentes (nunca misturar)

| Conceito | O que é | Quem decide |
|---|---|---|
| **Slot** | J1 / J2: de onde vêm teclas e gamepad | seleção |
| **Molde (`IdentidadeRig`)** | geometria do rig: BRANCO ou PRETO (perfis usam o do base) | classe do lutador |
| **Papel** | aplicador / receptor da técnica | `SistemaCombate` |
| **Lado / orientação** | esquerda ou direita da tela | posição; o `GamePanel` espelha quem olha para a esquerda |

As poses do MiauStudio estão no espaço **local** do lutador (sempre desenhadas olhando para a direita;
o editor e o jogo espelham o personagem inteiro pela orientação). Por isso **espelhar a cena não
troca papéis nem dados**: a mesma variante serve com o aplicador à esquerda ou à direita.

## Como uma técnica escolhe a animação

A técnica continua apontando para UMA animação base (`tecnicas.json`: `"animacao": "pegada_1_entrada"`).
Ao iniciar a técnica, `RegistroAnimacoesCombate.resolver(base, aplicador, receptor)`:

1. **AUTORAL** — existe uma animação (base ou variante) desenhada para (molde do aplicador, molde do
   receptor): ela é usada inteira.
2. **COMPOSTA_POR_PAPEL** — senão, cada trilha vem de uma animação em que aquele molde tem aquele
   papel. Ex.: Branco × Branco = trilha do aplicador da base (Branco aplicador) + trilha do receptor da
   variante do Preto (Branco receptor). Nenhum deslocamento é copiado de um molde para outro.
3. **BASE** — sem metadados ou molde desconhecido: a base, como até a 5I.

| Dupla (aplicador × receptor) | Resultado |
|---|---|
| Branco × Preto | `pegada_1_entrada` (autoral, original) |
| Preto × Branco | `pegada_1_entrada_preto` (autoral, nova) |
| Branco × Branco | composta: aplicador da original + receptor da nova |
| Preto × Preto | composta: aplicador da nova + receptor da original |

A escolha acontece uma vez, no início da técnica (nada troca no meio). O HUD técnico (F3) mostra a
variante e se ela é autoral ou composta.

## Registrar uma animação conjunta nova (convenção)

1. Desenhe no MiauStudio em modo CONJUNTA, com `aplicadorConjunto` correto (Branco ou Preto). O lado da
   tela em que você desenhou não importa.
2. Copie o projeto para `src/main/resources/conteudo/miaustudio/` com um nome identificado (o original
   fica onde estava). Só o `nomeAnimacao` pode ser ajustado na cópia.
3. Em `conteudo/animacoes/animacoes.json`:
   - técnica nova: uma entrada normal (`id`, `formato`, `arquivo`, `duracaoFrames`);
   - outra dupla para uma técnica existente: a mesma entrada **+ `"varianteDe": "<id da base>"`**.
4. O carregador lê do próprio projeto quem aplica e quem recebe. Uma variante precisa ter o mesmo número
   de quadros, a mesma duração, a mesma distribuição temporal e o mesmo offset visual da base; se não
   tiver, o jogo recusa com o arquivo e o campo no erro.

O formato JSON v2 do MiauStudio **não mudou**: os metadados novos ficam só no catálogo do jogo.

## Achado da auditoria: deslocamento horizontal entre editor e jogo

No projeto, `offsetCentroX` é posição de **cena** (o editor desenha em `centroInteracaoX + offset`). O
motor usa a variação em relação ao quadro 1 no espaço **local** do lutador. As duas coisas coincidem
quando quem se desloca foi desenhado olhando para a direita, e se invertem quando foi desenhado olhando
para a esquerda.

- `PEGADA_1_PRETO`: quem se desloca é o Branco receptor (olhando para a direita, +12): **igual ao editor**.
- `PEGADA_1` original: o Preto receptor (olhando para a esquerda, 140 → 125) se aproxima 15 no editor e,
  no jogo, se afasta 15 no último quadro (a pose mantida fica uns 30 px mais aberta que no editor).

Isso existe desde a 3A e foi validado visualmente nas entregas anteriores, então **não mudei** (mudaria a
pegada aprovada e várias asserções antigas). Para corrigir: converter `cena → local` pelo
`viradoDireita` do quadro autoral; é uma linha no ponto de aplicação, mas precisa de nova validação
visual da PEGADA_1 original.

## Fases visuais da PEGADA_1

| Fase | Origem do desenho | Estado visual |
|---|---|---|
| Entrada | quadros da variante (uma vez, 30 frames) | `PEGADA_ENTRADA` |
| Estabelecida | último quadro mantido (não reexecuta a entrada) | `PEGADA_ESTABELECIDA` |
| Arrasto (`PEGADA_ARRASTO`) | último quadro + passos da `CamadaPoseCombate` | `ARRASTO_CONJUNTO` |
| Desequilíbrio (`PEGADA_DESEQUILIBRIO`) | último quadro + reação da `CamadaPoseCombate` | (efeito do motor) |
| Encerramento / rompimento | o motor limpa as poses; volta o desenho normal | — |

### `animacao.CamadaPoseCombate` (fallback procedural, isolado e substituível)

- Soma valores pequenos a uma **cópia** da pose que o motor deixou e devolve a pose original quando
  nada acontece (sem acúmulo). Se o motor trocar a pose, a camada recomeça dela. Nunca move ninguém.
- **Arrasto:** passos alternados (joelho e pé avançam 3 unidades ≈ 2 px; o pé levanta 2) nos dois
  lutadores; a fase avança a cada 4 px de deslocamento **real** da dupla; para quando a dupla para e
  continua de onde estava. Vale para J1 ou J2 aplicador, esquerda ou direita, Clássico (LB/SHIFT/DELETE)
  ou Contextual (automático).
- **Desequilíbrio:** só quando o motor aplica o efeito DESEQUILIBRIO no receptor (o que só acontece com
  sucesso). Peito +8°, cintura +4°, cabeça −4°, joelho de apoio +3; sobe em 6 ticks, segura até 12 e
  volta até 24. Braços continuam na pegada; posição e resultado mecânico não mudam.

## Postura AJOELHADO

- `IrmaoMiau.EstadoCorporal.AJOELHADO` (postura corporal). Posições técnicas (pegada, guarda...) continuam
  sendo do motor de combate (`EstadoInteracaoCombate`).
- `entities.PosturaAjoelhada`: pernas por molde (pivôs diferentes no Branco e no Preto), joelhos no
  tatame, canelas deitadas para trás, corpo abaixado 38/39 unidades (≈ 27 px). Tronco, cabeça e braços
  livres.
- Estados visuais `AJOELHADO` e `AJOELHADO_MOVIMENTO` com JSON de fallback
  (`individuais/<branco|preto>_ajoelhado*.json`, origem "FALLBACK PROCEDURAL 5J") e, se o JSON faltar, o
  mesmo desenho em código no renderizador.
- **PEGADA_AJOELHADA (fallback):** quem está de joelhos recebe a pose da pegada com as pernas de joelhos e
  o corpo abaixado (`SistemaCombate.aplicarPoseNaPostura`). Nada de "levantar" visualmente.

## Contrato: o que trocar quando houver animação desenhada

| Identificador | Hoje | Para substituir |
|---|---|---|
| `POSTURA_AJOELHADO_IDLE` | `branco.ajoelhado.json` / `preto.ajoelhado.json` (1 quadro) | trocar os quadros do JSON pelo exportado do MiauStudio (mesmo id/estado) |
| `POSTURA_AJOELHADO_MOVIMENTO` | `*_ajoelhado_movimento.json` (3 quadros, sequência 0-1-0-2) | idem |
| `PEGADA_AJOELHADA` | adaptador `PosturaAjoelhada.adaptar` | registrar `pegada_1_ajoelhada` (+ variantes) e escolher pela postura no resolvedor |
| `PEGADA_ARRASTO` | `CamadaPoseCombate` (passos) | animação conjunta em loop para o estado `ARRASTO_CONJUNTO` |
| `PEGADA_DESEQUILIBRIO` | `CamadaPoseCombate` (reação) | animação do receptor disparada pelo efeito DESEQUILIBRIO |

Nenhum arquivo vazio foi cadastrado: os JSON de joelhos são poses reais e dizem que são fallback.

## Quadros para desenhar no MiauStudio (sugestão)

1. **Ajoelhado parado** (Branco e Preto): 1–2 quadros, joelhos no tatame, tronco reto, mãos prontas.
2. **Ajoelhado andando**: 4 quadros (neutro, joelho esquerdo à frente, neutro, joelho direito à frente).
3. **PEGADA_1 ajoelhada** (Branco aplicador e Preto aplicador): 3 quadros como a PEGADA_1, de joelhos.
4. **Arrasto na pegada** (em pé e de joelhos): 4 quadros em loop, braços fixos na pegada.
5. **Reação ao desequilíbrio** (receptor): 3 quadros (inclina, apoia, volta).
