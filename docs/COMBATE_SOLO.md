# Combate de quedas e de solo — Entrega 5P

O jiu-jitsu dos Irmãos Miau continua depois da queda. Nenhum papel é fixo: quem inicia uma técnica pode
terminar por baixo, e todas as regras valem igual para J1 e J2 (lados `A` = J1, `B` = J2).

## Arquitetura

| Quem | Cuida de |
|---|---|
| `IrmaoMiau` | o corpo de cada um: postura individual (em pé, de joelhos, deitado), fôlego, vínculo |
| `SistemaCombate` | a disputa **em pé** (pegadas, firmeza, arrasto, rompimento) — inalterado |
| `posicional.MotorSolo` (novo) | a **relação** entre os dois: posição, quem está por cima, quem controla, técnica em andamento, janela, exposição, estabilidade, aterrissagem |
| `posicional.CatalogoTecnicasSolo` (novo) | o repertório (dados): onde cada técnica começa, quem pode, comando, custo, janela, critério, contras e desfecho |
| `animacao.AnimadorSolo` (novo) | o desenho das posições, comandado pela posição relacional |

**Estado conjunto.** O `MotorSolo` é o **único** dono da relação (o `EstadoPosicional` imutável da 5E:
posição + por cima + controlador). Só ele muda a postura, o lugar e o vínculo dos dois, sempre juntos no
mesmo tick. Não existe "J1 acha que está montado e J2 acha que está livre". Ao cair, a pegada em pé acaba
(`SistemaCombate.encerrarInteracao`). Por cima ≠ controle: na guarda fechada quem está por cima está dentro
da guarda, e quem **controla** é quem está por baixo.

## Posições e transições

Posições (`Posicao`): `EM_PE_SEPARADOS`, `EM_PE_PEGADA` (lidas do `SistemaCombate`), `EM_PE_CONTRA_SOLO` (um
em pé, outro no chão, sem vínculo), `SOLO_DISPUTADO` (os dois no chão, ninguém controla), `GUARDA` (fechada),
`GUARDA_ABERTA`, `MEIA_GUARDA`, `CONTROLE_LATERAL` (cem quilos), `JOELHO_BARRIGA`, `MONTADA`, `COSTAS`.

```
            empurrão (derruba) ─────────────┐
EM PÉ ──queda c/ pegada / pernas──► ATERRISSAGEM ──(quem caiu: ↑)──► GUARDA
  │  ▲        │ (sem acompanhar)        │        ──(quem caiu: ←/→)─► MEIA-GUARDA
  │  │        ▼                         └──(sem reação)────────────► CEM QUILOS
  │  │   UM EM PÉ x SOLO ──entrar──► GUARDA ABERTA
  │  └──levantar──┘    └─passar em pé─► CEM QUILOS
  ├──puxar guarda──► GUARDA (quem puxou por baixo, controlando)
  └──pernas x base──► SOLO DISPUTADO ──subir──► CEM QUILOS / ──puxar──► GUARDA / ──levantar──► UM EM PÉ

GUARDA ──abrir──► GUARDA ABERTA ──passar──► CEM QUILOS ──montar──► MONTADA ──costas──► COSTAS
  ▲  ◄──fechar───┘   │ ──joelho cortando──► MEIA ──liberar──┘   ▲ │ ◄──100kg──┐       │
  │                  ▼                                          │ └─joelho──► JOELHO NA BARRIGA
  └── RASPAGEM (guarda/aberta/meia): quem raspou fica POR CIMA na guarda aberta do outro
ESCAPES: cem quilos/joelho ──quadril──► GUARDA ABERTA · cem quilos ──joelho no meio──► MEIA
         montada ──quadril──► MEIA · montada ──UPA──► GUARDA (quem deu a upa por cima)
         costas ──girar──► GUARDA (quem girou por cima) · costas ──escapar──► SOLO DISPUTADO
         cem quilos ──virar para os joelhos──► SOLO DISPUTADO (ou COSTAS se o outro aproveitar)
```

## Regra de ouro: toda transição gera vulnerabilidade

Toda técnica com **janela** (ticks de preparação) expõe quem a tenta (`Exposicao`): `BASE` (passar,
abrir a guarda, entrar), `CONTROLE_REDUZIDO` (o controlador desloca o peso para montar, ir ao joelho, pegar
as costas), `PERNAS` (entrada nas pernas), `AVANCO` (empurrão bloqueado), `POSTURA_QUEBRADA`. Durante a janela
o outro pode responder com uma **contra** do catálogo, que resolve **na hora** (vale o sucesso da contra).

Sem resposta, o **critério** decide, de forma determinística:

| Critério | Dá certo se… |
|---|---|
| `SE_NAO_CONTRADA` | ninguém respondeu (progressões, quedas, puxar guarda) |
| `ESTABILIDADE_BAIXA` | o controle do outro está abaixo do limiar **ou** o outro está exposto **ou** exausto (<15% do fôlego) |
| `SEM_POSTURA_DO_OUTRO` | o outro não está em postura **ou** está exposto (raspagens, recuperar guarda) |
| `OUTRO_EXPOSTO` | só aproveitando uma exposição (pegar as costas na virada, inverter do joelho) |

