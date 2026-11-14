# Combate posicional — Entrega 5E

Fundação para o jiu-jitsu posicional **em tempo real**: sem turnos, sem pausa, sem tela de golpes.
Nesta entrega o modelo existe, é testado e roda num simulador isolado. **No jogo, só o combate
em pé (PEGADA_1, 5A, 5B) continua jogável.**

## Responsabilidades

| Quem | Cuida de |
|---|---|
| `IrmaoMiau` | corpo de cada lutador: posição física, linhas, andar, pular, deitar, fôlego |
| `SistemaCombate` (inalterado) | interação compartilhada (PEGADA_1, 30 quadros, pose final), firmeza, arrasto, disputas 5A |
| `posicional.MotorPosicional` (novo) | posição **da dupla**, quem está por cima/por baixo, quem controla, transição em andamento |
| `posicional.LeituraPosicionalEmPe` (novo) | **lê** o estado em pé do `SistemaCombate` — sem guardar nada em dobro |

Em pé, o modelo posicional é só uma leitura: sem interação = `EM_PE_SEPARADOS`; PEGADA_1 nos 30
quadros = `EM_PE_SEPARADOS` com a transição `estabelecer_pegada` em andamento; estabelecida =
`EM_PE_PEGADA` com o controle de quem aplicou **agora**. O aplicador da PEGADA_1 não é um atacante
permanente: os papéis mudam a cada transição.

## Estado (`EstadoPosicional`, imutável)

- **posição**: `EM_PE_SEPARADOS`, `EM_PE_PEGADA`, `GUARDA`, `MEIA_GUARDA`, `CONTROLE_LATERAL`,
  `MONTADA`, `COSTAS`;
- **por cima / por baixo** (só no chão);
- **controlador** (pode ser quem está por baixo, ex.: guarda que segurou a passagem);
- **transição em andamento**: definição (origem e desfechos), quem iniciou, ticks decorridos,
  se o outro sinalizou defesa.

Papéis (`PapelPosicional`): `CONTROLADOR`, `CONTROLADO`, `POR_CIMA`, `POR_BAIXO`, `QUALQUER`.
Lados `A`/`B` = `SistemaCombate.getLutadorA/B` (o modelo não guarda referências a lutadores).

## Transições (`CatalogoTransicoes.referencia()`)

Cada `DefinicaoTransicao` tem: origem, papel exigido de quem inicia, condições, **cadência**
(`RAPIDA`, `PREPARADA`, `DISPUTA`), **duração em ticks do loop lógico** e um `Desfecho` para cada
resultado possível (`SUCESSO`, `DEFENDIDA`, `FALHA`, `INVERSAO`). O desfecho diz a posição de
destino e, relativo a quem iniciou, quem fica por cima e com o controle.

| Transição | Origem → sucesso | Quem inicia | Defendida | Falha | Inversão |
|---|---|---|---|---|---|
| `estabelecer_pegada` (= PEGADA_1, 30 ticks) | separados → pegada | qualquer | — | separados | — |
| `queda` | pegada → guarda, iniciador por cima | controlador | segue na pegada | separados | guarda com o **outro** por cima (sprawl) |
| `puxar_guarda` | pegada → guarda, iniciador **por baixo** com o controle | qualquer | — | segue na pegada | — |
| `passagem_guarda` | guarda → cem quilos | por cima | guarda, controle passa para baixo | guarda | montada do **outro** |
| `raspagem` | guarda → montada do iniciador | por baixo | guarda | guarda | — |
| `reposicao_guarda` | cem quilos → guarda | por baixo | cem quilos | cem quilos | — |

Durações além da PEGADA_1 são **provisórias** (só simulador). Nenhuma fórmula nova: o resultado vem
de fora, por um `ResolvedorTransicao`. No jogo ele será a disputa 5A (`ResolvedorDisputa`, com
atributos, fôlego e firmeza).

## Ciclo em tempo real (`MotorPosicional`)

1. `tentar(lado, id, fatos)` → `INICIADA` ou o motivo (`POSICAO_INCOMPATIVEL`, `PAPEL_INCOMPATIVEL`,
   `CONDICAO_NAO_ATENDIDA`, `TRANSICAO_EM_ANDAMENTO`, `NAO_LIBERADA_NO_JOGO`, `DESCONHECIDA`).
2. `atualizar()` uma vez por tick. É a preparação e a **janela de reação**: o outro pode
   `sinalizarDefesa(lado)` (defesa simultânea) até a tentativa ficar pronta.
3. `resolver(resultado)` ou `resolverCom(resolvedor, fatos)` aplica o desfecho. `cancelar()`
   interrompe (ex.: pegada rompida).

**Modo JOGO** recusa qualquer transição que possa terminar numa posição de chão (sem visual ainda).
**Modo SIMULACAO** libera tudo (testes, ferramentas).

## Ritmo e cadência (como conectar no futuro)

```
tentar ──► [preparação: N ticks]──────────────► pronta ──► resolver (disputa 5A) ──► desfecho
              │  animação "preparo" (progresso 0..1)           │ animação de sucesso/defesa/inversão
              │  janela: o outro pode sinalizarDefesa          │ estabilização (ticks sem nova tentativa)
              └─ fôlego: custo no início (como H/T/U/Y)         └─ recuperação de fôlego
```

- O progresso (`TransicaoEmCurso.progresso()`) sincroniza a animação de preparo, sempre por ticks.
- Ação rápida = poucos ticks; preparada = janela clara; disputa = os dois agem até resolver.
- Estabilização e janelas de oportunidade entram como condições (`CondicaoTransicao`) e durações,
  sem mudar o motor.

## Ações contextuais: posição × papel × ações

`RepertorioPosicional` é a **proposta** de até 4 ações principais por posição/papel, com variação
pelo modificador (L1/LB ou SHIFT). Não cria teclas.

| Posição | Papel | Ação 1 | Ação 1 + modificador |
|---|---|---|---|
| Em pé separados | qualquer | estabelecer pegada (hoje: H) | — |
| Pegada | controlador | queda | puxar guarda |
| Pegada | controlado | — | puxar guarda |
| Guarda | por cima | passagem | — |
| Guarda | por baixo | raspagem | — |
| Cem quilos | por baixo | reposição de guarda | — |

Futuro: as ações genéricas `EXECUTAR_TECNICA_1..4` (5C: `AcaoJogador` + teclado configurável + gamepad
5D) consultam o repertório. Cada uma dispara uma transição (e não uma técnica fixa), que escolhe as
animações pelo `ControladorAnimacoes`. Comandos contínuos (segurar para manter controle, como o
reforço/arrasto da 5B) continuam ações de toque/contínuas do `InputHandler`. H, T, U, Y e o arrasto
não mudaram.

## Fora do escopo desta entrega

- Posições de chão jogáveis (sem poses/animações): o modo JOGO as recusa.
- Fórmulas de quedas, passagens e raspagens.
- Finalizações, meia-guarda e costas com transições próprias (estão modeladas como posições).
