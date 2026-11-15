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

---

# Entrega 5K — Transições de postura, PEGADA_1 experimental e fluxo artístico

## Transições de postura (procedurais, `animacao.TransicoesPostura`)

Feitas com o rig de 30 valores de cada irmão: interpolam a pose da postura de origem até a de destino
(pernas, tronco, rotação global e altura do corpo). Em pé = pose neutra do rig (idêntica ao desenho em pé
legado); de joelhos = `PosturaAjoelhada`; deitado = quadro 1 do JSON do movimento no chão.

| Contrato | Transição | Ticks | Curva |
|---|---|---|---|
| `TRANSICAO_AJOELHAR` | em pé → joelhos | 12 | suave |
| `TRANSICAO_DEITAR` | joelhos → deitado | 14 | suave |
| `TRANSICAO_ERGUER_JOELHOS` | deitado → joelhos | 14 | suave |
| `TRANSICAO_LEVANTAR` | joelhos → em pé | 12 | suave |
| `TRANSICAO_RAPIDA_DEITAR` | em pé → deitado | 10 | cai acelerando, braços à frente |
| `TRANSICAO_RAPIDA_LEVANTAR_DO_CHAO` | deitado → em pé | 16 | explosiva: passa do ponto e assenta, braços impulsionam |
| `TRANSICAO_RAPIDA_LEVANTAR` | joelhos → em pé | 8 | explosiva |

A postura lógica muda no início; a animação só desenha (posição, linha e colisão não mudam). Estado
visual `TRANSICAO_POSTURA`, fonte `PROCEDURAL`. Se o combate assumir a pose, a transição para de desenhar.

## PEGADA_1 experimental

Quatro projetos MiauStudio v2 **novos** (abríveis no editor), gerados com os pivôs dos rigs:

| Arquivo | Aplicador | Postura |
|---|---|---|
| `PEGADA_1_BRANCO_EXPERIMENTAL.json` | Branco | em pé |
| `PEGADA_1_PRETO_EXPERIMENTAL.json` | Preto | em pé |
| `PEGADA_1_BRANCO_AJOELHADO_EXPERIMENTAL.json` | Branco | de joelhos |
| `PEGADA_1_PRETO_AJOELHADO_EXPERIMENTAL.json` | Preto | de joelhos |

4 quadros (7/7/8/8 frames, mesmos 30 da técnica): **preparo** (tronco inclina, mãos sobem), **alcance**,
**contato** (mão da frente na gola, mão de trás na manga do adversário, calculadas para a dupla a 84 px)
e **pegada assentada** (puxa um pouco, apoio ajusta). O receptor reage com cabeça, tronco e a mão da
frente. Sem deslocamento horizontal entre quadros (evita a diferença de coordenadas da 5J). Nas de
joelhos o quadril vem à frente; as pernas continuam com o adaptador de joelhos.

`PEGADA_1.json` e `PEGADA_1_PRETO.json` **não foram tocados** (teste por SHA-256).

## Como comparar

1. No jogo, **F6** → "ANIMAÇÕES DE PEGADA: EXPERIMENTAL" (vale na próxima pegada). F6 de novo volta à autoral.
2. F3 mostra a variante usada: "… (autoral)", "… (EXPERIMENTAL)" ou "… (EXPERIMENTAL, composta)".
3. Seleção no modo EXPERIMENTAL: experimental do mesmo molde, papel e postura; Branco × Branco e Preto ×
   Preto compõem por papel entre as experimentais; sem nenhuma compatível, cai na autoral.
4. O padrão de cada execução é AUTORAL. Nada é gravado.

## Catálogo (`animacoes.json`): campos novos

- `"experimental": true` — só é escolhida no modo de comparação. Pode ter outro número de quadros
  (mesma duração total); as autorais continuam exigindo os mesmos quadros da base.
- `"postura": "EM_PE" | "AJOELHADO"` — postura do aplicador para a qual foi desenhada.

## Fluxo artístico