Falhou = `DEFENDIDA`: quem tentou fica 40 ticks sem nova técnica e o controle do outro sobe +10.
Abandonar (repetir um comando durante a própria janela) desfaz a tentativa, mas a exposição fica 20 ticks.

**Estabilidade** (0–100) é a qualidade do controle de quem controla a posição: começa em 50 ao progredir e
30 ao chegar por reversão/escape; sobe sozinha +0,2/tick (metade se o controlador estiver cansado);
`CONSOLIDAR`/estabilizar +25; `ENQUADRAR` do outro −20; montada alta +35, mas expõe a base por 20 ticks.

## Comandos (mesma lógica em toda posição)

| Comando | Sentido |
|---|---|
| RS **↑** | avançar / progredir (passar, montar, raspar, subir, levantar) |
| RS **↓** | segurar / defender / estabilizar (postura, consolidar, retenção, fechar ou recuperar guarda, enquadrar) |
| RS **← / →** (iguais) | quadril e transições laterais (escape de quadril, joelho na barriga, costas, joelho cortando) |
| **RB / Select + RS** | o explosivo: quedas em pé; ponte/upa, levantar, escapar para o neutro, joelho no meio |
| **RT** | em pé: defesas (guardas, sprawl); agarrado ou sendo derrubado: **base**; sozinho: respirar |

Teclado: J1 = teclas de técnica **2 ↑ / 6 → / 7 ↓ / 8 ←**, **4** = RB, **3** = RT. J2 (teclado-controle) =
**setas**, **O/SHIFT** = RB, **P** = RT. Xbox: RS, RB/Select, RT. Clássico e Contextual chegam ao mesmo motor.

### Tabela por posição e papel

| Posição | Papel | RS ↑ | RS ↓ | RS ← → | RB + RS |
|---|---|---|---|---|---|
| Em pé (livre/pegada) | qualquer | pegadas / ações da pegada (como antes) | idem | idem | ↑ empurrão · → queda com a pegada · ↓ entrada nas pernas · ← puxar guarda |
| Um em pé × solo | em pé | entrar na guarda | — | — | ↑ passagem em pé |
| Um em pé × solo | no chão | levantar técnico | retenção com os pés | conectar na guarda | — |
| Solo disputado | qualquer | subir por cima | puxar para a guarda | — | ↑ levantar |
| Guarda fechada | por cima | abrir a guarda | postura | — | — |
| Guarda fechada | por baixo | raspagem | quebrar a postura | abrir (ajustar o quadril) | — |
| Guarda aberta | por cima | passar a guarda | controlar as pernas | joelho cortando (meia) | — |
| Guarda aberta | por baixo | raspagem | fechar a guarda | retenção | ↑ levantar da guarda |
| Meia-guarda | por cima | liberar a perna e passar | pressão de ombro | — | — |
| Meia-guarda | por baixo | raspagem da meia | recuperar a guarda | — | — |
| Cem quilos | controla | montar | consolidar | joelho na barriga | ↑ pegar as costas (na virada) |
| Cem quilos | controlado | virar para os joelhos | enquadrar | escape de quadril | ↓ joelho no meio (meia) |
| Joelho na barriga | controla | montar | pressão | voltar aos cem quilos | — |
| Joelho na barriga | controlado | — | enquadrar | empurrar o joelho e fugir | ↑ inverter (base alta) |
| Montada | controla | montada alta | estabilizar | pegar as costas | — |
| Montada | controlado | — | enquadrar | escape de quadril (meia) | ↑ ponte (upa) |
| Costas | controla | — | ajustar os ganchos | — | — |
| Costas | controlado | — | defender com as mãos | tirar os ganchos e girar | ↑ escapar para o neutro |

### Contras (respostas na janela)

| Tentativa | Contra(s) | Resultado da contra |
|---|---|---|
| Queda com a pegada | a mesma queda do outro (RB + →) | reversão: quem tentou cai, o outro cai por cima |
| Entrada nas pernas | sprawl (RT + RS ↓) | quem entrou fica de joelhos, o outro em pé por cima |
| Empurrão / queda | base (RT segurado) ou guarda (livre) | bloqueado; quem empurrou fica exposto (AVANÇO) |
| Abrir a guarda | raspagem / quebrar a postura | raspou / ficou na guarda fechada |
| Passar a guarda | raspagem / retenção | raspou / ficou na guarda |
| Joelho cortando | retenção | ficou na guarda |
| Liberar a perna (meia) | recuperar a guarda / raspagem da meia | guarda aberta / raspou |
| Montar (dos cem quilos ou do joelho) | escape de quadril / joelho no meio | guarda aberta / meia-guarda |
| Joelho na barriga | escape de quadril | guarda aberta |
| Pegar as costas (montada) | escape de quadril / upa | meia-guarda / inverteu |
| Virar para os joelhos | pegar as costas na virada | costas |
| Escapes e raspagens | consolidar / estabilizar / postura / ganchos (do outro) | defendida |
| Subir por cima (solo disputado) | puxar para a guarda | guarda do outro |
| Entrar na guarda / passagem em pé | retenção com os pés / conectar na guarda | defendida / guarda aberta |
| Levantar técnico / da guarda / do solo | entrar na guarda / passar / subir por cima | o outro aproveita |

