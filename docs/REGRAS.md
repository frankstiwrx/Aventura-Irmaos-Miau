# Regras e arbitragem (modo PONTOS + FINALIZAÇÃO)

Referência: **Livro de Regras da IBJJF, junho de 2024** (*Rules Book v6*, o vigente em novembro de 2026; as
mudanças anunciadas para 2027 **não** foram aplicadas). Esta é uma **adaptação** para o jogo, fiel aos
fundamentos, e não uma reprodução completa de uma competição oficial.

## Modos de luta (CONFIGURAÇÕES DA LUTA → REGRAS)

| Modo | Situação |
|---|---|
| FINALIZAÇÃO | Vence quem finaliza. Sem pontuação técnica (como antes). No fim do tempo, o critério de antes do jogo (condicionamento). |
| PONTOS + FINALIZAÇÃO | **Funcional.** Finalização encerra na hora; pontos, vantagens e placar; no fim do tempo, resultado pelo placar. |
| ARCADE | EM BREVE (não inicia a luta). |

## Tempo da luta (livre)

1, 1:30, 2, 3, 4, 5, 6, 7, 8, 9 e 10 minutos, ou **SEM LIMITE**, em qualquer modo e para qualquer faixa.
A faixa **nunca** escolhe nem muda o tempo de uma luta casual. Os tempos oficiais do adulto (branca 5, azul 6,
roxa 7, marrom 8, preta 10 min) ficam para um futuro modo CAMPEONATO (não implementado).

- O relógio mostra minutos e segundos (90 s = 1:30), conta em tempo de luta (ticks de 16 ms), para na pausa,
  acaba uma vez só, nunca fica negativo e recomeça a cada luta.
- Sem limite: nada acaba pelo relógio; o placar continua contando; os pontos sozinhos não encerram a luta.

## Arquitetura

- `MotorSolo` continua executando técnicas e mudando posições.
- `regras.ArbitroJiuJitsu` **só observa** o motor (posição, quem está por cima/controla, o ataque de
  finalização e os eventos) uma vez por tick de luta — nunca na pausa nem depois do fim — e decide pontos,
  vantagens e o resultado. Um só árbitro por luta (novo a cada luta), sem sistema concorrente.
- `regras.Placar` (pontos, vantagens, punições e a escala de punições), `regras.ResultadoLuta` (desempate),
  `regras.Pontuacao` (tabela), `hud.PlacarLuta` (desenho). O `GamePanel` só liga as peças.

## Pontuação (art. 2.5.2 e art. 4)

| Ação | Pontos | Como o árbitro reconhece |
|---|---|---|
| Queda | 2 | Da posição em pé para o chão com quem derrubou **por cima**, causada por uma técnica de queda dele (evento do motor) ou por uma contra-queda (4.1.7). Puxada de guarda e deitar não contam (4.1.11). Derrubar quem estava de joelhos só vale se quem derruba estava em pé (4.1.6). |
| Raspagem | 2 | Quem estava **por baixo** numa guarda (fechada, aberta, meia, borboleta, De La Riva, laço, aranha, single-leg X, ashi, ou o outro em pé diante das pernas) termina **por cima** (4.6). |
| Passagem | 3 | Quem estava **por cima** na guarda chega aos cem quilos, ao joelho na barriga ou à montada (4.2). |
| Joelho na barriga | 2 | A posição JOELHO NA BARRIGA do motor, com quem a controla (4.3). |
| Montada | 4 | A posição MONTADA do motor (4.4). |
| Costas | 4 | A posição COSTAS do motor (com ganchos, 4.5). |

**Estabilização (3.1):** toda pontuação espera **3 s de tempo de luta** (188 ticks de 16 ms) com a condição
valendo o tempo todo. Sem espera real, sem travar o jogo; a pausa congela a contagem.

**Progressão (3.4):** posições conquistadas em sequência somam, contando só os 3 s do fim (passagem → montada
= 3 + 4 = 7). Montada ↔ costas valem as duas (4.4.1). Cair direto nos cem quilos vale só a queda (obs. do 3.5);
queda direto na montada vale queda + montada.

**Finalização durante a contagem (3.3):** chegando à posição preso numa finalização, a contagem só começa
depois de escapar; se o outro prende uma finalização durante a contagem, ela é interrompida e vira **uma
vantagem por posição** (3.3.2).

## Sem pontos duplicados ou infinitos

