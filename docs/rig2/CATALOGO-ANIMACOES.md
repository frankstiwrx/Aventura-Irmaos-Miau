# Catálogo mestre de animações (Rig 2.0)

Arquivo **gerado** por `docs/rig2/inventario/gerar_catalogo.py`: não editar à mão. O estado no Rig 2.0 vem de `docs/rig2/inventario/estado-rig2.tsv`, e a lista completa, com todas as colunas, está em `docs/rig2/inventario/catalogo-completo.tsv`.

**Fontes (evidência):**
- `InventarioJogo.java` lê o jogo compilado sem modificá-lo: catálogos de técnicas, finalizações e transições, as chaves de quadros-chave de `AnimacoesTecnicas` (e as articulações que cada uma move de fato), transições de postura, acrobacias e posições.
- Os JSON de `src/main/resources/conteudo`.
- Uma lista de movimentos individuais, com a classe ou o arquivo de origem de cada um.

Um tick do jogo = 16 ms (`GamePanel.INTERVALO_GAME_LOOP_MS`).

**Situação no jogo atual:**
- 1 = implementado e funcional;
- 2 = implementado parcialmente;
- 3 = presente só em dados ou documentação;
- 4 = planejado.

**Estado no Rig 2.0:** Não iniciada · Em análise · Em implementação · Implementada experimentalmente · Testada automaticamente · Aguardando homologação visual · Homologada · Integrada ao jogo principal.

## Resumo (calculado do inventário)

| Medida | Quantidade |
|---|---|
| Movimentos inventariados | **267** |
| Com animação própria no jogo atual (quadros-chave, JSON ou procedural dedicado) | 127 |
| Já reproduzidos no Rig 2.0 (algum estado além de "Não iniciada") | 12 |
| Pendentes no Rig 2.0 | 255 |
| Exigem dois personagens | 233 |
| Exigem movimentação no chão | 241 |
| Dependem de orientação completa da raiz (deitar, cair, rolar), ainda inexistente no Rig 2.0 | 241 |
| Dependem de camadas por membro (pernas enroscadas no adversário), ainda inexistentes | 69 |

| Categoria | Quantidade | No Rig 2.0 |
|---|---|---|
| Ajuste de controle / postura | 43 | 0 |
| Finalização: estrangulamento | 34 | 0 |
| Finalização: braco | 26 | 0 |
| Troca / entrada de guarda | 19 | 0 |
| Queda / entrada | 18 | 1 |
| Controle de perna (ashi) | 12 | 0 |
| Raspagem / reversão | 11 | 0 |
| Finalização: perna | 10 | 0 |
| Passagem de guarda | 9 | 0 |
| Escape / reposição | 9 | 0 |
| Reação / estado (quadros-chave) | 8 | 1 |
| Transição de postura | 8 | 0 |
| Levantada / saída de posição | 7 | 0 |
| Transição de controle | 7 | 0 |
| Acrobacia / rolamento | 7 | 0 |
| Puxada de guarda | 6 | 0 |
| Transição posicional (motor legado) | 6 | 0 |
| Movimento básico | 6 | 6 |
| Movimento no chão | 6 | 0 |
| Defesa em pé | 4 | 0 |
| Pegada | 4 | 4 |
| Finalização (pose genérica) | 3 | 0 |
| Expressão | 2 | 0 |
| Chão (outra) | 1 | 0 |
| Finalização (reação) | 1 | 0 |

| Estado no Rig 2.0 | Quantidade |
|---|---|
| Não iniciada | 254 |
| Aguardando homologação visual | 8 |
| Homologada | 3 |
| Em análise | 1 |
| Em implementação | 1 |

## Movimentos já reproduzidos no Rig 2.0

| ID no jogo | Nome | Estado | Animação no Rig 2.0 | Evidência |
|---|---|---|---|---|
| `derrubada_forcada` | Derrubada forçada [FORÇAR QUEDA] | Aguardando homologação visual | derrubada_forcada (só o desfecho DEFENDIDO) | 6.3: testes de DuelosRig2; o desfecho de sucesso (queda) depende de orientação da raiz |
| `derrubada_forcada_defendida` | derrubada forcada defendida | Aguardando homologação visual | derrubada_forcada (reação de B) | 6.3: a reação do defensor faz parte da técnica sincronizada |
| `andar` | Andar | Homologada | caminhada | aprovada manualmente na 6.2 (laboratorio); testes de AnimacoesRig2 |
| `correr` | Correr | Aguardando homologação visual | corrida | 6.3: testes de MovimentosRig2 (suspensao, apoio sem escorregar, loop); capturas laboratorio-6.3/quadros/corrida-* |
| `agachar` | Agachar | Aguardando homologação visual | agachar + agachado + levantar-agachamento | 6.3: testes de MovimentosRig2 (profundidade, calcanhares, equilibrio); capturas agachar/agachado/levantar-* |
| `pular` | Pular | Aguardando homologação visual | pulo | 6.3: testes de MovimentosRig2 (voo, absorcao, fim no idle); capturas pulo-* |
| `idle_combate` | Idle de combate | Homologada | idle-combate | aprovada manualmente na 6.2 (laboratorio); testes de AnimacoesRig2 |
| `respiracao` | Respiração / exaustão | Homologada | idle-neutro | aprovada manualmente na 6.2 (respiracao visual; o folego do jogo nao foi alterado) |
| `pegada_1` | Pegada 1 — Uma lapela + um braco. | Aguardando homologação visual | pegada_1 + pegada_1-mantida | 6.3: testes de DuelosRig2 (contato exato, sem atravessar, cabecas separadas); capturas pegada_1-* |
| `pegada_2` | Pegada 2 — Cruzada: maos invertidas (frente na manga, tras cruza para a gola). (Entrega 5L) | Aguardando homologação visual | pegada_2 + pegada_2-mantida | 6.3: testes de DuelosRig2; capturas pegada_2-* |
| `pegada_3` | Pegada 3 — Duas mangas: as duas maos nas mangas. (Entrega 5L) | Aguardando homologação visual | pegada_3 + pegada_3-mantida | 6.3: testes de DuelosRig2; capturas pegada_3-* |
| `pegada_4` | Pegada 4 — Gola e nuca: mao da frente na nuca, mao de tras na gola. (Entrega 5L) | Em implementação | (sem animação executável) | 6.3: o braço atravessa a cabeça (raio 35) a caminho da nuca na distância do jogo; ver README seção 13 |

## Catálogo por categoria

**Como ler as colunas:**
- **Duração:** janela de preparação do motor ou duração da transição.
- **Articulações:** as que os quadros-chave do jogo movem de fato.
- **2P:** exige dois personagens.
- **Rig 2.0:** estado atual.
- **Requisitos Rig 2.0:** recursos que o Rig precisa ter para reproduzir o movimento.

