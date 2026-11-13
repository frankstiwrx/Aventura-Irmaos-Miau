# Controles — Entrega 5C

Controles configuráveis e base para ações contextuais. Nenhuma técnica nova; H, T, U, Y e o
arrasto funcionam exatamente como na 5B (mesmos custos, ordem e resultados).

## Fluxo

```
tecla física (KeyStroke)          MapaControles: slot + AcaoJogador -> TeclaFisica
   │  InputMap/ActionMap
   ▼
InputHandler.isAtiva(slot, acao)  estado pressionado/solto + TravaComandos (3A)
   │
   ▼
GamePanel                         CONTINUA: lida a cada tick (andar, correr, agachar, modificador)
   │                              TOQUE: DetectorToques / campos xxxAnterior (uma vez por pressionamento)
   ▼
AcoesContextuais / executarAcaoCombate -> SistemaCombate (tentarAcao, tentarDesequilibrio, ...)
```

O motor (`combate`) não conhece nenhuma tecla. O `InputHandler` continua sendo o único leitor do
teclado e mantém os nomes de comando de sempre (`brancoPegada1Pressionada`, `pretoRompimentoSolta`...),
de modo que a `TravaComandos` e os testes antigos não mudaram.

## Ações lógicas (`controles.AcaoJogador`)

| Grupo | Ação | Acionamento | J1 (padrão) | J2 (padrão) |
|---|---|---|---|---|
| MOVIMENTO | `MOVER_ESQUERDA` / `MOVER_DIREITA` / `MOVER_CIMA` / `MOVER_BAIXO` | contínua | A / D / W / S | ← / → / ↑ / ↓ |
| | `PULAR` | toque | ESPAÇO | N |
| | `PULO_CURTO` | toque | V | B |
| | `AGACHAR` | contínua | C | M |
| | `CORRER` | contínua | Z | K |
| | `ROLAR` | toque | R | O |
| | `DEITAR_LEVANTAR` | toque | F | J |
| | `RESPIRAR` | contínua | X | L |
| COMBATE | `INICIAR_PEGADA` | toque | H | — |
| | `TENTAR_DESEQUILIBRIO` | toque | T | — |
| | `REFORCAR_PEGADA` | toque | Y | — |
| | `MODO_DESLOCAMENTO_CONTROLADO` | modificador | SHIFT | — |
| DEFESA | `SPRAWL` | toque | G | I |
| | `ROMPIMENTO_TECNICO` | toque | — | U |
| SISTEMA (fixas) | pausa, debug, cenário, reiniciar treino, música, efeitos | — | ESC, F3, F4, F5, F9, F10 | |

"—" significa que o slot ainda não usa a ação (hoje só o Jogador 1 aplica pegada e só o Jogador 2
rompe). O Jogador 1 também controla o único lutador do modo individual (Branco **ou** Preto) e o
jogador do treino.

## Regras do mapeamento (`MapaControles`)

- **Uma tecla, uma ação, nos dois jogadores juntos.** Os dois dividem o teclado; uma tecla nunca é
  ambígua. Tentar usar uma tecla ocupada na tela pergunta se deve **trocar** as duas ações.
- **Reservadas:** ESC e F1–F12 (sistema). TAB não é suportada (o Swing usa para trocar o foco).
- **Toda ação usada pelo slot precisa de tecla** para poder salvar.
- **Modificador lógico:** o arrasto usa "modo controle + esquerda/direita" configurados. O modo
  controle pode ser SHIFT, CTRL, ALT ou qualquer tecla livre (ex.: Q). Cada tecla é registrada com
  todas as combinações de SHIFT/CTRL/ALT, então nada para de funcionar com um modificador segurado.
- **Foco:** quando a janela perde o foco, todas as teclas são soltas (`InputHandler.liberarTodas`),
  para nenhum lutador ficar andando sozinho.
- Um mapa com conflito nunca chega ao jogo: `PreferenciasControles` valida ao carregar e ao salvar e
  o `InputHandler` cai nos padrões se receber um.

## Persistência (`controles.json`)

Arquivo próprio, ao lado de `configuracoes.json` (que não mudou de formato):

```json
{
  "tipo": "CONTROLES_IRMAOS_MIAU",
  "versaoEsquema": 1,
  "jogadores": {
    "jogador1": { "MOVER_ESQUERDA": "A", "INICIAR_PEGADA": "H", "MODO_DESLOCAMENTO_CONTROLADO": "SHIFT", ... },
    "jogador2": { "MOVER_ESQUERDA": "LEFT", "ROMPIMENTO_TECNICO": "U", ... }
  }
}
```

| Situação | Comportamento |
|---|---|
| Sem arquivo | Padrões; nada é criado até SALVAR |
| JSON corrompido, tipo/versão errados, ação/jogador/tecla desconhecidos, conflito, tecla reservada | Padrões + aviso ao abrir o jogo; o arquivo **fica intacto** e só vira `controles.json.corrompido-<data>` quando o jogador salvar |
| Ação ausente (arquivo de versão anterior) | Recebe a tecla padrão se estiver livre; senão fica **sem tecla**, com aviso, e a tela CONTROLES pede uma tecla antes de salvar |
| Falha de disco ao salvar | Mensagem na tela; os controles anteriores continuam valendo |

