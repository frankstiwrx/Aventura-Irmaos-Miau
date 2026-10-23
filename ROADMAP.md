# 🐱 Aventura dos Irmãos Miau — Roadmap

> Roadmap vivo do projeto. Atualizado conforme novas mecânicas, ferramentas e etapas do motor de combate são concluídas.

## Legenda

- ✅ Concluído e funcional
- 🧪 Implementado / em validação
- 🔧 Em desenvolvimento
- ⬜ Planejado
- 💡 Ideia futura

---

# 📍 ESTADO ATUAL

O projeto já possui uma base jogável com dois personagens, movimentação completa, estados corporais, energia, animações próprias e o primeiro fluxo real do motor de combate.

## Motor de combate — progresso atual

- ✅ **5A — domínio base de combate**
  - `CategoriaAcao`
  - `ResultadoAcao`
  - `AcaoCombate`
  - `ContextoCombate`
- ✅ **5B — sistema e catálogo de ações**
  - `SistemaCombate`
  - `RegistroAcoes`
  - primeira ação cadastrada: `pegada_1`
- ✅ **5C — integração com o GamePanel**
  - motor atualizado junto ao game loop
  - leitura contínua do estado dos lutadores
- ✅ **6 — sistema espacial**
  - distância horizontal
  - distância em unidades de combate
  - diferença entre linhas
  - posição relativa
  - orientação dos personagens
  - verificação de frente para o oponente
- ✅ **7 — PEGADA_1 funcional**
  - input real
  - validação de alcance
  - validação de linhas
  - validação de orientação
  - custo de fôlego
  - estado compartilhado de interação
  - aplicador e receptor
  - encerramento da interação ao perder requisitos
- ✅ **8A — timeline compartilhada para animações de combate**
  - um único relógio para aplicador e receptor
- ✅ **8B — pose externa no IrmaoMiau**
  - pose temporária de combate
  - offsets visuais
  - bloqueio durante animação
- ✅ **8C — SistemaCombate controla a animação conjunta**
  - início da animação após sucesso
  - progresso compartilhado
  - bloqueio dos dois lutadores
  - liberação ao terminar
  - interação permanece ativa após a animação
- 🔧 **8D — renderizar a pose externa nos personagens**
- ⬜ **8E — testar animação conjunta provisória no jogo**
- ⬜ **8F — criar e integrar a animação real da PEGADA_1 no MiauStudio**

---

# 🏃 MOVIMENTO

- ✅ Movimento decimal
- ✅ Caminhada em múltiplas direções
- ✅ Sistema de 5 linhas
- ✅ Corrida
- ✅ Respiração
- ✅ Pulo alto
- ✅ Pulo curto
- ✅ Agachamento
- ✅ Rolamento em 8 direções
- ✅ Estado deitado
- ✅ Rastejamento / movimentação técnica no chão
- ✅ Sprawl
- ✅ Orientação automática em relação ao adversário
- ✅ Movimento com velocidades influenciadas pelos atributos
- ⬜ Revisão futura de colisões e limites entre lutadores

---

# ⚡ ENERGIA E CONDICIONAMENTO

- ✅ Fôlego individual
- ✅ Condicionamento individual
- ✅ Consumo de energia por corrida
- ✅ Consumo por pulo
- ✅ Consumo por rolamento
- ✅ Consumo por sprawl
- ✅ Recuperação natural
- ✅ Recuperação ao respirar
- ✅ Penalidade de movimento sem fôlego
- ✅ Custo de fôlego em ações do motor de combate
- ⬜ Custos específicos por técnica
- ⬜ Influência mais profunda da fadiga nas disputas

---

# 🛡️ DEFESA

- ⬜ Guarda alta
- ⬜ Guarda baixa
- ⬜ Esquiva por rolamento como ação defensiva contextual
- ⬜ Sprawl contra tentativa real de queda
- ⬜ Bloqueios
- ⬜ Defesas de pegada
- ⬜ Quebra de pegada
- ⬜ Contra-ataques
- ⬜ Reversões

