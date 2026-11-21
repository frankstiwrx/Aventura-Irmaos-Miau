# Checklist de regressão

Rodar **a cada entrega** antes de commitar. A parte automática não substitui a
parte manual: "passou no Maven" não significa "aprovado visualmente".

## 1. Automático

```bash
mvn clean test
```

Esperado: `Tests run: 1207, Failures: 0, Errors: 0, Skipped: 0` e `BUILD SUCCESS`
(o número cresce quando novos testes entram). No NetBeans: **Test Project** (Alt+F6).

`mvn clean verify` também testa o JAR final (`JarExecutavelIT`, 4 testes): manifesto, DLLs do GLFW
e `java -jar … --diagnostico-gamepad 0`.

```bash
git diff --stat -- src/main/java/com/irmaosmiau/entities src/main/java/com/irmaosmiau/editor src/main/java/com/irmaosmiau/combate/RegistroAnimacoesCombate.java
```

Esperado: vazio, a menos que a entrega tenha autorização explícita para tocar nesses arquivos.

## 2. Manual no NetBeans

### Menu oficial (`Main` — Run Project, F6) — fluxo antigo; desde a 5M vale "Título, menu e seleção" abaixo

- [ ] Abre o menu com o fundo `menuprincipal.png` e o botão COMEÇAR
- [ ] COMEÇAR → seleção: MIAU BRANCO, MIAU PRETO, OS DOIS MIAUS
- [ ] Cada opção abre o jogo com o(s) personagem(ns) correto(s)
- [ ] ESC pausa; os botões do menu de pausa voltam ao menu, trocam personagem ou retomam

### JAR executável

- [ ] `mvn package` e depois `java -jar target/AventuraIrmaosMiau-1.0-SNAPSHOT.jar` abre o mesmo menu oficial

### Protótipo (`MainPrototipo` — botão direito → Run File, Shift+F6)

Controles — Branco: A/D/W/S, espaço, C agachar, Z correr, V pulo curto, X respirar,
R rolar, F deitar, G sprawl, **H pegada 1**. Preto: setas, N, M, K, B, L, O, J, I.

- [ ] Dois Miaus aparecem, frente a frente, e se viram quando cruzam
- [ ] Andar, correr, agachar, pular alto/curto, respirar normais nos dois
- [ ] Rolamento nas direções, sprawl, deitar/levantar e rastejar normais nos dois
- [ ] Fôlego e condicionamento descem/recuperam como antes
- [ ] Longe um do outro, H não faz nada (debug: `PEGADA_1: INDISPONIVEL`, `ULTIMA: pegada_1 -> INDISPONIVEL`)
- [ ] Aproximar até o debug mostrar `PEGADA_1: DISPONIVEL` (≤ ~87 px / 125 u)
- [ ] H uma vez: fôlego do Branco cai **3** uma única vez
- [ ] Branco e Preto mostram os **3 quadros sincronizados** (`ANIM: pegada_1_entrada x/30`)
- [ ] Durante a animação nenhum dos dois responde às teclas
- [ ] Ao terminar, a **terceira pose permanece** e `INTERACAO: PEGADA_1`
- [ ] H de novo com a pegada ativa não cobra fôlego (`-> INDISPONIVEL`)
- [ ] Debug mostra `FONTE ANIM: /conteudo/miaustudio/PEGADA_1.json`
- [ ] Afastar um dos dois (ou subir/descer 2 linhas) → `INTERACAO: LIVRE` e o desenho normal volta

#### Política de interação (Entrega 3A) — repetir para o Branco e para o Preto

- [ ] Chegar andando, segurar a direção e apertar H: depois dos 30 frames **ninguém sai andando sozinho**; a pegada continua
- [ ] Soltar a direção e apertar de novo: anda e rompe (`ROMPIMENTO: MOVIMENTO`), desenho normal na hora
- [ ] Apertar pulo durante a animação: nada acontece depois que ela termina
- [ ] Parado alguns segundos (inclusive respirando): pegada e terceira pose continuam
- [ ] Depois da pegada: pular → `PULO`, agachar → `AGACHAMENTO`, deitar → `DEITAR_OU_LEVANTAR`, rolar → `ROLAMENTO`, sprawl → `SPRAWL`, andar para cima/baixo ou correr → `MOVIMENTO`
- [ ] Em todos os casos a ação acontece normalmente e não sobra pose congelada
- [ ] Depois de romper, a pegada pode ser feita de novo

### Apresentação e treino (Entrega 4A)

- [ ] Menu toca a música provisória (F9/F10 só existem dentro do jogo); botões COMEÇAR e **TREINO**
- [ ] TREINO abre no **dojo** com selo `MODO TREINO`, painéis JOGADOR (Branco) e PARCEIRO (Preto)
- [ ] Painéis mostram nome, FÔLEGO e CONDIC. com valores (ex.: `25/25`, `35/35`; Preto `40/40`, `25/25`)
- [ ] Setas/N/M/J/O/I **não** movem o Preto no treino; WASD, Z, ESPAÇO, V, C, R, F, G movem o Branco
- [ ] H longe: mensagem `Pegada 1: fora de alcance`; de costas: `de costas`
- [ ] H perto: `Pegada 1!`, som curto, 3 quadros, `PEGADA 1 · ESTABELECIDA`, fôlego 25 → 22
- [ ] Parado: pegada continua; andar/pular/agachar: `Pegada desfeita: ...` + som de rompimento
- [ ] F5: os dois voltam às posições iniciais, fôlego cheio, `Posições reiniciadas`
- [ ] ESC → MENU PRINCIPAL volta ao menu (e a música do menu volta)
- [ ] F3 mostra/esconde o debug (inclusive `FONTE ANIM`); F4 alterna dojo/original; F9/F10 desligam música/efeitos
- [ ] HUD não cobre os personagens em nenhuma das 5 linhas, nem no pulo alto
- [ ] Modos Branco, Preto e Os Dois abrem no dojo e funcionam como antes; `MainPrototipo` abre com debug ligado
- [ ] Rodar com `-Dirmaosmiau.audio=desligado` (ou sem placa de som): jogo funciona em silêncio

### Configurações e perfis (Entrega 4C)

- [ ] Menu mostra COMEÇAR, TREINO, CONFIGURAÇÕES, PERSONAGENS PERSONALIZADOS (textos inteiros)
- [ ] CONFIGURAÇÕES: desmarcar Música para o som na hora; mover o volume muda na hora
- [ ] VOLTAR sem salvar pergunta; "Não" desfaz; "Cancelar" fica na tela
- [ ] SALVAR, fechar o jogo e abrir de novo: valores mantidos
- [ ] Escolher "Campo original", salvar, entrar em Os Dois / Treino: abre no campo original
- [ ] F9/F10/F4 dentro do jogo e voltar ao menu → CONFIGURAÇÕES mostra o mesmo estado; reiniciar o jogo mantém
- [ ] RESTAURAR PADRÕES + SALVAR volta a música/efeitos ligados, 60%/80%, dojo
- [ ] PERSONAGENS PERSONALIZADOS: aviso "ainda não são usados nas lutas" visível
- [ ] Base Preto → NOVO PERFIL → nome → mudar os 8 atributos → SALVAR → aparece na lista
- [ ] Fechar e abrir o jogo: perfil continua; selecionar mostra os valores salvos
- [ ] Mudar um valor e clicar em outro perfil: pergunta salvar/descartar/cancelar
- [ ] Digitar 500 em Força e SALVAR: mensagem de erro, nada gravado
- [ ] EXCLUIR pede confirmação; "Não" mantém; "Sim" remove
- [ ] Branco/Preto OFICIAIS nas lutas continuam com os atributos oficiais (ex.: fôlego 25/25 e 40/40 no HUD)

