# Rodas: códigos do Modo Desenvolvedor

Com o **F8** (Modo Desenvolvedor) ligado numa partida, cada roda aberta mostra, logo abaixo dela,
o seu código estável (`RODA: M-026`). Sem o F8 nada muda. A tabela vem de
`src/main/resources/dev/rodas.txt` (fonte única); `hud.IdentificacaoRodas` monta a chave de
contexto da roda aberta. Roda nova: acrescente uma linha no fim desse arquivo com o próximo código
livre — códigos nunca são renumerados nem reutilizados (o teste `IdentificacaoRodasTest` cobra).

| Código | Chave | Contexto | Controles |
|---|---|---|---|
| M-001 | `EMPE/PEGADAS/LIVRE/UM` | Em pé, pegadas · papel livre | RS |
| M-002 | `EMPE/PEGADAS/LIVRE/DOIS` | Em pé, pegadas · papel livre | LT + RS |
| M-003 | `EMPE/PEGADAS/LIVRE/TRES` | Em pé, pegadas · papel livre | LB + RS |
| M-004 | `EMPE/PEGADAS/LIVRE/QUATRO` | Em pé, pegadas · papel livre | LT + LB + RS |
| M-005 | `EMPE/PEGADAS/ENTRANDO/UM` | Em pé, pegadas · papel entrando | RS |
| M-006 | `EMPE/PEGADAS/ENTRANDO/DOIS` | Em pé, pegadas · papel entrando | LT + RS |
| M-007 | `EMPE/PEGADAS/ENTRANDO/TRES` | Em pé, pegadas · papel entrando | LB + RS |
| M-008 | `EMPE/PEGADAS/ENTRANDO/QUATRO` | Em pé, pegadas · papel entrando | LT + LB + RS |
| M-009 | `EMPE/PEGADAS/APLICADOR/UM` | Em pé, pegadas · papel aplicador | RS |
| M-010 | `EMPE/PEGADAS/APLICADOR/DOIS` | Em pé, pegadas · papel aplicador | LT + RS |
| M-011 | `EMPE/PEGADAS/APLICADOR/TRES` | Em pé, pegadas · papel aplicador | LB + RS |
| M-012 | `EMPE/PEGADAS/APLICADOR/QUATRO` | Em pé, pegadas · papel aplicador | LT + LB + RS |
| M-013 | `EMPE/PEGADAS/RECEPTOR/UM` | Em pé, pegadas · papel receptor | RS |
| M-014 | `EMPE/PEGADAS/RECEPTOR/DOIS` | Em pé, pegadas · papel receptor | LT + RS |
| M-015 | `EMPE/PEGADAS/RECEPTOR/TRES` | Em pé, pegadas · papel receptor | LB + RS |
| M-016 | `EMPE/PEGADAS/RECEPTOR/QUATRO` | Em pé, pegadas · papel receptor | LT + LB + RS |
| M-017 | `EMPE/DEFESAS` | Em pé, defesas (guardas e sprawl) | RT + RS |
| M-018 | `EMPE/QUEDAS` | Em pé, quedas (reservado) | RB + RS |
| M-019 | `SEMMOTOR/SOLO` | Sem motor de solo: alguém deitado (reservado) | RS (e RB/LT/LB) |
| M-020 | `MOTOR/CAINDO` | Caindo: como receber a queda | RS (e RB/LT/LB) |
| M-021 | `FIN/ATACA/ENTRADA` | Finalização em andamento · quem ataca · entrada | RS (e RB/LT/LB) |
| M-022 | `FIN/ATACA/DISPUTA` | Finalização em andamento · quem ataca · disputa | RS (e RB/LT/LB) |
| M-023 | `FIN/DEFENDE/ENTRADA` | Finalização em andamento · quem defende · entrada | RS (e RB/LT/LB) |
| M-024 | `FIN/DEFENDE/DISPUTA` | Finalização em andamento · quem defende · disputa | RS (e RB/LT/LB) |
| M-025 | `MOTOR/EM_PE_SEPARADOS/NEUTRO/R1` | Em pé, separados · neutro | RB + RS |
| M-026 | `MOTOR/EM_PE_SEPARADOS/NEUTRO/R1/JOELHOS` | Em pé, separados · neutro · de joelhos | RB + RS |
| M-027 | `MOTOR/EM_PE_SEPARADOS/NEUTRO/L1` | Em pé, separados · neutro | LB + RS |
| M-028 | `MOTOR/EM_PE_SEPARADOS/NEUTRO/L1/JOELHOS` | Em pé, separados · neutro · de joelhos | LB + RS |
| M-029 | `MOTOR/EM_PE_SEPARADOS/NEUTRO/L2` | Em pé, separados · neutro | LT + RS |
| M-030 | `MOTOR/EM_PE_SEPARADOS/NEUTRO/L2/JOELHOS` | Em pé, separados · neutro · de joelhos | LT + RS |
| M-031 | `MOTOR/EM_PE_PEGADA/CTRL/RS` | Em pé, disputa de pegada · controla | RS |
| M-032 | `MOTOR/EM_PE_PEGADA/CTRL/RS/JOELHOS` | Em pé, disputa de pegada · controla · de joelhos | RS |
| M-033 | `MOTOR/EM_PE_PEGADA/CTRL/R1` | Em pé, disputa de pegada · controla | RB + RS |
| M-034 | `MOTOR/EM_PE_PEGADA/CTRL/R1/JOELHOS` | Em pé, disputa de pegada · controla · de joelhos | RB + RS |
| M-035 | `MOTOR/EM_PE_PEGADA/CTRL/L1` | Em pé, disputa de pegada · controla | LB + RS |
| M-036 | `MOTOR/EM_PE_PEGADA/CTRL/L1/JOELHOS` | Em pé, disputa de pegada · controla · de joelhos | LB + RS |
| M-037 | `MOTOR/EM_PE_PEGADA/CTRL/L2` | Em pé, disputa de pegada · controla | LT + RS |
| M-038 | `MOTOR/EM_PE_PEGADA/CTRL/L2/JOELHOS` | Em pé, disputa de pegada · controla · de joelhos | LT + RS |
| M-039 | `MOTOR/EM_PE_PEGADA/CTRL/L1_L2` | Em pé, disputa de pegada · controla | LT + LB + RS |
| M-040 | `MOTOR/EM_PE_PEGADA/NEUTRO/R1` | Em pé, disputa de pegada · neutro | RB + RS |
| M-041 | `MOTOR/EM_PE_PEGADA/NEUTRO/R1/JOELHOS` | Em pé, disputa de pegada · neutro · de joelhos | RB + RS |
| M-042 | `MOTOR/EM_PE_PEGADA/NEUTRO/L1` | Em pé, disputa de pegada · neutro | LB + RS |
| M-043 | `MOTOR/EM_PE_PEGADA/NEUTRO/L1/JOELHOS` | Em pé, disputa de pegada · neutro · de joelhos | LB + RS |
| M-044 | `MOTOR/GUARDA/CIMA/RS` | Guarda · cima | RS |
| M-045 | `MOTOR/GUARDA/CIMA/R1` | Guarda · cima | RB + RS |
| M-046 | `MOTOR/GUARDA/BAIXO_CTRL/RS` | Guarda · baixo (controla) | RS |
| M-047 | `MOTOR/GUARDA/BAIXO_CTRL/R1` | Guarda · baixo (controla) | RB + RS |
| M-048 | `MOTOR/GUARDA/BAIXO_CTRL/L1` | Guarda · baixo (controla) | LB + RS |
| M-049 | `MOTOR/GUARDA/BAIXO_CTRL/L2` | Guarda · baixo (controla) | LT + RS |
| M-050 | `MOTOR/GUARDA/BAIXO_CTRL/L1_L2` | Guarda · baixo (controla) | LT + LB + RS |
| M-051 | `MOTOR/MEIA_GUARDA/CIMA/RS` | Meia-guarda · cima | RS |
| M-052 | `MOTOR/MEIA_GUARDA/CIMA/R1` | Meia-guarda · cima | RB + RS |
| M-053 | `MOTOR/MEIA_GUARDA/BAIXO_CTRL/RS` | Meia-guarda · baixo (controla) | RS |
| M-054 | `MOTOR/MEIA_GUARDA/BAIXO_CTRL/R1` | Meia-guarda · baixo (controla) | RB + RS |
| M-055 | `MOTOR/CONTROLE_LATERAL/CIMA_CTRL/RS` | Controle lateral (cem quilos) · cima (controla) | RS |
| M-056 | `MOTOR/CONTROLE_LATERAL/CIMA_CTRL/R1` | Controle lateral (cem quilos) · cima (controla) | RB + RS |
| M-057 | `MOTOR/CONTROLE_LATERAL/CIMA_CTRL/L1` | Controle lateral (cem quilos) · cima (controla) | LB + RS |
| M-058 | `MOTOR/CONTROLE_LATERAL/CIMA_CTRL/L2` | Controle lateral (cem quilos) · cima (controla) | LT + RS |
| M-059 | `MOTOR/CONTROLE_LATERAL/CIMA_CTRL/L1_L2` | Controle lateral (cem quilos) · cima (controla) | LT + LB + RS |
| M-060 | `MOTOR/CONTROLE_LATERAL/CIMA_CTRL/R2` | Controle lateral (cem quilos) · cima (controla) | RT + RS |
| M-061 | `MOTOR/CONTROLE_LATERAL/BAIXO/RS` | Controle lateral (cem quilos) · baixo | RS |
| M-062 | `MOTOR/CONTROLE_LATERAL/BAIXO/R1` | Controle lateral (cem quilos) · baixo | RB + RS |
| M-063 | `MOTOR/MONTADA/CIMA_CTRL/RS` | Montada · cima (controla) | RS |
| M-064 | `MOTOR/MONTADA/CIMA_CTRL/R1` | Montada · cima (controla) | RB + RS |
| M-065 | `MOTOR/MONTADA/CIMA_CTRL/L1` | Montada · cima (controla) | LB + RS |
| M-066 | `MOTOR/MONTADA/CIMA_CTRL/L2` | Montada · cima (controla) | LT + RS |
| M-067 | `MOTOR/MONTADA/CIMA_CTRL/L1_L2` | Montada · cima (controla) | LT + LB + RS |
| M-068 | `MOTOR/MONTADA/CIMA_CTRL/R2` | Montada · cima (controla) | RT + RS |
| M-069 | `MOTOR/MONTADA/BAIXO/RS` | Montada · baixo | RS |
| M-070 | `MOTOR/MONTADA/BAIXO/R1` | Montada · baixo | RB + RS |
| M-071 | `MOTOR/COSTAS/CIMA_CTRL/RS` | Controle das costas · cima (controla) | RS |
| M-072 | `MOTOR/COSTAS/CIMA_CTRL/R1` | Controle das costas · cima (controla) | RB + RS |
| M-073 | `MOTOR/COSTAS/CIMA_CTRL/L1` | Controle das costas · cima (controla) | LB + RS |
| M-074 | `MOTOR/COSTAS/CIMA_CTRL/L2` | Controle das costas · cima (controla) | LT + RS |
| M-075 | `MOTOR/COSTAS/CIMA_CTRL/L1_L2` | Controle das costas · cima (controla) | LT + LB + RS |
| M-076 | `MOTOR/COSTAS/CIMA_CTRL/R2` | Controle das costas · cima (controla) | RT + RS |
| M-077 | `MOTOR/COSTAS/BAIXO/RS` | Controle das costas · baixo | RS |
| M-078 | `MOTOR/COSTAS/BAIXO/R1` | Controle das costas · baixo | RB + RS |
| M-079 | `MOTOR/GUARDA_ABERTA/CIMA/RS` | Guarda aberta · cima | RS |
| M-080 | `MOTOR/GUARDA_ABERTA/CIMA/R1` | Guarda aberta · cima | RB + RS |
| M-081 | `MOTOR/GUARDA_ABERTA/BAIXO_CTRL/RS` | Guarda aberta · baixo (controla) | RS |
| M-082 | `MOTOR/GUARDA_ABERTA/BAIXO_CTRL/R1` | Guarda aberta · baixo (controla) | RB + RS |
| M-083 | `MOTOR/GUARDA_ABERTA/BAIXO_CTRL/L1` | Guarda aberta · baixo (controla) | LB + RS |
| M-084 | `MOTOR/GUARDA_ABERTA/BAIXO_CTRL/L2` | Guarda aberta · baixo (controla) | LT + RS |
| M-085 | `MOTOR/GUARDA_ABERTA/BAIXO_CTRL/L1_L2` | Guarda aberta · baixo (controla) | LT + LB + RS |
| M-086 | `MOTOR/JOELHO_BARRIGA/CIMA_CTRL/RS` | Joelho na barriga · cima (controla) | RS |
| M-087 | `MOTOR/JOELHO_BARRIGA/CIMA_CTRL/R1` | Joelho na barriga · cima (controla) | RB + RS |
| M-088 | `MOTOR/JOELHO_BARRIGA/BAIXO/RS` | Joelho na barriga · baixo | RS |
| M-089 | `MOTOR/JOELHO_BARRIGA/BAIXO/R1` | Joelho na barriga · baixo | RB + RS |
| M-090 | `MOTOR/EM_PE_CONTRA_SOLO/CIMA/RS` | Um em pé, outro no chão · cima | RS |
| M-091 | `MOTOR/EM_PE_CONTRA_SOLO/CIMA/R1` | Um em pé, outro no chão · cima | RB + RS |
| M-092 | `MOTOR/EM_PE_CONTRA_SOLO/BAIXO/RS` | Um em pé, outro no chão · baixo | RS |
| M-093 | `MOTOR/EM_PE_CONTRA_SOLO/BAIXO/R1` | Um em pé, outro no chão · baixo | RB + RS |
| M-094 | `MOTOR/EM_PE_CONTRA_SOLO/BAIXO/L1` | Um em pé, outro no chão · baixo | LB + RS |
| M-095 | `MOTOR/EM_PE_CONTRA_SOLO/CIMA_CTRL/RS` | Um em pé, outro no chão · cima (controla) | RS |
| M-096 | `MOTOR/EM_PE_CONTRA_SOLO/CIMA_CTRL/R1` | Um em pé, outro no chão · cima (controla) | RB + RS |
| M-097 | `MOTOR/SOLO_DISPUTADO/NEUTRO/RS` | Solo disputado · neutro | RS |
| M-098 | `MOTOR/SOLO_DISPUTADO/NEUTRO/R1` | Solo disputado · neutro | RB + RS |
| M-099 | `MOTOR/GUARDA_BORBOLETA/CIMA/RS` | Guarda borboleta · cima | RS |
| M-100 | `MOTOR/GUARDA_BORBOLETA/CIMA/R1` | Guarda borboleta · cima | RB + RS |
| M-101 | `MOTOR/GUARDA_BORBOLETA/BAIXO_CTRL/RS` | Guarda borboleta · baixo (controla) | RS |
| M-102 | `MOTOR/GUARDA_BORBOLETA/BAIXO_CTRL/R1` | Guarda borboleta · baixo (controla) | RB + RS |
| M-103 | `MOTOR/GUARDA_BORBOLETA/BAIXO_CTRL/L1` | Guarda borboleta · baixo (controla) | LB + RS |
| M-104 | `MOTOR/GUARDA_BORBOLETA/BAIXO_CTRL/L2` | Guarda borboleta · baixo (controla) | LT + RS |
| M-105 | `MOTOR/DE_LA_RIVA/CIMA/RS` | Guarda De La Riva · cima | RS |
| M-106 | `MOTOR/DE_LA_RIVA/CIMA/R1` | Guarda De La Riva · cima | RB + RS |
| M-107 | `MOTOR/DE_LA_RIVA/BAIXO_CTRL/RS` | Guarda De La Riva · baixo (controla) | RS |
| M-108 | `MOTOR/DE_LA_RIVA/BAIXO_CTRL/R1` | Guarda De La Riva · baixo (controla) | RB + RS |
| M-109 | `MOTOR/DE_LA_RIVA/BAIXO_CTRL/L1` | Guarda De La Riva · baixo (controla) | LB + RS |
| M-110 | `MOTOR/DE_LA_RIVA/BAIXO_CTRL/L2` | Guarda De La Riva · baixo (controla) | LT + RS |
| M-111 | `MOTOR/DE_LA_RIVA/BAIXO_CTRL/L1_L2` | Guarda De La Riva · baixo (controla) | LT + LB + RS |
| M-112 | `MOTOR/GUARDA_LACO/CIMA/RS` | Guarda laço · cima | RS |
| M-113 | `MOTOR/GUARDA_LACO/CIMA/R1` | Guarda laço · cima | RB + RS |
| M-114 | `MOTOR/GUARDA_LACO/BAIXO_CTRL/RS` | Guarda laço · baixo (controla) | RS |
| M-115 | `MOTOR/GUARDA_LACO/BAIXO_CTRL/R1` | Guarda laço · baixo (controla) | RB + RS |
| M-116 | `MOTOR/GUARDA_LACO/BAIXO_CTRL/L1` | Guarda laço · baixo (controla) | LB + RS |
| M-117 | `MOTOR/GUARDA_LACO/BAIXO_CTRL/L2` | Guarda laço · baixo (controla) | LT + RS |
| M-118 | `MOTOR/GUARDA_ARANHA/CIMA/RS` | Guarda aranha · cima | RS |
| M-119 | `MOTOR/GUARDA_ARANHA/CIMA/R1` | Guarda aranha · cima | RB + RS |
| M-120 | `MOTOR/GUARDA_ARANHA/BAIXO_CTRL/RS` | Guarda aranha · baixo (controla) | RS |
| M-121 | `MOTOR/GUARDA_ARANHA/BAIXO_CTRL/R1` | Guarda aranha · baixo (controla) | RB + RS |
| M-122 | `MOTOR/GUARDA_ARANHA/BAIXO_CTRL/L1` | Guarda aranha · baixo (controla) | LB + RS |
| M-123 | `MOTOR/GUARDA_ARANHA/BAIXO_CTRL/L2` | Guarda aranha · baixo (controla) | LT + RS |
| M-124 | `MOTOR/SINGLE_LEG_X/CIMA/RS` | Single-leg X · cima | RS |
| M-125 | `MOTOR/SINGLE_LEG_X/CIMA/R1` | Single-leg X · cima | RB + RS |
| M-126 | `MOTOR/SINGLE_LEG_X/BAIXO_CTRL/RS` | Single-leg X · baixo (controla) | RS |
| M-127 | `MOTOR/SINGLE_LEG_X/BAIXO_CTRL/R1` | Single-leg X · baixo (controla) | RB + RS |
| M-128 | `MOTOR/SINGLE_LEG_X/BAIXO_CTRL/L1` | Single-leg X · baixo (controla) | LB + RS |
| M-129 | `MOTOR/SINGLE_LEG_X/BAIXO_CTRL/L1_L2` | Single-leg X · baixo (controla) | LT + LB + RS |
| M-130 | `MOTOR/ASHI_GARAMI/CIMA/RS` | Ashi garami (controle de perna) · cima | RS |
| M-131 | `MOTOR/ASHI_GARAMI/CIMA/R1` | Ashi garami (controle de perna) · cima | RB + RS |
| M-132 | `MOTOR/ASHI_GARAMI/BAIXO_CTRL/RS` | Ashi garami (controle de perna) · baixo (controla) | RS |
| M-133 | `MOTOR/ASHI_GARAMI/BAIXO_CTRL/R1` | Ashi garami (controle de perna) · baixo (controla) | RB + RS |
| M-134 | `MOTOR/ASHI_GARAMI/BAIXO_CTRL/L1` | Ashi garami (controle de perna) · baixo (controla) | LB + RS |
| M-135 | `MOTOR/ASHI_GARAMI/BAIXO_CTRL/L2` | Ashi garami (controle de perna) · baixo (controla) | LT + RS |
| M-136 | `MOTOR/ASHI_GARAMI/BAIXO_CTRL/L1_L2` | Ashi garami (controle de perna) · baixo (controla) | LT + LB + RS |