## Entradas e resultados das quedas

- **Empurrão** (RB + ↑, alcance 95 px): o outro recua 34 px e perde 45 de base (100 se estava exposto); com a
  base em 0 ele cai e quem empurrou fica em pé (resultado A). Cada empurrão custa 20 da **própria** base;
  insistir com a base baixa é avanço excessivo (exposto, pode cair sozinho). Contra base/guarda: bloqueado.
- **Queda com a pegada** (RB + →, só com a pegada estabelecida, janela 24): entra se a firmeza ≥ 30 ou a
  base do outro < 60 ou o outro está exposto. **Segurando o LS para o adversário na janela, acompanha**
  (aterrissagem) — senão fica em pé (A). Contra-queda do outro = reversão (C). Base (RT) = defendida, os dois
  continuam em pé e quem entrou fica exposto.
- **Entrada nas pernas** (RB + ↓, alcance 120 px, janela 14): sprawl na janela = parada; base sem sprawl =
  os dois ao chão sem controle (E); sem defesa = derrubou e acompanhou. Longe = falhou, exposto.
- **Puxar guarda** (RB + ←, com pegada ou a até 80 px): quem puxa vai por baixo e controla a guarda fechada
  (D). Se o outro estiver recuando no fim da janela, ele fica em pé e o puxador fica no chão.
- **Aterrissagem** (10 ticks): quem caiu escolhe como recebe — **↑ fecha a guarda**, **←/→ pega a meia**, nada
  = cem quilos de quem derrubou (B).

## Fôlego e fadiga

| Ação | Custo |
|---|---|
| Ajustes instantâneos (postura, consolidar, enquadrar, retenção, ganchos…) | 1,5 |
| Puxar guarda, entrar na guarda, fechar a guarda, puxar no solo | 2,0 |
| Empurrão, levantar, abrir a guarda, joelho cortando, montar | 2,5 – 3,5 |
| Passar, escapes de quadril, virar, girar nas costas | 4,0 |
| Raspagens, passagem em pé, escapar para o neutro, inverter | 5,0 |
| Queda com a pegada, ponte (upa) | 6,0 |
| Entrada nas pernas | 7,0 |

- **5P.1:** respirar de propósito agora recupera devagar no vínculo (ver "Recuperação contextual" no fim).
- **Vínculo de solo** (`IrmaoMiau.setVinculadoNoSolo`, todas as posições de chão menos "um em pé × solo")
  bloqueia **toda** recuperação de fôlego e condicionamento, igual à pegada da 5O.3 (mesmas quatro funções,
  sem rota nova). Separou (escapou para em pé / um em pé × solo): recupera normalmente.
- **Cansado** (< 25% do fôlego): as próprias janelas ficam 50% mais longas (o outro tem mais tempo) e o
  controle cresce pela metade. **Exausto** (< 15%): o controle dele pode ser escapado mesmo firme.
- Sem fôlego para o custo, a técnica é recusada ("SEM FÔLEGO"). Nada fica negativo.

## Prioridade dos comandos

1. Transição de postura em andamento: nada (recusa "EM MOVIMENTO") — **5P.1:** só em pé; no chão o comando é aceito.
2. Ligado no solo ou um em pé × solo: RS e RB + RS vão para o `MotorSolo`; RT não levanta guarda; não anda,
   não rola (R3), não pula, não faz sprawl, não muda de postura pelo L3/F.
3. Em pé: RB + RS = quedas; RT + RS = defesas; RS = pegadas / ações da pegada (como antes); R3 = mobilidade.
4. Mesmo tick dos dois: uma contra vence; senão quem **controla** a posição tem a iniciativa (em pé sem
   controle: quem tem mais base; empate = "choque", os dois recusados).
5. Durante a própria entrada (queda, pernas, empurrão, puxar guarda) o iniciador não anda nem arrasta: o LS é
   a intenção de acompanhar.

## Movimento no solo

Ligados, ninguém se afasta andando: o motor encaixa a dupla (3 px/tick, sem teletransporte) na distância da
posição, na **mesma linha** e dentro da arena (se o encaixe passaria da borda, a dupla é empurrada para dentro).
A orientação também é do motor (nas costas os dois olham para o mesmo lado). Ao escapar para em pé, cada um
volta a andar.

## Miau Branco e Miau Preto

Os dois usam o mesmo motor, sem vantagens numéricas nesta entrega. A identidade futura (Branco passador:
pressão e estabilização; Preto guardeiro: retenção e raspagens) entra como parâmetros por personagem no
catálogo/motor (custo, janela, ganho de estabilidade), sem mudar a estrutura.

