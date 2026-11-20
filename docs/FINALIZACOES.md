# Finalizações (Entrega 5Q)

> Substitui a parte "nada implementado" de `FINALIZACOES_PLANEJAMENTO.md`: os 48 espaços agora apontam para
> técnicas reais. Base: o *Manual técnico de finalizações e transições* (5Q), com os ajustes indicados abaixo.

Nomes dos controles: **L1** = LB / E, **L2** = LT / Q, **R1** = RB / O (J1 teclado: 4), **R2** = RT / P,
**RS** = analógico direito / setas (J1 teclado: 2/6/7/8).

## 1. Arquitetura

| Peça | Papel |
|---|---|
| `posicional/Finalizacao` | dados de uma finalização numa posição (técnica base, categoria, graduação, comando, requisito, custo, entrada, ganho de pressão, escape, falha) |
| `posicional/CatalogoFinalizacoes` | o repertório por posição (uma ação por comando) |
| `posicional/RegistroFinalizacoes` | os 48 espaços (ids estáveis `fin.<posição>.<família>.<direção>`), cada um apontando para a técnica real |
| `MotorSolo.Ataque` / `FaseFinalizacao` | o ataque em andamento (no máximo um), com fase e pressão |
| `MotorSolo.Vitoria` | fim da luta por finalização (vencedor, técnica, rendição ou consolidada) |
| `regras/Graduacao`, `regras/ModoRegras` | graduação lógica do lutador e tabela de elegibilidade por modo |
| `IrmaoMiau.getGraduacao / aprendeu` | graduação e técnicas aprendidas (independentes da faixa desenhada) |
| `MotorSolo.efeitoVisual` + `AnimadorSolo.aplicarEfeito` | poses provisórias dos dois lutadores, sempre a partir do estado do motor |

## 2. Estados de uma tentativa

**Preparação** (requisitos, conferidos antes; recusa não cobra nada) → **Entrada** (janela da técnica × 1,3;
quem ataca fica exposto) → **Disputa** (pressão 0–100, começa em 30) → **Desfecho**.

| Quem | Comando | Efeito |
|---|---|---|
| Ataca | RS ↑ ou o mesmo comando da técnica | **Apertar**: +14 × eficiência (controle e cansaço); recarga 12 ticks |
| Ataca | RS ↓ | **Soltar**: o ataque acaba, a posição continua |
| Defende | RS ↓ | **Defender** (rótulo por categoria: PROTEGER BRAÇO / MÃOS NO PESCOÇO / GIRAR O PÉ). Na entrada: **corta a entrada**. Na disputa: −22 × eficiência (fôlego e cansaço); recarga 12 |
| Defende | R1 ↑ | **Escapar**: só com pressão ≤ 45 → posição de escape; acima disso falha (+8 de pressão, recarga 24) |
| Defende | R1 ↓ | **Bater** (rendição): prioridade sobre tudo no mesmo tick; solta o controle na hora |

Sozinha, a pressão sobe 0,35–0,40 × eficiência por tick: sem defesa a técnica consolida (~3 s). Defender a cada
recarga vence um atacante que só espera; quem aperta e controla bem ganha a disputa. Sem aleatoriedade.

Desfechos: **finalização consolidada** (pressão 100) ou **rendição** → vitória; **defesa** (pressão 0) → a
posição de *falha* da técnica (algumas custam a posição, ex.: armlock da montada → o outro por cima na sua
guarda); **escape** → a posição de *escape*; **soltar** → nada muda.

## 3. Catálogo (graduação mínima entre parênteses)

### Guarda fechada (quem está por baixo)
| | ↑ | → | ↓ | ← |
|---|---|---|---|---|
| L1 | Armlock (Br) | Kimura (Br) | Omoplata (Az) | Chave de punho (Az) |
| L2 | Cruzado (Br) | Triângulo (Br) | Guilhotina (Br) | Loop choke (Rx) |
| L1+L2 | entrada na perna | entrada na perna | entrada na perna | entrada na perna |

Requisitos: armlock/kimura/omoplata/triângulo = o outro **sem postura**; guilhotina = o outro **exposto** (postura
quebrada); cruzado/punho/loop = controle ≥ 40. Escape: a guarda abre (guarda aberta).

### Cem quilos (quem controla)
| | ↑ | → | ↓ | ← |
|---|---|---|---|---|
| L1 | Americana (Br) | Kimura (Br) | Armlock (Az) | Braço reto (Az) |
| L2 | Cortando o pão (Az) | Katagatame (Az) | Ezequiel (Rx) | Baseball bat (Rx) |
| L1+L2 | entrada na perna (arriscada) | … | … | … |

