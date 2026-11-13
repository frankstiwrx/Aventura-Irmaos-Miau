# Contratos do motor de combate

Comportamentos que o código atual **garante** e que os testes em
`src/test/java/com/irmaosmiau/combate/` verificam. Mudar qualquer item exige
decisão do criador, atualização deste documento e dos testes.

Convenção: 1 tick = 1 chamada de `SistemaCombate.atualizar()` = 1 frame do
`GamePanel` (Timer de 16 ms, ≈ 60 FPS).

## C1 — `AcaoCombate` (dados)

| Regra | Teste |
|---|---|
| `id` e `nome` obrigatórios; `id` normalizado (`trim` + minúsculas); `nome` com `trim` | `AcaoCombateTest` |
| `categoria` obrigatória | idem |
| Com id de animação → duração > 0; sem id (nulo/branco) → duração = 0 | idem |
| `alcanceHorizontal` ≥ 0, `diferencaMaximaLinhas` ≥ 0, `custoFolego` ≥ 0 | idem |
| `dificuldade` em [0, 100] (inclusivo) | idem |
| Pesos ≥ 0; a soma não precisa ser 1 | idem |

## C2 — `RegistroAcoes` (catálogo)

| Regra | Teste |
|---|---|
| Registro padrão (legado, `new RegistroAcoes()`) contém exatamente `pegada_1` | `RegistroAcoesTest` |
| Busca ignora maiúsculas e espaços; id desconhecido ou nulo → `null`/`false` | idem |
| Id duplicado ou ação nula → `IllegalArgumentException` | idem |
| `listar()` preserva ordem de cadastro e é somente leitura | idem |

## C3 — Timeline conjunta

| Regra | Teste |
|---|---|
| Um único relógio: `atualizar()` avança 1 frame; ao chegar na duração, `finalizada = true` e progresso = 1.0; nunca passa da duração | `TimelineAnimacaoTest` |
| Progresso → índice: `floor(progresso × quadros)`, limitado a [0, quadros−1] → quadros de mesma duração | idem |
| Aplicador e receptor sempre recebem o **mesmo índice** | idem, `Pegada1SistemaCombateTest` |
| Deslocamento X = offset visual + (cenaX do quadro − cenaX do quadro 1): o quadro 1 nunca teletransporta | idem |
| Cada quadro tem exatamente 30 valores; tracks com mesma quantidade de quadros; cópias defensivas | idem |

### Formato de 30 valores (MiauStudio)

```
0 cenaX(relativo)  1 linha(relativa)  2 escalaX10  3 viradoDireita
4 cinturaX 5 cinturaY 6 rotCintura  7 troncoX 8 troncoY 9 rotTronco
10 rotGlobal 11 rotCabeca 12 cabX 13 cabY
14 cotEsqX 15 cotEsqY 16 maoEsqX 17 maoEsqY  18 cotDirX 19 cotDirY 20 maoDirX 21 maoDirY
22 joelEsqX 23 joelEsqY 24 peEsqX 25 peEsqY  26 joelDirX 27 joelDirY 28 peDirX 29 peDirY
```

## C4 — PEGADA_1 (Branco aplicador, Preto receptor)

| # | Regra | Teste |
|---|---|---|
| 1 | Requisitos: distância entre cinturas ≤ **125 u** (u = px ÷ escala do atacante; 87,5 px na escala 0,7), diferença de linhas ≤ 1, Branco de frente para o Preto, fôlego ≥ **3,0**, nenhuma animação de combate ativa | `Pegada1SistemaCombateTest` |
| 2 | Requisito falho → `INDISPONIVEL`, sem custo e sem estado | idem |
| 3 | Sucesso → `SUCESSO`, fôlego do Branco −3,0 **uma vez**; Preto não paga | idem |
| 4 | Estado vira `PEGADA_1` com aplicador/receptor/id da ação registrados | idem |
| 5 | Repetir H com a pegada ativa (durante ou depois da animação) → `INDISPONIVEL`, sem novo custo | idem |
| 6 | Animação `pegada_1_entrada`, 30 ticks: quadro 1 no tick do H até o frame 9; quadro 2 nos frames 10–19; quadro 3 nos frames 20–29 | idem |
| 7 | Durante os 30 ticks os dois ficam `bloqueadoPorAnimacaoCombate` | idem |
| 8 | No tick 30 a animação termina, o bloqueio sai e a **pose 3 persiste** enquanto a interação existir | idem |
| 9 | Posição física (X) não muda por causa da animação | idem |
| 10 | Manutenção a cada tick: se sair do alcance, passar de 1 linha de diferença ou o Branco ficar de costas → `LIVRE`, poses e deslocamentos limpos nos dois | idem |
| 11 | Romper durante a entrada cancela a animação e desbloqueia os dois | idem |
| 12 | `encerrarInteracao()` limpa estado, poses e bloqueio | idem |
| 13 | Arrays e parâmetros da PEGADA_1 são idênticos aos aprovados | `Pegada1RegressaoArraysTest` |