## Animação

`AnimadorSolo` (procedural, camada depois das transições de postura): quem está por baixo deitado (ou de
joelhos nas costas), quem está por cima de joelhos, com altura e inclinação por posição; a distância é do
motor. As quedas usam as animações de transição de postura existentes. F6 e F7 não mudaram. Cada posição pode
ser trocada depois por poses do MiauStudio (JSON) sem mexer no motor.

## Limitações (5P)

- Desenho procedural simples: posições reconhecíveis, mas sem contato fino de mãos e pernas; as quedas usam a
  transição de postura (cair/ajoelhar), não uma animação conjunta de arremesso.
- Sem finalizações, tartaruga e controle de joelho na barriga do outro lado.
- Ligados no solo, a dupla não se desloca pela arena (escapar ou levantar devolve o movimento).
- Os números (custos, janelas, limiares) são um primeiro balanceamento; estão todos em `MotorSolo` e no
  catálogo.

# Entrega 5P.1 — Refinamento do solo

## Solo livre (deitar por vontade própria)

Deitar sem ter sido derrubado entra no **mesmo** `MotorSolo`: `EM_PE_CONTRA_SOLO`, com quem ficou em pé
"por cima", **sem controlador e sem vínculo** (ninguém conquista nada a distância; entrar na guarda e
passagens continuam exigindo alcance). O "SOLO: EM BREVE" saiu: os dois lados veem os comandos da posição.

| | Deitou por vontade | Foi derrubado |
|---|---|---|
| Janela do levantar técnico | × 0,6 (mais rápido) | × 1,4 por `DESORIENTADO_TICKS` = 60 |
| Puxar guarda do chão | estabilidade inicial 60 | estabilidade normal |

## Recuperação contextual (evolui a 5O.3)

A recuperação **passiva** (parado) continua bloqueada em qualquer vínculo (pegada ou solo). **Respirar de
propósito** (RT segurado / X) agora recupera em todo lugar, multiplicado por um fator de contexto
(`IrmaoMiau.fatorRespiracaoContexto`, valores em `CustosFolego`):

| Situação | Fator da respiração |
|---|---|
| Livre (em pé, de joelhos, deitado) | 1,0 (por postura, como na 5O.2) |
| Andando | × 0,5 |
| Na pegada (aplicando ou sofrendo) | 0,25 |
| Solo, por cima ou sem ser dominado | 0,35 |
| Solo, dominado por baixo | 0,35 → 0,15 conforme a estabilidade do outro |
| Durante a própria técnica | 0 |

**Vulnerabilidade (sutil):** quem respira solta menos o corpo: as próprias técnicas saem com janela × 1,25
(mais tempo para o outro reagir) e quem **controla** e respira perde 0,3 de estabilidade por tick em vez de
ganhar. Nada acontece sozinho: a consequência só vem se o outro agir (escape, raspagem, upa).

**RT: respirar × base × defesas.** RT **segurado** = respirar. RT **apertado** (borda) durante a janela da
entrada do outro (± `BUFFER_BASE_TICKS` = 6) = base. Enquanto se sofre uma entrada em pé, o RT não conta
como respiração. RT + RS livre = defesas (sem mudança).

## Custos (antes → agora)

| Item | 5P | 5P.1 |
|---|---|---|
| Pegadas (JSON) | custo do JSON | × 0,6 (`FATOR_CUSTO_PEGADAS`), ex.: PEGADA_1 3,0 → 1,8 |
| Desequilíbrio / rompimento técnico / reforço | 3,0 / 2,5 / 2,0 | 2,0 / 1,75 / 1,5 |
| Arrasto por pixel / resistência por tick | 0,0025 / 0,004 | 0,0015 / 0,0025 |
| Distância de joelhos / deitado | × 2,5 / × 5,0 | × 2,0 / × 3,5 |
| Técnicas de solo (tabela acima) | custo base | base × 0,5 × √(fôlego máx / 32,5) (`CustosFolego.custoSolo`) |

Com a raiz do fôlego máximo, o Branco (25) paga ~0,44 da base e o Preto (40) ~0,55: a **fração** do tanque
gasta é parecida. Ex.: passar a guarda 4,0 → 1,75 (Branco) / 2,22 (Preto).

## Execução mais acessível

| Parâmetro | 5P | 5P.1 |
|---|---|---|
| Janelas das técnicas | catálogo | × 1,3 (`FATOR_JANELA`) |
| Recarga / recarga após falha | 18 / 40 | 12 / 28 |
| Aterrissagem (escolher como cair) | 10 | 24 |
| Ganho passivo de estabilidade | 0,2 | 0,12 |
| Enquadrar baixa o controle em | 20 | 25 |
| Limiares de escape (quadril, meia, virar, fugir, upa, escape da montada, girar, neutro) | 50/60/60/60/45/60/50/35 | 60/65/65/65/55/65/55/45 |

