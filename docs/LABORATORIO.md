# Laboratório técnico do treino (Entrega 5Q.2)

Ferramenta **só do modo treino** para começar direto numa posição e repetir uma técnica quantas vezes quiser.
Não muda nenhuma regra de combate: a situação é aplicada pelo próprio `MotorSolo`
(`MotorSolo.iniciarSituacao`) e tudo o que aparece no HUD técnico e no catálogo vem do motor.

## Como usar

1. Entre no **treino** (menu principal → treino), escolha personagens, faixas e kimonos como sempre.
2. Na luta, abra a **pausa** (ESC / Start) → **LABORATÓRIO**.
3. Na vista do laboratório:
   - **SITUAÇÃO: …** — cada clique/confirmação passa para a próxima posição;
   - **CONTROLA: J1/J2** — inverte os papéis (quem domina ou quem tem a guarda);
   - **J2: PASSIVO/CONTROLADO** — com CONTROLADO, um segundo jogador usa o teclado virtual (WASD/setas/E/Q/O/P) ou um
     gamepad no J2 (como numa partida); PASSIVO é o parceiro parado de sempre;
   - **INICIAR SITUAÇÃO** — recria os lutadores (fôlego e condicionamento cheios, mesmas faixas e kimonos) e começa
     na posição escolhida, já voltando para a luta;
   - **DESLIGAR LABORATÓRIO** — volta ao treino comum; **VOLTAR** — volta à pausa.
4. Durante a situação:
   - **F5** (ou pausa → **REINICIAR SITUAÇÃO**) repete a mesma situação, com os mesmos papéis;
   - **F8** mostra/esconde o **catálogo** das finalizações da posição atual.

Com o laboratório desligado o F5 continua reiniciando o treino como antes. A partida normal não tem laboratório.

## Quem é o "ativo" (CONTROLA)

| Situação | Posição real (`Posicao`) | O lado que CONTROLA fica |
|---|---|---|
| Em pé, sem pegada | `EM_PE_SEPARADOS` | — (ninguém controla) |
| Em pé, com pegada | `EM_PE_PEGADA` (PEGADA_1 de verdade pelo `SistemaCombate`, estabelece em ~0,5 s) | aplica a pegada |
| Solo livre | `EM_PE_CONTRA_SOLO` | em pé |
| Guarda fechada / aberta / meia / borboleta / De La Riva / laço / aranha / single-leg X | `GUARDA`, `GUARDA_ABERTA`, `MEIA_GUARDA`, `GUARDA_BORBOLETA`, `DE_LA_RIVA`, `GUARDA_LACO`, `GUARDA_ARANHA`, `SINGLE_LEG_X` | por baixo (com a guarda) |
| 100 kg / joelho na barriga / montada / costas | `CONTROLE_LATERAL`, `JOELHO_BARRIGA`, `MONTADA`, `COSTAS` | por cima / nas costas |
| Ashi garami | `ASHI_GARAMI` | segurando a perna |

Laço e aranha começam com a **manga** presa por quem controla (sem ela essas guardas não existem).
Fora da lista ficou só o **solo disputado** (transitório, sem controlador).

## O que a inicialização garante (testado)

Mesma linha, distância exata da posição, posturas certas (por baixo deitado; por cima de joelhos ou em pé na De La
Riva/laço/single-leg X/solo livre), vínculo nos dois, papéis e controlador do motor, manga quando exigida,
nenhuma tentativa/finalização/vitória pendente, fôlego e condicionamento cheios, faixa e kimono das escolhas.

## HUD técnico (centro, abaixo dos avisos)

`LAB · POSIÇÃO · CONTROLA Jx` · `J1 FAIXA … · J2 FAIXA …` · a finalização em curso (`ENTRADA` ou `DISPUTA 45%`),
a técnica na janela ou o último desfecho (defesa, escape, rendição, finalização, pela frase do narrador) e o
**BLOQUEIO** mais recente (o motivo devolvido pelo motor, ex.: `GRADUAÇÃO AZUL`, `SEM A MANGA`).

## Catálogo (F8)

Lista as finalizações **reais** da posição atual, para cada lado que tem o papel delas, com: **LIBERADA**,
**FAIXA X** (graduação), **NÃO APRENDIDA**, **PROIBIDA NO MODO** ou **AGORA NÃO: motivo** (requisito do momento).

## Limitações

- O menu do laboratório passa as situações só para a frente (16 opções, circular).
- O J1 no teclado continua sem L1/L2 (use o Xbox para as finalizações do J1).
- "J2 controlado" religa os dispositivos do J2 ao iniciar a situação; trocar a opção pede um INICIAR/F5.
- Sem um "modo campeonato" ligado à tela (a tabela provisória da 5Q continua fora do jogo).

## Checklist físico (5Q e 5Q.1) usando o laboratório

- [ ] Guarda fechada, J1 controla: LB ↑ armlock → J2 (controlado) defende com ↓ → escapa com RB ↑ (pressão ≤ 45%) → guarda aberta; F5 repete
- [ ] Mesma situação com CONTROLA J2: o J2 ataca, o J1 defende/bate (RB ↓ / 4 + 7)
- [ ] Montada, faixa branca: americana sai; kimura mostra "FAIXA AZUL" no catálogo e "·Az" na cruz
- [ ] Costas: LT ↑ mata-leão até "J1 VENCEU POR FINALIZAÇÃO"? (no treino só avisa) → F5 volta às costas com fôlego cheio
- [ ] Ashi garami com faixa roxa: chave de pé reta liberada; joelho/mão de vaca/panturrilha pedem marrom
- [ ] De La Riva: LT+LB ↑ berimbolo → costas; LT+LB ↓ entra na perna
- [ ] Laço e aranha: começam com a manga; L2 ← / L1 ↓ trocam entre elas
- [ ] Em pé com pegada: J2 aplicando; RS ← tiro de guarda, RS ↓ derrubada forçada
- [ ] Dois Miaus iguais, faixas e kimonos diferentes: a faixa acompanha a cintura em todas as situações
- [ ] Fortaleza / Campeonato / Academia dos Irmãos Miau: os efeitos vivos não atrapalham a leitura

## Complemento da 5Q.8

O catálogo do laboratório lista também as finalizações do RT (montada, 100 kg e costas: 12 nas costas,
por exemplo), com o mesmo status por graduação (LIBERADA / GRADUAÇÃO / NÃO APRENDIDA). As 4 entradas do
LT + LB das costas não aparecem como finalizações: são rotas para as chaves de perna do ashi.
