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

- **Vínculo de solo** (`IrmaoMiau.setVinculadoNoSolo`, todas as posições de chão menos "um em pé × solo")
  bloqueia **toda** recuperação de fôlego e condicionamento, igual à pegada da 5O.3 (mesmas quatro funções,
  sem rota nova). Separou (escapou para em pé / um em pé × solo): recupera normalmente.
- **Cansado** (< 25% do fôlego): as próprias janelas ficam 50% mais longas (o outro tem mais tempo) e o
  controle cresce pela metade. **Exausto** (< 15%): o controle dele pode ser escapado mesmo firme.
- Sem fôlego para o custo, a técnica é recusada ("SEM FÔLEGO"). Nada fica negativo.

## Prioridade dos comandos

1. Transição de postura em andamento: nada (recusa "EM MOVIMENTO").
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