Causa principal dos comandos "mortos": a transição de postura (~14 ticks) bloqueava **todo** comando de solo
depois de cada queda/raspagem. Agora só bloqueia em pé. Requisitos (postura, exposição, estabilidade) mantidos.

## Guardas: ações distintas

PUXAR GUARDA (em pé ou do chão) · FECHAR (a própria guarda) · MANTER (retenção) · REC. GUARDA / REC. MEIA
(escapes que recuperam) · ABRIR (a guarda do outro) · SOLTAR (a própria). Ao cair, quem cai vê a cruz
"CAINDO: COMO RECEBER?" (↑ FECHAR GUARDA, ←/→ MEIA) durante a aterrissagem.

## Avisos no topo

`hud/AvisosCombate`: só resultados **confirmados** do motor (via `NarradorSolo`: "J1 derrubou J2!",
"J2 conseguiu uma raspagem!", "J1 foi ao chão." …); nunca tentativas nem recusas. Um por vez, 110 ticks
(~1,8 s; 60 com fila), fila de 3, abaixo do relógio (y 34–56) e entre os painéis de fôlego.

## Reiniciar luta

Pausa → **REINICIAR LUTA**: mesmo caminho do "tentar novamente" (mesmos Miaus, arena, modo, início e tempo);
fôlego, posições, pegadas, solo e avisos zerados. F5 do treino continua igual.

# Entrega 5P.2 — Famílias contextuais, guardas novas, desengajar e condicionamento

Nomes (padrão PlayStation): **L1** = LB / E, **L2** = LT / Q, **R1** = RB / O (Select = RB), **R2** = RT / P,
**RS** = analógico direito / setas. Os mapeamentos físicos não mudaram.

## Famílias: antes e depois

| Família | Antes (5P.1) | Agora (5P.2) |
|---|---|---|
| RS | fundamentos no chão; pegadas / reforço / desequilíbrio / romper em pé | igual + **aplicador da pegada**: ← tiro de guarda, ↓ derrubada forçada |
| R1 + RS | quedas em pé; explosivos no chão (upa, levantar...) | igual + **guarda fechada por baixo**: ↑ desequilibrar, ←/→ ângulo, ↓ abrir e afastar; **por cima em qualquer guarda**: ↓ criar espaço e levantar; **dominando**: ↓ soltar a posição; **solo livre**: em pé ↓ joelho direto, no chão ↓ afastar |
| L1 + RS | reservado (células "GUARDA*" da 5E) | **guardas**: em pé ↑ tiro, ↓ puxar guarda, ← borboleta, → De La Riva; no chão por baixo: roda de guardas (↑ aberta, ← borboleta, → DLR, ↓ laço); posições de controle: finalizações futuras |
| L2 + RS | reservado ("QUEDA*") | **quedas** com a pegada: ↑ rasteira, → projeção de quadril, ← queda pela manga, ↓ derrubada forçada; no chão: ↓ pegar a manga; posições de controle: finalizações futuras |
| L1 + L2 + RS | vazio | **especiais**: ↑ berimbolo (De La Riva); posições de controle: 3ª família de finalizações; em pé: nada |

### Prioridade (uma ação por comando)

1. L1 e L2 segurados juntos = sempre **L1+L2** (a camada QUATRO do Contextual): nunca a família de um só.
2. RB segurado = R1 (a família PROJEÇÕES do 5O; RT + RB continua = defesas).
3. A técnica do **catálogo** vence; se não há técnica e a posição/papel tem espaço de finalização, o comando
   é recusado como "FINALIZAÇÃO FUTURA" (sem custo).
4. Em pé com a pegada, RS ↑ / → continuam no `SistemaCombate` (reforço, desequilíbrio) e RS ← / ↓ vão para o
   motor (tiro, derrubada). O receptor continua com RS ↓ = romper e o arrasto.
5. Teste automático: para todas as posições, papéis, famílias e direções existe **no máximo uma** técnica,
   e nenhum espaço de finalização colide com uma técnica real.

## Pegada ativa: tiro de guarda e derrubada forçada

- **Tiro de guarda** (RS ← ou L1 ↑; aplicador): janela 12 (×1,3), custo 4, explosiva. Dá certo → guarda
  fechada com quem saltou controlando (estabilidade 55). Falha (o outro **recua**, faz **base** com RT na
  janela, ou firmeza < 40) → quem saltou cai **deitado sem guarda** e o outro fica em pé.
- **Derrubada forçada** (RS ↓ ou L2 ↓; aplicador): janela 16, custo 5, exposição de avanço. Tira 40 + 30% da
  firmeza da base do outro; só derruba quem fica **sem base** (ou exposto) — a primeira normalmente só
  desequilibra. LS para o outro no fim = acompanha (aterrissagem por cima). Base firme (RT) → quem forçou
  perde base e fica com o avanço exposto (o outro pode empurrar/derrubar).

## Quedas novas (L2, exigem a pegada estabelecida e aplicada por quem tenta)

