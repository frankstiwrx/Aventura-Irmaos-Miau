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