- Queda e raspagem saem de uma **transição** (acontecem uma vez).
- Montada, costas, joelho na barriga e passagem ficam na **memória** de quem domina: ficar na posição não
  pontua de novo, nem sair e voltar por vontade própria (3.2).
- A memória só se apaga quando o domínio é perdido **por ação do outro** (escape, raspagem, recuperar a
  guarda...). Aí, conquistar de novo pontua de novo.
- Nada pontua por passagem de tempo ou a cada quadro; a posição inicial da luta é só a referência.

## Vantagens (art. 5) — só com critério verificável

| Vantagem | Critério |
|---|---|
| Quase pontuou (5.1, 5.6) | A posição pontuável foi **alcançada** mas não estabilizou (o outro escapou, raspou, a posição mudou). Uma por sequência. |
| Contagem interrompida (3.3.2) | O outro prendeu uma finalização durante a contagem: uma por posição. |
| Finalização com perigo real (5.3) | Ataque em fase de disputa com pressão ≥ 70 (de 100) ou na ponta, que terminou sem vitória. |
| Fim do tempo (5.4) | Posição alcançada sem estabilizar, ou finalização com perigo real em andamento. |

Tentativas que não chegaram à posição, e finalizações cortadas na entrada ou sem pressão, **não** dão vantagem.

## Punições (art. 6 e 7)

**Nenhuma punição é aplicada automaticamente.** O motor ainda não reconhece com segurança nenhuma das
infrações (falta de combatividade, fugir da área, pegadas por dentro da manga, etc.). A estrutura está
pronta: `Placar.punir` segue a escala do art. 7.2.1 (1ª marcada, 2ª vantagem ao adversário, 3ª +2 ao
adversário, 4ª desclassificação), testada à parte.

## Resultado (art. 2.5 e 2.6)

- Finalização (rendição ou consolidada): vitória na hora, em qualquer tempo, com o placar na mensagem.
- Fim do tempo: mais **pontos**; empate, mais **vantagens**; empate, menos **punições**.
- Empate em tudo: a regra manda o **árbitro decidir** (2.6). Essa decisão não existe no jogo: a tela diz
  **EMPATE (decisão do árbitro ainda não existe no jogo)**. Nada de sorteio nem critério inventado.
- Os outros fins de antes (os dois / um sem condicionamento) continuam iguais.

## Técnicas por faixa (tabela de faltas técnicas, adulto com kimono)

| Técnica (catálogo) | IBJJF (adulto) | Jogo |
|---|---|---|
| Chave de punho, mão de vaca | proibidas na branca | azul / roxa ✔ |
| Bíceps, panturrilha, chave de joelho, toe hold | proibidas até a roxa | marrom ✔ |
| Pé reto, ezequiel, omoplata, triângulo, katagatame, guilhotina | permitidas para adultos | ✔ |
| Heel hook, chaves que torcem o joelho, slam | proibidas para todos | não existem no catálogo ✔ |

A graduação do personagem já bloqueia o que a IBJJF proíbe nos casos conferidos (teste
`TecnicasPorFaixaIbjjfTest`), então o modo com pontos usa a elegibilidade LIVRE. **Não verificadas** (o
nome do jogo não basta para classificar): Aoki lock, Estima lock, chave de joelho invertida, chave de joelho
com giro, banana split, Mir lock, Von Flue. O `CAMPEONATO_PROVISORIO` antigo (só pé reto nas pernas) **não**
é usado: ele bloquearia o que a IBJJF permite para marrom e preta.

## Diferenças e limitações conhecidas

- O motor não modela detalhes de posição: braços presos na montada (montada sobre os dois braços = vantagem
  na IBJJF), ganchos x pés cruzados x triângulo de corpo nas costas (o TRIÂNGULO de corpo da roda das costas
  ainda conta como costas), joelho oposto no chão no joelho na barriga, tartaruga (4.1.2/4.6.2), saída da área
  (4.1.3) e os dois puxando a guarda (3.5): ficam como estão até haver estado no motor.
- Passagem/raspagem sem guarda formal: "um em pé, outro no chão" conta como guarda (o de baixo usa as pernas),
  exceto na aterrissagem da própria queda.
- Decisão do árbitro e sorteio (2.6/2.7) não existem.
- Punições: nenhuma automática (ver acima).

## Futuro modo CAMPEONATO (não implementado)

Tempo oficial por faixa/categoria, chaves, progressão entre lutas, classificação e restrições por
categoria (inclusive as técnicas não verificadas acima). O tempo livre das lutas casuais continua.
