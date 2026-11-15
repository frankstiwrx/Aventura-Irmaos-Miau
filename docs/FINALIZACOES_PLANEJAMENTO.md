# Planejamento das finalizações (Entrega 5P.2)

> **Nada aqui está implementado.** Nesta entrega existem só os **espaços** (`posicional/RegistroFinalizacoes`):
> 12 por posição (L1, L2, L1+L2 × 4 direções do RS), em 4 posições = 48. Todo espaço é recusado pelo motor
> ("FINALIZAÇÃO FUTURA", sem custo) e aparece no HUD com `*` só quando a família é segurada.
> Não há dano, submissão, vitória por finalização nem animação.

Nomes dos controles: L1 = LB / E, L2 = LT / Q, R1 = RB / O, R2 = RT / P, RS = analógico direito / setas.

## Organização das famílias nas posições de controle

| Família | Tipo de ataque | Ideia |
|---|---|---|
| L1 + RS | chaves de articulação (braço, ombro) | ↑ ataque principal, → variação lateral, ↓ ataque que inverte o lado |
| L2 + RS | estrangulamentos (com e sem quimono) | ↑ pela gola, → variação, ↓ braço e pescoço |
| L1 + L2 + RS | triângulos e especiais | ↑ triângulo da posição; os outros ficam livres |

Identificador estável do espaço: `fin.<posição>.<família>.<direção>` (ex.: `fin.montada.l2.cima`,
`fin.cem_quilos.l1l2.baixo`). A animação futura usa `finalizacao/<id do espaço>`. Quando uma finalização
for implementada, ela entra no `CatalogoTecnicasSolo` como `TecnicaSolo` com o mesmo comando e o espaço
passa a apontar para ela (`RegistroFinalizacoes.comTecnica`). A técnica do catálogo sempre vence o espaço
reservado, então nunca saem duas ações por comando.

## Catálogo sugerido

Complexidade: **B** = baixa (só janela/critério com o que já existe), **M** = média (precisa de controle de
braço/gola novo no motor), **A** = alta (nova posição intermediária ou animação conjunta complexa).

### Guarda fechada (quem está por baixo e controla)

| Técnica | Pegada / controle exigido | Família | RS | Defesas | Após falha | Animação futura | Compl. |
|---|---|---|---|---|---|---|---|
| Chave de braço | postura quebrada do outro; braço do outro dentro | L1 | ↑ | postura, empilhar, puxar o braço | guarda aberta (o outro abre) | giro do quadril + braço estendido | M |
| Kimura | punho do outro; o outro apoiou a mão | L1 | → | segurar a própria faixa, postura | raspagem de kimura (transição) | rotação de ombro | M |
| Omoplata | braço do outro preso entre as pernas | L1 | ↓ | rolar, postura alta | guarda aberta ou costas do outro | giro completo de quadril | A |
| Estrangulamento cruzado | duas golas (pegada de gola no chão) | L2 | ↑ | defender as mãos, postura | o outro abre a guarda | mãos na gola + puxada | M |
| Guilhotina | cabeça do outro baixa (postura quebrada) | L2 | ↓ | girar para o lado do braço, empilhar | meia-guarda por baixo | braço no pescoço + quadril | M |
| Triângulo | um braço dentro, outro fora (postura quebrada) | L1+L2 | ↑ | postura, empilhar, passar para o lado | guarda aberta; o outro pode passar | pernas em triângulo | A |

### Cem quilos (quem controla)