## Tela CONFIGURAÇÕES → CONTROLES

- Botões JOGADOR 1 / JOGADOR 2; lista com rolagem agrupada em MOVIMENTO, COMBATE, DEFESA.
- Clicar na tecla de uma ação → "pressione uma tecla…" → a tecla apertada passa a valer.
  ESC cancela. Reservadas e não suportadas são recusadas com mensagem.
- **Durante a captura todas as teclas são consumidas pela tela** (`KeyEventDispatcher`): nenhum
  botão reage a ESPAÇO/ENTER e nenhum comando de jogo roda (não há partida aberta nas telas de menu).
- RESTAURAR PADRÕES (os dois jogadores), SALVAR e VOLTAR (pergunta salvar/descartar/cancelar).
- Os controles salvos valem a partir da próxima partida.

## Ajuda ao jogador

- **Faixa de dicas do HUD** (uma linha, contextual): mostra só as ações úteis agora, com as teclas
  configuradas. Livre: `H pegar`; aplicador: `T desequilibrar · Y reforçar · SHIFT+A/D arrastar`;
  receptor: `U romper`. O modo individual mostra a mesma faixa de antes.
- **Pausa → CONTROLES:** todas as ações de cada jogador em campo, linha "AGORA" com as ações de
  combate do estado atual e os atalhos fixos. ESC ou VOLTAR fecha (o jogo continua pausado).

## Como adicionar uma ação lógica

1. Nova constante em `AcaoJogador`: sufixo do comando, rótulo, verbo, grupo, acionamento e a tecla
   padrão de cada slot (`null` = o slot não usa). A tela CONTROLES, a ajuda, a validação e o
   `controles.json` passam a conhecê-la automaticamente.
2. Escolha uma tecla padrão **livre nos dois slots** (o teste `padraoIdenticoAosControlesAnteriores`
   e `MapaControles.problemas()` acusam repetição).
3. Leia a ação no `GamePanel` com `input.isAtiva(slot, acao)`: contínua direto; toque com
   `DetectorToques`; se for de combate, inclua em `AcoesContextuais.ACOES_DE_COMBATE` e trate em
   `executarAcaoCombate`.
4. Compatibilidade: arquivos antigos não têm a ação e recebem a tecla padrão (ou ficam sem tecla
   com aviso). **Não renomeie** nomes de constantes já gravados; para renomear ou remover uma ação,
   aumente `RepositorioControles.VERSAO_ESQUEMA` e converta o nome antigo ao ler.

## Plano de migração para ações contextuais (entrega futura)

Hoje cada técnica tem uma ação: H → `INICIAR_PEGADA`, T → `TENTAR_DESEQUILIBRIO`, U →
`ROMPIMENTO_TECNICO`, Y → `REFORCAR_PEGADA`. Os pontos de extensão já existem:

- `PapelCombate.de(sistema, lutador)`: LIVRE, ENTRANDO, APLICADOR ou RECEPTOR;
- `AcoesContextuais.disponiveis(papel, slot)`: o que faz sentido no estado atual;
- `GamePanel.executarAcaoCombate(slot, acao)`: único ponto que traduz ação → `SistemaCombate`.

Passos previstos:

1. Criar a ação genérica `EXECUTAR_TECNICA` (e, se fizer falta, `TECNICA_ALTERNATIVA`) com tecla
   padrão livre, sem remover H/T/U/Y.
2. Dar ao lutador uma lista de **técnicas equipadas** (ids do catálogo de técnicas, por perfil).
3. `AcoesContextuais` passa a escolher, entre as equipadas, a primeira autorizada pelo papel e pelo
   estado (alcance, orientação, firmeza, fôlego, recarga): ex. LIVRE → uma pegada; APLICADOR →
   desequilíbrio/queda; RECEPTOR → rompimento/defesa.
4. `executarAcaoCombate` despacha a técnica escolhida para o `SistemaCombate` existente.
5. Só depois de validado no jogo: H/T/U/Y viram atalhos opcionais (ou somem com migração do
   `controles.json`, aumentando a versão do esquema).

## Limitações atuais

- **Teclado compartilhado:** muitos teclados não registram várias teclas ao mesmo tempo
  (*ghosting*), principalmente com os dois jogadores segurando várias teclas.
- O Jogador 2 ainda não aplica pegada e o Jogador 1 não rompe (regra do motor, não do teclado).
- Teclas fora da lista suportada (Ç, acentos e algumas teclas de layouts não americanos) são recusadas.
- Mudanças nos controles valem na próxima partida (uma partida em andamento mantém o mapa com que abriu).
- Sem suporte a gamepad.
