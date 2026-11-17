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