| Técnica | Pegada | Janela / custo | Entra quando | Falha |
|---|---|---|---|---|
| Rasteira (↑) | qualquer | 10 / 4,0 | o outro anda (LS), está sem base ou exposto | pé firme: exposto 20 ticks |
| Projeção de quadril (→) | cruzada ou nuca | 22 / 6,5 | sem base firme do outro | base firme na janela = **reversão** (quem projetava cai) |
| Queda pela manga (←) | duas mangas | 18 / 5,5 | firmeza ≥ 30, ou sem base | base firme: exposto 30 ticks |

## Guardas novas

| Guarda | Entrada | Por baixo | Por cima | Saídas |
|---|---|---|---|---|
| **Borboleta** (de joelhos por cima) | L1 ← em pé (perto ou com pegada), L1 ← do chão (solo livre), roda de guardas | ↑ elevação (raspa: fica por cima na guarda aberta do outro), ←/→ desequilibrar, ↓ manter os ganchos | ↑ amassar e passar (controle < 60), ↓ base, ←/→ tirar os ganchos | passar, raspar, levantar, afastar |
| **De La Riva** (por cima **em pé**) | L1 → com pegada, L1 → do chão, roda | ↑ raspagem (meia por cima), ←/→ desequilibrar, ↓ manter o gancho, **L1+L2 ↑ berimbolo** (controle ≥ 50 e o outro sem base → costas) | ↑ quebrar o gancho e passar (controle < 55), ↓ base, ←/→ livrar a perna | idem + livrar a perna |
| **Laço** (por cima em pé) | só com a **manga** (L2 ↓ pegar a manga, ou ter caído com a pegada das mangas) e L1 ↓ | ↑ raspagem pelo próprio controle (≥ 40), ↓ travar o laço | ↑ passar (controle < 45, mais difícil), ↓ base, ←/→ soltar a manga (volta à guarda aberta) | idem |

A roda de guardas (por baixo, L1) é uma transição curta (6 ticks) e **exposta**: um passe do outro na janela
entra como contra. Sair das guardas solta a manga.

## Solo livre (um em pé, outro no chão)

| Quem | RS | R1 | L1 |
|---|---|---|---|
| Em pé | ↑ entrar na guarda, ↓ recuar, ←/→ prender as pernas | ↑ passagem em pé, ↓ joelho na barriga direto (só com o outro exposto: pernas presas, levantando...) | — |
| No chão | ↑ levantar, ↓ pés (distância), ←/→ puxar para a guarda aberta | ↓ afastar | ← borboleta, → De La Riva |

Aproximar-se não muda a posição (nada de cem quilos automático); só uma entrada que dá certo consolida.
Pernas presas: a retenção com os pés não serve como contra ("PERNAS PRESAS").

## Desengajar e levantar

- **Soltar o próprio controle é simples**: quem domina (100 kg, joelho, montada, costas) usa R1 ↓ "soltar a
  posição" (janela 6) → fica em pé contra quem está no chão, sem vínculo.
- **Na guarda**: por baixo, R1 ↓ "abrir e afastar" (os passes do outro na janela entram como contra) e R1 ↑
  levantar direto (guardas abertas e meia). Por cima, R1 ↓ "criar espaço e levantar": só com o controle do
  outro < 60 (ou ele exposto/exausto); quebrar a postura / manter os ganchos na janela segura.
- **Dominado** não sai direto: precisa do escape (existente).
- Teste: em toda posição de chão, os dois lados têm pelo menos uma saída ou progresso.
- Ao separar, o vínculo acaba no mesmo tick e cada um volta a andar.

## Condicionamento (desgaste físico)

| Origem | Desgaste |
|---|---|
| Técnica | 10% do custo de fôlego (30% nas explosivas: R1, quedas em pé, tiro de guarda) — cobrado uma vez, no mesmo lugar do fôlego |
| Ser derrubado | 0,8 × (cond. máx / 30), uma vez por queda |
| Preso sob controle dominante | 0,003/tick × controle/100 × (cond. máx / 30) |
| Mantendo a pressão dominante | 0,0012/tick × (cond. máx / 30) |
| Livre, deitado ou nas guardas | nada; a recuperação passiva lenta de sempre volta assim que não há vínculo |

**Exaustão** (abaixo de 35% do condicionamento, gradual até 0): janelas até +40%, ganho de controle até −50%,
respiração até −40%, explosivas até +30% de custo. **Crítica** (≤ 5%): evento `EXAUSTAO_CRITICA` e aviso
"J1 está exausto!"; a luta **não** termina por isso (regra futura). Parâmetros em `CustosFolego`.

## Eventos de luta (para regras futuras)

`MotorSolo.consumirEventosLuta()` classifica: QUEDA, RASPAGEM, PASSAGEM, MONTADA, COSTAS, ESCAPE,
RECUPERACAO_GUARDA, ABANDONO, EXAUSTAO_CRITICA. Nenhum ponto é dado nesta entrega.

## Arquitetura

