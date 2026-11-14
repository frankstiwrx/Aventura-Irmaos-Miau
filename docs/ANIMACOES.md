# Animações — Entrega 5E

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