1. Criar ou editar no MiauStudio (modo CONJUNTA, `aplicadorConjunto` correto).
2. Exportar o projeto (JSON v2) para `conteudo/miaustudio/` com nome próprio (nunca sobre um autoral).
3. Definir técnica e postura: em `animacoes.json`, `varianteDe` = a animação base da técnica e `postura`.
4. Aplicador e receptor vêm do próprio projeto (`aplicadorConjunto`); o motor compõe por papel para
   duplas do mesmo molde.
5. Registrar primeiro como `"experimental": true`.
6. Validar esquerda/direita (o espelho vem da orientação; nada a fazer no arquivo).
7. Testar Branco×Preto, Preto×Branco, Branco×Branco, Preto×Preto, em pé e de joelhos.
8. Comparar no jogo com F6 e F3.
9. Aprovar: tirar `"experimental"` (vira autoral).
10. Voltar atrás: recolocar `"experimental": true` ou remover a entrada; o arquivo anterior continua lá.

Limitação conhecida (5J): `offsetCentroX` é posição de cena no editor e deslocamento local no motor.
Só diverge para quem foi desenhado olhando para a esquerda e se desloca entre quadros. Recomendação:
desenhar a dupla sem deslocamento horizontal entre quadros, ou com quem se move olhando para a direita.
A correção global continua pendente de validação visual da PEGADA_1 original.

## Contratos (referências estáveis)

| Contrato | Hoje | Substituir por |
|---|---|---|
| `PEGADA_1_ENTRADA` | `pegada_1_entrada` + variantes | novo projeto + `varianteDe` |
| `PEGADA_1_SUSTENTACAO` | último quadro da entrada | (idem) |
| `PEGADA_1_ARRASTO` | `CamadaPoseCombate` (passos) | animação conjunta em loop (pendente) |
| `PEGADA_1_DESEQUILIBRIO` | `CamadaPoseCombate` (reação) | animação do receptor (pendente) |
| `POSTURA_AJOELHADO_IDLE` / `_MOVIMENTO` | JSON de fallback (5J) | trocar os quadros do JSON |
| `TRANSICAO_*` | `TransicoesPostura` (procedural) | animação individual por transição (pendente: falta o estado no carregador) |

---

# Entrega 5L — Animações das pegadas, defesa e fuga de quadril

## Padrão visual

- **No jogo**, as animações feitas pelo Claude são o padrão (`ModoAnimacoesPegada.padraoDoJogo()`, aplicado
  pelo `Main`). **F6** volta às autorais do MiauStudio (e de volta). `-Dirmaosmiau.animacoes.pegada=autoral`
  abre já na autoral. O motor isolado (testes) continua AUTORAL, para os testes de regressão das poses aprovadas.
- F6 troca: a PEGADA_1 (autoral ↔ experimental 5K) e o movimento no chão (autoral ↔ fuga de quadril).
  As pegadas 2–4 só têm a proposta (não há autoral para comparar).

## Pegadas 2–4 (projetos MiauStudio v2 novos, `*_PROPOSTA.json`)

Geradas com os pivôs reais de cada rig, dupla a 84 px, 4 quadros (7/7/8/8 frames): preparo, alcance,
contato, pegada assentada. Sem deslocamento horizontal entre quadros.

| Pegada | Mão da frente | Mão de trás | Receptor |
|---|---|---|---|
| 2 Cruzada | manga (cotovelo da frente) | cruza para a gola | cabeça e tronco reagem |
| 3 Duas mangas | punho da frente | manga de trás (o receptor traz a mão à frente) | disputa as mangas |
| 4 Gola e nuca | nuca | gola | cabeça e tronco descem (puxado) |

| Combinação | Animação |
|---|---|
| Branco aplica (em pé) | `PEGADA_N_BRANCO_PROPOSTA` |
| Preto aplica (em pé) | `PEGADA_N_PRETO_PROPOSTA` (variante) |
| Branco × Branco / Preto × Preto | composta por papel entre as duas |
| De joelhos | a mesma, com o adaptador de joelhos (pernas e altura) — verificada nos testes; sem arquivo próprio |
| PEGADA_1 | autoral (F6) ou experimental 5K (padrão), com as variantes de joelhos da 5K |