### Não coberto (comportamento atual, a mudar na Entrega 3)

- Deitar, pular, sprawl ou rolar **depois** da entrada não rompem a pegada; a pose externa
  continua sobrepondo o desenho. Decisão aprovada: essas ações passarão a romper a interação.
- Sucesso é determinístico (pesos e dificuldade ainda não são usados).

## C6 — Conteúdo (Entrega 2)

| Regra | Teste |
|---|---|
| `PEGADA_1.json` importado = os 6 arrays aprovados, valor por valor; offsets, deslocamentos e índices por frame idênticos ao legado | `ConversorMiauStudioTest` |
| A PEGADA_1 via catálogo JSON se comporta **tick a tick** igual à legada (poses, deslocamentos, bloqueio, estado, fôlego, rompimento) | `ConteudoNoSistemaCombateTest` |
| O arquivo original não muda (SHA-256 conferido; `.gitattributes` impede conversão de fim de linha) | `ConversorMiauStudioTest` |
| Ordem dos 30 valores = `PoseSalva.paraLinhaJava()` do MiauStudio; cada campo cai no seu índice | idem |
| De 1 a 60 quadros; aplicador `Branco` ou `Preto` (troca os tracks) | idem |
| Todos os campos de cada pose são obrigatórios; erros dizem arquivo + caminho (`timelineConjunta[2].preto.offMaoEsqY`) | idem |
| Sem `duracoesQuadros` → tempos iguais (caminho original). Com distribuição → troca de quadro nos limites acumulados; `[10,10,10]` = comportamento original | `RegistrosExpansiveisTest` |
| Registros de ações, animações e personagens rejeitam IDs duplicados | `RegistrosExpansiveisTest`, `CarregadorConteudoCombateTest`, `CatalogoPersonagensTest` |
| Técnica `PENDENTE` → `podeTentarAcao = false`, `tentarAcao = INDISPONIVEL`, sem custo/estado/animação | `RegistrosExpansiveisTest`, `ConteudoNoSistemaCombateTest` |
| `SistemaCombate` com registro injetado não aceita técnica jogável apontando para animação inexistente | `RegistrosExpansiveisTest` |
| Catálogo JSON: técnica jogável exige todas as regras; campos desconhecidos são rejeitados; duração herdada da animação ou igual a ela | `CarregadorConteudoCombateTest` |
| Catálogo padrão: `pegada_1` jogável (= legado); `pegada_2..4` pendentes, sem animação nem estado | idem |
| Ação sem estado de interação: ao fim da animação as poses saem e o controle volta | `ConteudoNoSistemaCombateTest` |

## C7 — Personagens

| Regra | Teste |
|---|---|
| `CatalogoPersonagens.padrao()` = `miau_branco`, `miau_preto`, nessa ordem | `CatalogoPersonagensTest` |
| Nome, estilo e os 8 atributos da definição = os da instância criada pela fábrica | idem |
| Personagem fictício registrado só com definição + fábrica entra no `SistemaCombate` e executa a PEGADA_1 | idem |

## C5 — Pose externa em `IrmaoMiau`

| Regra |
|---|
| `aplicarPoseCombateExterna` exige 30 valores e guarda cópia defensiva |
| `limparEstadoAnimacaoCombate()` remove pose, deslocamentos e bloqueio |
| `consumirFolegoCombate(q)` retorna `false` sem alterar nada se fôlego < q; `q` negativo → exceção |
| Branco e Preto desenham a pose externa com prioridade sobre rolamento, sprawl, chão, taunt e em pé |