### Ajuste de controle / postura (43)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `retencao_pes` | Manter distância (pés) [PÉS] | EM_PE_CONTRA_SOLO → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `postura` | Postura [POSTURA] | GUARDA → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., braço esq., cabeça, deslocamento, joelho dir., mão dir., mão esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `quebrar_postura` | Quebrar a postura [QUEBRAR] | GUARDA → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., braço esq., joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq. | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `soltar_guarda` | Soltar a própria guarda [SOLTAR] | GUARDA → GUARDA_ABERTA (por cima: MANTEM) | instantânea | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `controlar_pernas` | Controlar as pernas [CONTROLAR] | GUARDA_ABERTA → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., braço esq., joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `retencao` | Manter a guarda [MANTER] | GUARDA_ABERTA → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `postura_meia` | Pressão de ombro [PRESSÃO] | MEIA_GUARDA → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., cabeça, deslocamento, mão dir., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `underhook_meia` | Underhook (tirar a pressão do ombro) [UNDERHOOK] | MEIA_GUARDA → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., braço esq., cintura, joelho dir., mão dir., mão esq. | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `consolidar` | Consolidar a pressão [PRESSÃO] | CONTROLE_LATERAL → ? (por cima: ?) | instantânea | própria (quadros-chave) | cabeça, cintura, deslocamento, joelho esq., mão dir., mão esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `enquadrar` | Enquadrar (criar espaço) [ENQUADRAR] | CONTROLE_LATERAL → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, mão dir., mão esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `pressao_joelho` | Pressão do joelho [PRESSÃO] | JOELHO_BARRIGA → ? (por cima: ?) | instantânea | própria (quadros-chave) | cintura, deslocamento, joelho dir., mão dir., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `voltar_cem_quilos` | Voltar aos cem quilos [100 KG] | JOELHO_BARRIGA → CONTROLE_LATERAL (por cima: MANTEM) | instantânea | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `enquadrar_joelho` | Enquadrar o joelho [ENQUADRAR] | JOELHO_BARRIGA → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, mão dir., mão esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `estabilizar_montada` | Estabilizar a montada [BASE] | MONTADA → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., braço esq., deslocamento, joelho dir., joelho esq., mão dir., mão esq. | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `montada_alta` | Montada alta [ALTA] | MONTADA → ? (por cima: ?) | instantânea | própria (quadros-chave) | cabeça, cintura, deslocamento, joelho dir., joelho esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `enquadrar_montada` | Enquadrar [ENQUADRAR] | MONTADA → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, mão dir., mão esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `ajustar_ganchos` | Ajustar os ganchos [GANCHOS] | COSTAS → ? (por cima: ?) | instantânea | própria (quadros-chave) | joelho dir., joelho esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `defender_maos` | Defender com as mãos [DEFENDER] | COSTAS → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., braço esq., cabeça, mão dir., mão esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `triangulo_corpo` | Triângulo de corpo (travar o quadril) [TRIÂNGULO] | COSTAS → ? (por cima: ?) | instantânea | própria (quadros-chave) | cabeça, joelho dir., joelho esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `prender_pernas` | Prender as pernas [PRENDER] | EM_PE_CONTRA_SOLO → ? (por cima: ?) | janela de 12 ticks (~192 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `segurar_pernas` | Segurar as pernas (arrastar) [SEGURAR] | EM_PE_CONTRA_SOLO → ? (por cima: ?) | janela de 10 ticks (~160 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `soltar_pernas` | Soltar as pernas [SOLTAR] | - → ? (por cima: ?) | instantânea | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `recuar` | Recuar (dar distância) [RECUAR] | EM_PE_CONTRA_SOLO → ? (por cima: ?) | instantânea | própria (quadros-chave) | joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `afastar_do_chao` | Afastar (criar distância) [AFASTAR] | EM_PE_CONTRA_SOLO → ? (por cima: ?) | instantânea | própria (quadros-chave) | cintura, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `desequilibrar_guarda` | Puxar e desequilibrar [DESEQ.] | GUARDA → ? (por cima: ?) | janela de 8 ticks (~128 ms) | própria (quadros-chave) | cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `angulo_guarda` | Ajustar o ângulo do quadril [ÂNGULO] | GUARDA → ? (por cima: ?) | instantânea | própria (quadros-chave) | cintura, joelho dir., joelho esq., rotação global, tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `pegar_manga` | Pegar a manga [MANGA] | DE_LA_RIVA, GUARDA_ABERTA, GUARDA_BORBOLETA → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., mão dir. | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `deseq_borboleta` | Desequilibrar para o lado [DESEQ.] | GUARDA_BORBOLETA → ? (por cima: ?) | janela de 8 ticks (~128 ms) | própria (quadros-chave) | cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `reter_borboleta` | Manter os ganchos [MANTER] | GUARDA_BORBOLETA → ? (por cima: ?) | instantânea | própria (quadros-chave) | joelho dir., joelho esq. | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `postura_borboleta` | Afundar o quadril (base) [BASE] | GUARDA_BORBOLETA → ? (por cima: ?) | instantânea | própria (quadros-chave) | cintura, deslocamento, joelho esq., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `tirar_ganchos` | Tirar os ganchos [GANCHOS] | GUARDA_BORBOLETA → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., braço esq., joelho dir., joelho esq., mão dir., mão esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `reter_dlr` | Manter o gancho [MANTER] | DE_LA_RIVA → ? (por cima: ?) | instantânea | própria (quadros-chave) | joelho dir., joelho esq. | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `deseq_dlr` | Desequilibrar a perna [DESEQ.] | DE_LA_RIVA → ? (por cima: ?) | janela de 8 ticks (~128 ms) | própria (quadros-chave) | cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `postura_dlr` | Base (quadril para trás) [BASE] | DE_LA_RIVA → ? (por cima: ?) | instantânea | própria (quadros-chave) | cintura, deslocamento, joelho esq., pé esq., tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `travar_laco` | Travar o laço [TRAVAR] | GUARDA_LACO → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., joelho dir., mão dir. | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `postura_laco` | Base [BASE] | GUARDA_LACO → ? (por cima: ?) | instantânea | própria (quadros-chave) | cintura, deslocamento, joelho esq., pé esq., tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `reter_aranha` | Esticar e recolher (manter) [MANTER] | GUARDA_ARANHA → ? (por cima: ?) | instantânea | própria (quadros-chave) | joelho dir., joelho esq. | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `deseq_aranha` | Desequilibrar com os pés [DESEQ.] | GUARDA_ARANHA → ? (por cima: ?) | janela de 8 ticks (~128 ms) | própria (quadros-chave) | cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `postura_aranha` | Base (cotovelos para dentro) [BASE] | GUARDA_ARANHA → ? (por cima: ?) | instantânea | própria (quadros-chave) | cintura, deslocamento, joelho esq., pé esq., tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `reter_slx` | Manter o X [MANTER] | SINGLE_LEG_X → ? (por cima: ?) | instantânea | própria (quadros-chave) | joelho dir., joelho esq. | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `postura_slx` | Base (peso na perna presa) [BASE] | SINGLE_LEG_X → ? (por cima: ?) | instantânea | própria (quadros-chave) | cintura, deslocamento, joelho esq., pé esq., tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `consolidar_ashi` | Ajustar o controle da perna [AJUSTAR] | ASHI_GARAMI → ? (por cima: ?) | instantânea | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `esconder_pe` | Esconder o pé [ESCONDER] | ASHI_GARAMI → ? (por cima: ?) | instantânea | própria (quadros-chave) | cintura, joelho dir., mão dir., pé dir. | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |

