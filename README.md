# 🐱 Aventura dos Irmãos Miau

**A 2D fighting game developed in Java, featuring two cat brothers who practice Brazilian Jiu-Jitsu.**

🇺🇸 English | 🇧🇷 Português abaixo

---

## 🇺🇸 English

### 🎮 About the project

**Aventura dos Irmãos Miau** is a 2D game developed in Java as a hands-on project for studying game development, object-oriented programming, animation systems and combat mechanics.

The game follows two cat brothers:

- 🤍 **Miau Branco**
- 🖤 **Miau Preto**

Both characters practice **Brazilian Jiu-Jitsu**, but have different physical attributes, movement characteristics and fighting styles.

The characters and animations are drawn programmatically using **Java2D**, without relying on traditional sprite sheets.

The project started as a small Java experiment and is gradually evolving into a more complete fighting game with its own combat engine, animation tools and grappling mechanics.

---

### 🥋 Current features

The project currently includes:

- Two playable characters
- Local two-player controls
- Character-specific attributes
- Walking and directional movement
- Running
- Crouching
- High and short jumps
- Breathing and stamina recovery
- Stamina and conditioning systems
- Rolling in multiple directions
- Sprawl
- Ground movement
- Standing and lying states
- Procedurally drawn character animations
- Character orientation based on opponent position
- Multi-line movement system
- Early combat engine
- Combat distance and positioning detection
- Grappling interaction states
- Shared combat animation infrastructure
- Custom pose and animation editor used during development

---

### ⚙️ Combat engine

A modular combat system is currently under development.

The goal is to separate:

- characters;
- controls;
- combat actions;
- positioning;
- attributes;
- animation;
- AI;
- and combat resolution.

This architecture is being designed so that future characters can use the same combat engine instead of requiring character-specific combat logic.

The first grappling interaction, **PEGADA_1**, is already functional at the logic level, including:

- distance requirements;
- line positioning;
- character orientation;
- stamina cost;
- interaction state;
- attacker and receiver roles.

Synchronized combat animations are currently being integrated.

---

### 🛠️ Technologies

- **Java**
- **Java Swing**
- **Java2D**
- **Maven**
- **Git / GitHub**

---

### 🧪 Development philosophy

This project is also a practical programming study.

Instead of being built from a finished game engine, many systems are being created manually in Java, including:

- rendering;
- movement;
- character states;
- animation interpolation;
- stamina management;
- combat interactions;
- and development tools.

This makes the project a playground for experimenting with software architecture and game programming concepts.

---

### 🚧 Project status

**Actively under development.**

The project is being developed incrementally, with frequent improvements to gameplay, animations and internal architecture.

Check [`ROADMAP.md`](ROADMAP.md) for planned features and future development.

---

### 🎥 Development videos

Gameplay and development videos are available through the project's YouTube playlist.

---

### 🎯 Planned features

Some of the systems planned for future versions include:

- More grappling positions
- Multiple grip states
- Takedowns
- Throws
- Striking
- Defensive actions
- Guard positions
- Guard passing
- Sweeps
- Submissions
- Escapes
- Attribute-based combat resolution
- Player timing mechanics
- Training mode
- Computer-controlled opponents
- Additional playable characters
- Character creation tools
- Expanded animation library

---

### 🤝 Contributions and feedback

The project is still experimental and evolving.

Suggestions, feedback and technical discussions are welcome through GitHub Issues.

If you find the project interesting, consider leaving a ⭐ on the repository.

---

## 🇧🇷 Português

### 🎮 Sobre o projeto

**Aventura dos Irmãos Miau** é um jogo 2D desenvolvido em Java como um projeto prático para estudar desenvolvimento de jogos, programação orientada a objetos, sistemas de animação e mecânicas de combate.

O jogo acompanha dois irmãos gatos:

- 🤍 **Miau Branco**
- 🖤 **Miau Preto**

Os dois praticam **Jiu-Jitsu Brasileiro**, mas possuem atributos físicos, características de movimento e estilos de luta diferentes.

