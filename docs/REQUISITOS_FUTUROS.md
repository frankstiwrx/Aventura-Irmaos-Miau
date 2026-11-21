# Requisitos arquiteturais para as próximas entregas

Registrados durante a Entrega 3A, a pedido do criador. **Nada aqui está
implementado ainda.** Cada item traz o que já existe e a limitação atual.

Princípio: a **lógica** de uma técnica e a sua **animação** evoluem em ritmos
diferentes (o criador, Ivo e outro assistente produzem animações em lotes no
MiauStudio). O motor deve permitir implementar e testar a lógica antes da arte,
e encaixar a arte depois, sem mexer no núcleo do combate.

## R1 — Lógica testável sem animação

**Objetivo:** implementar e testar toda a lógica de uma técnica com a animação pendente.

- Já existe: `AcaoCombate` não depende de animação; os testes de motor rodam sem tela;
  técnica sem `animacao` no catálogo é válida.
- Limitação: hoje uma técnica `JOGAVEL` sem animação executa "invisível" (estado muda,
  ninguém muda de pose). Para uma pegada isso é visualmente enganoso no jogo normal.
- Direção: um visual provisório explícito (ex.: pose de marcação ou texto no HUD de debug)
  e a possibilidade de habilitar técnicas "só lógica" apenas em modo de testes/treino.

## R2 — Três níveis de prontidão

**Objetivo:** distinguir *lógica pronta*, *visualmente pronta* e *indisponível*.

- Já existe: `DisponibilidadeAcao { JOGAVEL, PENDENTE }`.
- Limitação: dois níveis apenas, e "pronta" mistura lógica e arte.
- Direção: separar dois eixos no catálogo, por exemplo
  `estadoLogica: PENDENTE | PRONTA` e `estadoVisual: PENDENTE | PROVISORIO | PRONTO`,
  derivando a disponibilidade no jogo (normal exige os dois prontos; treino aceita lógica pronta).
  Migração compatível: `JOGAVEL` = lógica e visual prontos; `PENDENTE` = nada pronto.

## R3 — Associar o JSON depois

**Objetivo:** ligar um projeto do MiauStudio a uma técnica já implementada.

- Já existe: técnica e animação se ligam só por ID (`tecnicas.json` → `animacoes.json` → arquivo).
  Associar = copiar o `.json`, adicionar uma entrada em `animacoes.json` e o campo `animacao` na técnica.
- Limitação: técnica jogável que aponta para animação ainda inexistente é **erro** de carregamento
  (correto hoje; com R2 deverá ser permitido quando `estadoVisual = PENDENTE`).
- Direção: relatório de catálogo (lista de técnicas sem animação, animações sem técnica).

## R4 — Reuso de animações entre personagens compatíveis

**Objetivo:** a mesma animação servir a qualquer par compatível, por papel (aplicador/receptor),
respeitando orientação, espelhamento e proporções.

- Já existe: o runtime trabalha com **papéis**, não com personagens: o conversor troca os tracks
  conforme `aplicadorConjunto`, e o `SistemaCombate` aplica o track do aplicador em quem ataca.
  O espelhamento é feito pelo `GamePanel` a partir da orientação; o motor usa apenas deltas de X.
  Teste existente: o personagem fictício (`CatalogoPersonagensTest`) executa a PEGADA_1.
- Limitações:
  1. O MiauStudio grava as poses com as chaves `branco`/`preto` e só edita esses dois personagens.
  2. O deslocamento horizontal da animação (`deslocamentoAplicadorX`) é aplicado em **pixels**,
     sem multiplicar pela escala do personagem; as articulações são multiplicadas pela escala.
     Personagens com escala diferente de 0,7 ficariam desalinhados.
  3. O valor `viradoDireita` (índice 3) é ignorado no jogo; a orientação vem da posição relativa.
     A autoria assume aplicador à esquerda olhando para a direita.
  4. Não existe declaração de compatibilidade (qual rig/proporção a animação espera).
- Direção: um "contrato de rig" versionado (`rig: MIAU_V1`) na animação e no personagem;
  converter deslocamentos para unidades do rig (como já se faz com o alcance) antes de reusar entre escalas.

## R5 — Adicionar ou substituir animações sem mexer no núcleo

- Já existe: substituir = trocar o arquivo apontado em `animacoes.json`; os testes golden protegem a
  PEGADA_1 contra troca acidental.
- Limitação: uma técnica tem uma única animação de entrada; não há variações (por personagem,
  por lado, por estado) nem animação de saída/rompimento.
- Direção: animações nomeadas por fase (`entrada`, `manutencao`, `rompimento`) e variações opcionais.

## R6 — Evolução visual do Branco e do Preto

**Objetivo:** melhorar quimonos, rostos, sombras e acabamento preservando as articulações
e a compatibilidade com as animações existentes.

- Já existe: o formato de 30 valores é o contrato entre animação e renderer; pivôs (cintura/peito)
  são métodos sobrescrevíveis em `IrmaoMiau`.
