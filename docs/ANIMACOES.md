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