Os personagens e suas animações são desenhados programaticamente utilizando **Java2D**, sem depender de sprites tradicionais.

O projeto começou como um pequeno experimento em Java e está evoluindo gradualmente para um jogo de luta mais completo, com motor próprio de combate, ferramentas de animação e mecânicas de grappling.

---

### 🥋 Funcionalidades atuais

Atualmente o projeto possui:

- Dois personagens jogáveis
- Controles locais para dois jogadores
- Atributos individuais por personagem
- Caminhada e movimentação direcional
- Corrida
- Agachamento
- Pulo alto e pulo curto
- Respiração e recuperação de fôlego
- Sistema de fôlego e condicionamento
- Rolamento em múltiplas direções
- Sprawl
- Movimento no chão
- Estados em pé e deitado
- Animações desenhadas programaticamente
- Orientação automática em relação ao adversário
- Sistema de movimentação em múltiplas linhas
- Motor de combate em desenvolvimento
- Detecção de distância e posicionamento
- Estados de interação e pegada
- Infraestrutura para animações conjuntas de combate
- Editor próprio de poses e animações utilizado durante o desenvolvimento

---

### ⚙️ Motor de combate

Um sistema modular de combate está atualmente em desenvolvimento.

A proposta é manter separados:

- personagens;
- controles;
- ações de combate;
- posicionamento;
- atributos;
- animações;
- inteligência artificial;
- e resolução das ações.

Essa arquitetura está sendo construída para permitir que futuros personagens utilizem o mesmo motor de combate, sem a necessidade de criar regras específicas para cada lutador.

A primeira interação de grappling, **PEGADA_1**, já funciona no nível lógico, considerando:

- distância;
- diferença de linhas;
- orientação dos personagens;
- custo de fôlego;
- estado da interação;
- papel de aplicador e receptor.

As animações sincronizadas entre dois personagens estão atualmente sendo integradas ao sistema.

---

### 🛠️ Tecnologias

- **Java**
- **Java Swing**
- **Java2D**
- **Maven**
- **Git / GitHub**

---

### 🧪 Filosofia de desenvolvimento

O projeto também funciona como um estudo prático de programação.

Em vez de utilizar um motor de jogos pronto, diversos sistemas estão sendo desenvolvidos manualmente em Java, incluindo:

- renderização;
- movimentação;
- estados dos personagens;
- interpolação de animações;
- gerenciamento de energia;
- interações de combate;
- e ferramentas de desenvolvimento.

Isso transforma o projeto também em um ambiente para experimentar conceitos de arquitetura de software e programação de jogos.

---

### 🚧 Status do projeto

**Em desenvolvimento ativo.**

O projeto está sendo desenvolvido de forma incremental, com melhorias frequentes na jogabilidade, animações e arquitetura interna.

Consulte [`ROADMAP.md`](ROADMAP.md) para acompanhar funcionalidades planejadas e futuras etapas.

---

### 🎥 Vídeos de desenvolvimento

Vídeos de gameplay e do desenvolvimento do projeto estão disponíveis na playlist do projeto no YouTube.

---

### 🎯 Funcionalidades planejadas

Entre os sistemas planejados para versões futuras estão:

- Mais posições de grappling
- Diferentes tipos de pegada
- Quedas
- Projeções
- Golpes em pé
- Defesas
- Guarda
- Passagens de guarda
- Raspagens
- Finalizações
- Escapes
- Resolução de combate baseada em atributos
- Mecânicas de timing do jogador
- Modo de treinamento
- Adversários controlados por IA
- Novos personagens jogáveis
- Ferramentas para criação de personagens
- Biblioteca expandida de animações

---

### 🤝 Contribuições e feedback

O projeto ainda é experimental e está em constante evolução.

Sugestões, feedback e discussões técnicas são bem-vindos através das Issues do GitHub.

Se você gostar do projeto, considere deixar uma ⭐ no repositório.

---

## 👨‍💻 Author

Developed by **Frank William Araujo Souza**.

Built with Java, curiosity, cats and a lot of Jiu-Jitsu. 🐱🥋