- `TecnicaSolo.Familia` + `ComandoSolo(direção, família)` (o construtor antigo `(direção, explosivo)` vale RS/R1).
- `RequisitoTecnica` (pegada aplicada, mangas, cruzada/nuca, manga no chão, distância) conferido em
  `MotorSolo.motivoSem`; o HUD mostra a técnica cinza quando falta algo.
- `RegistroFinalizacoes` (48 espaços, ids estáveis), `EventoLuta`, conexão `MotorSolo.Conexao.MANGA`.
- Animações: `CatalogoTecnicasSolo.animacaoFutura(id)` = "solo/<id>"; hoje tudo usa as poses procedurais
  (`AnimadorSolo`: borboleta como as guardas; De La Riva e laço com quem está por cima em pé).

## Limitações (5P.2)

- J1 no **teclado** não tem L1/L2 (só RS/teclas de pegada e R1 pela tecla 4); as famílias L ficam no Xbox e no
  teclado virtual do J2. O tiro de guarda e a derrubada forçada funcionam no teclado do J1 (teclas ← e ↓ da pegada).
- Sem animações próprias (salto para a guarda, projeção, berimbolo usam as transições de postura).
- Guarda aranha, X e de lapela: planejadas (ver "Sugestões" no relatório), não implementadas.

## Entrega 5Q.6 — quedas

### Rasteira

- **Causa da dificuldade:** só entrava se o outro tivesse andado nos últimos 24 ticks, com base
  < 85 ou exposto; parado (o normal numa luta) quase nunca.
- **Agora (pontuação ≥ 40):** base perdida (100 − base) + passo recente do outro (+45) **ou** o
  outro foi puxado/arrastado pela pegada (+40) + desequilíbrio/exposição (+60) + 0,3 × firmeza da
  pegada. Janela do "passo recente": 40 ticks. Parado, com base cheia e pegada média, continua
  não entrando; o RT na janela segura quem está parado.
- **Rasteira defendida:** pequeno desequilíbrio (20 ticks) e volta à postura da pegada. Causa da
  deformação: a janela acabava no mesmo tick da resolução (sem volta suave) e a pose da pegada
  era apagada (a tentativa seguinte partia de outra pose e acumulava).

### Quedas explosivas (sem pegada)

| Queda | Comando | Alcance | Destino sem reação | Defesa |
|---|---|---|---|---|
| Single leg | LT ↑ em pé | 170 px | meia-guarda por cima | sprawl (RT sem sprawl: os dois ao chão sem controle) |
| High crotch | LT → em pé | 170 px | cem quilos | sprawl |
| Carregada no ombro | LT ↓ em pé | 160 px | cem quilos | sprawl |
| Ankle pick | LT ← em pé | 130 px | o outro senta, quem pegou fica em pé | base (RT) |
| Body lock | RB → em pé sem pegada | 150 px | cem quilos | base (RT) |
| Single leg de joelhos | LT ↑ de joelhos | 120 px | meia-guarda por cima | base (RT) |
| Double leg de joelhos | LT → de joelhos | 120 px | cem quilos | base (RT) |
| Snap down | LT ↓ de joelhos | 110 px | cem quilos | base (RT) |
| Arm drag | LT ← de joelhos | 110 px | costas | base (RT) |

Todas: avanço **real** até o contato durante a janela (até 9 px/tick, sem teletransporte), custo
de fôlego, recuar para longe no fim da janela faz falhar, a aterrissagem existente (o defensor
ainda pode reagir com guarda/meia-guarda), animação própria de quem entra e de quem recebe.
De joelhos não levanta: menos deslocamento e amplitude vertical.

### Projeções e entradas de guarda com a pegada (LB + LT)

| Técnica | Comando | Requisito | Resultado | Defesa |
|---|---|---|---|---|
| Uchi-mata | ↑ | pegada | projeta (cem quilos) | base (RT); pegada fraca falha |
| Osoto-gari | → | pegada | projeta (cem quilos) | base (RT); pegada fraca falha |
| Puxar para o laço | ↓ | pegada | guarda laço (segura a manga) | recuar / base |
| Puxar para a aranha | ← | pegada nas mangas | guarda aranha | recuar / base |

### Saída da guarda (quem está por cima, RB ↓)

- **Guarda aberta** (borboleta, DLR, laço, aranha, SLX, meia, aberta): janela 9, o controle do
  outro precisa estar ≥ 85 para segurar — sem controle efetivo é fácil criar distância.
- **Guarda fechada:** com o controle de quem está por baixo ≥ 45 precisa de **postura antes**
  ("FAÇA POSTURA ANTES (RS↓)"); janela 20 e segura com controle ≥ 45.
- **Afastar com os pés** (por baixo): quem estava por cima recua até 122 px (3 px/tick) e quem
  empurrou também escorrega 14 px para trás no chão (1 px/tick).

### Cem quilos → costas (RB ↑)

Antes só entrava com o outro **exposto** (virando para os joelhos): do cem quilos parado sempre
falhava ao fim da janela. Agora também entra com o controle consolidado (≥ 70) e, sem isso,
recusa na hora com "CONSOLIDE ANTES (n/70)".