### Perfis jogáveis (Entrega 4D)

- [ ] COMEÇAR: cada slot mostra o oficial + perfis daquela base; trocar o seletor mostra OFICIAL/PERSONALIZADO, base e os 8 atributos
- [ ] Perfil do Branco com fôlego 60 e velocidade 100 → MIAU BRANCO: HUD mostra o nome do perfil e `60/60`; anda visivelmente mais rápido
- [ ] OS DOIS MIAUS com perfil em cada slot: HUD com os dois nomes/valores; WASD e setas como antes
- [ ] TREINO (com perfis salvos) abre a escolha de jogador/parceiro; sem perfis, abre direto como antes
- [ ] No treino com perfil: gastar fôlego, F5 → fôlego volta ao máximo DO PERFIL
- [ ] PEGADA_1 com perfil: 3 quadros iguais, custo 3, pose final, rompimento normal
- [ ] Perfil com fôlego 2: H mostra "sem fôlego"
- [ ] Ordem de profundidade correta com perfis em linhas diferentes
- [ ] Arquivo de perfil corrompido: aviso na seleção; oficiais continuam escolhíveis
- [ ] Depois de jogar, o perfil no editor continua com os mesmos valores

### Combate avançado (Entrega 5A) — ver COMBATE_AVANCADO.md

- [ ] Sem pegada, T mostra "DESEQUILÍBRIO: só com a pegada estabelecida"; durante os 30 quadros também
- [ ] Pegada estabelecida (oficiais): T → "DESEQUILIBROU!", fôlego do Branco −4, estado "… Irmão Miau Preto DESEQUILIBRADO"
- [ ] A pose final da pegada não muda e a animação de entrada não recomeça
- [ ] T de novo logo em seguida → "aguarde"; segurar T não repete
- [ ] Preto desequilibrado anda mais devagar (~60%) por ~2 s, inclusive depois que a pegada acaba
- [ ] Os Dois: U com o Branco descansado → "DEFESA FALHOU", pegada continua, Preto −3 e Branco sem custo (5B)
- [ ] Gastar o fôlego do Branco (correr) antes da pegada e depois U → "PEGADA ROMPIDA!", o Preto não sai do lugar
- [ ] Mover/pular/agachar continua rompendo a pegada como antes (3A)
- [ ] Treino: U não faz nada; o parceiro nunca defende sozinho; F5 tira o desequilíbrio e zera as recargas
- [ ] Perfil sem força/técnica: T → "TENTATIVA DE DESEQUILÍBRIO: RESISTIU"
- [ ] F3: linha DISPUTA mostra as pontuações

### Controle da pegada (Entrega 5B)

- [ ] Durante os 30 quadros: "PEGADA 1 · ENTRANDO"; depois "PEGADA 1 · FIRMEZA 100%" caindo devagar
- [ ] Parado ~12 s: aparece "CONTROLE ENFRAQUECENDO"; ~17 s: "PEGADA DESFEITA: SEM CONTROLE"
- [ ] Y com a pegada caindo: "PEGADA REFORÇADA", firmeza +25, fôlego −3; Y de novo logo: "REFORÇO: AGUARDE"; com 100%: "PEGADA JÁ ESTÁ FIRME"
- [ ] SHIFT + D / SHIFT + A: a dupla anda junta, devagar, sem separar os braços nem trocar de lado; "DESLOCAMENTO CONTROLADO"
- [ ] Arrastar até a borda: para em "LIMITE DA ARENA"
- [ ] D sem SHIFT ainda rompe a pegada
- [ ] T/U contra quem não agiu não tiram fôlego do outro lado
- [ ] Os Dois: com a firmeza abaixo de ~30, U do Preto rompe ("PEGADA ROMPIDA!")
- [ ] Linhas diferentes (Preto uma linha abaixo): arrastar mantém a profundidade correta
- [ ] Treino: Y, T e SHIFT+A/D funcionam; parceiro passivo; F5 volta tudo ao início
- [ ] Fora da pegada, os controles com e sem SHIFT continuam normais

### Controles configuráveis (Entrega 5C) — ver CONTROLES.md

- [ ] Sem `controles.json`: todos os controles iguais aos de antes (WASD/H/T/Y/SHIFT, setas/U); a pasta de dados não ganha arquivo novo só por jogar
- [ ] CONFIGURAÇÕES → CONTROLES abre; JOGADOR 1 e JOGADOR 2 mostram suas ações com rolagem, agrupadas
- [ ] Clicar na tecla de "Iniciar pegada" → "pressione uma tecla…" → P; ESC durante a captura cancela; F5/ESC/TAB são recusadas
- [ ] Durante a captura, ESPAÇO/ENTER não acionam os botões da tela
- [ ] Escolher uma tecla já usada (ex.: D, ou A no Jogador 2) pergunta se deve trocar; "Não" mantém tudo
- [ ] VOLTAR com mudança pergunta salvar/descartar/cancelar; SALVAR, fechar e abrir o jogo: P continua
- [ ] Partida Os Dois: P faz a PEGADA_1 (H não faz nada); 3 quadros, custo 3, pose final, firmeza normal
- [ ] Remapear T, Y e U: desequilíbrio, reforço e rompimento funcionam pelas teclas novas, com as mesmas mensagens
- [ ] Remapear o modo controle para CTRL (ou Q) e esquerda/direita: arrasto funciona com as teclas novas; SHIFT não arrasta mais; direita sem o modo rompe
- [ ] Faixa de dicas mostra as teclas configuradas e muda com o papel (livre: pegar; aplicador: desequilibrar/reforçar/arrastar; receptor: romper)
- [ ] ESC → CONTROLES mostra a ajuda com as teclas reais e a linha AGORA; ESC/VOLTAR volta à pausa
- [ ] Segurar uma direção e trocar de janela (Alt+Tab): ao voltar, ninguém está andando sozinho
- [ ] Treino: teclas personalizadas funcionam; parceiro passivo; F5 reinicia
- [ ] RESTAURAR PADRÕES + SALVAR volta tudo ao original
- [ ] Editar `controles.json` à mão com lixo: o jogo abre com aviso e controles padrão; o arquivo só vira `.corrompido-…` ao salvar
- [ ] Modo individual (Branco ou Preto) usa as teclas do Jogador 1

### Gamepad (Entrega 5D) — ver CONTROLES.md

Sem controle conectado:
- [ ] `java -jar target/AventuraIrmaosMiau-1.0-SNAPSHOT.jar --diagnostico-gamepad 5`: "Leitura de gamepad: OK - GLFW …", "Gamepads conectados: 0", sem WARNING
- [ ] O jogo abre e todos os modos funcionam só com teclado, como na 5C; a pasta de dados não ganha arquivos só por jogar
- [ ] CONFIGURAÇÕES → CONTROLES: linha "Dispositivo" mostra "Nenhum gamepad conectado (GLFW …)"
- [ ] J2 em Gamepad + SALVAR; Os Dois: HUD avisa "J2: nenhum gamepad livre · usando o teclado" e as setas funcionam
- [ ] Rodar com `-Dirmaosmiau.gamepad=desligado`: a linha mostra o motivo; tudo funciona no teclado

