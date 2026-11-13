# Combate avançado — Entrega 5A

Primeira disputa técnica jogável: **desequilíbrio** (ataque do aplicador) e **rompimento técnico**
(defesa do receptor) a partir de uma PEGADA_1 **estabelecida**. Só lógica e HUD: nenhuma pose, array,
renderer, JSON do MiauStudio ou geometria foi alterado.

## Motor de disputas (`combate/ResolvedorDisputa`, `TipoDisputa`)

Determinístico, sem aleatoriedade, sem interface:

```
norm(a)     = limitar(a / 100, 0, 1)              técnica, força, agilidade
normPeso(p) = limitar((p − 40) / 80, 0, 1)        40 kg → 0 ; 120 kg → 1
base        = Σ peso_i × norm_i                    (pesos do tipo, soma 1,0, todos ≥ 0)
fôlego      = 0,6 + 0,4 × limitar(fôlegoAtual / fôlegoMáximo, 0, 1)
ataque      = base × fôlego × fatorPosição          defesa = base × fôlego
SUCESSO se ataque > defesa (+1e-9) ; EMPATE → o defensor resiste
```

| Tipo | Quem ataca | Pesos do ataque (téc / força / agil / peso) | Quem defende | Pesos da defesa | Fator posição |
|---|---|---|---|---|---|
| `DESEQUILIBRIO` | aplicador da pegada | 0,40 / 0,35 / 0,10 / 0,15 | receptor | 0,35 / 0,20 / 0,25 / 0,20 | 1,10 (controla a pegada) |
| `ROMPIMENTO_TECNICO` | receptor | 0,45 / 0,30 / 0,15 / 0,10 | aplicador | 0,40 / 0,40 / 0,00 / 0,20 | 1,00 |

Propriedades garantidas por teste: pontuações sempre em [0; 1,1], sem NaN; mais força/técnica nunca
piora o ataque; mais técnica/agilidade nunca piora a defesa; o peso move no máximo 0,20 da pontuação;
exausto = 60% do poder; mesmas entradas, mesmo resultado.

**Atributos que passaram a ter efeito real:** força, técnica, agilidade e peso (nas disputas). O fôlego
atual também pesa. As fórmulas antigas (velocidade, consumo, recuperação) não mudaram.

Referência com os oficiais: Branco (22/25 após a pegada) desequilibra o Preto descansado
(0,350 × 0,317). O Preto **não** rompe a pegada de um Branco descansado (0,283 × 0,331), mas rompe se o
Branco estiver exausto (0,283 × 0,209).

## Ações

| Ação | Tecla | Quem | Requisito | Custo | Sucesso | Falha |
|---|---|---|---|---|---|---|
| Desequilíbrio | **T** (slot 1) | aplicador | pegada estabelecida (não durante os 30 quadros) | 4 fôlego | receptor DESEQUILIBRADO por 120 ticks (~2 s); pegada continua | receptor resiste e paga 1 fôlego |
| Rompimento técnico | **U** (slot 2) | receptor | pegada estabelecida | 3 fôlego (sempre) | interação encerrada (`ROMPIMENTO_TECNICO`), sem se deslocar | aplicador paga 1 fôlego; pegada continua |

- Regras pelo **papel** (aplicador/receptor), não pelo nome: `SistemaCombate.tentarDesequilibrio(lutador)`
  e `tentarRompimentoTecnico(lutador)`. Hoje só o Branco aplica a PEGADA_1, mas um teste comprova
  que o motor funciona com o Preto como aplicador.
- Uma ação por toque (borda da tecla); segurar não repete. Recarga de 45 ticks (~0,75 s) por lutador.
- Sem fôlego suficiente: nada é cobrado, nada é disputado, sem recarga. O fôlego nunca fica negativo.
- O custo de resistência (1) é uma regra nova desta entrega: resistir cansa, então insistir pode vencer.
- O rompimento antigo por movimento/pulo/agachar etc. (3A) continua igual. O rompimento técnico é uma
  opção a mais para quem quer ficar parado. Liberdade de movimento durante a pegada = Entrega 5B.

## Estados e efeitos

- Interação: LIVRE → ENTRANDO (30 quadros, ações técnicas bloqueadas) → ESTABELECIDA → LIVRE (motivo).
  Novo motivo: `ROMPIMENTO_TECNICO`.
- `entities/EfeitosTemporarios`: por **instância** de lutador. `DESEQUILIBRIO` = velocidade × 0,6.
  Duração em ticks no `IrmaoMiau.atualizar()` (loop lógico). Reaplicar **renova** (não acumula).
  Ao expirar, o multiplicador volta a 1,0. Nada é gravado em perfis. Partida nova e F5 = instâncias
  novas = sem efeitos e sem recargas.
- O efeito continua depois que a pegada acaba (o receptor sai mais lento).

## HUD

Mensagens curtas (~1,6 s): `DESEQUILIBROU!`, `TENTATIVA DE DESEQUILÍBRIO: RESISTIU`, `PEGADA ROMPIDA!`,
`DEFESA FALHOU`, `...: SEM FÔLEGO`, `...: aguarde`, `...: só com a pegada estabelecida`.
Linha de estado: `PEGADA 1 · ESTABELECIDA` ou, com efeito, `PEGADA 1 · <nome> DESEQUILIBRADO`.
O texto central diminui a fonte se não couber entre os painéis. Debug (F3): `DISPUTA: tipo ataque x defesa -> resultado`.

## Treino

O jogador usa T normalmente. O parceiro passivo **não** defende sozinho e a tecla U é ignorada no treino.

## Limitações e próximos passos

- Sem animação de desequilíbrio ou de rompimento: o resultado aparece no estado lógico e no HUD.
  A arte virá do MiauStudio (nova animação no catálogo, sem mudar o motor).
- Só a PEGADA_1 existe; `TipoDisputa` e os papéis já servem para PEGADA_2..4 e quedas
  (novo tipo de disputa + nova ação no `SistemaCombate`).
- Pesos e custos são provisórios e ficam em `TipoDisputa` e nas constantes do `SistemaCombate`.
- Não há IA defensiva.
