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

**Aventura dos Irmãos Miau** ("The Miau Brothers' Adventure") is a 2D game developed in Java as a hands-on project for studying game development, object-oriented programming, animation systems and combat mechanics.

The game follows two cat brothers:

- 🤍 **Miau Branco** (White Miau)
- 🖤 **Miau Preto** (Black Miau)

Both characters practice **Brazilian Jiu-Jitsu**, but have different physical attributes, movement characteristics and fighting styles.

The characters and animations are drawn programmatically using **Java2D**, without relying on traditional sprite sheets.

The project started as a small Java experiment and has grown into a fighting game with its own combat engine, animation tools and an increasingly complete grappling system.

Brazilian Jiu-Jitsu is the current foundation of the game. The long-term vision is a fighting game with **multiple martial arts**: in the future, the project may add new characters, fighting styles and mechanics inspired by other disciplines.

---

## 🥋 Current features

### Movement and stamina

- Walking, running, crouching, high and short jumps
- Rolls, sprawl and acrobatics
- Ground movement, standing and lying states, and automatic orientation toward the opponent
- Multi-line movement
- Stamina and conditioning: every action costs energy, and breathing recovers it
- Character-specific attributes (strength, speed, technique, stamina and more)

### Grappling and submissions

- Standing grip fighting, takedowns, throws and guard pulls
- Ground fighting across many positions: closed and open guard, half guard, side control, mount, back control, knee on belly, modern guards and leg entanglements
- Sweeps, guard passes, escapes and transitions between positions
- Dozens of arm, choke and leg submissions, chosen from on-screen technique wheels
- Submissions with real reaction time: tap, defend, resist and escape
- Belts (white to black) that affect attributes and unlock more advanced techniques

### Game modes

- **PLAY:** local two-player match, with each player confirming their own fighter
  - **Submission:** the first to submit wins
  - **Points + Submission:** points, advantages and a scoreboard, refereed with rules **adapted** from the IBJJF ruleset
  - Free match time, from 1 to 10 minutes or no limit
- **TRAINING:** no timer and no referee, with a lab of ready-made situations, switching which cat you control, and the option for a second player to control the partner
- Arena selection with previews, including dojo, academies, tournament, championship and more

### Customization

- Character editor for creating profiles based on the White or the Black Miau, with their own name and attributes
- Custom appearance: colors for the kimono top, pants, belt (visual only) and fur
- Belt and kimono color selection for the original characters

### Controls

- Two players on the same computer, using keyboard or gamepad
- Xbox and PlayStation controller support on Windows, with button remapping
- Configurable keys and menus that work with keyboard, controller and mouse
- Soundtrack and sound effects

### Development tools

- **MiauStudio**, a custom pose and animation editor
- In-match Developer Mode with engine and command-wheel information
- Training lab for testing specific situations

---

## ⚙️ Combat engine

The game runs on its own modular combat engine.

The goal is to keep the following systems separated:

- characters;
- controls;
- combat actions;
- positioning;
- attributes;
- animation;
- rules and refereeing;
- combat resolution.

This way, new characters can use the same combat engine instead of requiring character-specific combat logic.

Today the engine already handles:

- distance, lines and character orientation;
- grip exchanges with attacker and receiver roles;
- ground positions, tracking who is on top and who is in control;
- stamina costs and attribute-based resolution;
- submissions with reaction and defense windows;
- synchronized animations between both characters;
- points and advantages in the refereed mode.

---

## 🛠️ Technologies

- **Java**
- **Java Swing**
- **Java2D**
- **Maven**
- **LWJGL / GLFW** (gamepad input)
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

The first planned public version is **v0.1.0 — Beta**. As an early release, it may contain bugs, gameplay adjustments and visual changes.

The project is developed incrementally, with frequent improvements to gameplay, animations and internal architecture.

### 📦 Download

When versions are available, you will find them on the repository's **[Releases](https://github.com/frankstiwrx/Aventura-Irmaos-Miau/releases)** page.

---

## 🎯 Planned features

Ideas for future versions include:

- Computer-controlled opponents
- Additional playable characters
- New fighting styles and other martial arts
- Striking
- Arcade mode and championship modes
- More techniques, positions and transitions
- Expanded animation library
- Visual evolution of the characters

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