## Entrega 5Q.7 — solo livre, falhas das quedas e acrobacias

### PÉS (quem está no chão, RS ↓)

Antes só aparecia a mensagem. Agora quem empurra com os pés **escorrega para trás** de verdade
(`MotorSolo.RECUO_PES_PX` = 16 px, 1 px por tick, coordenada real), continua deitado, para na borda
da arena e mantém a animação das pernas empurrando. Vale usado sozinho ou como resposta (contra)
ao ENTRAR, ao JOELHO DIRETO e à passagem em pé. Afastar da guarda e o rastejo não mudaram.

### SEGURAR AS PERNAS e arrastar (nova)

- **Comando:** RS → de quem está por cima no solo livre, perto (o mesmo alcance do PRENDER, 110 px,
  mesma faixa). Janela curta (10 na escala do catálogo); a resposta com os PÉS na janela defende.
- **Pegada persistente:** enquanto durar, quem segura **anda** (LS) e leva o outro junto pelas
  coordenadas reais; o outro continua deitado. Em pé ou de joelhos.
- **Balanceamento** (`MotorSolo`): velocidade de quem arrasta × `FATOR_VELOCIDADE_ARRASTO` (0,35);
  com o outro se debatendo × `FATOR_RESISTENCIA_ARRASTO` (0,5); fôlego `CUSTO_FOLEGO_POR_PX_ARRASTO`
  (0,06 por px arrastado); a borda da arena segura os dois (ninguém atravessa); a profundidade
  acompanha.
- **Romper:** o segurado acumula `RESISTENCIA_POR_TICK` (1) por tick se debatendo (recupera 0,25 por
  tick parado) e rompe em `RESISTENCIA_PARA_ROMPER` (30, ~0,5 s); PÉS, puxar guarda, levantar ou
  qualquer técnica aceita do segurado também rompem.
- **Termina sozinho:** posição mudou, alguém levantou/caiu, distância > `DISTANCIA_MAXIMA_ARRASTO`
  (140 px) ou linhas diferentes, fôlego do segurador < 1, luta encerrada, outra ação do segurador.
- **Soltar:** o mesmo RS → (a roda mostra SOLTAR enquanto segura).
- Não conta como queda, passagem nem finalização (`EventoLuta`), natureza CONTROLE (creme).

### Rasteira e derrubada forçada que falham

- **Causa da deformação:** a pose da pegada já estende o braço do defensor até o kimono do outro;
  as animações da tentativa somavam deslocamentos de mão e cotovelo pensados para o braço em
  repouso. No meio da reação o antebraço da frente ia de ~1,1x a ~1,6x e o de trás encolhia para
  ~0,3x (medido tick a tick). A pose final já voltava certa (5Q.6); o meio, não.
- **Correção:** `animacao/IntegridadeMembros` — quando a técnica parte da pose da pegada, os braços
  **giram em torno do ombro e dobram o cotovelo** com os comprimentos que tinham antes (giro
  máximo 30°, a mão pode chegar até 55% do alcance). Vale para quem ataca e quem recebe, só no
  desenho.
- **Derrubada forçada segurada com RT:** nova reação curta (`derrubada_forcada_defendida:RECEBE`,
  20 ticks) no estilo do empurrão aprovado: tronco cede, meio passo, volta à pegada.
- **Derrubada forçada que só desequilibra:** já usava a reação do empurrão; agora sem deformar.
- **Quedas que entram:** iguais (a mesma janela, agora sem esticar os braços).

### Acrobacias: recuperação dinâmica (estrela, mortal para frente e para trás)

A **execução** (duração da animação) não mudou; só o intervalo **depois** dela (`IrmaoMiau.recargaAcrobaciaDinamica`):

```
recarga = 24 × (1 − 0,8 × mobilidade) × (1 + 1,0 × (1 − condição)),   limitada a 3..60 ticks
mobilidade = (0,6 × agilidade + 0,4 × velocidade) / 100   (até 1; atributos já com a faixa)
condição  = 0,5 × fôlego atual/máximo + 0,5 × condicionamento atual/máximo
```

A graduação entra **só pelos atributos** (`EvolucaoGraduacao`), sem bônus extra. Exemplos
descansados: Branco faixa branca ~19 ticks, Preto faixa branca ~15, Branco faixa preta ~15, Preto
faixa preta ~8 (antes: 24 para todos). Cansado, até o dobro. Com fôlego/condicionamento infinitos
(Modo Desenvolvedor/treino) a condição fica no máximo; o que ficou limitado continua contando.

### Rolamento técnico de joelhos

`IrmaoMiau.FATOR_ROLAMENTO_JOELHOS_5Q7` = 0,6 (40% mais lento): multiplicador 0,8 → 0,48, duração
28 → 46 ticks (~1,65x), **mesma distância** (a velocidade cai na mesma proporção e a animação anda
pelo progresso: sem deslizar). Rolamento deitado, em pé, sprawl, movimentação de joelhos, rastejo e
levantada técnica não mudaram.