Escape: o outro recupera a guarda aberta.

### Montada (quem controla)
| | ↑ | → | ↓ | ← |
|---|---|---|---|---|
| L1 | Armlock (Br) | Americana (Br) | Kimura (Az) | Chave de punho (Az) |
| L2 | Cruzado (Br) | Ezequiel (Az) | Katagatame (Az) | Triângulo da montada (Rx) |
| L1+L2 | entrada na perna (abandona a montada) | … | … | … |

Escape: meia-guarda. Armlock e triângulo defendidos: perde a montada (o outro por cima, na guarda).

### Costas (quem controla)
| | ↑ | → | ↓ | ← |
|---|---|---|---|---|
| L1 | Armlock (Az) | Kimura (Rx) | Braço reto (Az) | Hammerlock (Rx) |
| L2 | Mata-leão (Br) | Arco e flecha (Az) | Gola por trás (Az) | Cruzado pelas costas (Rx) |
| L1+L2 | entrada na perna (abandona as costas) | … | … | … |

Escape: o outro gira para a guarda (por cima).

### Controle de perna (ashi garami, quem segura a perna)
| L1+L2 | ↑ Chave de pé reta (Rx) | → Chave de joelho (Mr) | ↓ Mão de vaca (Mr) | ← Compressão de panturrilha (Mr) |
|---|---|---|---|---|

Escape: solo disputado. **Ajuste ao manual:** no ashi garami a direção escolhe sempre a mesma chave (um mapa só
para memorizar); nas outras posições o L1+L2 é só a **entrada** (a chave vem depois, no ashi garami). Não existe
heel hook.

### Guardas aranha e laço (por baixo, com a manga)
Aranha: L1 → armlock (Az), L1 ← omoplata (Az), L2 → triângulo (Az). Laço: L1 ← omoplata (Az).
(L1 ↑ / L1 ↓ nessas guardas são trocas de guarda; por isso os ataques ficam nos lados.)

## 4. Entradas no controle de perna (L1+L2)

| De | Requisito | Janela | Se falhar |
|---|---|---|---|
| Guarda fechada (por baixo) | próprio controle ≥ 30; a postura do outro na janela segura | 14 | a guarda abre (guarda aberta) |
| 100 kg / joelho / montada / costas | próprio controle ≥ 50; os escapes do outro entram como contra | 16 | **o outro recupera a guarda** |
| Guarda aberta / single-leg X (L1+L2 ↑), De La Riva (L1+L2 ↓) | o outro sem postura | 10 | continua |

O **berimbolo continua sendo L1+L2 ↑ na De La Riva**; a entrada na perna da DLR é L1+L2 ↓.

No ashi garami: quem controla ajusta (RS ↓) ou solta (R1 ↓); quem tem a perna presa esconde o pé (RS ↓), gira e
livra (RS ←/→, controle < 60) ou levanta puxando a perna (R1 ↑, controle < 45).

## 5. Graduação, aprendizado e modo (separados)

- **Graduação lógica** (`IrmaoMiau.setGraduacao`): Branca → fundamentos; Azul → intermediárias; Roxa → transições
  complexas e a chave de pé reta; Marrom → chaves de perna avançadas; Preta → tudo. **Padrão dos dois Miaus:
  Preta** (repertório completo no modo livre); a faixa desenhada é só cosmética.
- **Técnica aprendida** (`esquecerTecnica` / `aprenderTecnica`, pela técnica base): padrão = todas.
- **Modo** (`MotorSolo.setModoRegras`): `LIVRE` (padrão) libera tudo o que a graduação libera.
  `CAMPEONATO_PROVISORIO` é uma tabela **provisória e conservadora** (só a chave de pé reta entre as de perna) e
  **precisa ser conferida com o regulamento oficial** antes de ser usada; não está ligado a nenhuma tela.
- Uma técnica bloqueada (graduação, aprendizado, modo, requisito, recarga ou fôlego) não começa, não anima e não
  cobra nada.

HUD: técnica disponível = clara; requisito de posição/controle = cinza; graduação = cinza com a sigla
("KIMURA·Az"); proibida/não aprendida = cinza com "·X". Durante a disputa a cruz mostra "ATACANDO X 62%" /
"DEFENDA: X 62%" com as ações de cada lado.

## 6. Fim da luta

