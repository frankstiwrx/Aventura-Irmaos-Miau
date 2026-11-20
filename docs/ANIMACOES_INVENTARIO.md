# Inventário de animações (Entrega 5Q)

Legenda: **Própria** = animação dedicada (JSON ou procedural específica); **Provisória** = pose procedural genérica
sobre a pose da postura/posição (`AnimadorSolo.aplicarEfeito`), sempre a partir do estado do motor; **Ausente** =
só a pose da posição (sem movimento da ação).

| Ação | Estado lógico | Animação usada hoje | Ideal | Própria? | Provisória? | Prioridade |
|---|---|---|---|---|---|---|
| Empurrar | tentativa EMPURRAO / DERRUBADA_FORCADA | **5Q:** tronco à frente + braços estendidos (EMPURRANDO) | preparação, passo, transferência de peso, recuperação | não | sim | alta |
| Ser empurrado | base reduzida pelo empurrão | **5Q:** tronco para trás + braços (EMPURRADO, 18 ticks); o recuo real é do motor | passos de recuperação, cambaleio por intensidade | não | sim | alta |
| Afastar no chão / abrir e afastar / recuar | AFASTAR_DA_GUARDA, AFASTAR_DO_CHAO, RECUAR, pés | **5Q:** extensão dos braços e tronco para trás (AFASTANDO) | pernas empurrando o quadril do outro | não | sim | alta |
| Soltar controle / abandonar posição / tirar ganchos / livrar mangas | ABANDONAR_POSICAO, SAIR_DA_GUARDA, SOLTAR_*… | **5Q:** braços abrindo (SOLTANDO) | separação gradual dos dois | não | sim | média |
| Levantar (técnico, da guarda, do solo, do ashi) | LEVANTAR_* | **5Q:** inclinação + braço de apoio (LEVANTANDO) + transição de postura existente | levantar técnico com mão de base | parcial | sim | alta |
| Finalização (atacar) | `MotorSolo.Ataque` | **5Q:** pose por categoria (braço / estrangulamento / perna), cresce com a pressão | 5 fases por técnica, JSON do MiauStudio | não | sim | alta |
| Finalização (defender) | defensor do ataque | **5Q:** proteger o braço / mãos no pescoço / esconder o pé | reação específica por técnica | não | sim | alta |
| Bater (rendição) | `Vitoria.rendicao` | **5Q:** mão batendo no tatame (BATENDO, 40 ticks) | — | não | sim | média |
| Quedas (pegada, pernas, rasteira, quadril, manga) | QUEDA_* / derrubar | transição de postura (cair/ajoelhar) | desequilíbrio + arremesso conjunto | não | sim | alta |
| Desequilíbrio | DESEQUILIBRAR / base | ausente | inclinação do tronco | não | não | média |
| Puxar guarda / tiro de guarda | PUXAR_GUARDA / TIRO_GUARDA | transição de postura (deitar) | salto/sentar com as pernas envolvendo | não | sim | alta |
| Derrubada forçada | DERRUBADA_FORCADA | **5Q:** EMPURRANDO / EMPURRADO | pressão com a pegada | não | sim | média |
| Berimbolo | BERIMBOLO → COSTAS | troca de pose das posições | inversão e giro | não | não | alta |
| Raspagens | RASPAGEM_* | troca de pose das posições | troca progressiva cima/baixo | não | não | alta |
| Passagens | PASSAR_* | troca de pose + encaixe gradual | progressão ao redor da guarda | não | não | alta |
| Trocas de guarda (borboleta, DLR, laço, aranha, SLX) | IR_* | pose da posição (borboleta como guarda; DLR/laço/SLX com quem está por cima em pé) | sentar, ganchos, pés nos bíceps | parcial | sim | média |
| Controles dominantes (100 kg, montada, costas, joelho) | posição | pose procedural por posição (5P) | variações de pressão | sim | — | baixa |
| Escapes (quadril, upa, girar) | ESCAPE_* | troca de pose das posições | fuga de quadril / ponte | não | não | média |
| Ashi garami | ASHI_GARAMI | pose genérica de "por baixo/por cima" | pernas entrelaçadas | não | sim | média |

Tudo o que é provisório tem um identificador futuro (`solo/<id>` para técnicas, `finalizacao/<id>` para
finalizações) para receber JSON do MiauStudio sem mexer no motor.

---

# Atualização 5Q.3 — O jiu-jitsu ganha vida

## Pipeline (auditoria)

