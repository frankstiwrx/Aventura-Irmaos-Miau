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

O projeto começou como um pequeno experimento em Java e evoluiu para um jogo de luta com motor próprio de combate, ferramentas de animação e um sistema de grappling cada vez mais completo.

O Jiu-Jitsu Brasileiro é a base atual do desenvolvimento. A visão de longo prazo é um jogo de combate com **várias artes marciais**: no futuro, o projeto poderá ganhar novos personagens, estilos de luta e mecânicas inspiradas em outras modalidades.

---

## 🥋 Funcionalidades atuais

### Movimentação e resistência

- Caminhada, corrida, agachamento, pulos alto e curto
- Rolamentos, sprawl e acrobacias
- Movimento no chão, estados em pé e deitado e orientação automática em relação ao adversário
- Movimentação em múltiplas linhas
- Fôlego e condicionamento: cada ação cansa, e respirar recupera
- Atributos individuais por personagem (força, velocidade, técnica, fôlego e outros)

### Grappling e finalizações

- Disputa de pegadas em pé, quedas, projeções e puxadas para a guarda
- Combate no chão com diversas posições: guardas fechada e aberta, meia-guarda, cem quilos, montada, costas, joelho na barriga, guardas modernas e controle de perna
- Raspagens, passagens de guarda, escapes e transições entre posições
- Dezenas de finalizações de braço, estrangulamento e perna, escolhidas em rodas de técnicas na tela
- Finalizações com tempo real de reação: bater, defender, resistir e escapar
- Faixas (da branca à preta) que influenciam os atributos e liberam técnicas mais avançadas

### Modos de jogo

- **JOGAR:** luta local para dois jogadores, cada um confirmando o seu lutador
  - **Finalização:** vence quem finalizar
  - **Pontos + Finalização:** pontos, vantagens e placar, com arbitragem **adaptada** das regras da IBJJF
  - Tempo de luta livre, de 1 a 10 minutos ou sem limite
- **TREINO:** sem tempo nem arbitragem, com laboratório de situações prontas, troca do Miau controlado e a opção de um segundo jogador comandar o parceiro
- Seleção de arenas com prévia, entre dojo, academias, torneio, campeonato e outras

### Personalização

- Editor de personagens para criar perfis a partir do Miau Branco ou do Miau Preto, com nome e atributos próprios
- Aparência personalizada: cores do kimono, da calça, da faixa (apenas visual) e da pelagem
- Escolha da faixa e da cor do kimono para os personagens originais

### Controles

- Dois jogadores no mesmo computador, por teclado ou gamepad
- Suporte a controles Xbox e PlayStation no Windows, com remapeamento de botões
- Teclas configuráveis e menus navegáveis por teclado, controle e mouse
- Trilha sonora e efeitos sonoros

### Ferramentas de desenvolvimento

- **MiauStudio**, editor próprio de poses e animações
- Modo Desenvolvedor dentro da partida, com informações do motor e das rodas de comandos
- Laboratório de treino para testar situações específicas

---

## ⚙️ Motor de combate

O jogo usa um motor de combate próprio e modular.

A proposta é manter separados:

- personagens;
- controles;
- ações de combate;
- posicionamento;
- atributos;
- animações;
- regras e arbitragem;
- resolução das ações.

Assim, novos personagens podem usar o mesmo motor de combate, sem a necessidade de criar regras específicas para cada lutador.

Hoje o motor já cuida de:

- distância, linhas e orientação dos personagens;
- disputas de pegada com aplicador e receptor;
- posições no chão, com quem está por cima e quem controla;
- custo de fôlego e resolução baseada em atributos;
- finalizações com janelas de reação e defesa;
- animações sincronizadas entre os dois personagens;
- pontuação e vantagens no modo com arbitragem.

---

## 🛠️ Tecnologias

- **Java**
- **Java Swing**
- **Java2D**
- **Maven**
- **LWJGL / GLFW** (leitura de gamepads)
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

A primeira versão pública planejada é a **v0.1.0 — Beta**. Por ser uma versão inicial, ela pode ter bugs, ajustes de jogabilidade e mudanças visuais.

O projeto é desenvolvido de forma incremental, com melhorias frequentes na jogabilidade, nas animações e na arquitetura interna.

### 📦 Download

Quando houver versões disponíveis, elas estarão na página de **[Releases](https://github.com/frankstiwrx/Aventura-Irmaos-Miau/releases)** do repositório.

---

## 🎯 Funcionalidades planejadas

Entre as ideias para versões futuras estão:

- Adversários controlados pelo computador
- Novos personagens jogáveis
- Novos estilos de combate e outras artes marciais
- Golpes em pé
- Modo Arcade e modos de campeonato
- Mais técnicas, posições e transições
- Biblioteca expandida de animações
- Evolução visual dos personagens

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