Arquivos autorais e experimentais da 5K: intactos (SHA-256 nos testes).

## Defesa

| Contrato | Quando (evento do motor) | Desenho |
|---|---|---|
| `DEFESA_EVASAO` | pegada evitada abaixando | abaixa e inclina para trás |
| `DEFESA_ALCANCE_FALHO` | (no atacante) pegada evitada | estica os braços e recolhe |
| `DEFESA_ROMPIMENTO_SUCESSO` | ROMPER funcionou | recua o tronco, recolhe os braços |
| `DEFESA_SOLTOU` | (no aplicador) foi rompido | braços caem para a frente e voltam |
| `PEGADA_RESISTENCIA` | receptor forçando | puxa o tronco para trás (a pegada fica) |
| `PEGADA_DEFESA_FALHOU` | ROMPER resistido | puxão mais forte e curto (a pegada fica) |

Os dois últimos são da `CamadaPoseCombate` (dentro da pegada); os quatro primeiros, de
`animacao.ReacoesDefesa` (14 ticks, estado visual `REACAO_DEFESA`). Nenhuma reação mostra fuga enquanto o
motor considera a pegada estabelecida.

## Fuga de quadril (movimento deitado)

- `individuais/<branco|preto>_fuga_quadril.json` (formato individual do jogo), feita sobre o quadro 1 do
  MOVIMENTO_CHAO autoral (o deitado parado continua idêntico): 0 deitado, 1 joelhos sobem e pés vêm ao
  quadril, 2 pés empurram e o quadril gira de lado com o braço apoiando, 3 recolhe.
- O quadro avança pela **distância real** andada (um quadro a cada 3 px): parou = parado; voltou a andar =
  continua. Para longe do oponente roda 0-1-2-3; para perto, ao contrário.
- Física, velocidade e fôlego iguais (testado nos dois modos). Preso numa pegada, não rasteja (5L-A).
- O `movimento_chao` autoral continua no perfil e volta com F6.

# Entrega 5N — Reações do receptor e arrasto multidirecional

## Reação COMPARTILHADA do receptor (por técnica, não por personagem)

Cada técnica de pegada tem UMA reação de quem recebe, em
`/conteudo/animacoes/receptor/receptor_<idTecnica>.json` (ex.: `receptor_pegada_1.json`).
Formato: o mesmo das animações individuais (`ANIMACAO_INDIVIDUAL_IRMAOS_MIAU`, esquema 1), com
`"uso": "RECEPTOR"`, `"estado": "PEGADA_ESTABELECIDA"` e `"rig": "miau_v1"`. O JSON v2 do MiauStudio
não mudou e nenhum projeto autoral foi tocado.

Contrato dos 4 quadros (30 valores; os 4 primeiros são ignorados). Os valores são **deltas** somados à
pose que o motor deixou no receptor, em espaço **local** (+X = para onde ele olha, −Y = para cima;
braço `Esq` = o da frente do tronco, `Dir` = o braço esticado à frente; `rotPeito` positivo = inclina
para a frente):

| Quadro | Uso |
|---|---|
| 0 CONTATO | sobe a partir de 40% da entrada do aplicador e chega no pico em 70% |
| 1 SUSTENTADA_A / 2 SUSTENTADA_B | pose de controle; alterna devagar entre as duas (período = 2 × `duracaoQuadroMs`) enquanto a pegada existir |
| 3 LIBERACAO | recuo curto quando a pegada acaba (a liberação parte da última pose sustentada, sem salto) |