---

# 👊 COMBATE EM PÉ

- ⬜ Empurrão
- 🔧 Agarrão / pegada inicial
  - ✅ lógica da `PEGADA_1`
  - 🔧 animação conjunta
- ⬜ Soco alto
- ⬜ Soco baixo
- ⬜ Chute alto
- ⬜ Chute baixo / rasteira
- ⬜ Interrupções
- ⬜ Combinações de golpes
- ⬜ Golpes característicos por lutador

---

# 🥋 GRAPPLING

## Estados iniciais

- ⬜ Clinch
- 🔧 Sistema de 4 pegadas
  - ✅ `PEGADA_1` — lógica funcional
  - ⬜ `PEGADA_2`
  - ⬜ `PEGADA_3`
  - ⬜ `PEGADA_4`

## Regras

- ✅ Pegada representada como estado compartilhado entre dois lutadores
- ✅ Aplicador e receptor identificados pelo motor
- ✅ Requisitos de distância
- ✅ Requisitos de linha
- ✅ Requisitos de orientação
- ✅ Custo de fôlego
- ⬜ Técnicas disponíveis de acordo com a pegada
- ⬜ Troca entre pegadas
- ⬜ Disputa de pegada
- ⬜ Quebra de pegada
- ⬜ Entrada em clinch
- ⬜ Saída de clinch

## Quedas

- ⬜ Single leg
- ⬜ Double leg
- ⬜ Projeções
- ⬜ Quedas dependentes da pegada
- ⬜ Defesa de queda
- ⬜ Sprawl contextual
- ⬜ Resultado parcial / desequilíbrio
- ⬜ Contra-quedas

---

# 🤼 CHÃO

## Movimento

- ✅ Estado deitado
- ✅ Movimento técnico no chão
- ✅ Animação própria de movimentação no chão

## Posições planejadas

- ⬜ Guarda
- ⬜ Meia-guarda
- ⬜ Controle lateral
- ⬜ Montada
- ⬜ Costas
- ⬜ Tartaruga

## Ações

- ⬜ Raspagens
- ⬜ Passagens de guarda
- ⬜ Transições
- ⬜ Controles
- ⬜ Escapes
- ⬜ Reversões
- ⬜ Finalizações

## Finalizações

- ⬜ Sistema progressivo de submissão
- ⬜ Progresso de finalização `0 → 100`
- ⬜ Disputa entre ataque e escape
- ⬜ Armbar
- ⬜ Triângulo
- ⬜ Estrangulamentos
- ⬜ Outras finalizações

---

# 🎲 RESOLUÇÃO DE COMBATE

- ✅ Requisitos determinísticos antes de uma ação poder ser tentada
- ✅ Ações cadastradas por ID
- ✅ Categorias de ação
- ✅ Dificuldade armazenada na ação
- ✅ Pesos de atributos armazenados por ação
- ⬜ Cálculo de qualidade da execução
- ⬜ Resposta do defensor
- ⬜ Sucesso parcial
- ⬜ Defesa
- ⬜ Contra-ataque
- ⬜ Pequena aleatoriedade controlada
- ⬜ RNG reproduzível por seed
- ⬜ Sistema genérico de efeitos

## Efeitos planejados

- ⬜ Empurrar
- ⬜ Derrubar
- ⬜ Desequilibrar
- ⬜ Perder fôlego
- ⬜ Perder condicionamento
- ⬜ Entrar em posição
- ⬜ Sair de posição
- ⬜ Iniciar submissão
- ⬜ Romper pegada

---

# 📊 ATRIBUTOS

## Base atual

- ✅ Condicionamento
- ✅ Fôlego
- ✅ Força
- ✅ Velocidade
- ✅ Agilidade
- ✅ Técnica
- ✅ Recuperação
- ✅ Peso