Com um controle (Xbox e/ou PlayStation; USB e Bluetooth):
- [ ] Diagnóstico por 20 s: o controle aparece com nome; cada botão mostra o rótulo e as ações esperadas
- [ ] J2 em Gamepad: Os Dois → HUD "J2: GAMEPAD · <nome>"; analógico/D-pad movem o Preto; setas não movem
- [ ] J1 em Gamepad: analógico anda e troca de linha; ruído em repouso não move; OESTE (X/Quadrado) faz a PEGADA_1 (3 quadros, custo 3, firmeza)
- [ ] LESTE (B/Bola) desequilibra (J1) / rompe (J2); NORTE (Y/Triângulo) reforça uma vez por toque
- [ ] LB/L1 segurado + analógico: arrasto conjunto; analógico sem LB rompe a pegada
- [ ] Segurar o analógico durante os 30 quadros da pegada: ninguém sai andando sozinho
- [ ] Start pausa e despausa; com a ajuda aberta, Start fecha a ajuda
- [ ] Desconectar o cabo/Bluetooth andando: o lutador para na hora, "GAMEPAD DESCONECTADO · teclado ativo", teclado assume
- [ ] Reconectar segurando um botão: nada dispara até soltar; "GAMEPAD CONECTADO"
- [ ] Alt+Tab segurando o analógico: ao voltar, nada anda sozinho
- [ ] Dois controles com J1 e J2 em Gamepad: cada um controla um jogador; nunca os dois
- [ ] Treino com gamepad: Y/T/arrasto funcionam; parceiro passivo; F5 reinicia
- [ ] Fechar o jogo com o controle conectado: o processo termina sem travar

### Fundação posicional, animações e diagnóstico (Entrega 5E)

Nada novo deve aparecer no jogo; esta parte confirma que **nada mudou**:
- [ ] Os Dois e Treino: andar, correr, agachar, pular, rolar, sprawl, deitar e **rastejar no chão** com a mesma aparência e o mesmo ritmo de antes
- [ ] PEGADA_1: 3 quadros, pose final, firmeza, T/U/Y, SHIFT+A/D (arrasto) e profundidade como na 5B
- [ ] Nenhuma posição de chão (guarda, cem quilos...) aparece nem é acionável
- [ ] Perfis personalizados: mesma aparência do personagem base
- [ ] Teclado configurável (5C) e escolha de dispositivo (5D) funcionando, sem joystick

Diagnóstico de joystick:
- [ ] Sem controle: `java -jar target/AventuraIrmaosMiau-1.0-SNAPSHOT.jar --diagnostico-gamepad 0` mostra "1. Biblioteca: OK", "2. Joysticks detectados pelo sistema: 0" e a conclusão "nenhum joystick chegou ao GLFW"
- [ ] Com o controle que falhou na 5D: anotar o que o item 2 mostra (nome, GUID, eixos/botões, "gamepad: SIM/NÃO") e a conclusão
- [ ] Se "gamepad: NÃO": criar um mapeamento SDL com o GUID (docs/CONTROLES.md) e repetir; o item 4 deve dizer "aceito pelo GLFW" e o aparelho virar "gamepad: SIM"
- [ ] Arquivo de mapeamento com lixo: o diagnóstico lista as linhas ignoradas e o jogo abre normalmente

### Remapeamento do gamepad e comandos contextuais (Entrega 5F) — Xbox

Remapeamento:
- [ ] CONTROLES → Editar: Gamepad mostra os botões do Clássico (correr = RB/R1)
- [ ] Clicar em "Correr" e apertar outro botão (ex.: R3): pergunta se troca; "Sim" troca com "Deitar"
- [ ] Clicar e apertar LT ou RT só um pouco: nada; apertar até o fim: aceito
- [ ] Clicar e segurar um botão desde antes: ignorado até soltar e apertar de novo
- [ ] ESC e Start cancelam; Backspace deixa sem botão (tirar o RB de uso)
- [ ] Durante a captura, ESPAÇO/ENTER e botões do controle não acionam a tela
- [ ] SALVAR, fechar e abrir o jogo: os botões continuam; RESTAURAR PADRÕES volta ao Clássico
- [ ] J2 tem os próprios botões e o próprio esquema

Esquema Contextual (Dispositivo = Gamepad, Esquema = Contextual):
- [ ] O D-pad não anda; o analógico anda e troca de linha; B agacha; RB não faz falta
- [ ] Livre, perto do Preto: D-pad ↑ faz a PEGADA_1 (3 quadros, custo 3)
- [ ] Na pegada: D-pad → desequilibra; D-pad ↑ reforça (uma vez por toque, mesmo segurando)
- [ ] Segurar LT: a cruz mostra "LT + D-pad" e QUEDA*; LT + ↑ só avisa "ainda não disponível", sem cair no chão
- [ ] LT + LB: a cruz mostra GUARDA*; nada de chão é ativado
- [ ] Apertar →, depois segurar LT/LB sem soltar a direção: nenhum golpe novo
- [ ] LB + analógico: arrasto como na 5B; LT + LB + analógico não arrasta
- [ ] J1 no teclado e J2 no Xbox contextual: J2 rompe com D-pad ↓; H/T/U/Y do teclado iguais
- [ ] Start pausa; D-pad na pausa não dispara nada ao voltar; Alt+Tab e desconectar também limpam
- [ ] Treino com o Xbox contextual: técnicas do J1 funcionam; parceiro passivo; F5 reinicia
- [ ] Voltar ao Clássico: D-pad anda de novo, sem cruz no HUD

### Animações JSON ativas e ergonomia do gamepad (Entrega 5G)

Animações (teclado ou Xbox, Os Dois e Treino):
- [ ] Deitar (F / L3) e rastejar para os dois lados: mesma aparência e ritmo de antes, sem "salto" ao começar e ao parar
- [ ] Rastejar e pausar (ESC/Start): a pose congela; ao voltar, continua
- [ ] Rastejar e mudar de linha (W/S): a profundidade e o desenho continuam certos
- [ ] Levantar enquanto rasteja: volta a ficar em pé normalmente
- [ ] Caminhada, corrida, pulo, agachar, rolamento e sprawl iguais à 5F
- [ ] PEGADA_1 (3 quadros, pose final), T/U/Y, firmeza e arrasto iguais à 5F
- [ ] Treino: F5 durante o rastejar reinicia normalmente
- [ ] O console do NetBeans não mostra mensagens de `[animacoes]` (os JSON estão no JAR/recursos)

Gamepad (Xbox):
- [ ] A = pulo normal; RB + A = pulo alto (mais alto, mais fôlego); segurar não repete
- [ ] A primeiro e depois RB: só o pulo normal
- [ ] R3 com o analógico inclinado = rolamento; L3 = deitar/levantar (Clássico e Contextual)
- [ ] CONTROLES → Editar: Gamepad → grupo "PULO ALTO": trocar o modificador por outro botão livre (ex.: Y no Contextual) e testar
- [ ] Escolher LT como modificador no Contextual: pergunta se troca com a camada 2
- [ ] Com um gamepad.json da 5F nunca personalizado: aviso de "padrões atualizados" ao abrir; personalizado: aviso de "mantidos"

### Analógico direito e arrasto automático (Entrega 5H) — Xbox no Contextual

