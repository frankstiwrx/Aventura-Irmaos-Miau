# 🐱 Aventura dos Irmãos Miau

<p align="center">
  <a href="README.md">
    <img src="https://img.shields.io/badge/English-README-2ea44f?style=for-the-badge" alt="English">
  </a>
  <a href="README.pt-BR.md">
    <img src="https://img.shields.io/badge/Português-README-0969da?style=for-the-badge" alt="Português">
  </a>
</p>

**A 2D fighting game developed in Java, featuring two cat brothers who practice Brazilian Jiu-Jitsu.**

---

## 🎮 About the project

**Aventura dos Irmãos Miau** is a 2D game developed in Java as a hands-on project for studying game development, object-oriented programming, animation systems and combat mechanics.

The game follows two cat brothers:

- 🤍 **Miau Branco**
- 🖤 **Miau Preto**

Both characters practice **Brazilian Jiu-Jitsu**, but have different physical attributes, movement characteristics and fighting styles.

The characters and animations are drawn programmatically using **Java2D**, without relying on traditional sprite sheets.

The project started as a small Java experiment and is gradually evolving into a more complete fighting game with its own combat engine, animation tools and grappling mechanics.

---

## 🥋 Current features

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
- Programmatically drawn character animations
- Character orientation based on opponent position
- Multi-line movement system
- Early modular combat engine
- Combat distance and positioning detection
- Grappling interaction states
- Shared combat animation infrastructure
- Custom pose and animation editor used during development

---

## ⚙️ Combat engine

A modular combat system is currently under development.

The goal is to keep the following systems separated:

- characters;
- controls;
- combat actions;
- positioning;
- attributes;
- animation;
- AI;
- combat resolution.

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

## 🛠️ Technologies

- **Java**
- **Java Swing**
- **Java2D**
- **Maven**
- **Git / GitHub**

---

## 🧪 Development philosophy

This project is also a practical programming study.

Instead of being built on top of a finished game engine, many systems are being created manually in Java, including:

- rendering;
- movement;
- character states;
- animation interpolation;
- stamina management;
- combat interactions;
- development tools.

This makes the project a playground for experimenting with software architecture and game programming concepts while continuously building something playable.

---

## 🚧 Project status

**Actively under development.**

The project is being developed incrementally, with frequent improvements to gameplay, animations and internal architecture.

---

## 🎯 Planned features

Future versions are planned to include:

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

## 🎥 Development videos

Gameplay tests and development videos are published throughout the evolution of the project.

---

## 🤝 Contributions and feedback

The project is still experimental and constantly evolving.

Suggestions, feedback and technical discussions are welcome through GitHub Issues.

If you find the project interesting, consider leaving a ⭐ on the repository.

---

## 👨‍💻 Author

Developed by **Frank William Araujo Souza**.

Built with Java, curiosity, cats and a lot of Jiu-Jitsu. 🐱🥋