## Futuro

- ✅ Ações podem possuir pesos diferentes para cada atributo
- ⬜ Resolução real utilizando os pesos
- ⬜ Influência contextual do peso
- ⬜ Influência de fadiga
- ⬜ Estilos de lutador
- ⬜ Afinidades técnicas
- ⬜ Golpes característicos

---

# 🎯 SISTEMA DE DISPUTA / TIMING

- ⬜ `DisputaCombate`
- ⬜ Janela de timing influenciada por técnica
- ⬜ Agilidade influenciando controle / reação
- ⬜ Força influenciando progresso quando aplicável
- ⬜ Peso influenciando determinadas ações
- ⬜ Fadiga alterando desempenho
- ⬜ Resultado combinando atributos, habilidade do jogador, situação e pequena aleatoriedade

---

# 🎬 ANIMAÇÕES DE COMBATE

- ✅ Sistema de animações individuais
- ✅ Interpolação entre poses
- ✅ Animações com quantidade variável de quadros no MiauStudio
- ✅ Timeline conjunta no MiauStudio
- ✅ Aplicador e receptor
- ✅ Posição relativa ao centro de interação
- ✅ Offsets visuais de animação
- ✅ Timeline única de combate no jogo
- ✅ Pose externa suportada pela classe-base
- ✅ Bloqueio dos lutadores durante animação conjunta
- 🔧 Renderer de pose externa
- ⬜ Primeira animação conjunta provisória no jogo
- ⬜ Animação real da `PEGADA_1`
- ⬜ Biblioteca de animações de combate
- ⬜ Sistema de prioridade entre animações / estados

---

# 🎨 MIAUSTUDIO

Editor próprio utilizado para construir e ajustar poses e animações dos personagens.

- ✅ Edição de poses
- ✅ Manipulação direta pelo canvas
- ✅ Edição da cintura / root
- ✅ Edição da cabeça
- ✅ Rotação e offsets de partes do corpo
- ✅ Undo / redo
- ✅ Preview de animação
- ✅ Espelhamento entre personagens
- ✅ Modo de animação individual
- ✅ Modo de animação conjunta
- ✅ Aplicador e receptor
- ✅ Quantidade variável de quadros
- ✅ Adicionar / remover quadros
- ✅ Salvar projeto em JSON
- ✅ Abrir projeto JSON
- ✅ Compatibilidade / migração de projeto anterior
- ✅ Centro de interação
- ✅ Posição relativa em animações conjuntas
- ✅ Offset visual X/Y
- ✅ Exportação de arrays para Java
- ⬜ Biblioteca interna de animações
- ⬜ Organização por categorias
- ⬜ Integração futura com criador de personagens

---

# 🤖 IA E TREINAMENTO

## Modo de treinamento

- ⬜ Boneco parado
- ⬜ Defesa automática
- ⬜ IA ativa
- ⬜ Fôlego infinito opcional
- ⬜ Condicionamento infinito opcional
- ⬜ Exibição de dados de debug
- ⬜ Forçar resultado
- ⬜ Configurar RNG

## Evolução da IA

- ⬜ IA parada
- ⬜ IA defensiva
- ⬜ IA com ações aleatórias
- ⬜ IA contextual
- ⬜ IA baseada em estilo de lutador

---

# 🏗️ ARQUITETURA

## Personagens

- ✅ Classe-base `IrmaoMiau`
- ✅ Mecânicas comuns centralizadas
- ✅ Atributos separados em `AtributosLutador`
- ✅ Branco e Preto com dados / aparência / animações próprias
- ✅ Motor recebe `IrmaoMiau`, sem depender diretamente de Branco ou Preto
- ⬜ Lutador completamente genérico carregado por dados
- ⬜ Catálogo de personagens
- ⬜ Espécie
- ⬜ Arte marcial
- ⬜ Lista de técnicas
- ⬜ Golpe característico
- ⬜ Menu gerado automaticamente pelo catálogo