- [ ] Andar com o analógico esquerdo e, perto do Preto, inclinar o direito para cima: PEGADA_1 sem soltar o esquerdo
- [ ] Na pegada: RS → desequilibra; RS ↑ reforça; segurar inclinado não repete; girar sem voltar ao centro não dispara outra
- [ ] Inclinar o RS de leve: nada; na diagonal exata: nada até assentar num lado
- [ ] Segurar LT: a cruz mostra "LT + RS" (QUEDA*); LB sozinho: "LB + RS" (GUARDA*); LT + LB: "LT + LB + RS", vazia
- [ ] D-pad continua funcionando como antes
- [ ] Clicar R3 com o analógico esquerdo inclinado: rola para esse lado e nenhuma técnica dispara
- [ ] Na pegada, só o analógico esquerdo para os lados: a dupla anda junta, devagar, até a borda do dojo, sem LB
- [ ] Na pegada, analógico esquerdo para cima/baixo: rompe (como antes)
- [ ] J2 no Xbox como receptor: andar rompe a pegada e não leva o J1
- [ ] Clássico: tudo igual à 5G (LB + analógico arrasta; andar sem LB rompe; RS não faz nada)
- [ ] Botões personalizados continuam; um gamepad.json da 5G não personalizado mostra aviso de padrões atualizados

### Branco e Preto jogáveis (Entrega 5I)

- [ ] Seleção: J1 = Preto e J2 = Branco (seletor IRMÃO); a partida abre com o Preto à esquerda nas teclas WASD
- [ ] J2 (setas) chega perto e aperta END: o Preto aplica a PEGADA_1 no Branco; as poses ficam encaixadas, espelhadas da pegada do J1
- [ ] J2 aplicador: PAGE UP reforça, PAGE DOWN desequilibra, DELETE + setas arrasta a dupla devagar
- [ ] J1 receptor: 5 rompe; T/Y não fazem nada além do aviso; SHIFT + A/D não arrasta (andar rompe)
- [ ] H e END no mesmo instante: só o J1 pega
- [ ] Treino: escolher JOGADOR = Miau Preto; o Branco vira parceiro e fica parado; WASD + H pega, Y reforça, SHIFT + D arrasta; F5 mantém o Preto
- [ ] Xbox Clássico no J2: X pega, Y reforça, B desequilibra (aplicador) / rompe (receptor), LB + direção arrasta
- [ ] Xbox Contextual no J2: RS ↑ pega; analógico esquerdo arrasta sozinho quando aplicador
- [ ] Menus só com o Xbox: D-pad/analógico movem a borda amarela, A confirma, B volta; nenhum A "vaza" para a tela seguinte
- [ ] Pausa pelo Start: foco em CONTINUAR; A continua sem pular; B também continua
- [ ] Teclado nos menus: setas, ENTER, ESC volta (na partida o ESC segue pausando)

### De joelhos e variantes (Entrega 5J)

- [ ] Seleção: INÍCIO vem "De joelhos"; OS DOIS MIAUS: os dois ajoelhados, perto, frente a frente, sem pegada
- [ ] J2 (END) pega de joelhos: os dois continuam de joelhos durante toda a entrada e na pegada mantida
- [ ] Preto aplicando (J2 da direita): a pegada usa o desenho novo do Preto; F3 mostra "pegada_1_entrada_preto (autoral)"
- [ ] Preto aplicando da esquerda (J1 = Preto): a mesma pegada, espelhada, sem trocar mãos nem personagens
- [ ] Branco aplicando no Preto: a PEGADA_1 original de sempre
- [ ] Branco × Branco e Preto × Preto: F3 mostra "composta por papel"; nada atravessa o outro
- [ ] Arrastando (LB/SHIFT/DELETE ou automático no Contextual): pernas dos dois dão passinhos; param quando a dupla para
- [ ] Desequilíbrio com sucesso: o receptor inclina e volta; tentativa inválida ou resistida não mexe nada
- [ ] De joelhos: andar é bem lento e não levanta; F/L3 levanta; pulo/rolar/sprawl não fazem nada
- [ ] Treino de joelhos controlando o Preto; F5 mantém de joelhos
- [ ] INÍCIO "Em pé": tudo como na 5I

### Posturas, fôlego e pegada experimental (Entrega 5K)

- [ ] Em pé, F (ou L3) uma vez: depois de ~¼ s ajoelha; de novo: deita; de novo: ajoelha; de novo: levanta
- [ ] Em pé, F F rápido: deita direto (sem passar por joelhos); deitado, F F: levanta com impulso
- [ ] De joelhos (início da luta), L3 L3: levanta rápido
- [ ] Segurar F/L3: só uma mudança
- [ ] J1 no teclado e J2 no Xbox mudando de postura ao mesmo tempo, sem interferência
- [ ] Na pegada, F/L3 não muda a postura (aviso) — nem aplicador nem receptor
- [ ] F3: postura (sobe/desce), clique pendente, transição, fôlego e último custo mudam como esperado
- [ ] Andar ~10 s: o fôlego cai ~1; parado volta a subir; de joelhos gasta menos
- [ ] Gastar o fôlego e tentar F F: "SEM FÔLEGO" e nada muda; esperar parado e tentar de novo
- [ ] Arrastar (J1 e J2, Clássico e Contextual): só o aplicador gasta; dupla parada não gasta; passinhos continuam
- [ ] Desequilíbrio com sucesso: F3 mostra "DESEQUILIBRADO"; receptor lento por ~2 s; reação visual
- [ ] F6: "EXPERIMENTAL"; nova pegada em pé e de joelhos, Branco e Preto aplicando, dos dois lados; F6 volta à autoral
- [ ] Pausar com clique pendente / com o A segurado: nada muda ao voltar

### Defesa e quatro pegadas (Entrega 5L)

- [ ] Pegada estabelecida: o receptor anda, muda de linha, pula, rola, agacha, faz sprawl, usa L3 — nada solta, ele não sai do lugar
- [ ] Receptor forçando: F3 mostra "RECEPTOR FORCANDO"; a firmeza cai mais rápido; o aplicador reforça; o receptor perde fôlego
- [ ] ROMPER (↓ no RS ou U/5/B): resistido = puxão e a pegada fica; com firmeza baixa ou aplicador cansado, rompe (recuo)
- [ ] De joelhos, J2 aperta agachar e logo depois J1 tenta pegar: "EVITADA"; segurando agachar por muito tempo antes, a pegada entra
- [ ] RS ↑ → ↓ ← (livre) nos dois esquemas: Gola e manga / Cruzada / Duas mangas / Gola e nuca (F3: "PEGADA: …")
- [ ] De joelhos no início: 2 e 4 só depois de chegar mais perto
- [ ] Teclado: 6/7/8 (J1) e NUM 7/8/9 (J2)
- [ ] Com pegada, o RS volta a reforçar/desequilibrar/romper (Contextual); no Clássico o RS não faz nada
- [ ] As 4 pegadas com Branco e Preto aplicando, dos dois lados, Branco×Branco e Preto×Preto, em pé e de joelhos
- [ ] Deitado rastejando: fuga de quadril (joelhos dobram, quadril sai); parar volta ao deitado; F6 volta ao movimento antigo
- [ ] F6: PEGADA_1 autoral ↔ proposta; o jogo abre na proposta

### HUD do J2 e balanceamento (Entrega 5L.1)

- [ ] J1 no Xbox e J2 no teclado: cruz "J2 · RS" à direita com PEGAR/CRUZADA/MANGAS/NUCA; na pegada, as ações do papel
- [ ] Preto aplicando no Branco: desequilibrar funciona em poucas tentativas (antes nunca)
- [ ] Preto receptor do Branco: romper funciona em poucas tentativas, não na primeira
- [ ] Pegada parada dura ~27 s; reforço dá +30 por 2 de fôlego; o fôlego sobra para testar as técnicas

### Título, menu e seleção (Entrega 5M)