`MotorSolo.efeitoVisual(lado)` → `AnimadorSolo.aplicar` → **`AnimacoesTecnicas`** (novo: quadros-chave por técnica e
papel, interpolação suave) → se a técnica não tem animação própria, a pose provisória genérica da 5Q
(`AnimadorSolo.aplicarEfeito`). O motor só fornece dados de desenho (técnica, marco em que resolve, recuo já
aplicado, ponto de interrupção, pulso da defesa); **a animação nunca decide nada**. JSON do MiauStudio, transições
de postura e reações do receptor continuam como estavam (as transições de postura têm prioridade, como antes).

## Animações definitivas desta entrega (aplicador e receptor)

| Técnica | Aplicador | Receptor | Interrupção |
|---|---|---|---|
| Empurrar / derrubada forçada | carrega o peso, estende os braços na altura do peito, passo à frente, recupera | impacto (tronco e cabeça para trás, braços), passo atrás, recupera a base; o corpo **desliza** do lugar antigo até o recuo que o motor aplicou | — (queda só quando o motor derruba: a transição de postura assume) |
| Afastar no chão (abrir e afastar) | recolhe os joelhos e estende as pernas | tronco para trás, mãos soltam, recuo leve | volta suave (LIBERANDO) |
| Armlock da guarda fechada | segura o braço, angula o quadril, perna sobe por cima, quadril sobe com a pressão | braço preso esticado, postura quebrada, cabeça baixa; na defesa empilha e segura a própria mão | defesa/escape/soltar: os dois voltam suave |
| Americana da montada | procura o punho, prende o braço no chão ao lado da cabeça | braço dobrado ao lado da cabeça; na defesa empurra o peito e gira o quadril | idem |
| Mata-leão das costas | braço passa pelo pescoço, trava com a mão atrás da cabeça, cabeça aparece sobre o ombro | mãos no braço que estrangula; na defesa queixo baixo | idem; rendição = BATENDO |
| Berimbolo | vira o quadril por baixo, inverte (de cabeça para baixo), gira e sobe por trás | perde a base, pé da frente sobe, joelho desce | defendido: volta suave |

Todas são locais (a frente de cada lutador), então valem para J1 ou J2, Branco ou Preto, em qualquer linha de
profundidade e espelhamento. A pressão da finalização controla a intensidade; a defesa gera um "pulso" de reação.

## Provisórias (pose genérica da 5Q)

As outras finalizações (por categoria: braço / estrangulamento / perna), soltar/abandonar, levantar, bater
(mão no tatame), afastar do chão em solo livre (AFASTAR_DO_CHAO), recuar e a retenção com os pés.

## Ausentes

Quedas como animação conjunta (usam a transição de postura), tiro de guarda, raspagens, passagens, trocas de guarda,
escapes (quadril, upa), ashi garami e chaves de perna, triângulo/omoplata específicos.

## Problemas visuais conhecidos

- **Afastar no chão**: o motor não aumenta a distância depois do afastamento (mecânica fora do escopo), então quem
  levanta continua perto das pernas de quem está no chão.
- **Costas (mata-leão)**: pelo layout de costas da 5P o aplicador fica atrás do defensor; o braço e a cabeça
  aparecem, mas boa parte do corpo fica escondida.
- **Berimbolo → costas**: no fim da inversão a transição de postura do motor assume e há uma pequena troca de pose.
- **Guarda (armlock)**: o tronco de quem está por cima ainda cobre parte do peito de quem está por baixo.
- **Problema anterior registrado (5Q)**: o comentário da pose genérica dizia que 16/17 e 20/21 eram ombro/cotovelo;
  na verdade são posições das mãos (x/y). As poses genéricas da 5Q funcionam assim mesmo (o comentário foi
  corrigido; o comportamento não foi alterado).

# Atualização 5Q.4 — Consolidação visual

## Novas animações próprias (AnimacoesTecnicas)

| Técnica | Aplicador | Receptor | Fim |
|---|---|---|---|
| Quebrar a postura (guarda fechada, ajuste instantâneo) | puxa com os joelhos e os braços para o peito | **postura quebrada**: tronco cai para a frente, mãos no chão; fica curvado os 40 ticks da exposição | recompõe devagar; RS ↓ (postura) recompõe na hora, com volta suave |
| Defendida pela postura (raspagem contra quem está em postura) | perde a puxada, joelhos abrem | **se apruma**: tronco para trás, mãos no quadril do outro | termina na pose da posição |
| Prender as pernas (em pé sobre quem está no chão) | abaixa, alcança as canelas, junta e puxa | pernas juntas e puxadas | volta suave (LIBERANDO) |
| Ajustar o controle da perna (ashi) | aperta os joelhos, abraça o pé, gira o quadril | a perna é puxada, tronco para trás | termina na pose da posição |
| Reter com os pés | as pernas recolhem e empurram (mesmos quadros do afastar) | empurrado para trás | volta suave |
| Afastar do chão (ajuste instantâneo) | recolhe e estende as pernas | — | termina na pose da posição |