`MotorSolo.getVitoria()` → o `GamePanel` encerra a partida pela tela de fim existente
("J1 VENCEU POR FINALIZAÇÃO · ARMLOCK" ou "… (BATEU)"); REINICIAR LUTA / TENTAR NOVAMENTE limpam tudo. No
**treino** a vitória vira só um aviso e a luta segue. Eventos `FINALIZACAO` (e `ESCAPE` nos escapes) entram em
`consumirEventosLuta()` para a futura pontuação; nenhum ponto é dado. Sem lesões, desmaios ou dano persistente.

## 7. Grafo de guardas e transições (implementado)

```text
ABERTA ──L1←── BORBOLETA ──L2→── SINGLE-LEG X ──L1+L2↑── ASHI GARAMI
  │  ──L1→── DE LA RIVA ──L2→── SINGLE-LEG X
  │            ├──L1+L2↑── BERIMBOLO → COSTAS
  │            └──L1+L2↓── ASHI GARAMI
  ├──L2↓ pegar a manga──L1↓── LAÇO ──L2←── ARANHA ──L1↓── LAÇO
  ├──L2←── ARANHA (com a manga)            ARANHA/LAÇO/BORBOLETA/DLR/SLX ──L1↑── ABERTA
  └──L1+L2↑── ASHI GARAMI
FECHADA ──RS←/→ soltar── ABERTA      ABERTA ──RS↓ fechar── FECHADA
100 KG / MONTADA / COSTAS / FECHADA ──L1+L2── ASHI GARAMI (ou falha)
```
Toda troca de guarda tem janela curta e expõe a base (um passe do outro na janela entra como contra).

## 8. Procedimento de teste

Automático: `MotorSolo5QTest`, `Finalizacoes5QGamePanelTest`, `Familias5P2GamepadTest` (Xbox) e os da 5P/5P.2.
Manual: ver `docs/REGRESSAO.md` (seção 5Q).

## 9. Pendências e sugestões

- 50/50 e outros controles de perna; borboleta → costas; omoplata/triângulo a partir da aranha com animação própria.
- Defesas específicas por direção (empilhar no triângulo, girar para o lado certo no armlock) usando o RS ←/→.
- Tabela oficial do modo campeonato (conferir o regulamento antes).
- Persistir graduação e técnicas aprendidas nos perfis.

## 10. Entrega 5Q.1 — graduação selecionável (fonte única)

- A graduação do `IrmaoMiau` define ao mesmo tempo os **atributos**, a **cor da faixa** e a **elegibilidade**
  das técnicas. **Padrão agora: faixa branca** (os atributos originais); na 5Q era preta. Os testes da 5Q que
  usam o repertório completo colocam a faixa preta explicitamente.
- Atributos: `regras/EvolucaoGraduacao`, evolução composta a partir da branca, `floor` a cada promoção
  (Azul ×1,20, Roxa ×1,17, Marrom ×1,18, Preta ×1,15), conta exata em `BigDecimal`. O **peso não muda**.
  A base nunca é alterada (`IrmaoMiau.getAtributosBase()`); trocar a faixa recalcula a partir dela.

| Atributo | Branco: Br / Az / Rx / Mr / Pt | Preto: Br / Az / Rx / Mr / Pt |
|---|---|---|
| Condicionamento | 35 / 42 / 49 / 57 / 65 | 25 / 30 / 35 / 41 / 47 |
| Fôlego | 25 / 30 / 35 / 41 / 47 | 40 / 48 / 56 / 66 / 75 |
| Força | 42 / 50 / 58 / 68 / 78 | 18 / 21 / 24 / 28 / 32 |
| Velocidade | 22 / 26 / 30 / 35 / 40 | 45 / 54 / 63 / 74 / 85 |
| Agilidade | 25 / 30 / 35 / 41 / 47 | 45 / 54 / 63 / 74 / 85 |
| Técnica | 25 / 30 / 35 / 41 / 47 | 28 / 33 / 38 / 44 / 50 |
| Recuperação | 22 / 26 / 30 / 35 / 40 | 38 / 45 / 52 / 61 / 70 |
| Peso | 72 (sempre) | 68 (sempre) |

- Faixa desenhada: branca, azul, roxa, marrom, preta, com contorno cinza por dentro (visível em kimono da mesma
  cor) e a tarja na ponta (vermelha na preta). Desenhada na mesma camada do corpo, então acompanha a cintura
  em todas as poses e animações (nenhuma animação duplicada).
- Kimono (casaco e calça juntos): branco, azul ou preto, para qualquer personagem. Padrões reais: Branco =
  preto, Preto = branco.