- [ ] O jogo abre na tela de título: pátio do dojo, letreiro descendo, Branco e Preto se olhando (idle)
- [ ] ENTER, ESPAÇO, A ou START vão ao menu uma vez só; segurar A/START ao abrir não pula a tela
- [ ] Menu: JOGAR, TREINO, PERSONAGENS, CONFIGURAÇÕES, SAIR (setas/D-pad, ENTER/A); não há "1 JOGADOR"
- [ ] TREINO, PERSONAGENS e CONFIGURAÇÕES abrem as telas de antes; SAIR pergunta e fecha
- [ ] JOGAR → grade: J1 (azul, WASD + ESPAÇO/H, Q cancela) e J2 (vermelho, setas + ENTER/END, BACKSPACE) ao mesmo tempo
- [ ] Com gamepads associados (Configurações → dispositivos), cada controle move o seu cursor
- [ ] Espaços com cadeado: "BLOQUEADO", não confirmam e não revelam nada
- [ ] Rostos finos em todas as telas (5M.1): bigodinho em traço fino, nunca "bolinhas"
- [ ] Piscar (5M.2): só os olhos mudam; a boca não engrossa com os olhos fechados (menus e luta)
- [ ] Prévia, "MIAU BRANCO"/"MIAU PRETO", "Estilo: Jiu-Jitsu" e barras de atributos reais; perfis salvos aparecem como "< Perfil >"
- [ ] Mesmo personagem nos dois lados funciona
- [ ] J1 pronto: MAPA (Dojo/Original) e TEMPO (SEM LIMITE/60/90/180); DIFICULDADE "EM BREVE" e pulada; LUTAR espera o J2
- [ ] A luta usa o mapa escolhido e começa com os dois em pé
- [ ] Relógio no topo; ESC pausa e o relógio para; ao zerar: FIM "TEMPO ESGOTADO" + vantagem/empate
- [ ] TENTAR NOVAMENTE repete mapa e tempo; SELECIONAR PERSONAGENS volta à grade
- [ ] Treino, F5, F3, F6, as quatro pegadas e os dois esquemas do Xbox continuam iguais

### Poses fixas, treino na grade e tipografia (Entrega 5M.3)

- [ ] Abertura, menu e prévias da seleção: gatos parados na postura neutra (sem guarda balançando nem taunt), só piscam
- [ ] Na luta, depois de ~7 s parado, o taunt continua aparecendo; o idle de combate também
- [ ] TREINO → grade "TREINO: ESCOLHA SEU LUTADOR": um cursor só (WASD ou setas, qualquer gamepad)
- [ ] Painel da direita = PARCEIRO (passivo); escolher o Preto deixa o Branco de parceiro
- [ ] Linhas MAPA / PARCEIRO / INÍCIO; sem TEMPO nem DIFICULDADE; TREINAR! não espera o J2
- [ ] O treino abre no mapa e no início escolhidos; F5, pausa, TENTAR NOVAMENTE e SELECIONAR PERSONAGENS (volta à grade de treino)
- [ ] Configurações, Controles e Personagens personalizados: título em letreiro amarelo, textos sem corte, acentos ok

### Receptor e arrasto multidirecional (Entrega 5N)

- [ ] As 4 pegadas, Branco aplicando e Preto aplicando: o receptor muda de pose de acordo com a técnica (P1 braço da manga, P2 giro, P3 dois braços, P4 cabeça/tronco)
- [ ] A reação começa quando a mão chega (não antes) e fica enquanto a pegada existir; nada de guarda nem taunt por cima
- [ ] Branco × Branco e Preto × Preto; J1 e J2 aplicando; dos dois lados da tela; em pé e de joelhos (ninguém levanta)
- [ ] Resistir, ROMPER resistido, reforço e desequilíbrio aparecem no corpo; ROMPER com sucesso e soltar = liberação suave
- [ ] SHIFT + ↑/↓ (e diagonais) leva a dupla de linha junta; a pegada continua; linha 1 e 5 bloqueiam
- [ ] Bordas esquerda/direita: ninguém passa, ninguém fica para trás; na diagonal desliza pela borda
- [ ] Xbox Contextual: analógico em qualquer direção conduz; Clássico: LB + analógico
- [ ] F3: "RECEPTOR: J2 ... SUSTENTADA" e "ARRASTO: FRENTE OK / BLOQUEADO"; F6 volta ao autoral (receptor parado como antes)

### Defesas, solo, arena e acrobacias (Entrega 5O)

- [ ] Sem modificador, RS (ou H/6/7/8) = as 4 pegadas como antes
- [ ] RT (ou 3 / INSERT): o HUD vira "DEFESAS"; RS ↑ guarda alta, ← média, → baixa (seguradas, respirando), ↓ sprawl
- [ ] Trocar de guarda e soltar: transição suave; com RT nada pega
- [ ] RB (ou 4 / HOME): HUD "QUEDAS: EM BREVE"; nada executa; RT + RB = defesas
- [ ] R3 + direção rola; RB + A pulo alto; B agacha (Contextual); corrida igual
- [ ] F7 alterna rolamento/sprawl ORIGINAL × EXPERIMENTAL 5O (8 direções); F6 continua só das pegadas
- [ ] Alguém deitado: as pegadas não saem (HUD "SOLO: EM BREVE"); em pé e de joelhos continuam
- [ ] Ninguém sai da arena andando, correndo, pulando, rolando ou na acrobacia; nem quando a pegada acaba na borda
- [ ] Y (9 / NUM 5) + direção: estrela e mortais; não atravessa o adversário; não sai de pegada; recarga
- [ ] F3: FAMILIA, RB fis/log, DEFESA, RECUSA, MOV (F7)

### MiauStudio (`EditorDePoses` — Run File)

- [ ] Abre normalmente; carregar `src/main/resources/conteudo/miaustudio/PEGADA_1.json` e conferir a prévia dos 3 quadros
- [ ] **Não salvar por cima** do arquivo de `resources` ao testar (salvar como outro nome); se salvar de propósito, rodar `mvn test` — o teste de SHA-256 vai acusar a mudança

### Conteúdo (Entrega 2)

- [ ] `git status` mostra `src/main/resources/conteudo/miaustudio/PEGADA_1.json` sem modificação
- [ ] Os testes `ConversorMiauStudioTest` e `ConteudoNoSistemaCombateTest` passaram

## 3. Arte e animações

- [ ] `git diff` não contém alterações em arrays `*_MIAUSTUDIO`, renderers do Branco/Preto ou `RegistroAnimacoesCombate`
- [ ] `Pegada1RegressaoArraysTest` passou (protege os arrays aprovados da PEGADA_1)

### R3 contextual, RT respira, Select = RB, teclado-controle do J2 e guarda andando (Entrega 5O.1)
- [ ] Em pé: R3 + direção = estrela (fundo/frente) ou mortal (frente/trás); Y + direção não faz mais nada
- [ ] De joelhos: R3 + direção rola nas 8 direções, perto do tatame, e continua de joelhos
- [ ] Deitado: R3 + direção rola mais devagar e continua deitado
- [ ] F7 ORIGINAL × EXPERIMENTAL: rolamento (bola baixa) e sprawl (prancha) só mudam no experimental
- [ ] RT sozinho respira; RT + RS = guardas/sprawl (sem respirar); RB e Select + A = pulo alto; Start pausa
- [ ] Guarda alta/média/baixa + LS anda a 50%, sem correr, sem cancelar a guarda; trocar de guarda andando
- [ ] J2 no teclado: WASD, setas (RS), K/L/J/I, Z/M, T/H/G/F, O/P/E/Q, ENTER, SHIFT
- [ ] J1 no teclado: NUM 8/4/5/6, NUM 0 correr, 1 deitar, 2 pegada 1, 0 desequilibrar, CTRL arrasto, - sprawl
- [ ] F3 mostra RB físico, Select físico e RB lógico dos dois jogadores