"Quebrada" e "defendida" são visualmente opostas (curvar x aprumar). A raspagem contra postura ativa é a
tentativa realmente defendida no motor (critério SEM_POSTURA_DO_OUTRO); quebrar postura é um ajuste
instantâneo que sempre entra.

## Pernas por guarda (`animacao/PernasGuarda`)

Quem está por baixo, deitado, ganha pernas próprias em cada guarda: fechada (canelas cruzadas atrás da cintura),
aberta (pés no quadril), meia (uma presa, a outra no escudo de joelho), borboleta (joelhos altos, ganchos),
De La Riva (gancho por fora da perna), laço (canela alta no braço), aranha (os dois pés nos bíceps), single-leg X
(uma baixa, a outra alta no quadril) e ashi garami (as duas enroscadas numa perna). As pernas são montadas por
**ângulo a partir do quadril com o comprimento real** da coxa e da canela de cada irmão: nenhuma estica.

## Membros sem esticar nas poses provisórias

`AnimadorSolo.aplicarEfeito` movia só a **mão** (até ~140 px no estrangulamento) e o cotovelo ficava parado: o
antebraço virava uma linha. Agora a mão respeita um alcance (`LIMITE_MAO` = 34 px) na mesma direção e o
cotovelo acompanha metade. As animações próprias da 5Q.3 não passam por aí e não mudaram.

## Contornos

Membros (braço, manga, mão, perna, pé), tronco e pelve têm uma borda fina num tom próximo da própria cor
(mais escuro nas cores claras, mais claro nas escuras), desenhada logo antes de cada membro: branco de kimono
branco e preto de kimono preto separam mão, manga, tronco e perna. Vale em pé e em todas as poses do
MiauStudio, para os dois irmãos e os três kimonos. **A faixa não mudou.**

## Pendente / estrutural (não alterado)

- **Ordem de desenho no solo**: quem está por cima é desenhado depois, então as pernas de quem está por baixo
  na guarda ficam parcialmente atrás do tronco do outro. Corrigir exige desenhar as pernas de baixo numa camada
  própria depois do de cima (mudança no `GamePanel`/renderer), não feita aqui.
- **Afastar no chão não aumenta a distância lógica** (registrado na 5Q.3). Proposta, sem aplicar: no sucesso de
  `AFASTAR_DA_GUARDA`/`AFASTAR_DO_CHAO`/`RETENCAO_PES`, o motor reposicionaria quem está em pé para
  `afastamento(EM_PE_CONTRA_SOLO) + 40 px` (o mesmo cálculo do RECUAR, limitado pela arena). Muda alcance de
  técnicas (CONECTAR_GUARDA, PRENDER_PERNAS): precisa de aprovação e de rebalancear os alcances.
- Poses de quem está **por cima** nas guardas abertas (DLR, laço, aranha, SLX, ashi) continuam a mesma de
  joelhos genérica.

# Atualização 5Q.5 — Refinamento da jogabilidade

Legenda: **própria** = quadros-chave em `AnimacoesTecnicas` (ou animador dedicado) para quem faz e quem recebe;
**provisória** = pose procedural genérica (`AnimadorSolo.aplicarEfeito`) ou reaproveitada; **pendente** = sem
animação específica ainda.