- Seleção (partida e treino): cada slot escolhe personagem, **FAIXA** e **KIMONO** (no painel do jogador, com
  prévia e atributos calculados). J1 sobe da linha LUTAR até FAIXA/KIMONO; o J2 (partida) usa ↑/↓ para trocar
  entre faixa e kimono e ←/→ para mudar; no treino, o J1 também escolhe FAIXA/KIMONO do parceiro. A escolha
  segue na `EscolhaLutador`, então REINICIAR LUTA recria tudo igual (sem bônus duplicado).

## 11. Arenas vivas (5Q.1)

`Cenario.atualizarAmbiente(tick)` (uma vez por tick do loop; sem timer próprio). Camada dinâmica desenhada
depois do cache estático e **atrás** dos lutadores: neve caindo na Fortaleza, bandeirinhas tremulando e torcida
pulando no Campeonato, copa da árvore balançando na Academia dos Irmãos Miau.

## 12. Entrega 5Q.5 — tempo de reação, escapes e parâmetros centrais

Todos os números de velocidade e pressão das finalizações estão em **`posicional/ParametrosFinalizacao`**
(o `MotorSolo` só os repassa). O jogo roda a ~62 ticks/s.

| Parâmetro | Antes | 5Q.5 | Por quê |
|---|---|---|---|
| Entrada (fator sobre a janela 10/14) | 1,3 (13/18 ticks ≈ 0,2-0,3 s) | **3,0** (30/42 ticks ≈ 0,5-0,7 s) | não dava tempo de cortar a entrada |
| Pressão passiva | 0,35-0,40/tick | **× 0,6** | sem defesa, 100% em ~6-8 s (antes ~3 s) |
| Consolidar em 100% | no mesmo tick | **NA PONTA por 45 ticks (~0,7 s)** | sempre há tempo de bater, defender ou soltar |
| Postura quebrada (exposição) | 40 ticks | **75 ticks**; a postura não volta nos primeiros **40** | janela real da guilhotina/triângulo/raspagens |
| Aperto, defesa, escape, recargas | 14 / 22 / ≤45 / 12 | iguais | o ritmo da disputa já era bom |

Comandos: ver `docs/CONTROLES.md` (5Q.5). O "soltar" do triângulo não respondia porque, com o LT ainda
segurado, o RS ↓ chegava como **L2 ↓** e era recusado ("EM FINALIZAÇÃO"); o mesmo acontecia com a defesa.
O bater podia perder para outro comando do mesmo tick (`putIfAbsent`): agora sempre ganha.

### Escapes (100 kg, montada, joelho na barriga, meia-guarda)

Causa: quem está por cima cancelava qualquer escape com um único ajuste instantâneo (consolidar/postura),
sem custo nem recarga, e o enquadrar não construía nada além de −25 de estabilidade.

- **Enquadrar cria ESPAÇO** por 60 ticks: o escape seguinte tolera **+20** de estabilidade e o consolidar de
  quem está por cima **não cancela**, só **fecha o espaço** (+10 em vez de +25). Reagir a tempo ainda defende.
  Rota dos 100 kg: **enquadrar (RS ↓) → recuperar a guarda (RS ←/→)**; montada: enquadrar → escapar (←/→);
  joelho: enquadrar → empurrar o joelho (←/→).
- Quem usa um **ajuste como contra** fica **24 ticks** sem outro ajuste (antes: contra infinita e grátis).
- **Meia-guarda**: novo ajuste de quem está por baixo, **underhook (RS ←/→)**, que quebra a pressão de ombro
  (mesma exposição da postura quebrada). Rota: underhook → recuperar a guarda (RS ↓) / raspar (RS ↑).
- Sem enquadrar, o domínio continua vantajoso: o consolidar cancela o escape como antes.

### Quedas

- **Rasteira**: entra contra quem andou nos **últimos 24 ticks** (antes: só no tick exato), com base abaixo de
  **85** (antes 70; a base volta rápido a 100), exposto ou desequilibrado. Parado com base cheia, ou com RT na
  janela, continua não entrando.
- **Joelho na barriga direto (RB ↓)**: entra se o outro **não reagir** (retenção com os pés / puxar guarda na
  janela). Antes exigia o outro já exposto e o RB ↓ direto sempre falhava. Com as pernas presas a retenção é
  recusada.

### Repertório

O catálogo do código tem **40 finalizações executáveis** (23 técnicas-base, por posição). O spec da 5Q.5 cita
47: nada foi adicionado nem removido; a divergência fica registrada aqui.