### Contextual padrão, J2 rompendo/arrastando e respiração por postura (Entrega 5O.2)
- [ ] J1 agarra o J2; o J2 rompe com a seta ↓ (teclado) / RS ↓ (Xbox)
- [ ] J2 agarra o J1 e arrasta com WASD (4 direções e diagonais), sem sair da arena
- [ ] J2 no Contextual: L agacha, J corre (não pega), D-pad não anda
- [ ] RT respira em pé, de joelhos e deitado (sem mudar a postura); andar respirando é mais lento embaixo
- [ ] F3: `ESQUEMA J1 CONTEXTUAL J2 CONTEXTUAL`

### Fôlego na pegada (Entrega 5O.3)
- [ ] J1 agarrando o J2 (e o J2 agarrando o J1): ninguém recupera fôlego, nem segurando o RT
- [ ] Soltou a pegada: parado e respirando, o fôlego volta a subir

### Arenas novas (Entrega 5O.4)
- [ ] A seleção de partida/treino e CONFIGURAÇÕES listam os 4 mapas; o F4 passa pelos quatro
- [ ] Campo aberto remasterizado e Arena do Grande Torneio: pés nas faixas do chão nas 5 linhas, bordas da arena
- [ ] Campo original e Dojo iguais aos de antes

### Quedas e solo (Entrega 5P)
- [ ] J1 agarra e derruba (RB + →) segurando o LS para o J2: aterrissa por cima; sem o LS: fica em pé
- [ ] J2 reverte a queda com RB + → na janela; J2 com RT segurado faz base (os dois em pé)
- [ ] Entrada nas pernas (RB + ↓) parada pelo sprawl (RT + ↓); com base sem sprawl: solo disputado
- [ ] Puxar guarda (RB + ←); recuar com o LS recusa a guarda
- [ ] Ao cair: ↑ fecha a guarda, ←/→ pega a meia, nada = cem quilos
- [ ] Guarda: abrir → raspagem na janela; passar → cem quilos → montar → escape de quadril na janela
- [ ] Montada: estabilizar segura a upa; montada alta deixa a upa entrar; costas → girar
- [ ] Ligados no solo ninguém anda/rola/muda de postura; F5 e luta nova soltam tudo; F3 mostra a linha SOLO

### Refinamento do solo (Entrega 5P.1)
- [ ] J1 deita sozinho (e o J2 também): HUD "SOLO LIVRE", sem "EM BREVE"; de longe nada acontece, perto dá para entrar na guarda
- [ ] Levantar depois de deitar por vontade é rápido; depois de derrubado é mais lento
- [ ] Na pegada e no solo, segurar o RT recupera um pouco; parado sem RT não recupera
- [ ] Respirando por cima o controle afrouxa (o escape do outro fica mais fácil); sem ação do outro nada muda
- [ ] RT segurado de antes não vira base; apertar o RT na entrada do outro faz base
- [ ] Ao cair dá tempo de escolher ↑ fechar a guarda / ←/→ meia (cruz "CAINDO")
- [ ] Logo depois de uma queda ou raspagem os comandos do solo respondem
- [ ] Uma sequência queda → passar → montada → escape não esgota o Branco
- [ ] Aviso curto no topo só com resultados (derrubou, raspou, passou, escapou), ~2 s, sem cobrir relógio e fôlego
- [ ] Pausa → REINICIAR LUTA: mesma luta do zero, mais de uma vez; F5 do treino igual

### Famílias e solo (Entrega 5P.2)
- [ ] Com a pegada (J1 aplicando): RS ← tiro de guarda fecha a guarda; com o RT do J2 na janela, J1 cai sem guarda
- [ ] RS ↓ derrubada forçada: a primeira desequilibra, a segunda derruba; com RT do J2, J1 fica exposto
- [ ] Reforço, desequilíbrio, romper e arrasto continuam
- [ ] LB (E) + RS em pé: guardas; LT (Q) + RS: quedas (quadril/manga cinzas sem a pegada certa); LT+LB: nada em pé
- [ ] Solo livre: em pé ←/→ prende as pernas, R1 ↓ joelho direto só depois; no chão L1 ←/→ borboleta / De La Riva
- [ ] De La Riva: J1 por cima em pé; L1+L2 ↑ berimbolo com controle; laço só depois de pegar a manga (L2 ↓)
- [ ] RB na guarda fechada por baixo: DESEQ. / ÂNGULO / AFASTAR; R1 ↓ por cima levanta com espaço; dominando, R1 ↓ solta
- [ ] Depois de afastar/soltar, o de baixo levanta e os dois andam livres
- [ ] Montada / 100 kg / costas / guarda fechada: LB, LT e LT+LB mostram finalizações com `*` e não executam
- [ ] Condicionamento cai devagar sob a montada e nas quedas; parado/deitado livre não cai; exausto: aviso no topo, a luta continua

### Mapas novos (Entrega 5P.3)
- [ ] Seleção de luta e de treino: depois de "Arena do Grande Torneio" vêm "Academia de jiu-jitsu" e "Fortaleza nevada ao luar"
- [ ] F4 passa pelos seis mapas e volta ao Dojo
- [ ] Academia e fortaleza: pés nas faixas do chão nas 5 linhas; nada cobre os lutadores; HUD legível
- [ ] Fortaleza: neve no chão, lua, lanternas acesas; montes de neve só nos cantos de baixo
- [ ] Dojo, campo original, campo remasterizado e torneio iguais aos de antes

### Mapas novos (Entrega 5P.4)
- [ ] Seleção, treino, configurações e F4: oito mapas; depois da fortaleza vêm "Academia dos Irmãos Miau" e "Campeonato Miau"
- [ ] Os dois: pés nas faixas do tatame nas 5 linhas; nada cobre os lutadores; placar do campeonato só decorativo

### Finalizações (Entrega 5Q)
- [ ] Guarda fechada por baixo: LB ↑ armlock → o outro defende (↓) → escapa (RB ↑) com pressão baixa → guarda aberta
- [ ] Montada: LB → americana; sem defesa a luta termina ("J1 VENCEU POR FINALIZAÇÃO · AMERICANA")
- [ ] Costas: LT ↑ mata-leão; quem defende bate com RB ↓ (J1 teclado: 4 + 7) e a luta termina "(BATEU)"
- [ ] DLR: LT+LB ↑ ainda é o berimbolo; LT+LB ↓ entra na perna; no ashi, LT+LB ↑ chave de pé reta
- [ ] Montada com controle baixo: LT+LB → perde a montada (o outro recupera a guarda)
- [ ] Aranha (pegar a manga com LT ↓, depois LT ←), laço (LB ↓ da aranha), single-leg X (LT → da borboleta)
- [ ] HUD: "DEFENDA: X 45%" / "ATACANDO X 62%"; com graduação baixa a célula mostra "·Az"
- [ ] Empurrão: quem empurra inclina para frente, quem recebe recua o tronco; afastar e levantar têm pose própria
- [ ] Treino: finalizar só avisa; REINICIAR LUTA depois da finalização volta tudo ao zero

