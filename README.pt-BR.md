# 🐱 Aventura dos Irmãos Miau

<p align="center">
  <a href="README.md">
    <img src="https://img.shields.io/badge/English-README-2ea44f?style=for-the-badge" alt="English">
  </a>
  <a href="README.pt-BR.md">
    <img src="https://img.shields.io/badge/Português-README-0969da?style=for-the-badge" alt="Português">
  </a>
</p>

**Um jogo de luta 2D desenvolvido em Java, protagonizado por dois irmãos gatos praticantes de Jiu-Jitsu Brasileiro.**

---

## 🎮 Sobre o projeto

**Aventura dos Irmãos Miau** é um jogo 2D desenvolvido em Java como um projeto prático para estudar desenvolvimento de jogos, programação orientada a objetos, sistemas de animação e mecânicas de combate.

O jogo acompanha dois irmãos gatos:

- 🤍 **Miau Branco**
- 🖤 **Miau Preto**

Os dois praticam **Jiu-Jitsu Brasileiro**, mas possuem atributos físicos, características de movimento e estilos de luta diferentes.

Os personagens e suas animações são desenhados programaticamente utilizando **Java2D**, sem depender de sprites tradicionais.

O projeto começou como um pequeno experimento em Java e está evoluindo gradualmente para um jogo de luta mais completo, com motor próprio de combate, ferramentas de animação e mecânicas de grappling.

---

## 🥋 Funcionalidades atuais

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
- Motor modular de combate em desenvolvimento
- Detecção de distância e posicionamento
- Estados de interação e pegada
- Infraestrutura para animações conjuntas de combate
- Editor próprio de poses e animações utilizado durante o desenvolvimento

---

## ⚙️ Motor de combate

Um sistema modular de combate está atualmente em desenvolvimento.

A proposta é manter separados:

- personagens;
- controles;
- ações de combate;
- posicionamento;
- atributos;
- animações;
- inteligência artificial;
- resolução das ações.

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

## 🛠️ Tecnologias

- **Java**
- **Java Swing**
- **Java2D**
- **Maven**
- **Git / GitHub**

---

## 🧪 Filosofia de desenvolvimento

O projeto também funciona como um estudo prático de programação.

Em vez de utilizar um motor de jogos pronto, diversos sistemas estão sendo desenvolvidos manualmente em Java, incluindo:

- renderização;
- movimentação;
- estados dos personagens;
- interpolação de animações;
- gerenciamento de energia;
- interações de combate;
- ferramentas de desenvolvimento.

Isso transforma o projeto também em um ambiente para experimentar conceitos de arquitetura de software e programação de jogos enquanto algo jogável continua sendo construído.

---

## 🚧 Status do projeto

**Em desenvolvimento ativo.**

O projeto está sendo desenvolvido de forma incremental, com melhorias frequentes na jogabilidade, animações e arquitetura interna.

---

## 🎯 Funcionalidades planejadas

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

## 🎥 Vídeos de desenvolvimento

Testes de gameplay e vídeos do desenvolvimento são publicados durante a evolução do projeto.

---

## 🤝 Contribuições e feedback

O projeto ainda é experimental e está em constante evolução.

Sugestões, feedback e discussões técnicas são bem-vindos através das Issues do GitHub.

Se você gostar do projeto, considere deixar uma ⭐ no repositório.

---

## 👨‍💻 Autor

Desenvolvido por **Frank William Araujo Souza**.

Feito com Java, curiosidade, gatos e muito Jiu-Jitsu. 🐱🥋