- Limitações:
  1. Cada personagem tem seu próprio renderer de poses (`desenharPoseMiauStudio`, centenas de linhas
     duplicadas entre `IrmaoMiauBranco` e `IrmaoMiauPreto`); um ajuste visual precisa ser feito duas vezes.
  2. A geometria do corpo é escrita em números dentro do código de desenho.
  3. **Não há teste de regressão visual**: hoje só os dados das poses são protegidos, não o desenho.
- Direção (nesta ordem, cada passo com aprovação):
  1. Criar testes de imagem: desenhar poses-chave (em pé, rolamento, sprawl, chão, PEGADA_1) em
     `BufferedImage` e comparar com imagens aprovadas.
  2. Só depois extrair um renderer de rig compartilhado, com "camadas" de acabamento
     (quimono, rosto, sombra) configuráveis por personagem, mantendo os 30 valores intactos.
  3. Melhorias de arte entram como camadas, sem mexer nas articulações.

## Outros pontos herdados

- `EstadoInteracaoCombate` ainda é enum: uma pegada nova exige editar o enum (migrar para ID).
- A `PoliticaInteracao` é única por `SistemaCombate`; técnicas futuras com movimentação conjunta
  vão precisar de política por técnica (campo no catálogo).
- Os requisitos de início da PEGADA_1 não verificam se o oponente está rolando, pulando ou deitado
  (mantidos como estavam por decisão desta entrega).

## Itens herdados do antigo ROADMAP.md (5Q.4)

O `ROADMAP.md` da raiz foi removido na entrega 5Q.4: estava desatualizado (marcava como planejado o
que já existe: guardas, finalizações com disputa, faixas, fôlego/condicionamento infinitos no Modo
Desenvolvedor, laboratório). O histórico completo continua no git. Ficam aqui só os itens que **ainda
não existem** no jogo, como ideias (nenhum é compromisso de entrega):

- **Combate em pé**: socos, chutes, interrupções, combinações e golpes característicos por lutador.
- **Grappling em pé**: clinch (entrada, saída e disputa) como estado próprio.
- **Chão**: posição de tartaruga.
- **Resolução**: pequena aleatoriedade controlada com RNG reproduzível por seed (e opção de configurar
  o RNG no laboratório); sistema genérico de efeitos de combate.
- **Atributos**: estilos de lutador e afinidades técnicas.
- **IA**: IA parada/defensiva/contextual/por estilo; controlador genérico (jogador 1, jogador 2, IA,
  boneco de treino).
- **Dados**: lutador completamente genérico carregado por dados; técnicas e catálogo externos (JSON).
- **Criador de personagens**: nome, peso, escala, cores, proporções, atributos, estilo, animações,
  técnicas, golpe característico; exportar/carregar a definição em JSON.
- **Progressão**: especialização por estilo; carreira/campanha.
- **MiauStudio**: biblioteca interna de animações organizada por categoria; integração com o criador.
- **Projeto**: screenshots/GIFs no README, releases jogáveis mais frequentes, issues para bugs e sugestões.

## Registrados na 5Q.5

- **Rig 2.0 — camadas por membro**: hoje cada lutador é desenhado inteiro. A ordem por posição (5Q.5) resolve a
  maior parte das guardas, mas uma perna por trás do outro e outra pela frente ao mesmo tempo (triângulo, laço)
  exige desenhar membros em camadas separadas.
- Poses próprias de quem está **por cima** nas guardas abertas (DLR, laço, aranha, SLX, ashi).
- Quadros próprios para enquadrar/escapar dos domínios e para consolidar/estabilizar.
- Propostas de simplificação de controles (bater por Select segurado, combo de escape sugerido no HUD, agrupar
  rodas quase vazias): ver `docs/CONTROLES.md` (5Q.5). Dependem de aprovação.

## Depois da 5Q.6

- Heel hook (interno/externo): só com um modo de regras próprio (sem kimono); hoje fora do catálogo.
- Poses próprias das novas finalizações de perna e do enquadrar/consolidar (hoje gesto curto/provisória).
- Transição suave (fade) entre músicas.
- Rig 2.0: camadas por membro (perna entre os braços na single leg, triângulo, laço, pernas
  em volta da cintura no tiro de guarda sem atravessar o tronco do outro).
- Trocar as músicas provisórias pelas próprias (arquivos externos já suportados).

## Depois da 5Q.7

- **Desgaste corporal localizado** (ideia registrada, não implementada): tentativas repetidas de
  finalização com pressão efetiva reduziriam aos poucos a tolerância daquela articulação ao longo
  da luta (ex.: armlocks seguidos no mesmo braço). Não muda nada no sistema atual de finalizações,
  pressão ou resistência.
- Animações individuais para as finalizações de perna (hoje parecidas entre si).
- Barra inferior de informações/controles: reformulação em uma entrega futura.
- Pontuação das lutas, regras competitivas, novas técnicas e novos personagens.
- Recalibrar com os testes físicos: `FATOR_ROLAMENTO_JOELHOS_5Q7`, recarga das acrobacias e o
  balanceamento do arrasto pelas pernas.

## Depois da 5Q.7 corretiva

- Desenho único para o deitado no solo livre (hoje: desenho legado parado, pose reta quando segurado).
- Recalibrar com os testes físicos: execução/recarga das acrobacias, janela do R3 guardado, esforço do arrasto.