### Graduação, kimonos e arenas vivas (Entrega 5Q.1)
- [ ] Seleção (partida): J1 escolhe FAIXA e KIMONO subindo da linha LUTAR; J2 usa ↑/↓ e ←/→; prévia e atributos mudam
- [ ] Dois Miaus Brancos com faixas/kimonos diferentes; faixa preta em kimono preto e branca em kimono branco continuam visíveis
- [ ] Faixa acompanha a cintura em pé, agachado, de joelhos, deitado, nas quedas e no solo
- [ ] HUD mostra o fôlego da faixa (ex.: Branco preta 47/47); REINICIAR LUTA mantém faixa e kimono
- [ ] Treino: faixa/kimono do jogador e do parceiro
- [ ] Faixa branca: omoplata (azul) bloqueada ("·Az"); faixa azul libera
- [ ] Fortaleza: neve caindo; Campeonato: bandeirinhas e torcida; Academia dos Irmãos Miau: árvore balançando

### Laboratório do treino (Entrega 5Q.2)
- [ ] Pausa do treino → LABORATÓRIO → situação/controla/J2 → INICIAR: começa direto na posição, com HUD técnico
- [ ] F5 repete a situação (posição, papéis, fôlego); F8 mostra o catálogo com LIBERADA / FAIXA X / AGORA NÃO
- [ ] J2 CONTROLADO responde no teclado virtual/gamepad; PASSIVO continua parado
- [ ] Partida normal sem o botão LABORATÓRIO e com o F5 de sempre
- Checklist físico completo da 5Q/5Q.1 em `docs/LABORATORIO.md`

### Animações (Entrega 5Q.3)
- [ ] Empurrão: quem empurra carrega e estende os braços; quem recebe recua o tronco, dá um passo e desliza (sem pular)
- [ ] Guarda fechada (laboratório): RB ↓ afastar — pernas recolhem e estendem; quem estava por cima recua o tronco
- [ ] Armlock da guarda: perna sobe por cima, braço preso; defesa (↓) mostra a reação; escape volta suave
- [ ] Americana da montada e mata-leão das costas: entrada, disputa (intensidade cresce com a pressão), defesa, saída
- [ ] Berimbolo: inversão de cabeça para baixo e subida pelas costas; com J1 e com J2, Branco e Preto

### Consolidação e Modo Desenvolvedor (Entrega 5Q.4) — `entities` alterado com autorização (contornos e recursos do modo)
- [ ] Seleção da partida: escolher o personagem leva o J1 para FAIXA; A segue FAIXA → KIMONO → LUTAR; o J2 não muda junto
- [ ] Seleção do treino: A segue também pela FAIXA/KIMONO DO PARCEIRO; mouse muda faixa/kimono de cada lado e clica em LUTAR
- [ ] Faixa e kimono escolhidos aparecem na luta e continuam depois do F5 / REINICIAR LUTA
- [ ] Desequilibrar pela pegada e logo forçar a queda (v / LB ↑): cai; com RT do outro na janela: "defendeu com base (RT)"
- [ ] Rasteira contra quem está parado e com base cheia: "a queda não entrou (base firme de …)"; andando ou desequilibrado: entra
- [ ] De joelhos (início de joelhos **e** depois de uma queda/troca de postura): pegada, empurrão, desequilibrar, puxar guarda
  funcionam; tiro de guarda e entrada nas pernas mostram "SÓ EM PÉ (DE JOELHOS NÃO)"
- [ ] 100 kg / meia: as recusas de escape mostram o motivo com o valor do controle
- [ ] Menu principal → MODO DESENVOLVEDOR: J1 (Xbox) e J2 (teclado virtual) lutam; selo vermelho no canto; fôlego e
  condicionamento parados no máximo normal (ex.: 47/47)
- [ ] Pausa do Modo Desenvolvedor: FÔLEGO INFINITO / CONDICIONAMENTO INFINITO alternam (o selo muda); RESTAURAR RECURSOS
- [ ] F4 troca o cenário; F5 recomeça a luta no cenário escolhido com faixas/kimonos mantidos
- [ ] JOGAR e TREINO continuam gastando fôlego normalmente; o laboratório continua no treino
- [ ] Contornos discretos: Branco de kimono branco, Preto de kimono preto e kimono azul — mão, manga, tronco e perna
  separados; faixa igual à aprovada
- [ ] Finalizações provisórias (estrangulamentos, braço): braços sem esticar
- [ ] Guardas no laboratório: fechada, aberta, meia, borboleta, DLR, laço, aranha, SLX e ashi com pernas diferentes de
  quem está por baixo
- [ ] Quebrar postura (RS ↓ por baixo na fechada): o de cima curva e se recompõe; RS ↓ por cima (postura) recompõe na hora;
  raspagem contra postura: o de cima se apruma (diferente da quebra)
- [ ] Prender as pernas (em pé sobre quem está no chão), ajustar o ashi, reter com os pés: animados nos dois lados
- [ ] Configurações: sem "Cenário preferido"; SALVAR não apaga o cenário já salvo
- [ ] Menus: mouse num botão e controle em outro — só um destaque aceso
- [ ] Preservados: projeção de quadril, tiro de guarda, raspagens/passagens, joelho na barriga, montada/montada alta,
  pegada das costas, mata-leão, trocas de guarda, levantada técnica, rolamento técnico, estrelas/mortais, faixa


# Entrega 5Q.5 — Refinamento da jogabilidade e do combate

Automático: `ParametrosFinalizacao`, `Finalizacoes5Q5Test` (7), `Escapes5Q5Test` (4), `Quedas5Q5Test` (7),
`Movimento5Q5GamePanelTest` (6), `Cenarios5Q5Test` (4).

Manual (Xbox no J1, teclado virtual no J2; repetir com os papéis trocados):

1. **Finalizações**: entrar num armlock da guarda e cortar a entrada com RS ↓ (dá tempo?). Deixar chegar a 100%:
   aparece "BATA OU DEFENDA" e ainda dá para bater (RB + RS ↓) ou defender (RS ↓).
2. **Triângulo**: entrar com LT + RS → e, **ainda segurando o LT**, soltar com RS ↓ (deve soltar).
3. **Bater**: segurar RS ↓ (defendendo) e então apertar RB (deve bater na hora).
4. **Guilhotina**: quebrar a postura (RS ↓ por baixo) e entrar na guilhotina (LT + RS ↓); quem está por cima não
   consegue refazer a postura logo em seguida ("POSTURA QUEBRADA").
5. **100 kg**: RS ↓ (enquadrar) e logo RS ←/→ (recuperar a guarda). Quem está por cima apertando RS ↓ só "fecha
   o espaço". Montada e joelho na barriga: o mesmo.
6. **Meia-guarda**: RS ←/→ (underhook) e depois RS ↓ (recuperar a guarda).
7. **Rasteira**: com a pegada, o J2 dá um passo e o J1 faz a rasteira (L2 ↑): cai, com animação dos dois. Parado
   com base cheia: não cai.
8. **Entrada nas pernas** (RB ↓ em pé) e **joelho na barriga direto** (RB ↓ em pé contra quem está deitado):
   animações dos dois; o joelho entra com J1 e com J2 por cima.
9. **Recuo em pé** (RS ↓ no solo livre): dois passos para trás, sem salto.
10. **Levantada técnica** (RS ↑ deitado, depois de uma queda): senta, apoia a mão, passa a perna e levanta.
11. **De joelhos**: arrastar com a pegada (bem lento); andar de joelhos com **X** (mais rápido, sem levantar,
    gasta fôlego). **Deitado com X**: impulsos de quadril.
