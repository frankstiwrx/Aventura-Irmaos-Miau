# Modo Desenvolvedor (Entrega 5Q.4)

Ferramenta de teste **separada do treino**: menu principal → **MODO DESENVOLVEDOR**. Usa as mesmas
regras, o mesmo motor (`MotorSolo`/`SistemaCombate`), os mesmos personagens e os mesmos mapas de uma
partida. Muda só a política de recursos.

## Como abrir

1. Menu principal → **MODO DESENVOLVEDOR** (entre TREINO e PERSONAGENS).
2. A mesma seleção da partida: personagem, **faixa e kimono de cada slot** (J1 e J2 independentes) e mapa.
   O tempo escolhido é ignorado: a luta não tem relógio.
3. LUTAR.

J1 e J2 são controláveis como numa partida (Xbox, teclado do J1 e teclado virtual do J2).

## O que muda

| | Partida normal | Modo Desenvolvedor |
|---|---|---|
| Fôlego | gasta e recupera | fica no **máximo normal** do lutador (ex.: 47/47 na faixa preta); nada de número gigante |
| Condicionamento | gasta | fica no máximo normal |
| Tempo | o escolhido | sem tempo |
| Selo | — | selo vermelho permanente no canto: `MODO DESENVOLVEDOR · FÔLEGO ∞ · COND. ∞` |
| F5 | reinicia o treino (só no treino) | **recomeça a luta** com a mesma sessão (personagens, faixas, kimonos, mapa, opções) |
| F4 | troca o cenário e salva como preferido | troca o cenário **só para esta sessão**; o F5 recomeça no cenário escolhido; nada é salvo |

### Pausa (ESC / Start)

Além das opções de sempre:

- **FÔLEGO INFINITO: SIM/NÃO**: liga/desliga para os dois lutadores.
- **CONDICIONAMENTO INFINITO: SIM/NÃO**: idem.
- **RESTAURAR RECURSOS**: devolve fôlego e condicionamento ao máximo normal agora.

O selo mostra o estado atual (`∞` ou `NORMAL`).

## Isolamento

- A sessão (`game.ModoDesenvolvedor`) é um objeto que o `Main` cria ao confirmar a seleção e repassa
  só para essa luta e para os reinícios dela (F5 / REINICIAR LUTA). **Não há estado global.**
- Voltar ao menu descarta a sessão. Partidas (JOGAR) e treinos nunca a recebem; o `GamePanel` do treino
  ignora o modo (o **Laboratório** continua dentro do treino, sem mudanças).
- No lutador, a política fica em `IrmaoMiau.setRecursosInfinitos(folego, condicionamento)`: os gastos
  (`gastarEnergia`, `gastarCondicionamento`, `consumirFolegoCombate`) não descontam e o valor é mantido
  no máximo dos atributos reais (faixa incluída) a cada tick.

## Testes

`ModoDesenvolvedor5Q4Test`: recursos infinitos nos dois lutadores no máximo normal; consumo normal fora
do modo; treino ignora o modo e partida nova nasce sem ele; opções da pausa alternam e restauram; F5
recomeça pela sessão e F4 guarda o cenário; selo desenhado. `Menus5MTest`: a opção no menu chama a ação.

## Limitações

- Não há IA: o J2 é sempre um jogador (ou fica parado).
- O F4 do Modo Desenvolvedor não grava preferência; o cenário inicial vem da seleção.
