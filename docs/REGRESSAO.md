# Checklist de regressão

Rodar **a cada entrega** antes de commitar. A parte automática não substitui a
parte manual: "passou no Maven" não significa "aprovado visualmente".

## 1. Automático

```bash
mvn clean test
```

Esperado: `Tests run: 145, Failures: 0, Errors: 0, Skipped: 0` e `BUILD SUCCESS`
(o número cresce quando novos testes entram). No NetBeans: **Test Project** (Alt+F6).

```bash
git diff --stat -- src/main/java/com/irmaosmiau/entities src/main/java/com/irmaosmiau/editor src/main/java/com/irmaosmiau/combate/RegistroAnimacoesCombate.java
```

Esperado: vazio, a menos que a entrega tenha autorização explícita para tocar nesses arquivos.

## 2. Manual no NetBeans

### Menu oficial (`Main` — Run Project, F6)

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

### MiauStudio (`EditorDePoses` — Run File)

- [ ] Abre normalmente; carregar `src/main/resources/conteudo/miaustudio/PEGADA_1.json` e conferir a prévia dos 3 quadros
- [ ] **Não salvar por cima** do arquivo de `resources` ao testar (salvar como outro nome); se salvar de propósito, rodar `mvn test` — o teste de SHA-256 vai acusar a mudança

### Conteúdo (Entrega 2)

- [ ] `git status` mostra `src/main/resources/conteudo/miaustudio/PEGADA_1.json` sem modificação
- [ ] Os testes `ConversorMiauStudioTest` e `ConteudoNoSistemaCombateTest` passaram

## 3. Arte e animações

- [ ] `git diff` não contém alterações em arrays `*_MIAUSTUDIO`, renderers do Branco/Preto ou `RegistroAnimacoesCombate`
- [ ] `Pegada1RegressaoArraysTest` passou (protege os arrays aprovados da PEGADA_1)