| Técnica | Pegada / controle exigido | Família | RS | Defesas | Após falha | Animação futura | Compl. |
|---|---|---|---|---|---|---|---|
| Americana | braço próximo do outro no chão | L1 | ↑ | esticar o braço, enquadrar | continua nos 100 kg, controle menor | braço dobrado para cima | B |
| Kimura | braço afastado do outro | L1 | → | segurar a faixa, virar de lado | joelho na barriga | rotação de ombro | M |
| Chave de braço (girando) | braço isolado; o outro empurra | L1 | ↓ | puxar o cotovelo, virar | o outro recupera a guarda | giro por cima da cabeça | A |
| Estrangulamento pela gola | gola do outro | L2 | ↑ | queixo baixo, mãos | continua nos 100 kg | mão na gola + pressão | M |
| Kata-gatame | braço e cabeça do outro juntos | L2 | ↓ | tirar o braço, girar | meia-guarda | braço e pescoço | M |
| Norte-sul | posição norte-sul (variação) | L1+L2 | ↑ | quadril, mãos | volta aos 100 kg | troca para norte-sul | A |

### Montada (quem controla)

| Técnica | Pegada / controle exigido | Família | RS | Defesas | Após falha | Animação futura | Compl. |
|---|---|---|---|---|---|---|---|
| Americana | braço do outro no chão | L1 | ↑ | esticar o braço, upa | continua montado | braço dobrado | B |
| Chave de braço | braço do outro levantado (empurrando) | L1 | → | puxar o cotovelo, upa | o outro escapa para a guarda | giro sobre o braço | M |
| Estrangulamento cruzado | duas golas | L2 | ↑ | mãos, upa | montada alta exposta | mãos cruzadas na gola | M |
| Ezequiel | braço por trás da cabeça; manga própria | L2 | → | mãos no peito, upa | continua montado | braço sob o pescoço | M |
| Kata-gatame | braço do outro cruzado | L2 | ↓ | tirar o braço, girar | meia-guarda | braço e pescoço | M |
| Triângulo da montada | montada alta (S-mount) | L1+L2 | ↑ | upa, quadril | o outro reverte para a guarda | perna sobre o braço | A |

### Costas (quem controla)

| Técnica | Pegada / controle exigido | Família | RS | Defesas | Após falha | Animação futura | Compl. |
|---|---|---|---|---|---|---|---|
| Chave de braço (das costas) | braço do outro isolado | L1 | ↑ | puxar o braço, girar | o outro gira para a guarda | giro para o lado | A |
| Mata-leão | ganchos ajustados; braço sob o queixo | L2 | ↑ | defender as mãos, queixo baixo | continua nas costas | braço no pescoço + aperto | M |
| Arco e flecha | gola + calça/perna | L2 | → | tirar os ganchos, girar | o outro escapa para o neutro | puxada em arco | A |
| Estrangulamento pela gola | gola do outro | L2 | ↓ | mãos, girar | continua nas costas | mão na gola | M |
| Triângulo de costas | ganchos + braço preso | L1+L2 | ↑ | girar, empilhar | o outro gira para a guarda | pernas em triângulo nas costas | A |

Espaços sem linha nas tabelas ficam **livres** de propósito: não há uma técnica coerente para eles hoje.

## Variações que precisam de condição extra

- **Ezequiel**: só na montada com a manga do próprio quimono (registrar `CONEXAO_MANGA` do próprio lado).
- **Omoplata / triângulo** a partir da guarda: só com a **postura quebrada** do outro (`Exposicao.POSTURA_QUEBRADA`).
- **Norte-sul**: exige uma variação "norte-sul" dos 100 kg (posição nova ou estado interno).
- **Berimbolo** (já lógico nesta entrega, L1+L2 ↑ na De La Riva) leva às costas; as finalizações das costas
  ficam disponíveis depois disso.

## Integração futura

1. Criar a `TecnicaSolo` no catálogo, com o mesmo comando do espaço, origem, papel, custo, janela, exposição,
   critério e contras (defesas).
2. Novo desfecho "FINALIZADA" (evento `FINALIZACAO` no `EventoLuta`) para a regra de vitória.
3. Ligar o espaço com `comTecnica`; o HUD passa a mostrar a célula como executável.
4. Animação: pose procedural primeiro; depois JSON do MiauStudio com o id `finalizacao/<espaço>`.