## Motor de combate

- ✅ `AcaoCombate`
- ✅ `CategoriaAcao`
- ✅ `ResultadoAcao`
- ✅ `ContextoCombate`
- ✅ `RegistroAcoes`
- ✅ `SistemaCombate`
- ✅ `EstadoInteracaoCombate`
- ✅ `RelacaoHorizontal`
- ✅ `AnimacaoCombateAtiva`
- ⬜ `PosicaoCombate`
- ⬜ `EfeitoCombate`
- ⬜ `DisputaCombate`
- ⬜ Registro externo de técnicas
- ⬜ Técnicas carregadas por JSON

## Controle dos personagens

- 🔧 Controles ainda associados aos dois Miaus atuais
- ⬜ Controlador genérico
- ⬜ Jogador 1
- ⬜ Jogador 2
- ⬜ IA
- ⬜ Boneco de treino

Objetivo futuro:

```text
Personagem
    ↓
Controlador
    ↓
Comandos
```

Assim qualquer personagem poderá ser utilizado como jogador ou controlado pela IA.

---

# 🧬 CRIADOR DE PERSONAGENS

Planejado como evolução futura do MiauStudio.

- ⬜ Nome
- ⬜ Peso
- ⬜ Altura / escala
- ⬜ Cores
- ⬜ Kimono
- ⬜ Faixa
- ⬜ Proporções corporais
- ⬜ Atributos
- ⬜ Estilo de luta
- ⬜ Animações
- ⬜ Técnicas disponíveis
- ⬜ Golpe característico
- ⬜ Exportação de definição do personagem
- ⬜ Carregamento por JSON

---

# 🥋 PROGRESSÃO

Sistema de progressão de faixa planejado:

- ⬜ Branca
- ⬜ Azul
- ⬜ Roxa
- ⬜ Marrom
- ⬜ Preta

Possibilidades futuras:

- 💡 evolução de atributos
- 💡 desbloqueio de técnicas
- 💡 especialização por estilo
- 💡 carreira / campanha

---

# 🐾 ELENCO FUTURO

A arquitetura deverá permitir novos lutadores e diferentes espécies.

- 🐱 Gatos
- 🐯 Tigres
- 🦁 Leões
- 🐶 Cachorros
- 🐺 Lobos
- 🐻 Ursos
- 💡 Outros

Todos deverão poder utilizar o mesmo motor de combate, desde que possuam os dados, rig, animações e técnicas necessárias.

---

# 🌎 PROJETO / GITHUB

- ✅ Repositório público
- ✅ Git utilizado no fluxo diário de desenvolvimento
- ✅ Commits incrementais
- ✅ README principal em inglês
- ✅ README em português (`README.pt-BR.md`)
- ✅ Seletor de idioma no topo dos READMEs
- 🔧 ROADMAP mantido como documento vivo
- ⬜ Screenshots / GIFs de gameplay no README
- ⬜ Releases jogáveis mais frequentes
- ⬜ Issues para bugs e sugestões
- ⬜ Documentação técnica do motor de combate

---

# 🚀 PRÓXIMOS PASSOS IMEDIATOS

1. 🔧 **8D — Renderizar pose externa de combate**
2. ⬜ **8E — Rodar uma animação conjunta provisória**
3. ⬜ **8F — Criar a animação real da `PEGADA_1` no MiauStudio**
4. ⬜ Validar a animação no jogo
5. ⬜ Refinar manutenção e quebra da pegada
6. ⬜ Iniciar sistema de atributos / disputa da ação
7. ⬜ Evoluir para novas pegadas e primeiras técnicas dependentes de pegada

---

> O roadmap não representa uma ordem rígida para todo o desenvolvimento. Ele funciona como mapa geral do projeto e será atualizado conforme novas decisões de design e arquitetura forem validadas.