| Técnica | Reação |
|---|---|
| PEGADA_1 gola e manga | o braço da manga é puxado à frente/abaixo; a mão livre segura o punho que pega a gola; tronco levemente puxado |
| PEGADA_2 cruzada | cintura gira, tronco inclina; o braço da frente é levado para baixo e atravessado; o outro recua; o pescoço mantém a cabeça orientada |
| PEGADA_3 duas mangas | os dois braços à frente e abaixo (cotovelos acompanhando); quadril recua |
| PEGADA_4 gola e nuca | cabeça e tronco inclinam (postura quebrada, sem exagero); braços sobem em moldura |

**Adaptação** (`AdaptadorRigReacao`): usa a reação se `IrmaoMiau.getRigAnimacao()` for o rig dela
(hoje `miau_v1` para todos); fatores de proporção opcionais (tronco/braços/pernas); deltas limitados a
±24; de joelhos as pernas ficam com o adaptador de joelhos do motor (joelhos no tatame). Rig
incompatível = fallback: a pose do motor fica como está.

**Personagem futuro (Miau Judô, Sambo, Karatê...)**: se usar o esqueleto de 30 valores, não precisa de
nenhum arquivo de receptor — as quatro reações já servem. Se os membros forem mais longos/curtos:
`AdaptadorRigReacao.registrarProporcao(MiauJudo.class, new Proporcao(1.0, 1.2, 1.0))`. Se o esqueleto
for outro, sobrescreve `getRigAnimacao()` e cria reações para o novo rig (ou fica no fallback). Uma
técnica nova só precisa de `receptor_<id>.json`.

**Prioridade na pegada** (o idle/taunt nunca substitui o receptor agarrado: a pose de combate tem
prioridade no desenho e segurar/ser segurado conta como atividade): transição/estados especiais →
liberação/rompimento (`ReacoesDefesa`) → desequilíbrio → força/ROMPER resistido/reforço → passos do
arrasto e direção do puxão → pose sustentada → idle/taunt só livre.

**F6**: no modo autoral (comparação), as camadas da 5N ficam desligadas e o receptor volta à pose do motor
(como até a 5M); no experimental (padrão do jogo) as reações aparecem.

## Arrasto multidirecional

Com o modo controle (SHIFT / DELETE; LB no Clássico; analógico no Contextual), o aplicador conduz a dupla
para os lados, para a frente/fundo (linhas 1–5) e nas diagonais. Uma operação só: os dois andam o mesmo
tanto ou nenhum anda naquele eixo; na diagonal cada eixo anda 1/√2 (não fica mais rápido); na borda o eixo
livre continua (desliza). Fôlego: só o aplicador paga `ARRASTO_POR_PIXEL` × distância realmente andada
(bloqueado não custa; nunca negativo). As referências da pegada andam junto (X, Y e linha), então a
mudança conjunta de linha não rompe. Passos dos dois pela distância andada em X e em profundidade; o
receptor inclina conforme é puxado/empurrado e vira a cabeça ao mudar de linha.

# Entrega 5O — Defesas, sprawl e rolamento experimentais, acrobacias

## Originais preservados (comparáveis no jogo)

- **Rolamento técnico original**: `IrmaoMiauBranco.ROLAMENTO_MIAUSTUDIO` / `IrmaoMiauPreto` (Java, intacto),
  desenhado por `desenharRolamento` — tocado ao contrário para a frente, como sempre.
- **Sprawl original**: `SPRAWL_MIAUSTUDIO` nos mesmos arquivos (Java, intacto).
- **F7** alterna ROLAMENTO/SPRAWL entre **ORIGINAL** e **EXPERIMENTAL 5O** (o jogo abre no experimental; o F3
  mostra "MOV (F7)"). O **F6 não mudou**: continua só das pegadas e das reações do receptor.

## Movimentos compartilhados (rig `miau_v1`, por técnica — nunca por personagem)

`/conteudo/animacoes/movimentos/<id>.json`, formato individual com `"uso": "MOVIMENTO"` e, nas sequências,
`deslocamentosY` (um por quadro: assenta o corpo no tatame ou sobe no ar). Nenhum projeto JSON v2 mudou.