| Técnica / movimento | Executante | Adversário | Situação |
|---|---|---|---|
| Rasteira (L2 ↑ na pegada) | tira o peso, levanta o pé, varre baixo puxando as mangas | o pé sai do chão, tronco para trás, perde a base | **própria** (entrada → execução → queda pelas transições de postura; defendida: volta suave) |
| Entrada nas pernas (RB ↓ em pé) | baixa o nível, passo de penetração, abraça e empurra | pernas presas, quadril empurrado, braços sobem | **própria** |
| Joelho na barriga direto (RB ↓ no solo livre) | sobe o joelho, desce na barriga, mão na gola | mãos sobem para enquadrar, impacto | **própria** (depois: transição de postura até o joelho na barriga) |
| Levantada técnica (RS ↑ deitado no solo livre) | janela: senta, apoia a mão de trás e planta o pé; transição própria `LEVANTADA_TECNICA` (30 ticks): tira o quadril, passa a perna para trás, sobe e assenta | — | **própria** (antes: transição rápida de 16 ticks, girando 88° de uma vez) |
| Recuo em pé (RS ↓ no solo livre) | dois passos para trás alternando a base; o corpo **desliza** do lugar antigo até o novo (sem teletransporte) | — | **própria** |
| Postura (RS ↓ por cima na guarda fechada) | apruma, mãos no quadril do outro, segura enquanto a postura dura (60 ticks) | perde a puxada | **própria** |
| Recompor a postura (vindo da postura quebrada) | parte de curvado, passa do ponto e segura aprumado | perde a puxada | **própria** |
| Pernas presas (depois de prender as pernas) | as mãos **ficam** nas canelas enquanto o controle mecânico (PERNAS_PRESAS, 45 ticks) durar | pernas juntas e puxadas | **própria**; termina com volta suave |
| Entradas no controle de perna (L1+L2 na guarda fechada/aberta/DLR) | gira o quadril, as pernas descem e enroscam | a perna da frente é puxada, o tronco vem junto | **própria** |
| Abandonar o domínio para a perna (L1+L2 por cima) | senta para trás e abraça a perna (fica exposto) | a perna sobe presa | **própria** |
| Underhook na meia-guarda (RS ←/→ por baixo) | braço por baixo, quadril vira | o de cima fica curvado (postura quebrada) | **própria** |
| Deslocamento acelerado de joelhos (X) | passada própria (`AnimadorJoelhosAcelerado`): joelhos alternando com passo maior, tronco inclinado, braços acompanhando, quique leve; continua de joelhos | — | **própria** (procedural) |
| Deslocamento acelerado deitado (X) | impulsos de quadril: reaproveita o JSON `fuga_quadril` (avança pela distância andada) | — | **reaproveitada** |
| Afastar da guarda | (5Q.3) pernas empurram | (5Q.3) empurrado | própria; agora a distância lógica também aumenta (3 px/tick até 122 px) |
| Escapes de 100 kg / montada / joelho (enquadrar + escape) | — | — | **provisória** (o escape usa a pose da posição; enquadrar não tem quadros próprios) |
| Reação ao bater/defender/apertar | (5Q.3) | (5Q.3) | própria nas 5 finalizações da 5Q.3; as demais seguem a pose provisória por categoria |

## Ordem de desenho (resolvido)

A prioridade de desenho agora vem da **posição**, não de J1/J2 (`GamePanel.frenteNoSolo`), só quando os dois
estão ligados no solo e na mesma linha:

- guardas (fechada, aberta, meia, borboleta, DLR, laço, aranha, SLX) e ashi garami: **quem está por baixo na
  frente**, então as pernas da guarda envolvem quem está por cima e ficam visíveis;
- domínio por cima (100 kg, joelho na barriga, montada, costas): **quem está por cima na frente**;
- fora do vínculo (em pé, solo livre, solo disputado): a ordem antiga por linha (estável).

Limitação estrutural (Rig 2.0): cada lutador é desenhado inteiro de uma vez. Uma perna que passa **por trás** do
outro e outra **pela frente** ao mesmo tempo (triângulo, laço) exige camadas por membro, e isso fica para o Rig 2.0.

## Pendente

- Poses de quem está **por cima** nas guardas abertas (DLR, laço, aranha, SLX, ashi) continuam a pose de joelhos
  genérica.
- Enquadrar e escapar dos domínios usam a pose da posição (sem quadros próprios).
- O ajuste de estabilizar/consolidar (100 kg, joelho, montada) não tem quadros próprios.

## Entrega 5Q.6

### Novas (próprias, aplicador e receptor)