### Finalização: estrangulamento (34)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `cruzado@guarda` | Estrangulamento cruzado [CRUZADO] | GUARDA → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `triangulo@guarda` | Triângulo [TRIÂNGULO] | GUARDA → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR]; falha → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR] | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `guilhotina@guarda` | Guilhotina [GUILHOT.] | GUARDA → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `loop_choke@guarda` | Estrangulamento em laço (loop choke) [LOOP] | GUARDA → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 14 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `gogoplata@guarda` | Gogoplata (canela no pescoço) [GOGOPLATA] | GUARDA → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR]; falha → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR] | entrada 14 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `ezequiel_guarda@guarda` | Ezequiel da guarda [EZEQUIEL] | GUARDA → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `cortando_pao@controle_lateral` | Estrangulamento cortando o pão [PÃO] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `katagatame@controle_lateral` | Katagatame [KATA-GAT.] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `ezequiel@controle_lateral` | Ezequiel [EZEQUIEL] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 14 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `baseball_bat@controle_lateral` | Estrangulamento baseball bat [BASEBALL] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 14 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `norte_sul@controle_lateral` | Estrangulamento norte-sul [NORTE-SUL] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 14 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `darce@controle_lateral` | D'arce [D'ARCE] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO] | entrada 14 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `anaconda@controle_lateral` | Anaconda [ANACONDA] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO] | entrada 14 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `gravata_peruana@controle_lateral` | Gravata peruana (peruvian necktie) [PERUANA] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 14 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `cortador_papel@controle_lateral` | Estrangulamento cortador de papel (paper cutter) [PAPEL] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `lapela@controle_lateral` | Estrangulamento de lapela dos 100 kg [LAPELA] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 14 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `von_flue@controle_lateral` | Estrangulamento Von Flue (ombro no pescoço) [VON FLUE] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 14 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `cruzado@montada` | Estrangulamento cruzado [CRUZADO] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `ezequiel@montada` | Ezequiel [EZEQUIEL] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `katagatame@montada` | Katagatame [KATA-GAT.] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `triangulo@montada` | Triângulo da montada [TRIÂNGULO] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR] | entrada 14 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `gola_unica@montada` | Estrangulamento de gola única [GOLA ÚNICA] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `lapela@montada` | Estrangulamento de lapela da montada [LAPELA] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `antebraco@montada` | Estrangulamento com o antebraço [ANTEBRAÇO] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `guilhotina@montada` | Guilhotina da montada (cotovelo alto) [GUILHOT.] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR] | entrada 14 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `mata_leao@costas` | Mata-leão [MATA-LEÃO] | COSTAS → rendição (bater); escape → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 10 ticks; pressão +0.4/tick | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, mão dir., mão esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `arco_flecha@costas` | Arco e flecha [ARCO] | COSTAS → rendição (bater); escape → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `gola_tras@costas` | Estrangulamento de gola por trás [GOLA] | COSTAS → rendição (bater); escape → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `cruzado_tras@costas` | Estrangulamento cruzado pelas costas [CRUZADO] | COSTAS → rendição (bater); escape → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 14 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `short_choke@costas` | Short choke (estrangulamento curto) [SHORT] | COSTAS → rendição (bater); escape → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `ezequiel@costas` | Ezequiel pelas costas [EZEQUIEL] | COSTAS → rendição (bater); escape → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 14 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `triangulo@costas` | Triângulo pelas costas [TRIÂNGULO] | COSTAS → rendição (bater); escape → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR]; falha → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR] | entrada 14 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `lapela@costas` | Estrangulamento de lapela por trás [LAPELA] | COSTAS → rendição (bater); escape → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `triangulo@guarda_aranha` | Triângulo da aranha [TRIÂNGULO] | GUARDA_ARANHA → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=MANTEM, controlador=MANTEM]; falha → Desfecho[destino=GUARDA_ABERTA, porCima=MANTEM, controlador=MANTEM] | entrada 10 ticks; pressão +0.4/tick | provisória (pose genérica ESTRANGULAMENTO) | pose genérica da categoria ESTRANGULAMENTO | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |

### Finalização: braco (26)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `armlock@guarda` | Armlock [ARMLOCK] | GUARDA → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 10 ticks; pressão +0.35/tick | própria (quadros-chave) | braço dir., cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., rotação global, tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `kimura@guarda` | Kimura [KIMURA] | GUARDA → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `omoplata@guarda` | Omoplata [OMOPLATA] | GUARDA → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR]; falha → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR] | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `chave_punho@guarda` | Chave de punho [PUNHO] | GUARDA → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `baratoplata@guarda` | Baratoplata (ombro pela perna) [BARATOPL.] | GUARDA → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR]; falha → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR] | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `tarikoplata@guarda` | Tarikoplata (ombro com a perna) [TARIKOPL.] | GUARDA → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR]; falha → Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR] | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `americana@controle_lateral` | Americana [AMERICANA] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `kimura@controle_lateral` | Kimura [KIMURA] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `armlock@controle_lateral` | Armlock (girando) [ARMLOCK] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO] | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `braco_reto@controle_lateral` | Chave de braço reto [BRAÇO RETO] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `mir_lock@controle_lateral` | Mir lock (chave de ombro com o punho preso) [MIR LOCK] | CONTROLE_LATERAL → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO]; falha → Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO] | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `armlock@montada` | Armlock da montada [ARMLOCK] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR] | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `americana@montada` | Americana [AMERICANA] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 10 ticks; pressão +0.35/tick | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, mão dir., mão esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `kimura@montada` | Kimura da montada [KIMURA] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `chave_punho@montada` | Chave de punho [PUNHO] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `monoplata@montada` | Monoplata [MONOPLATA] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR] | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `armlock_s@montada` | Armlock da montada em S (barriga para baixo) [ARMLOCK S] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR] | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `mao_de_vaca@montada` | Mão de vaca (chave de punho) [MÃO DE VACA] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `biceps@montada` | Chave de bíceps (esmagamento) [BÍCEPS] | MONTADA → rendição (bater); escape → Desfecho[destino=MEIA_GUARDA, porCima=INICIADOR, controlador=OUTRO]; falha → null | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `armlock@costas` | Armlock das costas [ARMLOCK] | COSTAS → rendição (bater); escape → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR]; falha → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR] | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `kimura@costas` | Kimura das costas [KIMURA] | COSTAS → rendição (bater); escape → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `braco_reto@costas` | Chave de braço reto [BRAÇO RETO] | COSTAS → rendição (bater); escape → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `hammerlock@costas` | Hammerlock [HAMMER] | COSTAS → rendição (bater); escape → Desfecho[destino=GUARDA, porCima=OUTRO, controlador=INICIADOR]; falha → null | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `armlock@guarda_aranha` | Armlock da aranha [ARMLOCK] | GUARDA_ARANHA → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=MANTEM, controlador=MANTEM]; falha → Desfecho[destino=GUARDA_ABERTA, porCima=MANTEM, controlador=MANTEM] | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `omoplata@guarda_aranha` | Omoplata da aranha [OMOPLATA] | GUARDA_ARANHA → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=MANTEM, controlador=MANTEM]; falha → Desfecho[destino=GUARDA_ABERTA, porCima=MANTEM, controlador=MANTEM] | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `omoplata@guarda_laco` | Omoplata do laço [OMOPLATA] | GUARDA_LACO → rendição (bater); escape → Desfecho[destino=GUARDA_ABERTA, porCima=MANTEM, controlador=MANTEM]; falha → Desfecho[destino=GUARDA_ABERTA, porCima=MANTEM, controlador=MANTEM] | entrada 10 ticks; pressão +0.35/tick | provisória (pose genérica BRACO) | pose genérica da categoria BRACO | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |

### Troca / entrada de guarda (19)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `entrar_guarda` | Entrar na guarda [ENTRAR] | EM_PE_CONTRA_SOLO → GUARDA_ABERTA (por cima: INICIADOR) | janela de 12 ticks (~192 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `conectar_guarda` | Puxar guarda (do chão) [PUXAR GUARDA] | EM_PE_CONTRA_SOLO → GUARDA_ABERTA (por cima: OUTRO) | janela de 8 ticks (~128 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `puxar_para_guarda` | Puxar guarda [PUXAR GUARDA] | SOLO_DISPUTADO → GUARDA (por cima: OUTRO) | janela de 8 ticks (~128 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `abrir_guarda` | Abrir a guarda do outro [ABRIR] | GUARDA → GUARDA_ABERTA (por cima: MANTEM) | janela de 16 ticks (~256 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `joelho_cortando` | Joelho cortando [MEIA] | GUARDA_ABERTA → MEIA_GUARDA (por cima: INICIADOR) | janela de 14 ticks (~224 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `fechar_guarda` | Fechar a guarda [FECHAR] | GUARDA_ABERTA → GUARDA (por cima: OUTRO) | janela de 10 ticks (~160 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `recuperar_guarda` | Recuperar a guarda [REC. GUARDA] | MEIA_GUARDA → GUARDA_ABERTA (por cima: OUTRO) | janela de 12 ticks (~192 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `conectar_borboleta` | Sentar na borboleta [BORBOLETA] | EM_PE_CONTRA_SOLO → GUARDA_BORBOLETA (por cima: OUTRO) | janela de 8 ticks (~128 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `conectar_dlr` | Gancho De La Riva [DE LA RIVA] | EM_PE_CONTRA_SOLO → DE_LA_RIVA (por cima: OUTRO) | janela de 10 ticks (~160 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `ir_borboleta` | Para a guarda borboleta [BORBOLETA] | DE_LA_RIVA, GUARDA_ABERTA → GUARDA_BORBOLETA (por cima: MANTEM) | janela de 6 ticks (~96 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `ir_dlr` | Para a De La Riva [DE LA RIVA] | GUARDA_ABERTA, GUARDA_BORBOLETA, GUARDA_LACO → DE_LA_RIVA (por cima: MANTEM) | janela de 6 ticks (~96 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `ir_laco` | Para a guarda laço [LAÇO] | DE_LA_RIVA, GUARDA_ABERTA, GUARDA_ARANHA, GUARDA_BORBOLETA → GUARDA_LACO (por cima: MANTEM) | janela de 6 ticks (~96 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `ir_aberta` | Voltar à guarda aberta [ABERTA] | DE_LA_RIVA, GUARDA_ARANHA, GUARDA_BORBOLETA, GUARDA_LACO, SINGLE_LEG_X → GUARDA_ABERTA (por cima: MANTEM) | janela de 6 ticks (~96 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `livrar_gancho` | Livrar a perna do gancho [LIVRAR] | DE_LA_RIVA → GUARDA_ABERTA (por cima: MANTEM) | janela de 10 ticks (~160 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `soltar_manga` | Soltar a manga [SOLTAR MANGA] | GUARDA_LACO → GUARDA_ABERTA (por cima: MANTEM) | janela de 10 ticks (~160 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `ir_aranha` | Para a guarda aranha [ARANHA] | GUARDA_ABERTA, GUARDA_LACO → GUARDA_ARANHA (por cima: MANTEM) | janela de 6 ticks (~96 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `livrar_mangas` | Livrar as mangas [LIVRAR] | GUARDA_ARANHA → GUARDA_ABERTA (por cima: MANTEM) | janela de 10 ticks (~160 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `ir_slx` | Para a single-leg X [SLX] | DE_LA_RIVA, GUARDA_BORBOLETA → SINGLE_LEG_X (por cima: MANTEM) | janela de 8 ticks (~128 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `livrar_slx` | Tirar a perna do X [LIVRAR] | SINGLE_LEG_X → GUARDA_ABERTA (por cima: MANTEM) | janela de 10 ticks (~160 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |

### Queda / entrada (18)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `empurrao` | Empurrão / pressão [EMPURRÃO] | EM_PE_PEGADA, EM_PE_SEPARADOS → EM_PE_CONTRA_SOLO (por cima: INICIADOR) | janela de 10 ticks (~160 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `queda_pegada` | Queda com a pegada [QUEDA] | EM_PE_PEGADA → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 24 ticks (~384 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Em análise | dois personagens, orientação da raiz (deitar/cair) |
| `entrada_pernas` | Entrada nas pernas [PERNAS] | EM_PE_PEGADA, EM_PE_SEPARADOS → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 14 ticks (~224 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `derrubada_forcada` | Derrubada forçada [FORÇAR QUEDA] | EM_PE_PEGADA → EM_PE_CONTRA_SOLO (por cima: INICIADOR) | janela de 16 ticks (~256 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Aguardando homologação visual | dois personagens, orientação da raiz (deitar/cair) |
| `rasteira` | Rasteira [RASTEIRA] | EM_PE_PEGADA → EM_PE_CONTRA_SOLO (por cima: INICIADOR) | janela de 10 ticks (~160 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, deslocamento, joelho dir., mão dir., mão esq., pé dir., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `projecao_quadril` | Projeção de quadril [QUADRIL] | EM_PE_PEGADA → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 22 ticks (~352 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `queda_manga` | Queda pela manga [MANGA] | EM_PE_PEGADA → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 18 ticks (~288 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `single_leg` | Single leg (uma perna) [SINGLE LEG] | EM_PE_SEPARADOS → MEIA_GUARDA (por cima: INICIADOR) | janela de 14 ticks (~224 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `high_crotch` | High crotch (por fora, no quadril) [HIGH CROTCH] | EM_PE_SEPARADOS → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 14 ticks (~224 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `ankle_pick` | Ankle pick (puxa a cabeça, pega o tornozelo) [TORNOZELO] | EM_PE_SEPARADOS → EM_PE_CONTRA_SOLO (por cima: INICIADOR) | janela de 10 ticks (~160 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `carregada_ombro` | Carregada no ombro [OMBRO] | EM_PE_SEPARADOS → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 18 ticks (~288 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `body_lock` | Agarrar a cintura (body lock) [CINTURA] | EM_PE_SEPARADOS → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 14 ticks (~224 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `single_leg_joelhos` | Single leg de joelhos [SINGLE LEG] | EM_PE_SEPARADOS → MEIA_GUARDA (por cima: INICIADOR) | janela de 12 ticks (~192 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, deslocamento, joelho dir., mão dir., mão esq., pé dir., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `double_leg_joelhos` | Double leg de joelhos (tombar de lado) [DOUBLE LEG] | EM_PE_SEPARADOS → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 12 ticks (~192 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, mão dir., mão esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `arm_drag` | Arrastar o braço (arm drag) [ARM DRAG] | EM_PE_SEPARADOS → COSTAS (por cima: INICIADOR) | janela de 10 ticks (~160 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, mão dir., mão esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `snap_down` | Puxar a cabeça (snap down) [SNAP DOWN] | EM_PE_SEPARADOS → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 10 ticks (~160 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, deslocamento, mão dir., mão esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `uchi_mata` | Uchi-mata (ceifa por dentro da coxa) [UCHI-MATA] | EM_PE_PEGADA → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 20 ticks (~320 ms) | própria (quadros-chave) | braço dir., cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `osoto_gari` | Osoto-gari (grande ceifada por fora) [OSOTO] | EM_PE_PEGADA → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 16 ticks (~256 ms) | própria (quadros-chave) | cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |

### Controle de perna (ashi) (12)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `entrada_perna_guarda` | Abrir a guarda e entrar na perna [→ PERNA] | GUARDA → ASHI_GARAMI (por cima: OUTRO); falha: Desfecho[destino=GUARDA_ABERTA, porCima=OUTRO, controlador=INICIADOR] | janela de 14 ticks (~224 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., rotação global, tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `entrada_perna_dominante` | Abandonar o controle e disputar a perna [→ PERNA] | CONTROLE_LATERAL, COSTAS, JOELHO_BARRIGA, MONTADA → ASHI_GARAMI (por cima: OUTRO); falha: Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO] | janela de 16 ticks (~256 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `costas_perna_pe_reta` | Largar as costas e atacar o pé (chave de pé reta) [→ PÉ RETA] | COSTAS → ASHI_GARAMI (por cima: OUTRO); falha: Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO] | janela de 16 ticks (~256 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `costas_perna_joelho` | Largar as costas e atacar o joelho (chave de joelho) [→ JOELHO] | COSTAS → ASHI_GARAMI (por cima: OUTRO); falha: Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO] | janela de 16 ticks (~256 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `costas_perna_toe_hold` | Largar as costas e atacar o pé (toe hold) [→ TOE HOLD] | COSTAS → ASHI_GARAMI (por cima: OUTRO); falha: Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO] | janela de 16 ticks (~256 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `costas_perna_panturrilha` | Largar as costas e atacar a panturrilha [→ PANTURR.] | COSTAS → ASHI_GARAMI (por cima: OUTRO); falha: Desfecho[destino=GUARDA_ABERTA, porCima=INICIADOR, controlador=OUTRO] | janela de 16 ticks (~256 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `entrada_perna_aberta` | Controlar a perna (ashi garami) [ASHI] | GUARDA_ABERTA, SINGLE_LEG_X → ASHI_GARAMI (por cima: OUTRO) | janela de 10 ticks (~160 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., rotação global, tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `entrada_perna_aberta_dlr` | Da De La Riva para a perna [ASHI] | DE_LA_RIVA → ASHI_GARAMI (por cima: OUTRO) | janela de 10 ticks (~160 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., rotação global, tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `ashi_para_slx` | Subir para o single-leg X [SLX] | ASHI_GARAMI → SINGLE_LEG_X (por cima: OUTRO) | janela de 12 ticks (~192 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `ashi_para_guarda` | Recompor a guarda aberta [G. ABERTA] | ASHI_GARAMI → GUARDA_ABERTA (por cima: OUTRO) | janela de 10 ticks (~160 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `soltar_perna` | Soltar a perna [SOLTAR] | ASHI_GARAMI → SOLO_DISPUTADO (por cima: NENHUM) | janela de 6 ticks (~96 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `girar_livrar_perna` | Girar e livrar a perna [GIRAR] | ASHI_GARAMI → SOLO_DISPUTADO (por cima: NENHUM) | janela de 14 ticks (~224 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |

### Raspagem / reversão (11)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `raspagem` | Raspagem [RASPAR] | GUARDA, GUARDA_ABERTA → GUARDA_ABERTA (por cima: INICIADOR) | janela de 14 ticks (~224 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `raspagem_meia` | Raspagem da meia-guarda [RASPAR] | MEIA_GUARDA → GUARDA_ABERTA (por cima: INICIADOR) | janela de 14 ticks (~224 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `raspagem_borboleta` | Raspagem de borboleta (elevação) [ELEVAR] | GUARDA_BORBOLETA → GUARDA_ABERTA (por cima: INICIADOR) | janela de 14 ticks (~224 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `raspagem_dlr` | Raspagem De La Riva [RASPAR] | DE_LA_RIVA → MEIA_GUARDA (por cima: INICIADOR) | janela de 12 ticks (~192 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `berimbolo` | Berimbolo [BERIMBOLO] | DE_LA_RIVA → COSTAS (por cima: INICIADOR) | janela de 20 ticks (~320 ms) | própria (quadros-chave) | cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., rotação global, tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `berimbolo_kiss` | Kiss of the dragon (inversão por baixo) [KISS DRAGON] | DE_LA_RIVA → COSTAS (por cima: INICIADOR) | janela de 24 ticks (~384 ms) | própria (quadros-chave) | cabeça, cintura, deslocamento, joelho dir., joelho esq., pé dir., pé esq., rotação global, tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `berimbolo_invertido` | Baby bolo (berimbolo para cima) [BABY BOLO] | DE_LA_RIVA → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 16 ticks (~256 ms) | própria (quadros-chave) | cabeça, cintura, deslocamento, joelho dir., mão dir., mão esq., pé dir., rotação global, tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `raspagem_laco` | Raspagem do laço [RASPAR] | GUARDA_LACO → MEIA_GUARDA (por cima: INICIADOR) | janela de 14 ticks (~224 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `raspagem_aranha` | Raspagem da aranha [RASPAR] | GUARDA_ARANHA → GUARDA_ABERTA (por cima: INICIADOR) | janela de 14 ticks (~224 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `raspagem_slx` | Raspagem da single-leg X [RASPAR] | SINGLE_LEG_X → EM_PE_CONTRA_SOLO (por cima: INICIADOR) | janela de 12 ticks (~192 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `raspagem_ashi` | Raspar levantando (do ashi) [RASPAR] | ASHI_GARAMI → EM_PE_CONTRA_SOLO (por cima: INICIADOR) | janela de 14 ticks (~224 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |

### Finalização: perna (10)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `chave_pe_reta@ashi_garami` | Chave de pé reta [PÉ RETA] | ASHI_GARAMI → rendição (bater); escape → Desfecho[destino=SOLO_DISPUTADO, porCima=NENHUM, controlador=NENHUM]; falha → null | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica PERNA) | pose genérica da categoria PERNA | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `chave_joelho@ashi_garami` | Chave de joelho [JOELHO] | ASHI_GARAMI → rendição (bater); escape → Desfecho[destino=SOLO_DISPUTADO, porCima=NENHUM, controlador=NENHUM]; falha → null | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica PERNA) | pose genérica da categoria PERNA | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `toe_hold@ashi_garami` | Toe hold (mata-leão no pé) [TOE HOLD] | ASHI_GARAMI → rendição (bater); escape → Desfecho[destino=SOLO_DISPUTADO, porCima=NENHUM, controlador=NENHUM]; falha → null | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica PERNA) | pose genérica da categoria PERNA | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `panturrilha@ashi_garami` | Compressão de panturrilha [PANTURR.] | ASHI_GARAMI → rendição (bater); escape → Desfecho[destino=SOLO_DISPUTADO, porCima=NENHUM, controlador=NENHUM]; falha → null | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica PERNA) | pose genérica da categoria PERNA | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `aoki_lock@ashi_garami` | Aoki lock (chave de tornozelo girando) [AOKI] | ASHI_GARAMI → rendição (bater); escape → Desfecho[destino=SOLO_DISPUTADO, porCima=NENHUM, controlador=NENHUM]; falha → null | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica PERNA) | pose genérica da categoria PERNA | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `estima_lock@ashi_garami` | Estima lock (chave de pé) [ESTIMA] | ASHI_GARAMI → rendição (bater); escape → Desfecho[destino=SOLO_DISPUTADO, porCima=NENHUM, controlador=NENHUM]; falha → null | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica PERNA) | pose genérica da categoria PERNA | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `pe_reta_5050@ashi_garami` | Chave de pé reta da 50/50 (variação) [PÉ 50/50] | ASHI_GARAMI → rendição (bater); escape → Desfecho[destino=SOLO_DISPUTADO, porCima=NENHUM, controlador=NENHUM]; falha → Desfecho[destino=SOLO_DISPUTADO, porCima=NENHUM, controlador=NENHUM] | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica PERNA) | pose genérica da categoria PERNA | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `joelho_invertido@ashi_garami` | Chave de joelho invertida [JOELHO INV.] | ASHI_GARAMI → rendição (bater); escape → Desfecho[destino=SOLO_DISPUTADO, porCima=NENHUM, controlador=NENHUM]; falha → null | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica PERNA) | pose genérica da categoria PERNA | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `banana_split@ashi_garami` | Banana split (abrir as pernas) [BANANA] | ASHI_GARAMI → rendição (bater); escape → Desfecho[destino=SOLO_DISPUTADO, porCima=NENHUM, controlador=NENHUM]; falha → null | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica PERNA) | pose genérica da categoria PERNA | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `kneebar_giro@ashi_garami` | Chave de joelho com giro (variação) [JOELHO GIRO] | ASHI_GARAMI → rendição (bater); escape → Desfecho[destino=SOLO_DISPUTADO, porCima=NENHUM, controlador=NENHUM]; falha → Desfecho[destino=CONTROLE_LATERAL, porCima=OUTRO, controlador=OUTRO] | entrada 14 ticks; pressão +0.35/tick | provisória (pose genérica PERNA) | pose genérica da categoria PERNA | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |

### Passagem de guarda (9)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `passagem_em_pe` | Passagem em pé (toreando) [PASSAR] | EM_PE_CONTRA_SOLO → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 16 ticks (~256 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `passar_guarda` | Passar a guarda [PASSAR] | GUARDA_ABERTA → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 20 ticks (~320 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `liberar_perna` | Liberar a perna e passar [PASSAR] | MEIA_GUARDA → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 18 ticks (~288 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `joelho_direto` | Joelho na barriga direto [JOELHO] | EM_PE_CONTRA_SOLO → JOELHO_BARRIGA (por cima: INICIADOR) | janela de 14 ticks (~224 ms) | própria (quadros-chave) | braço dir., braço esq., cintura, deslocamento, joelho dir., mão dir., mão esq., pé dir., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `passar_borboleta` | Amassar e passar [PASSAR] | GUARDA_BORBOLETA → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 18 ticks (~288 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `passar_dlr` | Quebrar o gancho e passar [PASSAR] | DE_LA_RIVA → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 18 ticks (~288 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `passar_laco` | Passar o laço [PASSAR] | GUARDA_LACO → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 20 ticks (~320 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `passar_aranha` | Passar a aranha [PASSAR] | GUARDA_ARANHA → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 20 ticks (~320 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `passar_slx` | Livrar a perna e passar [PASSAR] | SINGLE_LEG_X → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 18 ticks (~288 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |

### Escape / reposição (9)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `escape_quadril` | Escape de quadril (recuperar a guarda) [REC. GUARDA] | CONTROLE_LATERAL → GUARDA_ABERTA (por cima: OUTRO) | janela de 12 ticks (~192 ms) | própria (quadros-chave) | braço dir., braço esq., cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `recuperar_meia` | Joelho no meio (recuperar meia-guarda) [REC. MEIA] | CONTROLE_LATERAL → MEIA_GUARDA (por cima: OUTRO) | janela de 10 ticks (~160 ms) | própria (quadros-chave) | braço dir., cintura, joelho dir., mão dir., pé dir. | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `virar_joelhos` | Virar para os joelhos [VIRAR] | CONTROLE_LATERAL → SOLO_DISPUTADO (por cima: NENHUM) | janela de 14 ticks (~224 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, joelho dir., joelho esq., mão dir., mão esq., rotação global, tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `fugir_do_joelho` | Empurrar o joelho (recuperar a guarda) [REC. GUARDA] | JOELHO_BARRIGA → GUARDA_ABERTA (por cima: OUTRO) | janela de 10 ticks (~160 ms) | própria (quadros-chave) | braço dir., braço esq., cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `inverter_joelho` | Inverter (base alta) [INVERTER] | JOELHO_BARRIGA → SOLO_DISPUTADO (por cima: NENHUM) | janela de 8 ticks (~128 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `ponte` | Ponte (upa) [UPA] | MONTADA → GUARDA (por cima: INICIADOR) | janela de 8 ticks (~128 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `escape_montada` | Escape da montada (meia-guarda) [ESCAPAR] | MONTADA → MEIA_GUARDA (por cima: OUTRO) | janela de 14 ticks (~224 ms) | própria (quadros-chave) | braço dir., cintura, deslocamento, joelho dir., joelho esq., mão dir., pé dir., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `girar_nas_costas` | Tirar os ganchos e girar [GIRAR] | COSTAS → GUARDA (por cima: INICIADOR) | janela de 16 ticks (~256 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, joelho dir., mão dir., mão esq., rotação global, tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `escapar_neutro` | Escapar para o neutro [ESCAPAR] | COSTAS → SOLO_DISPUTADO (por cima: NENHUM) | janela de 12 ticks (~192 ms) | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, joelho dir., mão dir., mão esq., rotação global, tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |

### Reação / estado (quadros-chave) (8)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `derrubada_forcada_defendida` | derrubada forcada defendida | - → - | relativa ao efeito | própria (quadros-chave) | cabeça, cintura, deslocamento, joelho esq., mão dir., mão esq., pé esq., tronco | sim | média | Aguardando homologação visual | dois personagens |
| `desequilibrar_pes` | desequilibrar pes | - → - | relativa ao efeito | própria (quadros-chave) | cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | média | Não iniciada | dois personagens |
| `pernas_presas` | pernas presas | - → - | relativa ao efeito | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | média | Não iniciada | dois personagens |
| `postura_firme` | postura firme | - → - | relativa ao efeito | própria (quadros-chave) | braço dir., braço esq., cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., tronco | sim | média | Não iniciada | dois personagens |
| `postura_quebrada` | postura quebrada | - → - | relativa ao efeito | própria (quadros-chave) | braço dir., braço esq., cabeça, deslocamento, mão dir., mão esq., tronco | sim | média | Não iniciada | dois personagens |
| `postura_recompor` | postura recompor | - → - | relativa ao efeito | própria (quadros-chave) | braço dir., braço esq., cabeça, deslocamento, mão dir., mão esq., tronco | sim | média | Não iniciada | dois personagens |
| `rasteira_defendida` | rasteira defendida | - → - | relativa ao efeito | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | média | Não iniciada | dois personagens |
| `segurando_pernas` | segurando pernas | - → - | relativa ao efeito | própria (quadros-chave) | braço dir., braço esq., cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | média | Não iniciada | dois personagens |

### Transição de postura (8)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `AJOELHAR` | ajoelhar | EM_PE → AJOELHADO | 12 ticks | própria (interpolação de poses) | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `DEITAR` | deitar | AJOELHADO → DEITADO | 14 ticks | própria (interpolação de poses) | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `ERGUER_JOELHOS` | erguer joelhos | DEITADO → AJOELHADO | 14 ticks | própria (interpolação de poses) | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `LEVANTAR` | levantar | AJOELHADO → EM_PE | 12 ticks | própria (interpolação de poses) | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `RAPIDA_DEITAR` | rapida deitar | EM_PE → DEITADO | 10 ticks | própria (interpolação de poses) | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `RAPIDA_LEVANTAR_DO_CHAO` | rapida levantar do chao | DEITADO → EM_PE | 16 ticks | própria (interpolação de poses) | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `RAPIDA_LEVANTAR` | rapida levantar | AJOELHADO → EM_PE | 8 ticks | própria (interpolação de poses) | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `LEVANTADA_TECNICA` | levantada tecnica | DEITADO → EM_PE | 30 ticks | própria (interpolação de poses) | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |

### Levantada / saída de posição (7)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `levantar_tecnico` | Levantar técnico [LEVANTAR] | EM_PE_CONTRA_SOLO → EM_PE_SEPARADOS (por cima: NENHUM) | janela de 14 ticks (~224 ms) | própria (quadros-chave) | braço esq., joelho dir., mão dir., mão esq., pé dir., rotação global | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `levantar_do_solo` | Levantar [LEVANTAR] | SOLO_DISPUTADO → EM_PE_CONTRA_SOLO (por cima: INICIADOR) | janela de 10 ticks (~160 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `levantar_da_guarda` | Levantar da guarda [LEVANTAR] | DE_LA_RIVA, GUARDA_ABERTA, GUARDA_ARANHA, GUARDA_BORBOLETA, GUARDA_LACO, MEIA_GUARDA, SINGLE_LEG_X → EM_PE_SEPARADOS (por cima: NENHUM) | janela de 16 ticks (~256 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `afastar_da_guarda` | Abrir e afastar com os pés [AFASTAR] | DE_LA_RIVA, GUARDA, GUARDA_ABERTA, GUARDA_ARANHA, GUARDA_BORBOLETA, GUARDA_LACO, MEIA_GUARDA, SINGLE_LEG_X → EM_PE_CONTRA_SOLO (por cima: OUTRO) | janela de 12 ticks (~192 ms) | própria (quadros-chave) | braço dir., cabeça, cintura, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `sair_da_guarda` | Criar espaço e levantar [LEVANTAR] | DE_LA_RIVA, GUARDA, GUARDA_ABERTA, GUARDA_ARANHA, GUARDA_BORBOLETA, GUARDA_LACO, MEIA_GUARDA, SINGLE_LEG_X → EM_PE_CONTRA_SOLO (por cima: INICIADOR) | janela de 16 ticks (~256 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `abandonar_posicao` | Soltar a posição [SOLTAR] | CONTROLE_LATERAL, COSTAS, JOELHO_BARRIGA, MONTADA → EM_PE_CONTRA_SOLO (por cima: INICIADOR) | janela de 6 ticks (~96 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `levantar_do_ashi` | Levantar e puxar a perna [LEVANTAR] | ASHI_GARAMI → EM_PE_CONTRA_SOLO (por cima: INICIADOR) | janela de 12 ticks (~192 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |

### Transição de controle (7)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `montar` | Ir para a montada [MONTAR] | CONTROLE_LATERAL → MONTADA (por cima: INICIADOR) | janela de 18 ticks (~288 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `joelho_na_barriga` | Joelho na barriga [JOELHO] | CONTROLE_LATERAL → JOELHO_BARRIGA (por cima: INICIADOR) | janela de 10 ticks (~160 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `pegar_costas_virada` | Pegar as costas na virada [COSTAS] | CONTROLE_LATERAL → COSTAS (por cima: INICIADOR) | janela de 8 ticks (~128 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `montar_do_joelho` | Ir para a montada [MONTAR] | JOELHO_BARRIGA → MONTADA (por cima: INICIADOR) | janela de 12 ticks (~192 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `pegar_costas` | Pegar as costas [COSTAS] | MONTADA → COSTAS (por cima: INICIADOR) | janela de 16 ticks (~256 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `costas_para_montada` | Virar para a montada [MONTADA] | COSTAS → MONTADA (por cima: INICIADOR) | janela de 14 ticks (~224 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `costas_para_cem` | Ir para os cem quilos [100 KG] | COSTAS → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 12 ticks (~192 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |

### Acrobacia / rolamento (7)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `rolamento_frente` | Rolamento para a frente | - → - | - | JSON | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `rolamento_tras` | Rolamento para trás | - → - | - | JSON | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `rolamento_lateral` | Rolamento lateral | - → - | - | JSON | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `ESTRELA_ESQUERDA` | Estrela (esquerda) | - → - | 26 ticks | JSON | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `ESTRELA_DIREITA` | Estrela (direita) | - → - | 26 ticks | JSON | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `MORTAL_FRENTE` | Mortal para a frente | - → - | 28 ticks | JSON | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `MORTAL_TRAS` | Mortal para trás | - → - | 30 ticks | JSON | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |

### Puxada de guarda (6)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `puxar_guarda` | Puxar guarda [PUXAR GUARDA] | EM_PE_PEGADA, EM_PE_SEPARADOS → GUARDA (por cima: OUTRO) | janela de 10 ticks (~160 ms) | própria (quadros-chave) | cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `tiro_guarda` | Tiro de guarda [TIRO GUARDA] | EM_PE_PEGADA → GUARDA (por cima: OUTRO) | janela de 12 ticks (~192 ms) | própria (quadros-chave) | cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `puxar_borboleta` | Puxar para a borboleta [BORBOLETA] | EM_PE_PEGADA, EM_PE_SEPARADOS → GUARDA_BORBOLETA (por cima: OUTRO) | janela de 10 ticks (~160 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `puxar_dlr` | Puxar para a De La Riva [DE LA RIVA] | EM_PE_PEGADA → DE_LA_RIVA (por cima: OUTRO) | janela de 12 ticks (~192 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `puxar_aranha` | Puxar para a guarda aranha [ARANHA] | EM_PE_PEGADA → GUARDA_ARANHA (por cima: OUTRO) | janela de 12 ticks (~192 ms) | própria (quadros-chave) | cabeça, deslocamento, joelho dir., joelho esq., mão dir., mão esq., pé dir., pé esq., tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |
| `puxar_laco` | Puxar para a guarda laço [LAÇO] | EM_PE_PEGADA → GUARDA_LACO (por cima: OUTRO) | janela de 12 ticks (~192 ms) | própria (quadros-chave) | braço dir., cabeça, deslocamento, joelho dir., mão dir., mão esq., pé dir., tronco | sim | muito alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair), camadas por membro (pernas enroscadas) |

### Transição posicional (motor legado) (6)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `transicao/estabelecer_pegada` | Estabelecer pegada | EM_PE_SEPARADOS → EM_PE_PEGADA, EM_PE_SEPARADOS | 30 ticks (~480 ms) | sem animação própria | - | sim | média | Não iniciada | dois personagens |
| `transicao/queda` | Queda | EM_PE_PEGADA → EM_PE_PEGADA, EM_PE_SEPARADOS, GUARDA | 45 ticks (~720 ms) | sem animação própria | - | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `transicao/puxar_guarda` | Puxar para a guarda | EM_PE_PEGADA → EM_PE_PEGADA, GUARDA | 20 ticks (~320 ms) | sem animação própria | - | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `transicao/passagem_guarda` | Passagem de guarda | GUARDA → CONTROLE_LATERAL, GUARDA, MONTADA | 60 ticks (~960 ms) | sem animação própria | - | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `transicao/raspagem` | Raspagem | GUARDA → GUARDA, MONTADA | 45 ticks (~720 ms) | sem animação própria | - | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `transicao/reposicao_guarda` | Reposição de guarda | CONTROLE_LATERAL → CONTROLE_LATERAL, GUARDA | 40 ticks (~640 ms) | sem animação própria | - | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |

### Movimento básico (6)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `andar` | Andar | - → - | - | própria (procedural) | - | não | baixa | Homologada | - |
| `correr` | Correr | - → - | - | própria (procedural) | - | não | baixa | Aguardando homologação visual | - |
| `agachar` | Agachar | - → - | - | própria (procedural) | - | não | baixa | Aguardando homologação visual | - |
| `pular` | Pular | - → - | - | própria (procedural) | - | não | baixa | Aguardando homologação visual | - |
| `idle_combate` | Idle de combate | - → - | loop | própria (procedural) | - | não | baixa | Homologada | - |
| `respiracao` | Respiração / exaustão | - → - | loop | própria (procedural) | - | não | baixa | Homologada | - |

### Movimento no chão (6)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `ajoelhado` | Pose ajoelhado | - → - | - | JSON (AnimacaoIndividual) | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `ajoelhado_movimento` | Andar de joelhos | - → - | - | JSON (AnimacaoIndividual) | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `movimento_chao` | Movimento deitado no chão | - → - | - | JSON (AnimacaoIndividual) | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `fuga_quadril` | Fuga de quadril | - → - | - | JSON (AnimacaoIndividual) | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `joelhos_acelerado` | Deslocamento acelerado de joelhos | - → - | - | própria (procedural) | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |
| `deitado_acelerado` | Deslocamento acelerado deitado | - → - | - | reaproveitada | - | não | média | Não iniciada | orientação da raiz (deitar/cair) |

### Defesa em pé (4)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `guarda_alta` | Guarda alta | - → - | - | JSON (BibliotecaMovimentos) | - | não | baixa | Não iniciada | - |
| `guarda_media` | Guarda média | - → - | - | JSON (BibliotecaMovimentos) | - | não | baixa | Não iniciada | - |
| `guarda_baixa` | Guarda baixa | - → - | - | JSON (BibliotecaMovimentos) | - | não | baixa | Não iniciada | - |
| `sprawl` | Sprawl | - → - | 32 ticks (5Q.6) | JSON | - | não | baixa | Não iniciada | - |

### Pegada (4)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `pegada_1` | Pegada 1 — Uma lapela + um braco. | EM_PE_SEPARADOS → estado PEGADA_1 | 30 quadros | JSON MiauStudio + reação do receptor (receptor/receptor_pegada_1.json) | corpo inteiro dos dois (pose MiauStudio de 30 valores) | sim | média | Aguardando homologação visual | dois personagens, mãos (pegada) |
| `pegada_2` | Pegada 2 — Cruzada: maos invertidas (frente na manga, tras cruza para a gola). (Entrega 5L) | EM_PE_SEPARADOS → estado PEGADA_2 | 30 quadros | JSON MiauStudio + reação do receptor (receptor/receptor_pegada_2.json) | corpo inteiro dos dois (pose MiauStudio de 30 valores) | sim | média | Aguardando homologação visual | dois personagens, mãos (pegada) |
| `pegada_3` | Pegada 3 — Duas mangas: as duas maos nas mangas. (Entrega 5L) | EM_PE_SEPARADOS → estado PEGADA_3 | 30 quadros | JSON MiauStudio + reação do receptor (receptor/receptor_pegada_3.json) | corpo inteiro dos dois (pose MiauStudio de 30 valores) | sim | média | Aguardando homologação visual | dois personagens, mãos (pegada) |
| `pegada_4` | Pegada 4 — Gola e nuca: mao da frente na nuca, mao de tras na gola. (Entrega 5L) | EM_PE_SEPARADOS → estado PEGADA_4 | 30 quadros | JSON MiauStudio + reação do receptor (receptor/receptor_pegada_4.json) | corpo inteiro dos dois (pose MiauStudio de 30 valores) | sim | média | Em implementação | dois personagens, mãos (pegada) |

### Finalização (pose genérica) (3)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `generica_braco` | Finalização de braço (pose genérica) | - → - | - | provisória | - | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `generica_estrangulamento` | Estrangulamento (pose genérica) | - → - | - | provisória | - | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |
| `generica_perna` | Finalização de perna (pose genérica) | - → - | - | provisória | - | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |

### Expressão (2)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `taunt` | Provocação (taunt) | - → - | - | própria | - | não | baixa | Não iniciada | - |
| `pose_apresentacao` | Pose de apresentação | - → - | - | própria | - | não | baixa | Não iniciada | - |

### Chão (outra) (1)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `subir_por_cima` | Subir por cima [SUBIR] | SOLO_DISPUTADO → CONTROLE_LATERAL (por cima: INICIADOR) | janela de 12 ticks (~192 ms) | provisória (pose genérica) | não determinado (pose provisória genérica) | sim | alta | Não iniciada | dois personagens, orientação da raiz (deitar/cair) |

### Finalização (reação) (1)

| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |
|---|---|---|---|---|---|---|---|---|---|
| `bater` | Bater (rendição) | - → - | 40 ticks | provisória (procedural) | - | não | baixa | Não iniciada | - |