| Arquivo | Uso | Quadros |
|---|---|---|
| `guarda_alta/media/baixa.json` | guardas (as duas versões) | 2 DELTAS sobre a postura (respiração); de joelhos as pernas são as da postura |
| `sprawl_5o.json` | sprawl experimental | preparação, explosão, pernas atrás, proteção ×2, recolhe, agacha, guarda |
| `rolamento_frente_5o.json`, `rolamento_tras_5o.json` | rolamento experimental | preparação, mergulho/senta, 4 posições do giro de 360° grupado, agachado, levanta |
| `rolamento_lateral_5o.json` | rolamento para o fundo/frente da tela | abaixa, ombro, bola, rola, volta, abre, agachado, levanta |
| (diagonais) | — | giro da frente/trás + 30% do lateral (ombro e quadril), sem arquivo próprio |
| `estrela_5o.json`, `mortal_frente_5o.json`, `mortal_tras_5o.json` | acrobacias (as duas versões) | passo/impulso, mãos/salto, parada de mãos/grupado, roda/desce, pouso |

As sequências são tocadas pelo PROGRESSO do movimento (a mecânica — distância, tempo, custo, colisão — é a
mesma nas duas versões; só o desenho muda). Rig incompatível = nada aplicado (fallback do personagem).

**Personagem futuro (Judô, Sambo, Karatê)**: com o esqueleto de 30 valores, usa todos esses arquivos sem
nada novo. Variante específica = outro arquivo e uma linha na biblioteca. Para editar no MiauStudio: abrir
um projeto INDIVIDUAL com os mesmos 30 valores e copiar os quadros para o arquivo (o MiauStudio ainda não
exporta este formato diretamente).


# Entrega 5O.1 — Rolamento e sprawl mais baixos, guarda andando

- **Rolamentos experimentais** (`rolamento_frente/tras/lateral_5o.json`): o corpo vira uma **bola**
  (joelhos no peito, cabeça encolhida) e gira de 30 em 30 graus perto do tatame. Durante o giro a altura
  fica no máximo a de quem está de joelhos e, na média, ~0,6 da altura em pé (antes o corpo invertido com
  as pernas esticadas parecia um mortal). O fim do giro usa 360° (não "desenrola" ao contrário).
- **Sprawl experimental** (`sprawl_5o.json`): entrada → projeção das pernas → **proteção em prancha**
  (tronco e quadril perto do tatame, antebraço apoiando) → recuperação. Na proteção a altura é ~0,6 da
  altura em pé (antes ~0,77), um pouco acima da de quem está deitado.
- **Assentamento por personagem** (`AssentamentoMovimentos`): os `deslocamentosY` do arquivo foram medidos
  no Branco; o Preto tem outras proporções. Na primeira vez, cada movimento de chão é desenhado num lutador
  de rascunho da mesma classe (4 amostras entre quadros) e o ponto mais baixo é encostado no tatame. Erro
  medido: até 3 px nos dois irmãos. Personagem futuro sem o construtor (x, y, escala): valem os números do
  arquivo.
- **Rolamento de joelhos/deitado**: começa na pose da postura, entra na bola e termina de novo na postura
  (`BORDA_POSTURA_ROLAMENTO`); a postura lógica não muda. Deitado, o mesmo desenho, mais lento.
- **Guarda andando**: braços, tronco e cabeça são os da guarda; só as pernas dão passos curtos alternados
  (`FASE_PASSO_POR_TICK`), com rampa de 5 ticks para começar/parar (sem tranco) e 1 px de cabeça.
- **Originais preservados**: com o **F7 em ORIGINAL** continuam os arrays Java de sempre
  (`ROLAMENTO_MIAUSTUDIO`, `SPRAWL_MIAUSTUDIO`). Limitação: o rolamento ORIGINAL de joelhos/deitado usa o
  desenho em pé de sempre. O F6 não mudou.
- O laboratório de medição (desenho real, topo/base em pixels) virou o teste `Alturas51Test`.