12. **Prender as pernas**: as mãos ficam nas canelas enquanto o controle dura.
13. **Postura**: RS ↓ por cima na guarda fechada (apruma e segura); depois de quebrada, recompõe.
14. **Afastar da guarda** (RB ↓ por baixo): quem estava por cima recua até ficar fora do alcance.
15. **Ordem de desenho**: na guarda as pernas de quem está por baixo aparecem na frente; na montada/100 kg quem
    está por cima fica na frente, com J1 ou J2 em cada papel.
16. **Rodas**: nenhuma roda vazia aparece; avisos "▶ … EM EXECUÇÃO" e "✕ … motivo".
17. **Cenários**: nuvens andando devagar na Academia (externa), Campo Verde e Grande Torneio; prédio da Academia
    apoiado no chão; Campeonato com mais torcida viva e flashes discretos; neve da Fortaleza preservada.

# Entrega 5Q.6 — combate, repertório, interface e trilha sonora

Checklist da homologação física (Xbox no J1, teclado virtual no J2):

1. Rasteira: parado e com base cheia não entra; depois de **puxar** o outro com a pegada, entra.
2. Rasteira defendida 3× seguidas: o receptor balança e volta à postura da pegada (sem ficar torto).
3. Em qualquer finalização, **RS ←** (o item BATER da roda) termina a luta na hora — J1 e J2.
4. RB + RS ↓, e LT/LB ainda segurados + RS ←, também batem.
5. Defender (↓) e logo escapar (↑ — "ESCAPAR (ESPAÇO)") com pressão média.
6. Soltar o triângulo com o LT segurado (RS ↓).
7. LT em pé sem pegada: single leg, high crotch, carregada, ankle pick; o sprawl para as três primeiras.
8. De joelhos, LT: single leg, double leg, snap down, arm drag (sem levantar).
9. RB → sem pegada: body lock; com pegada: a queda de sempre.
10. Com pegada, LB + LT: uchi-mata, osoto, laço; aranha só com as mangas.
11. Ashi: LT e LB com as novas; LB + LT igual; toe hold com o nome novo; faixa azul vê "·Rx/·Mr".
12. De La Riva LB + LT: ↑ berimbolo, ← kiss of the dragon, → baby bolo, ↓ ashi.
13. Tiro de guarda (salto) x puxar guarda (senta) visivelmente diferentes.
14. Pressão, base, montada alta, ganchos, controlar pernas, ângulo, manga, desequilibrar com os pés: cada um mexe o corpo.
15. Cem quilos → costas (RB ↑): recusa "CONSOLIDE ANTES"; depois de consolidar, entra.
16. Guarda fechada: sem postura "FAÇA POSTURA ANTES"; guarda aberta sai rápido.
17. Afastar com os pés: quem está embaixo também escorrega para trás.
18. Levantar depois de uma queda: sobe aos poucos (sem aparecer de pé de repente).
19. X de joelhos para trás: tronco para trás. Rolamento de joelhos e sprawl um pouco mais pesados.
20. Cores das rodas (azul/verde/vermelho/âmbar, BATER com moldura) e bloqueadas apagadas.
21. F8 numa partida normal liga o Modo Desenvolvedor (selo, fôlego ∞, sem relógio); F8 de novo desliga e o relógio continua; o menu não tem mais a opção.
22. Música: título, menu, seleção, treino, laboratório, configurações, perfis, resultados; no combate a faixa troca ao terminar e não reinicia no F5/F4.
23. Colocar um WAV em `audio/musicas/combate/combate_01.wav` e ouvir sem recompilar.

# Entrega 5Q.7 — consolidação do combate, animações, mobilidade e interface

Checklist da homologação física (Xbox no J1, teclado virtual no J2; trocar os papéis):

1. Rasteira parada (não entra): olhar os braços do defensor **durante** a reação — nada esticado.
2. Rasteira segurada com RT, 3× seguidas sem romper a pegada: sem acumular deformação.
3. Forçar queda contra base cheia (desequilibra): reação do empurrão, braços inteiros.
4. Forçar queda segurada com RT: tronco cede um pouco e volta.
5. Rasteira e forçar queda que **entram**: iguais às aprovadas.
6. No chão contra quem está em pé: PÉS (RS ↓) escorrega um pouco para trás e a distância aumenta.
7. Em pé contra quem está no chão: RS ← PRENDER (igual) e RS → SEGURAR.
8. Segurando: andar arrasta devagar (o outro continua deitado); o fôlego cai.
9. A roda mostra SOLTAR no RS → enquanto segura; RS → solta.
10. Quem é segurado: debater-se (direção) por meio segundo rompe; PÉS rompe; levantar/puxar guarda rompem.
11. Arrastar até a borda: ninguém sai da arena.
12. Segurar de joelhos também funciona.
13. 100 kg por baixo: REC. GUARDA, VIRAR e ENQUADRAR mexem o corpo (sem trocar a posição se o controle estiver alto).
14. Montada por baixo: ESCAPAR e ENQUADRAR mexem o corpo.
15. Costas: GIRAR e DEFENDER mexem o corpo.
16. Rolamento técnico de joelhos visivelmente mais lento (mesma distância); deitado, em pé e sprawl iguais.
17. Faixa preta (Preto) descansado: estrelas e mortais quase seguidos; faixa branca cansado: espera maior.
18. Modo Desenvolvedor com fôlego/condicionamento infinitos: acrobacias encadeadas; desligar volta ao normal.
19. Mensagem no topo com a cor da natureza: queda âmbar, defesa azul, passagem verde, finalização vermelha, segurar creme, bater com moldura dourada.
20. A mensagem secundária ("▶ … EM EXECUÇÃO") continua neutra; a barra inferior não mudou.
21. Preservados: tiro de guarda, empurrão, levantada técnica, quedas explosivas, guardas aranha/laço, manga, berimbolos, rodas do ashi, F8, cenários e músicas.

# Entrega 5Q.7 corretiva — integridade corporal

Checklist da homologação física (Xbox no J1, teclado virtual no J2). Comparar com as capturas
enviadas (rasteira, forçar queda, arrasto) olhando os **quadros do meio**:

1. Rasteira que falha, J1 atacando: braços, tronco, cintura e pernas do defensor inteiros.
2. Rasteira que falha, J2 atacando.
3. FORÇAR QUEDA que não derruba, J1 e J2 atacando: a cintura não "quebra".
4. Rasteira segurada com RT e FORÇAR QUEDA segurado com RT: tronco cede, meio passo, volta. *(confirmar o RT)*
5. Três ou mais tentativas seguidas sem soltar a pegada: nada se acumula.
6. Alternar rasteira, empurrão e FORÇAR QUEDA rapidamente.
7. Quedas que entram: iguais às aprovadas.
8. Personagens bem próximos e com orientações trocadas.
9. Arrastar em pé para trás e para frente: o arrastado deitado reto; quem arrasta inclina e dá passos.
10. Arrastar de joelhos. *(confirmar)*
11. Arrastado resistindo (rompe) e soltura voluntária (RS →): volta suave. *(confirmar o rompimento)*
12. Arrastar até a borda da arena.
13. Guarda aberta por baixo: MANTER (RS ← / →) mexe pernas e quadril e volta; não troca de guarda.
14. MANTER como resposta à passagem.
15. Estrela e mortais mais rápidos; faixa preta ágil quase sem espera; apertar R3 um pouco antes do fim encadeia.
16. Modo Desenvolvedor / treino com recursos infinitos: acrobacias encadeadas fluidas.
17. Rolamento de joelhos (46 ticks), sprawl e rolamentos convencionais iguais.
18. Preservados: PÉS, microanimações defensivas, cores das mensagens, barra inferior, PRENDER no RS ←, músicas externas, itens da 5Q.6.
