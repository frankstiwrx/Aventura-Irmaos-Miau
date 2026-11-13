# Combate avançado — Entregas 5A e 5B

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
| Desequilíbrio | **T** (slot 1) | aplicador | pegada estabelecida (não durante os 30 quadros) | 4 fôlego | receptor DESEQUILIBRADO por 120 ticks (~2 s); pegada continua | receptor resiste **sem custo** (5B) |
| Rompimento técnico | **U** (slot 2) | receptor | pegada estabelecida | 3 fôlego (sempre) | interação encerrada (`ROMPIMENTO_TECNICO`), sem se deslocar | aplicador resiste **sem custo** (5B); pegada continua |

- Regras pelo **papel** (aplicador/receptor), não pelo nome: `SistemaCombate.tentarDesequilibrio(lutador)`
  e `tentarRompimentoTecnico(lutador)`. Hoje só o Branco aplica a PEGADA_1, mas um teste comprova
  que o motor funciona com o Preto como aplicador.
- Uma ação por toque (borda da tecla); segurar não repete. Recarga de 45 ticks (~0,75 s) por lutador.
- Sem fôlego suficiente: nada é cobrado, nada é disputado, sem recarga. O fôlego nunca fica negativo.
- **5B:** o custo de resistência passiva (1 de fôlego, criado na 5A) foi **removido**: repetir tentativas
  não drena quem não agiu. O caminho para romper agora é a queda da **firmeza** (abaixo).
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

---

# Entrega 5B — Controle e manutenção da pegada

## Firmeza (pertence à interação, nunca ao perfil)

| Constante (`SistemaCombate`) | Valor | Significado |
|---|---|---|
| `FIRMEZA_INICIAL` | 100 | ao estabelecer a pegada |
| `DESGASTE_FIRMEZA_POR_TICK` | 0,10 | só depois dos 30 quadros: 100 → 0 em 1000 ticks (~16,7 s) |
| `DESGASTE_EXTRA_DESLOCAMENTO_POR_TICK` | 0,15 | nos ticks com deslocamento conjunto (total 0,25: ~6,7 s) |
| `REFORCO_FIRMEZA` | +25 | por reforço (máximo 100) |
| `CUSTO_REFORCO` | 3 fôlego | pago pelo aplicador |
| `RECARGA_REFORCO_TICKS` | 60 | ~1 s, separada da recarga de T/U (45) |
| `LIMIAR_CONTROLE_FRACO` | 30 | aviso "CONTROLE ENFRAQUECENDO" |
| `VELOCIDADE_DESLOCAMENTO_CONJUNTO` | 1 px/tick | caminhada normal: 3,2 a 4,8 px/tick |

- Desgaste por tick do loop lógico (independe da renderização), sempre entre 0 e 100.
- Firmeza 0 → encerramento pelo mecanismo existente, motivo **`SEM_CONTROLE`**. Se a política de
  interação já encerrou a pegada no mesmo tick, o desgaste não roda (sem encerramento duplo).
- Qualquer encerramento zera a firmeza e o deslocamento pendente; efeitos independentes (desequilíbrio) ficam.

## Firmeza nas disputas (f = firmeza / 100)

```
DESEQUILIBRIO       fatorPosiçãoAtaque = 1 + 0,10 × f        (1,10 com f = 1 → igual à 5A)
ROMPIMENTO_TECNICO  defesa ×= 0,70 + 0,30 × f                (1,00 com f = 1 → igual à 5A)
```

Firmeza 100 reproduz exatamente os números da 5A (teste). Mesmo com firmeza 0 o aplicador mantém 70%
da defesa: força, técnica, agilidade, peso e fôlego continuam decidindo.
Oficiais: o Preto rompe a pegada do Branco descansado quando a firmeza fica abaixo de ~30
(cerca de 12 s sem reforço). Um perfil forte e técnico segura mesmo com firmeza baixa.

## Reforço — **Y** (aplicador do slot 1)

Pegada estabelecida, fôlego ≥ 3, fora da recarga e firmeza < 100 → +25 de firmeza. Não reinicia a
entrada, não move ninguém, não mexe em poses. Com 100% → "PEGADA JÁ ESTÁ FIRME" (nada cobrado).
Decisão de recursos: o fôlego do reforço é o mesmo que pagaria um desequilíbrio (T).

## Deslocamento conjunto — **SHIFT + A/D** (aplicador do slot 1)

- Âncoras auditadas: cada lutador desenha a pose conjunta relativa ao próprio `x` (deslocamento visual
  relativo, pivôs relativos, espelho em `x + 35·escala`). Mover **os dois pelo mesmo número inteiro de
  pixels** preserva o encaixe; um teste renderiza a dupla antes e depois e compara pixel a pixel.
- `SistemaCombate.deslocarPegadaControlada(aplicador, direção, xMín, xMáx)`: 1 px por tick, linhas e
  orientação preservadas, retratos da política transladados junto (não rompe por MOVIMENTO, mas
  pular, agachar etc. no mesmo tick ainda rompem). Bloqueado nos 30 quadros e fora do papel de aplicador.
- Limites: `GamePanel.LIMITE_ESQUERDO_ARENA = 15` e `LIMITE_DIREITO_ARENA = 715` (só para o deslocamento
  conjunto; a caminhada normal continua sem limite, como antes).
- Sem SHIFT, A/D é caminhada normal e rompe a pegada (regra da 3A preservada).
- Swing: cada tecla foi registrada também como "shift pressed/released X"; todos os controles continuam
  funcionando com SHIFT segurado e as teclas são soltas corretamente em qualquer ordem.

## HUD (5B)

`PEGADA 1 · ENTRANDO` → `PEGADA 1 · FIRMEZA 85%`. Mensagens: `PEGADA REFORÇADA`, `PEGADA JÁ ESTÁ FIRME`,
`REFORÇO: SEM FÔLEGO`, `REFORÇO: AGUARDE`, `DESLOCAMENTO CONTROLADO`, `LIMITE DA ARENA`,
`CONTROLE ENFRAQUECENDO`, `PEGADA DESFEITA: SEM CONTROLE`. Debug F3: firmeza, desgaste e recargas.

## Treino (5B)

Firmeza, reforço, desequilíbrio e arrasto funcionam; o parceiro continua passivo (U ignorado, sem defesa
automática). F5 cria lutadores e sistema novos: sem interação, firmeza, efeitos ou recargas.

## Limitações (5B)

- Sem animação de "arrastar": a pose final da PEGADA_1 é transladada inteira (encaixe correto, sem passos).
- SHIFT é compartilhado no teclado; só o slot 1 usa o modo de controle.
- Números provisórios, todos nas constantes acima.