| Animação | Observação |
|---|---|
| rasteira_defendida:RECEBE | pequeno desequilíbrio e volta à pose da pegada |
| single_leg, high_crotch, ankle_pick, carregada_ombro, body_lock | quedas explosivas (referência: a entrada nas pernas aprovada) |
| single_leg_joelhos, double_leg_joelhos, arm_drag, snap_down | de joelhos, menor amplitude |
| uchi_mata, osoto_gari, puxar_aranha, puxar_laco | roda LB + LT com pegada |
| berimbolo_kiss, berimbolo_invertido (baby bolo) | variações com giro/rota diferentes do berimbolo |
| tiro_guarda | prepara, salta, encaixa as pernas, um instante encaixado, desce para a guarda |
| puxar_guarda | senta devagar puxando (controlado): diferente do tiro |
| desequilibrar_pes (deseq. guarda, borboleta, DLR, aranha) | pés recolhem e empurram; o outro tropeça |
| microanimações: consolidar, postura_meia, pressao_joelho, estabilizar_montada, montada_alta, ajustar_ganchos, controlar_pernas, angulo_guarda, pegar_manga, reter_* (borboleta/DLR/aranha/SLX), travar_laco, postura_* (borboleta/DLR/laço/aranha/SLX) | gesto curto; as de efeito contínuo (controlar as pernas, base, montada alta) seguram a pose enquanto o efeito dura |
| manga presa (persistente) | a mão continua segurando enquanto a conexão de manga existir |

### Corrigidas

- **Levantada depois da queda:** a transição era recusada (a pose do solo continuava "dona") e o
  corpo ficava congelado deitado até aparecer de pé. Agora a `LEVANTADA_TECNICA` desenha do começo
  ao fim (apoio das mãos, pernas, quadril, postura). A levantada do duplo LS não mudou.
- **Corrida de joelhos para trás:** inclina para trás (relativo para onde o lutador olha).
- **Rolamento de joelhos** 22 → 28 ticks e **sprawl** 26 → 32 (mesma distância; janela do sprawl
  um pouco maior, recarga igual). Rolamento deitado e em pé não mudaram.

### Preservadas

Rasteira bem-sucedida, entrada nas pernas, berimbolo, underhook, levantada do duplo LS, recuo em
pé e de joelhos, corrida de joelhos para frente, arrasto de joelhos, rolamento deitado e todas as
anteriores.

### Ainda provisórias

Novas finalizações de perna (pose provisória da categoria PERNA), armlock das costas, hammerlock,
parte das finalizações de perna antigas, escapes/enquadrar e consolidar (agora com gesto curto,
mas sem pose final própria), quem está por cima nas guardas abertas. Rig 2.0 continua necessário
para membros na frente/atrás do adversário (triângulo, laço, single leg com a perna entre os braços).

## Entrega 5Q.7

### Novas

| Animação | Observação |
|---|---|
| derrubada_forcada_defendida:RECEBE | reação curta de quem segurou a derrubada (estilo do empurrão) |
| segurar_pernas (= entrada do prender), segurando_pernas:ATACA/RECEBE | pegada sustentada nas pernas; passos curtos procedurais de quem arrasta (`AnimadorSolo.passosDoArrasto`, pelo x real) |
| enquadrar, enquadrar_joelho, enquadrar_montada | antebraços entre os dois, empurra e volta (100 kg, joelho na barriga, montada) |
| defender_maos | mãos na gola, queixo baixo (costas) |
| escape_quadril (rec. guarda), fugir_do_joelho, recuperar_meia | fuga de quadril, joelhos entram |
| virar_joelhos | tronco e quadril giram |
| escape_montada | ponte curta e quadril escapa |
| girar_nas_costas, escapar_neutro | ombros e quadril giram |
| esconder_pe (ashi), tirar_ganchos (borboleta) | gestos curtos (auditoria do ENQUADRAR) |

As de janela mostram o **esforço**; a troca de posição continua sendo do motor: falhou, o corpo
volta suave à pose da posição. Todas preservam o tamanho dos braços (`IntegridadeMembros`).

### Corrigidas

- Rasteira e derrubada forçada que falham: braços sem esticar em nenhum instante (antes até ~1,6x).

### Ajustadas

- Rolamento de joelhos 28 → 46 ticks (mesma distância).

### Preservadas

Tiro de guarda, empurrão, levantada técnica, quedas explosivas em pé e de joelhos, quedas que
entram, berimbolos, guardas aranha e laço, manga persistente, microanimações da 5Q.6, sprawl,
rastejo, rolamento deitado e em pé.

### Ainda provisórias / limitações do rig

- As finalizações de perna continuam parecidas entre si (pose provisória da categoria PERNA):
  animações individuais ficam para uma etapa posterior.
- As microanimações defensivas são deslocamentos sobre a pose da posição: sem perna por dentro/por
  trás do adversário nem troca de lado do corpo (Rig 2.0).
- O segurado não estica as pernas na direção de quem segura (as pernas deitadas não giram
  livremente no rig atual).
