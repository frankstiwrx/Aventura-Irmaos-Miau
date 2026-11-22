# Rig 2.0: conceito visual (Superentrega 6.0.1)

Primeira proposta conceitual do **Irmão Miau Branco** em várias perspectivas.

**Material de estudo:** nada aqui é usado pelo jogo. Esta pasta não altera o renderizador de produção, o MiauStudio, as animações, o motor de combate nem o `pom.xml`. A pasta `docs/` também não entra no build do Maven.

## Arquivos

| Arquivo | O que é |
|---|---|
| `previas/prancha-cinco-perspectivas.png` / `.svg` | Prancha principal: frente, 3/4 frontal, perfil, 3/4 costas e costas, com guias de proporção. |
| `previas/combate-3-4.png` / `.svg` | Posição de combate na vista principal (3/4, 40°). |
| `previas/estrutura-3-4.png` / `.svg` | Vista técnica: raízes, articulações, ossos, ordem das camadas e guinada da câmera. |
| `previas-refinadas/` | As mesmas três prévias após o refinamento 6.0.2 (perfil, pescoço e gola), mais `comparativo-prancha.png` e `comparativo-combate.png` (antes × depois). |
| `previas-6.0.3/` | Pescoço com metade da exposição da 6.0.2 e perfil sem o traço da bochecha. Inclui `comparativo-prancha.png` e `comparativo-combate.png` com as três versões (6.0.1, 6.0.2 e 6.0.3). |
| `previas-6.0.4/` | **Versão visual aprovada (referência atual):** pescoço curto, cabeça mais perto da gola. Inclui os comparativos 6.0.1 × 6.0.3 × 6.0.4 (`comparativo-prancha.png`, `comparativo-combate.png`) e o detalhe ampliado da cabeça, do pescoço e da gola (`comparativo-detalhe-pescoco.png`). |
| `ConceitoRig2.java` | Modelo do Rig 2.0 (esqueleto, poses parametrizadas, câmera e desenho) e gerador das prévias em PNG e SVG. Gera a versão 6.0.4 sem nenhuma diferença de pixel e contém as verificações da 6.1. Na 6.2, a pose ficou explícita (quadril, ombros, pés livres com inclinação) para poder ser animada. |
| `LaboratorioRig2.java` | **6.1:** laboratório interativo em Swing que usa o mesmo modelo: rotação contínua de 0° a 180°, giro da cabeça, poses, braços, presets de pernas e esqueleto (seção 11). |
| `laboratorio-6.1/` | Capturas da janela do laboratório e, em `rotacao/`, as folhas de contato da rotação (passo de 5°, ângulos críticos com passo de 1° e teste dos braços). |
| `AnimacoesRig2.java` | **6.2:** camada de animação baseada em tempo (contrato `Animacao`, `Reprodutor`), as animações idle neutro, idle de combate, caminhada e transições, e as verificações da 6.2 (seção 12). |
| `laboratorio-6.2/` | Capturas da janela com as animações e, em `quadros/`, as folhas de contato (idles, caminhada em várias perspectivas, transições e caminhada girando). |
| `referencias/` | As referências recebidas: o SVG de perspectivas, quatro capturas do Rig 2.0 experimental e o perfil anatômico de gato usado no refinamento. |

As prévias de cada versão ficam congeladas:
- `previas/`: 6.0.1, gerador no commit `e99b380`;
- `previas-refinadas/`: 6.0.2, gerador no commit `8727d73`;
- `previas-6.0.3/`: 6.0.3, gerador no commit `7b7bf71`.

O gerador atual produz a 6.0.4. Para regenerá-la, com os comparativos, use Java 23 sem compilar o projeto:

```
java docs/rig2/ConceitoRig2.java docs/rig2/previas-6.0.4 docs/rig2/previas docs/rig2/previas-6.0.3
```

O primeiro argumento é a pasta de saída. Os seguintes são opcionais: são as versões anteriores usadas nos comparativos, da mais antiga para a mais nova. O gerador também confere:
- que braço, antebraço, coxa, canela e pescoço mantêm o comprimento nas duas poses;
- que o pescoço continua preso ao tronco e que a cabeça mantém a altura nas cinco vistas;
- que o rosto de frente e o de 3/4 não usam a silhueta de perfil.

## 1. Análise das referências

### SVG de perspectivas (`referencias/perspectivas-conceito.svg`)
- Ele confirma a direção pretendida: o mesmo boneco em 0°, ~45°, 90°, ~135° e 180°, com cabeça, ombros, kimono e faixa coerentes entre as vistas.
- Limitações encontradas:
  - Na vista 3/4 frontal, os olhos só se deslocam; a cabeça continua um círculo perfeito, e a gola é a única pista de rotação.
  - Nas vistas de costas, a gola e a faixa somem por completo (não há gola de trás nem volta da faixa).
  - O perfil não tem focinho.
  - Os braços são traços de largura fixa.
  - O tronco é um trapézio único que encolhe no perfil, sem profundidade real.

### Capturas do Rig 2.0 experimental (`referencias/rig2-experimental-*.png`)
- O código delas não está no repositório. Está em `Downloads\MiauExperimental.java`, um laboratório independente que não entra no jogo.
- Pontos fortes que foram mantidos:
  - cabeça e rosto idênticos aos do jogo;
  - kimono preto com faixa preta e ponteira vermelha;
  - membros com IK e ossos de comprimento fixo;
  - raízes em azul e articulações em amarelo.
- Problemas observados:
  - mangas e calças formadas por trapézios separados por uma junta redonda, o que dá aspecto de blocos;
  - gola desenhada como um V chapado;
  - nenhuma noção de profundidade: o modelo existe só "de frente/de lado" e é espelhado.
- **Sobre os valores 145 / 145 / 120:** no laboratório, são **percentuais dos sliders** "Braço" (75–145%), "Antebraço" (75–145%) e "Comprimento das pernas" (85–120%). Multiplicam o comprimento-base de cada osso da pose neutra; não são pixels.
  - Média dos ossos-base dos dois lados: braço 26,7, antebraço 20,4, coxa 38,7 e canela 38,4.
  - Valores no conceito: **braço 39, antebraço 30, coxa 46,5 e canela 46**, em unidades do rig (cabeça = 70).

### Jogo atual (fonte da verdade)
- `IrmaoMiau`, `IrmaoMiauBranco` e `IrmaoMiauPreto` desenham o corpo a partir de uma pose do MiauStudio.
  - A pose tem 26 campos (índices 4..29 em `ConversorMiauStudio`): deslocamentos de cotovelos, mãos, joelhos e pés, mais as rotações de cintura, peito, cabeça e global.
  - As juntas em repouso estão em `getBracosEmRepouso()` e `getPernasEmRepouso()`.
- A orientação hoje é só "virado para a esquerda ou para a direita". Não existe ângulo de vista.

## 2. Proposta visual

**Ideia central: um único esqueleto 3D leve, desenhado em 2D.**

- **O esqueleto:**
  - Cada junta tem `x` (lateral), `y` (altura) e `z` (frente do lutador).
  - A raiz é a pelve. Daí partem pescoço, cabeça, ombros e quadris, e cada membro é resolvido por um **IK de dois ossos em 3D** com comprimento fixo.
  - Nenhuma pose ou vista estica um membro; o gerador confere isso.
- **A câmera:**
  - Só gira em torno do eixo vertical (**guinada**): 0° frente, 40° 3/4 frontal, 90° perfil, 140° 3/4 costas, 180° costas.
  - Tem uma leve inclinação, como se olhasse um pouco de cima: o que está mais perto aparece um pouco mais baixo. É isso que curva a faixa e a barra do casaco e dá a sensação de 2,5D.
- **O desenho:** continua 100% Java2D (`Path2D`, `Area`, `Ellipse2D`). Não há motor 3D nem dependência nova.

### Kimono
- **Casaco por seções elípticas:**
  - O tronco é descrito por uma tabela de alturas com meia-largura e meia-profundidade.
  - A silhueta em qualquer ângulo sai dessas elipses: largo de frente, estreito de perfil e coerente no 3/4.
- **Gola e lapelas como faixas presas à superfície:**
  - Seguem o tronco em volta do pescoço (gola de trás visível nas costas) e descem cruzando o peito, com o painel esquerdo por cima do direito.
  - Só aparecem onde a superfície encara a câmera. Por isso o perfil mostra só a lapela do lado perto, e as costas mostram só a gola.
  - Têm espessura, tom mais claro e uma costura central.
- **Mangas e calças contínuas:**
  - Cada membro é uma única silhueta (dois troncos de cone mais a junta, unidos por `Area`), com um único contorno e sem emendas internas.
  - Têm barra no punho e no tornozelo e um vinco simples no lado interno de cotovelos e joelhos quando dobram.
- **Volume:**
  - A luz vem sempre de cima à esquerda da tela: uma faixa clara nas bordas de cima e uma sombra nas bordas de baixo.
  - É a mesma regra para todas as peças, então o kimono não fica chapado.
- **Costuras:** costuras laterais e da costa central aparecem conforme o ângulo e ajudam a ler a orientação do tronco.
- **Faixa:**
  - A volta acompanha a cintura em todas as vistas, com a linha das duas voltas.
  - O nó e as pontas, incluindo a ponteira vermelha da faixa preta, só aparecem de frente, no 3/4 e no perfil (na borda da frente).
  - Nas costas, aparece só a volta.
- **Saia do casaco:** abaixo da faixa, a borda do painel continua e há uma pequena abertura frontal.

### Cabeça e rosto
- **Frente:** idêntica em espírito ao jogo. O círculo tem diâmetro 70, e os olhos, o nariz rosa e a boca em "λ" estão nas mesmas posições relativas.
- **Volume:** o crânio é um elipsoide um pouco mais fundo que largo (35 × 35 × 38), e um **focinho** (13 × 10 × 13) se une ao crânio.
  - De frente, nada muda.
  - No 3/4, a cabeça alarga um pouco, e um contorno suave mostra o focinho encobrindo a bochecha do lado longe.
  - No perfil, o focinho e o nariz saem da silhueta.
- **Rosto na superfície:**
  - Olhos, nariz e boca são pontos na superfície da cabeça. Ao girar, aproximam-se, estreitam (os olhos ficam ovais) e somem quando passam para trás.
  - Nas vistas de costas, nenhum traço do rosto atravessa a cabeça.
- **Orelhas em concha:** são triângulos com profundidade na base, por isso nunca viram uma linha quando vistas de lado.
  - A parte interna, cinza-rosada clara, só aparece quando a orelha encara a câmera, o que ajuda a distinguir frente e costas.
  - É uma pequena adição, marcada como proposta.
- **Trapaça de câmera:** entre a frente e o perfil, o rosto gira 75% do ângulo do corpo (corpo 40° → rosto 30°). É um recurso clássico da animação 2D para manter os dois olhos legíveis no 3/4 de combate.

## 3. Comparativo com o personagem atual

| Aspecto | Atual (jogo / laboratório) | Conceito |
|---|---|---|
| Vistas | Frente/lado espelhado | Cinco vistas a partir de um só esqueleto; qualquer ângulo intermediário funciona |
| Rosto | Fixo na frente da cabeça | Preso à superfície; gira, estreita e some atrás |
| Cabeça | Círculo | Círculo de frente; crânio e focinho com volume no 3/4 e no perfil |
| Gola | V chapado | Gola em volta do pescoço + lapelas cruzadas, com espessura e costura |
| Mangas/calças | Trapézios com junta redonda | Uma silhueta contínua por membro, com barra e vinco |
| Tronco | Polígono fixo | Seções elípticas; largura e profundidade reais |
| Faixa | Retângulo + nó | Volta que acompanha a cintura + nó e pontas que aparecem só de frente |
| Profundidade | Não há | Lado longe mais escuro e atrás do tronco; câmera levemente de cima |
| Peito | Fundo escuro na abertura | Pelo sombreado na abertura em V (proposta) |
| Proporções | Ossos-base | Braço e antebraço 145%, pernas 120%, como no laboratório |

### O que foi preservado
- Cabeça branca de diâmetro 70.
- Orelhas triangulares nas mesmas posições.
- Olhos pretos simples, nariz rosa e boca em "λ".
- Patas felinas no formato do laboratório (mãos com três dedos marcados; pés arredondados).
- Silhueta cartunesca e cores do kimono e da faixa.
- Raízes em azul e articulações em amarelo.

### Diferença entre a frente e o 3/4
- **De frente:** simetria total. Os dois braços ficam à frente do tronco, a faixa é reta e o nó fica no centro.
- **No 3/4 (40°):**
  - o tronco estreita e mostra a costura lateral do lado perto;
  - as lapelas e o nó se deslocam para o lado da frente;
  - o braço e a perna do lado longe ficam mais escuros e por trás do tronco;
  - a faixa e a barra do casaco curvam com a perspectiva;
  - o rosto se desloca, estreita e revela o volume do focinho.

## 4. Como a estrutura poderia ser animada

- **O que a animação controla:** as **posições-alvo** das mãos e dos pés, mais a inclinação do tronco, o abaixamento do quadril e a guinada da cabeça.
- **O IK fica com o resto:** cotovelos e joelhos saem do IK, e os comprimentos ficam sempre iguais.
- **A vista é só mais um parâmetro.** A mesma animação pode ser vista de 40° na maior parte da luta e girar continuamente, por exemplo de 40° para 90° numa queda ou de 40° para 140° num giro. Não é preciso desenhar quadros novos para cada vista.
- **As poses atuais do MiauStudio são 2D.** Um caminho compatível: a vista atual de lado equivale a guinada de 90°, e os deslocamentos 2D dos cotovelos, mãos, joelhos e pés viram `x`/`y` com `z` = 0. Assim, as animações existentes continuariam valendo como "animações vistas de lado" enquanto as novas ganham profundidade.

### Transições entre vistas
- **Contínua (recomendada):** basta interpolar a guinada; como tudo vem do mesmo esqueleto, não há salto de proporção.
- **Discreta:** usar só as cinco vistas e trocar em 1 ou 2 quadros, escondendo a troca atrás de um movimento rápido (giro, queda).
- Pontos que pedem cuidado: o nó da faixa (aparece e some perto de ±95°), a parte interna da orelha e os traços do rosto. Todos já aparecem e somem por limiares suaves; numa transição contínua, convém um breve *fade*.

## 5. Compatibilidade técnica com o Java2D

Tudo nas prévias usa as mesmas primitivas do jogo: `Path2D`, `Area`, `Ellipse2D`, `BasicStroke`, `AffineTransform`. O SVG é só uma conversão das mesmas formas por `PathIterator`; não é desenhado à parte.

Para levar o conceito ao renderizador, seriam necessários:
1. Um modelo de esqueleto com `z` e uma pose com guinada, ao lado da pose atual e sem substituí-la.
2. Um conversor da pose atual do MiauStudio para o esqueleto (vista de lado = 90°).
3. A função de desenho por camadas (casaco, lapelas, faixa, cabeça, membros) com a ordem de profundidade decidida pela guinada.
4. A integração com o espelhamento atual (`virado para a esquerda`), com o lutador de costas para a câmera, com o `OrdemProfundidade` entre os dois lutadores e com as poses conjuntas (pegadas, chão).

### Custo
- O casaco usa algumas dezenas de amostras por quadro e várias operações de `Area`.
- Para dois lutadores a 60 fps, é recomendável armazenar em cache as silhuetas por vista e pose, ou trocar parte das `Area` por caminhos já calculados.
- Nada disso foi medido no jogo.

## 6. Limitações e o que ainda precisa ser estudado

- **Poses estudadas:** apenas a **neutra** e a **de combate**. Chão, quedas e finalizações não foram estudados; posições deitadas exigem rotação da câmera em outro eixo ou uma regra própria.
- **Miau Preto:** não foi feito. O conceito usa só o Miau Branco com kimono preto. A troca de cores é direta, mas o contraste do rosto preto ainda não foi verificado.
- **Vista 3/4:** o ângulo de 40° e a trapaça de 75% do rosto são escolhas iniciais para avaliarmos juntos.
- **Mãos:** em escorço forte (braço apontado para a câmera), a pata não muda de forma, e a mão fechada em pegada ainda não existe nesse modelo.
- **Pés:** são formas simples; não há pé virado de sola nem dedos dobrados.
- **Pescoço:** na 6.0.1 a cabeça repousava sobre a gola, sem pescoço visível. Isso foi tratado no refinamento 6.0.2 (seção 8).
- **Calça:** não tem reforço visível nos joelhos; ficou de fora para não poluir.
- **Integração:** nenhuma medição de desempenho foi feita no jogo, e nada foi integrado.

## 7. Próxima etapa sugerida

Se o conceito for aprovado, o primeiro incremento seguro seria **um protótipo isolado do esqueleto com guinada dentro do laboratório** (MiauExperimental ou uma tela nova de desenvolvimento), com:
- um slider de guinada de 0° a 180°;
- a pose de combate;
- a conversão de uma única pose existente do MiauStudio (por exemplo, a guarda em pé) para conferir que a vista de 90° reproduz o desenho atual.

Ele não tocaria no renderizador de produção. A integração ao jogo só viria depois dessa comparação lado a lado.

### Sugestões registradas, ainda não implementadas
- Uma luz própria por arena (hoje a luz é fixa).
- Pequena variação de largura da manga quando o braço se estende, simulando o tecido esticado.
- Bochechas mais marcadas no 3/4, se a leitura do rosto ainda parecer "plana".
- Uma versão do conceito com o Miau Preto e com kimono branco e azul.

## 8. Refinamento 6.0.2: perfil, pescoço e gola

Evolução localizada da 6.0.1. Proporções, kimono, lapelas, mangas, pernas, rosto de frente e rosto em 3/4 foram mantidos. Os resultados estão em `previas-refinadas/`; as prévias originais continuam em `previas/`.

### Rosto de perfil (90°)
Antes, a cabeça de perfil era o círculo do crânio com um focinho oval colado à frente. Agora ela tem uma silhueta própria (`PERFIL` no gerador), desenhada a partir da referência anatômica, mas mantendo o cartum:
- **Testa e focinho:** o crânio continua redondo atrás e em cima. A testa desce em uma "parada" suave na altura do olho, e uma ponte curta leva ao nariz.
- **Nariz:** projeta-se na ponta do focinho e virou uma cunha rosa visível. Antes, de perfil, ele quase sumia.
- **Boca e queixo:** a boca desce do nariz e volta para trás, e logo abaixo há um queixo pequeno. A mandíbula curva de volta até o pescoço.
- **Bochecha:** um traço leve marca a almofada dos bigodes, no mesmo cinza do contorno do focinho no 3/4.
- **Olho:** recuou da borda da testa para cerca de um terço do focinho e ficou um oval estreito.
- **Orelhas:**
  - Antes, as duas ficavam exatamente sobrepostas.
  - Agora a de longe aparece um pouco à frente e mais baixa, em branco acinzentado; a de perto fica sobre o alto da cabeça.
- **Tamanho preservado:** cerca de 72 de altura e 80 de comprimento, os mesmos valores da 6.0.1 de perfil.

A silhueta de perfil só entra perto de 90° (peso suave entre ~60° e 90°, e o mesmo do outro lado). Fora disso, a cabeça é exatamente a da 6.0.1.
- Entre as duas silhuetas, o contorno é uma mistura dos raios medidos a partir do centro. Os traços do rosto também se interpolam, então uma rotação contínua não dá salto.
- A trapaça do rosto (corpo 40° → rosto 30°) virou uma curva suave que continua dando 30° no 3/4 e chega a 90° sem salto. Antes, havia um degrau em 90°.

### Pescoço e gola
- **O pescoço:** um cilindro de pelo do pivô do pescoço até o pivô da cabeça.
  - Tem 26 de largura no alto e 29 na base.
  - Segue a inclinação do tronco porque usa os mesmos pivôs.
  - Recebe uma sombra curta projetada pela cabeça e uma sombra lateral.
- **A cabeça subiu 5 unidades** (centro em y 31; era 36). Aparece uma faixa curta de pescoço acima da gola, sem alongar o personagem. O tamanho da cabeça não mudou, mas a altura total cresceu 5 unidades (as guias "orelhas" e "olhos" subiram junto).
- **Ordem de desenho:** o pescoço é desenhado depois do corpo do casaco e antes da gola de trás e das lapelas.
  - Assim, a gola o abraça por fora.
  - O pescoço termina reto na borda de baixo da gola, então não aparece abaixo dela nas vistas de costas e a gola nunca o atravessa.
- **Peito em V:** passou a usar o mesmo tom da base do pescoço, para o pelo continuar do pescoço ao peito sem degrau. A gola em si não precisou mudar de geometria.

### Verificações (executadas pelo gerador)
- Braço, antebraço, coxa e canela mantêm o comprimento nas duas poses.
- A distância entre o pivô do pescoço e o pivô da cabeça é constante, ou seja, o pescoço é rígido.
- A base do pescoço fica dentro do tronco nas cinco vistas e nas duas poses.
- A silhueta da cabeça mede entre 69 e 74 de altura e entre 69 e 82 de largura em todas as vistas.
- Frente (0°), 3/4 (40°), 3/4 costas (140°) e costas (180°) têm peso de perfil 0, ou seja, usam exatamente a cabeça da 6.0.1. O perfil (90°) tem peso 1.
- A guinada do rosto não salta perto de 90°.
- Conferência visual: `comparativo-prancha.png` e `comparativo-combate.png` mostram antes e depois lado a lado.

### Limitações do refinamento
- A silhueta de perfil é uma curva desenhada à mão, não sai do elipsoide. Nos ângulos intermediários (60° a 90°), a mistura de raios é uma aproximação e ainda não foi vista em animação.
- **Rosto de frente e em 3/4:** a cabeça é a mesma da 6.0.1, mas agora fica 5 unidades mais alta e com o pescoço visível embaixo.
- **Pescoço no combate:** como o tronco inclina 8° e a cabeça recua, o pescoço aparece um pouco mais que na pose neutra. Vale avaliar se deve ficar mais curto nessa pose.
- O Miau Preto continua sem versão.

## 9. Ajuste 6.0.3: pescoço mais discreto e perfil mais limpo

Meio-termo entre a 6.0.1 (cabeça colada na gola) e a 6.0.2 (pescoço aparente demais). Nada mais do conceito mudou. As prévias estão em `previas-6.0.3/`.

### Pescoço e cabeça
- **A cabeça desceu 2,5 unidades:** o centro passou de y 31 para y 33,5 (na 6.0.1 era 36).
  - A faixa de pescoço visível acima da gola ficou com cerca de metade da exposição da 6.0.2.
  - A altura total do personagem ficou 2,5 unidades acima da 6.0.1 e 2,5 abaixo da 6.0.2.
- **Pescoço e gola não mudaram de construção.** O pescoço continua indo do pivô do pescoço ao pivô da cabeça, com a sombra curta da cabeça. Ele ainda termina reto na borda de baixo da gola, e a gola continua o abraçando por fora. Como a cabeça desceu, o trecho visível encurtou sozinho; não foi preciso mexer na gola.

### Rosto de perfil
- **Traço removido:** saiu o traço interno da bochecha, perto da boca. O focinho agora é definido só pela silhueta externa (testa, ponte do nariz, ponta do nariz, lábio e queixo), pelo nariz rosa e pela boca.
- **Silhueta, olho, orelhas e boca do perfil:** iguais aos da 6.0.2. Sem o traço, a leitura ficou boa, então não foi preciso compensar no contorno.

### Frente e 3/4
O rosto é o mesmo da 6.0.1 e da 6.0.2: o traço removido só existia no perfil. A única diferença é a altura da cabeça (2,5 unidades mais baixa que na 6.0.2), comum a todas as vistas.

### Verificações
O gerador continua conferindo tudo a cada execução, e todas as conferências passaram:
- comprimentos dos ossos e do pescoço;
- pescoço preso ao tronco nas cinco vistas;
- altura e largura da cabeça;
- frente e 3/4 sem a silhueta de perfil;
- rotação contínua do rosto.

`comparativo-prancha.png` e `comparativo-combate.png` mostram as três versões lado a lado.

## 10. Ajuste 6.0.4: pescoço curto (versão atual)

Mais um passo na direção da 6.0.1, mantendo uma pequena folga entre a cabeça e a gola. As prévias estão em `previas-6.0.4/`.

### O que mudou
**A cabeça desceu mais 1,25 unidade:** o centro passou de y 33,5 para y 34,75.
- A exposição do pescoço é medida pelo quanto a cabeça está acima da posição da 6.0.1, que tinha o centro em y 36 e a cabeça encostada na gola.
- Era 5 unidades na 6.0.2 e 2,5 na 6.0.3; agora é 1,25. É metade da 6.0.3, e uma faixa curta de pescoço continua visível acima da gola.

### Por que mover a cabeça e não encurtar o desenho do pescoço
A solução que mais preserva as articulações é mover só o pivô da cabeça. Assim:
- o pivô do pescoço (y 76), os ombros, o tronco, a gola, as lapelas, os braços (145%), os antebraços (145%) e as pernas (120%) ficam exatamente iguais;
- o pescoço continua um segmento rígido entre os dois pivôs, então uma animação que gire ou incline a cabeça continua girando em torno do mesmo ponto;
- o pescoço continua terminando na borda de baixo da gola, que o abraça por fora. Não foi preciso nenhum ajuste na gola.

### O que não mudou
- **Cabeça:** tamanho e formato iguais.
- **Rosto:** o perfil da 6.0.3 (silhueta, focinho, nariz, boca sem traço, olho e orelhas) e os rostos de frente e 3/4 ficaram idênticos.
- **Corpo:** mãos, pés, kimono, faixa e largura dos ombros também não mudaram.
- **Altura total:** 1,25 unidade acima da 6.0.1.

### Comparativos
- `comparativo-prancha.png`: as cinco vistas nas versões 6.0.1, 6.0.3 e 6.0.4.
- `comparativo-combate.png`: a pose de combate nas três versões.
- `comparativo-detalhe-pescoco.png`: a região da cabeça, do pescoço e da gola ampliada 2,5x, com frente, 3/4, perfil, costas e combate.

### Verificações
O gerador rodou todas as conferências desta versão, e todas passaram:
- comprimento constante de braço, antebraço, coxa, canela e pescoço nas duas poses;
- pescoço preso ao tronco nas cinco vistas;
- cabeça com 69 a 74 de altura em todas as vistas;
- frente e 3/4 sem a silhueta de perfil;
- rotação contínua do rosto.

### Observação
Na pose de combate o tronco inclina 8° e a cabeça recua um pouco, por isso o pescoço aparece ligeiramente mais do que na pose neutra. Vale observar isso quando as poses começarem a ser animadas.

## 11. Laboratório interativo (Superentrega 6.1)

O conceito 6.0.4 virou um laboratório interativo, em que o personagem é calculado a cada ângulo, não escolhido entre imagens prontas. O laboratório é experimental e isolado: não usa nenhuma classe do jogo, não altera `Main`, `MainPrototipo`, `GamePanel`, menus, MiauStudio nem `pom.xml`, e não entra no build do Maven.

### Como abrir (Windows, Java 22 ou mais novo, a partir da raiz do repositório)

Opção direta (o Java compila em memória os dois arquivos da pasta):

```
java docs/rig2/LaboratorioRig2.java
```

Opção com compilação explícita (os `.class` vão para uma pasta temporária, fora do repositório):

```
javac -d "%TEMP%\rig2-lab" docs\rig2\ConceitoRig2.java docs\rig2\LaboratorioRig2.java
java -cp "%TEMP%\rig2-lab" LaboratorioRig2
```

No PowerShell, troque `%TEMP%` por `$env:TEMP`.

Para gerar de novo as capturas e testar automaticamente todos os controles, sem abrir a janela:

```
java docs/rig2/LaboratorioRig2.java --capturas docs/rig2/laboratorio-6.1
```

Para rodar só as verificações do modelo:

```
java docs/rig2/ConceitoRig2.java --verificar-6.1
```

### O que dá para controlar
- **Rotação do corpo:** controle deslizante de 0° a 180°, em passos de 1°, com atalhos para 0°, 40°, 90°, 140° e 180°. Pelo teclado, ← → ou A/D giram 1°, e com Shift giram 10°.
- **Cabeça em relação ao tronco:** de −60° a +60° (Q/E no teclado). O painel mostra também o ângulo final em que o rosto é desenhado.
- **Pose-base:** NEUTRA ou COMBATE. Trocar a pose mantém o ângulo do corpo.
- **Cada braço:**
  - um preset: Da pose, Levantado, Estendido à frente, Recolhido junto ao tronco, Cotovelo flexionado ou Abaixado e solto;
  - três controles deslizantes: elevação lateral (−90° a 170°), avanço à frente (−50° a 150°) e flexão do cotovelo (0° a 150°).
- **Pernas (presets):** Da pose, Joelhos flexionados, Perna esquerda à frente, Perna direita à frente e Pés abertos.
- **Mostrar esqueleto e diagnóstico:**
  - raízes em azul, articulações em amarelo e ossos em laranja (mais escuros do lado longe);
  - eixo do tronco, centro da cabeça com a direção do rosto, pelve, e ombros e quadris marcados como "perto" e "longe";
  - ângulos do corpo e do rosto.
- **Restaurar pose** e **Restaurar perspectiva** (40°, cabeça a 0°; tecla R).
- **Ao abrir:** corpo a 40°, pose de combate, cabeça na orientação original da pose.
- **No painel:** uma conferência ao vivo de que os ossos mantêm o comprimento.

### Auditoria e reaproveitamento
O laboratório usa o mesmo modelo de `ConceitoRig2.java`. Não foi criado um segundo sistema corporal.

**O que já existia e foi reaproveitado:**
- o esqueleto 3D leve (pelve como raiz; pescoço, cabeça, ombros e quadris derivados dela);
- o IK de dois ossos com comprimento fixo;
- a câmera que só gira em torno do eixo vertical;
- o tronco por seções elípticas, as lapelas e a gola presas à superfície;
- a cabeça com a mistura contínua para a silhueta de perfil;
- a ordem de camadas.

**O que dependia de construções fixas:**
- as duas poses eram listas fixas de alvos de IK;
- alguns detalhes apareciam ou sumiam por limiar, aparecendo ou sumindo de uma vez num ângulo exato: olho, nariz e boca, parte interna da orelha, nó da faixa, abertura do casaco, dedos dos pés, vinco das dobras, cor do lado longe e contorno do focinho no 3/4.

**O que mudou no modelo (e as prévias 6.0.4 continuam idênticas):**
1. **Poses parametrizadas (`PoseBase`, `ParametrosPose`, `montarPose`):**
   - Cada braço passou a ser descrito por três ângulos em relação ao tronco (elevação, avanço e flexão do cotovelo); o IK continua garantindo os comprimentos.
   - Os valores padrão são calculados a partir das poses originais e as reproduzem exatamente.
   - A câmera não faz parte da pose, então girar o personagem nunca altera os membros.
2. **Giro próprio da cabeça:**
   - O rosto é desenhado com a guinada da vista mais o giro da cabeça, passando pela mesma "trapaça" de câmera da 6.0.2 (agora também para guinadas negativas).
   - A cabeça continua presa ao pivô do pescoço e não muda de desenho nem de tamanho.
3. **Transições graduais:**
   - Todos os detalhes acima aparecem e somem por esmaecimento ao longo de poucos graus.
   - Nos cinco ângulos de referência, os valores continuam exatamente os da 6.0.4.
4. **Camadas dos braços:**
   - O braço longe continua atrás do tronco, como na 6.0.4.
   - Perto das vistas de frente e de costas puras, onde nenhum lado é "longe", o braço passa para trás do tronco gradualmente.
   - Uma pata "da frente" que fica escondida atrás do tronco nas vistas de costas também vai para trás. Para decidir isso, a profundidade da pata é comparada com a da superfície visível do casaco no mesmo ponto da tela.
   - Isso corrigiu as patas da guarda de combate, que antes eram desenhadas por cima das costas entre ~140° e 180°.

**Como cada parte reage à rotação:**
- Tronco, gola, lapelas e faixa são calculados das elipses a cada ângulo.
- Braços, pernas, mãos e pés vêm do esqueleto projetado.
- A cabeça mistura a silhueta frontal e a de perfil conforme a guinada do rosto.
- As orelhas são triângulos em 3D com profundidade na base.
- O focinho, o nariz e a boca são pontos na superfície da cabeça.

### Verificações executadas

**`java docs/rig2/ConceitoRig2.java --verificar-6.1`:** todas passaram.
- **Comprimentos 3D constantes** em 21 poses: neutra, combate, teste 7.3, os cinco presets de pernas nas duas poses e oito combinações de extremos dos controles.
  - braço 39, antebraço 30, coxa 46,5, canela 46;
  - tronco (cintura → pescoço) 82;
  - pescoço → cabeça.
- **Limites dos controles:** valores fora da faixa são ajustados para os extremos.
- **Poses padrão:** os parâmetros padrão reproduzem exatamente as mãos das poses NEUTRA e COMBATE.
- **Rotação de 0° a 180° em passos de 0,5°,** em todas as poses, num total de 7.581 vistas:
  - nenhuma coordenada inválida;
  - nenhum osso projetado maior que o real, ou seja, a projeção só encurta;
  - nenhuma junta salta mais do que uma rotação rígida permite (o maior passo foi 0,99 do limite).
- **Pose de teste:** braço direito flexionado e esquerdo estendido, idêntica depois de 40° → 90° → 140° → 0°.
- **Guinada do rosto contínua,** inclusive com a cabeça girada ±60° (variação máxima de 0,135° por passo de 0,1°).
- **Continuidade visual:** diferença média por pixel entre quadros vizinhos, com passo de 1°. Não há nenhum salto isolado; o pior quadro de cada pose fica em até 1,6 vez a mediana.

  | Pose | Pior quadro | Diferença | Mediana |
  |---|---|---|---|
  | Neutra | 72° | 1,35 | 0,95 |
  | Combate | 7° | 1,57 | 1,01 |
  | Teste | 3° | 1,81 | 1,17 |

**Preservação da 6.0.4:** as três prévias 6.0.4 foram geradas de novo e comparadas pixel a pixel com as do commit. Deu 0 pixels diferentes.

**`java docs/rig2/LaboratorioRig2.java --capturas docs/rig2/laboratorio-6.1`:**
- Gerou as capturas a partir da própria janela Swing.
- Executou o teste 7.3 e confirmou as articulações idênticas.
- Exercitou todos os controles sem exceções: extremos de todos os sliders, todos os presets de braço e de pernas nas duas poses, rotação com a cabeça em ±60°, botões, esqueleto e teclado.

**A janela real foi aberta com `java docs/rig2/LaboratorioRig2.java`:** apareceu com o título "Rig 2.0 · Laboratório interativo (6.1) · experimental", sem nada na saída de erro, e foi fechada em seguida.

### Capturas (`laboratorio-6.1/`)
- `janela-000.png`, `janela-040.png`, `janela-065.png`, `janela-090.png`, `janela-140.png`, `janela-180.png`: a janela completa em cada ângulo, na pose de combate.
- `janela-bracos-flexionado-e-estendido.png`: braço direito recolhido e flexionado, esquerdo estendido, a 40°.
- `teste-7.3-rotacao-com-bracos.png`: a mesma pose em 40°, 90°, 140° e 0°.
- `janela-esqueleto-3-4.png`: esqueleto e diagnóstico em 3/4.
- `janela-neutra-cabeca-e-perna.png`: pose neutra a 65°, cabeça girada +25° e perna esquerda à frente.
- **Sequências de rotação** (`rotacao/`):
  - `combate-passo-5.png` e `neutra-passo-5.png`: de 0° a 180°, em passos de 5°;
  - `combate-angulos-criticos.png`: passo de 1° em 0–10°, 38–42°, 88–96° e 138–142°;
  - `teste-bracos-passo-10.png`: a pose de teste dos braços, em passos de 10°.

### Limitações
- **Troca de camada entre ~3° e ~15°** (e o simétrico perto de 180°): o braço longe é desenhado duas vezes, uma atrás e outra na frente, e a cópia da frente vai sumindo. Por alguns graus a pata aparece semitransparente. Uma solução definitiva seria dividir o braço em segmentos com profundidade própria (antebraço na frente, braço atrás).
- **Pernas:** na vista exatamente frontal ou de costas são unidas numa só silhueta; fora dela, ficam separadas. A diferença é pequena porque a junção fica sob a saia do casaco.
- **Pernas só por presets:** o controle individual de quadril, joelho e pé ainda não existe.
- **Mãos e pés:** são formas 2D orientadas pelo antebraço e pela guinada do pé. Não há pata fechada para pegada nem sola visível.
- **Cotovelo:** com a elevação lateral muito alta e o avanço muito negativo, ele segue o vetor polo da pose. Algumas combinações extremas deixam o cotovelo em posições pouco naturais, mas sem quebrar o osso. Limites anatômicos por articulação ainda não existem.
- **Rotação só em torno do eixo vertical:** deitar, cair e girar no chão exigiriam outro eixo de câmera ou de corpo.
- **Só o Miau Branco:** o Miau Preto ainda não tem versão.
- **Desempenho:** o desenho recalcula a silhueta e as faixas a cada quadro. No laboratório é imediato, mas não foi medido para dois lutadores a 60 fps.

### Compatibilidade futura com o MiauStudio (análise, sem implementação)
- **Como é a pose do MiauStudio:** 26 valores 2D (`ConversorMiauStudio`, índices 4..29).
  - deslocamento e rotação de cintura e peito;
  - rotação global e da cabeça;
  - deslocamento da cabeça;
  - deslocamentos de cotovelos, mãos, joelhos e pés em relação às juntas de repouso (`getBracosEmRepouso`, `getPernasEmRepouso`).
  - Os valores estão em pixels locais da vista lateral e não têm profundidade.
- **O que pode ser reaproveitado:**
  - as rotações de cintura, peito e cabeça (viram a inclinação do tronco e o giro da cabeça do Rig 2.0);
  - as posições 2D de mãos e pés (viram alvos de IK);
  - a escolha do lado da dobra (vira o vetor polo).
- **Como interpretar uma pose antiga:** tratar a vista do MiauStudio como guinada de 90° (perfil).
  - O x e o y 2D viram z (frente do lutador) e y do corpo, com x lateral = 0.
  - Depois, o IK do Rig 2.0 recalcula cotovelos e joelhos com os comprimentos novos.
  - Com isso, qualquer pose antiga passa a poder ser vista de outros ângulos.
- **Diferenças entre os esqueletos:**
  - **Comprimentos:** o Rig 2.0 tem braços 145% e pernas 120% maiores; as poses antigas precisam ser reescaladas em torno da raiz de cada membro.
  - **Profundidade:** não existe nas poses antigas; os dois braços ficam no mesmo plano, então o lado (perto/longe) teria que vir de uma convenção.
  - **Pescoço:** o MiauStudio não tem pescoço separado.
  - **Poses de chão:** usam a rotação global de 90° e precisariam de um eixo de câmera ou de corpo diferente.
- **Adaptações necessárias:**
  - um conversor de pose (2D lateral → parâmetros do Rig 2.0);
  - um campo opcional de profundidade por articulação no formato de projeto, para as poses novas;
  - um modo de pré-visualização girável dentro do MiauStudio.
  - As animações conjuntas (pegadas, chão) exigem ainda que os dois lutadores compartilhem a mesma câmera.

### Próximos passos sugeridos
1. **Converter uma pose de cada tipo** (guarda em pé, pegada, chão) e comparar no laboratório a vista de 90° com o desenho atual do jogo. É o primeiro passo da 6.2.
2. **Dividir o braço em segmentos com profundidade própria,** para eliminar a troca de camada por esmaecimento.
3. **Controle individual das pernas e limites anatômicos** por articulação.
4. **Integração com o jogo:** só depois de aprovar as etapas anteriores, um renderizador do Rig 2.0 ao lado do atual (sem substituí-lo), ativado apenas em modo experimental, medindo o desempenho com dois lutadores.

## 12. Primeiras animações dinâmicas (Superentrega 6.2)

O Rig 2.0 ganhou uma camada de animação baseada em tempo e cinco animações:
- idle neutro com respiração;
- idle de combate;
- caminhada;
- transição neutra → combate;
- transição combate → neutra.

Tudo continua no laboratório experimental. O jogo, o MiauStudio e o `pom.xml` não foram tocados, e nada foi convertido das animações antigas. As animações foram criadas direto no Rig 2.0.

### Estrutura (três arquivos, um papel cada)
| Arquivo | Papel |
|---|---|
| `ConceitoRig2.java` | **Modelo do corpo e desenho.** Esqueleto, IK de dois ossos, câmera, kimono e cabeça. Não sabe nada de tempo nem de janela. |
| `AnimacoesRig2.java` | **Camada de animação (nova).** Contrato `Animacao`, as cinco animações, o `Reprodutor` e as verificações da 6.2. Não usa Swing nem o relógio do sistema. |
| `LaboratorioRig2.java` | **Janela Swing.** Pede animações, avança o relógio com o tempo real e mostra o resultado. |

**Uma animação é uma função pura do tempo:** `amostrar(t)` devolve os parâmetros do corpo (`ParametrosPose`) no instante `t`. O contrato também tem:
- identificador e nome;
- duração;
- se repete em loop;
- o deslocamento do corpo (para o chão "rolar" na caminhada no lugar);
- a animação seguinte (usada pelas transições);
- a pose-base em que o personagem termina.

**O `Reprodutor` guarda o relógio:**
- reproduzir, pausar, continuar, parar e reiniciar;
- velocidades de 0,5× a 2,0×;
- loop, e o encadeamento da transição para o idle, levando o tempo que sobrou;
- um passo máximo de 0,25 s por chamada, para que uma janela travada não faça o personagem "teleportar".

**Por que funções puras do tempo:**
- O movimento não depende da quantidade de repinturas: a janela avança o relógio pelo tempo real (`System.nanoTime`).
- Qualquer instante é reproduzível nos testes.
- No futuro, o laço do jogo pode alimentar o mesmo `Reprodutor` com o seu próprio `dt`.

**Mudanças no modelo, só o necessário** (as prévias 6.0.4 continuam idênticas pixel a pixel):
- **Pose explícita:** `ParametrosPose` passou a guardar todos os canais (inclinação do tronco, altura e deslocamento do quadril, ombros, ângulos dos braços, alvos dos tornozelos, polos dos joelhos, guinada e inclinação dos pés). A `PoseBase` continua fornecendo os valores padrão. Assim uma animação pode interpolar qualquer canal, e `ParametrosPose.interpolar(a, b, …)` mistura duas poses canal a canal.
- **Corpo:** ganhou o deslocamento do quadril (lateral e para a frente), aplicado ao corpo todo exceto aos pés, e a elevação dos ombros.
- **Pés:** ganharam inclinação em torno do tornozelo (calcanhar levantado ou ponta levantada). Sem inclinação, o pé é desenhado exatamente como na 6.0.4.
- **Pernas:** a caminhada controla cada perna pelo alvo do tornozelo, pelo polo do joelho e pela orientação e inclinação do pé. Quadril, coxa, joelho, canela e tornozelo participam, sempre pelo IK com comprimentos fixos; as pernas não são deslocadas como peças rígidas.

### As animações
- **Idle neutro (ciclo de 7,2 s, duas respirações):**
  - na inspiração, os ombros sobem um pouco e o peito se abre (o tronco recua menos de 1°);
  - os braços acompanham;
  - o peso passa devagar de uma perna para a outra;
  - a cabeça faz pequenos ajustes (até ±1,5°).
  - Os pés ficam plantados, e cabeça, pescoço, kimono e proporções não mudam.
- **Idle de combate (ciclo de 2,4 s):**
  - transferência de peso entre as pernas e "quique" leve dos joelhos (duas vezes por ciclo);
  - oscilação curta do tronco;
  - os braços ajustam a guarda em fases opostas;
  - a respiração continua.
  - A pose de combate aprovada não foi redesenhada.
- **Caminhada no lugar (ciclo de 1,1 s, dois passos):** cada perna alterna apoio (60%) e balanço (40%).
  - **Apoio:** o tornozelo recua em linha reta na mesma velocidade com que o chão rola sob o personagem. O pé de apoio não escorrega em relação ao chão, e as marcas do chão na janela andam junto com ele. No fim do apoio o calcanhar sobe, com a ponta do pé no chão.
  - **Balanço:** a perna avança por uma curva que sai e chega com a velocidade do chão (sem tranco no toque), levanta o pé e pousa a ponta primeiro.
  - **Quadril:** sobe no meio do apoio, desce no duplo apoio e leva o peso para a perna de apoio; o tronco inclina 3° à frente.
  - **Braços:** balançam em oposição às pernas, sem marcha militar.
  - **Medidas relativas:** o passo (55% da perna) e a altura do pé (15%) são relativos ao comprimento da perna, para servirem a outros personagens.
- **Transições (1,0 s e 1,1 s):** cada canal tem seu próprio tempo.
  - **Indo para o combate:** os joelhos dobram primeiro, depois quadril e tronco, e por último os braços sobem para a guarda.
  - **Voltando:** os braços descem primeiro e as pernas por último.
  - **Passos:** cada pé dá um pequeno passo para mudar de base (levanta, avança e pousa), um de cada vez, com o peso indo para o pé que fica no chão. Os pés não deslizam.
  - **Começo e fim:** a transição parte da pose mostrada naquele instante (mesmo no meio de outra animação) e termina exatamente no primeiro quadro do idle de destino, que assume sem salto.

### Prioridade entre controles manuais e animação
- **Perspectiva:** a rotação do corpo é sempre manual e pode mudar durante a reprodução sem reiniciar o ciclo.
- **Cabeça:** o giro é sempre manual, também durante a reprodução. A animação só soma um pequeno acréscimo (no máximo ±1,5°), e o total fica limitado a ±60°.
- **Durante a reprodução ou a pausa:** tronco, braços e pernas pertencem à animação. Os controles dos braços e o preset de pernas ficam bloqueados e mostram os valores animados.
- **Parar:** volta à edição manual, com a pose de antes. Se uma transição terminou, volta à nova pose-base, porque a troca de pose foi confirmada.
- **Restaurar pose:** para a animação e restaura.
- **Trocar a pose-base:** com "Transição animada ao trocar a pose" marcada (padrão), a troca vira a transição correspondente, seguida do idle. Desmarcada, troca na hora, como na 6.1.

### Como executar
Abrir o laboratório (Java 22 ou mais novo, a partir da raiz do repositório):

```
java docs/rig2/LaboratorioRig2.java
```

Na seção "Animações (6.2)":
1. Escolha a animação.
2. Use ▶ Reproduzir, ❚❚ Pausar, ■ Parar e ↺ Reiniciar; a velocidade vai de 0,5× a 2,0×.
3. Para ver as transições, também dá para trocar a "Pose-base" entre Neutra e Combate.

Durante a reprodução continuam disponíveis a rotação do corpo (controle deslizante ou ← →), a cabeça (Q/E), o esqueleto e os botões de restauração.

| O que | Comando |
|---|---|
| Verificações das animações | `java docs/rig2/AnimacoesRig2.java` |
| Capturas da 6.2 e teste dos controles de reprodução | `java docs/rig2/LaboratorioRig2.java --capturas-6.2 docs/rig2/laboratorio-6.2` |
| Regressão da 6.1 (escreva numa pasta temporária, para não sobrescrever as capturas da 6.1) | `java docs/rig2/LaboratorioRig2.java --capturas <pasta temporária>` |
| Verificações do modelo | `java docs/rig2/ConceitoRig2.java --verificar-6.1` |

### Verificações executadas (todas passaram)

**`java docs/rig2/AnimacoesRig2.java`:**
- **Integridade em todos os quadros:** cada animação foi amostrada a 1/120 s, três ciclos nos loops. Em todos os quadros:
  - braço, antebraço, coxa, canela, tronco e pescoço com comprimento constante;
  - coordenadas válidas;
  - cabeça presa ao pescoço;
  - todo tornozelo alcança o alvo (pé nenhum flutua por falta de alcance) e nenhum pé fica abaixo do chão.
- **Loops (idle neutro, idle de combate, caminhada):**
  - o fim coincide com o início (diferença de ~10⁻¹⁴);
  - a velocidade é a mesma dos dois lados da emenda;
  - depois de 40 ciclos tocados pelo reprodutor não há desvio acumulado.
- **Caminhada:**
  - o pé de apoio acompanha o chão (erro de 10⁻¹⁰) e fica no chão, inclusive com o calcanhar levantado;
  - sempre há um pé apoiado e as pernas alternam;
  - o joelho dobra de 29° a 70°, e o pé inclina de −10° a 22°;
  - os braços ficam em oposição às pernas.
- **Transições:** começam na pose de partida, terminam exatamente no primeiro quadro do idle e são encadeadas sem salto. Também partem corretamente de uma pose qualquer, por exemplo no meio da caminhada.
- **Reprodutor:**
  - a 60 quadros/s e com quadros irregulares chega à mesma pose;
  - pausar, continuar, 2×, reiniciar e parar funcionam;
  - o giro manual da cabeça é somado;
  - o passo máximo é respeitado.
- **Continuidade visual:** diferença média por pixel entre quadros a 30 por segundo, a 40°. Nenhum salto isolado e nenhum salto na emenda dos loops.

  | Animação | Maior | Mediana | Emenda do loop |
  |---|---|---|---|
  | Idle neutro | 0,17 | 0,10 | 0,14 |
  | Idle de combate | 0,86 | 0,61 | 0,66 |
  | Caminhada | 3,54 | 2,77 | 2,69 |
  | Transição neutra → combate | 5,83 | 4,00 | — |
  | Transição combate → neutra | 5,21 | 3,45 | — |

**`--capturas-6.2`:**
- Gerou as capturas a partir da própria janela.
- Fez o experimento da caminhada girando 40° → 65° → 90° → 140° → 40°: o relógio sempre avançou e o ciclo não reiniciou.
- Exercitou pela interface, sem exceções:
  - reproduzir, pausar, continuar, as quatro velocidades, reiniciar e parar em todas as animações;
  - a rotação do corpo e da cabeça durante a reprodução;
  - o esqueleto;
  - a troca de pose com transição animada nos dois sentidos, terminando no idle e na pose manual corretos;
  - o bloqueio e a liberação dos controles manuais.

**Regressão:**
- `ConceitoRig2.java --verificar-6.1` passou inteiro.
- As prévias 6.0.4 geradas de novo deram 0 pixels diferentes.
- A rotina de controles da 6.1 (`--capturas`, numa pasta temporária) passou sem exceções.
- A janela real foi aberta e apareceu com o título "Rig 2.0 · Laboratório interativo (6.2) · experimental", sem nada na saída de erro.

### Capturas (`laboratorio-6.2/`)
- **Janela:**
  - `janela-caminhada-040.png`, `janela-caminhada-090.png` e `janela-caminhada-esqueleto-040.png`;
  - `janela-idle-neutro-040.png` e `janela-idle-combate-040.png`;
  - `janela-transicao-neutra-combate-040.png`.
- **Folhas de contato (`quadros/`):**
  - `idle-neutro-040.png` e `idle-combate-040.png`: 8 quadros por ciclo;
  - `caminhada-040.png`, `caminhada-090.png` e `caminhada-140.png`: 12 quadros por ciclo;
  - `caminhada-000.png` e `caminhada-180.png`;
  - `caminhada-esqueleto-040.png`;
  - `transicao-neutra-combate-040.png`, `transicao-neutra-combate-090.png` e `transicao-combate-neutra-040.png`;
  - `caminhada-girando.png`: 24 quadros de 40° a 140° e de volta, sem reiniciar o ciclo.

### Limitações
- **Caminhada no lugar:** o personagem não se desloca pelo cenário. As marcas do chão mostram o deslocamento equivalente. Mover a raiz de verdade é o próximo passo natural, e o pé de apoio já está pronto para isso, porque não escorrega.
- **Torção do tronco:** o modelo ainda não tem a rotação dos ombros contra o quadril na caminhada. É o que mais deixaria o andar natural.
- **Joelhos:** nunca ficam totalmente estendidos (mínimo de 29°), porque a altura do quadril vem da pose neutra aprovada, que já tem os joelhos levemente dobrados.
- **Pés:** a inclinação move a forma do pé, mas não a deforma; o rolamento da ponta é aproximado.
- **Mãos:** não giram nem fecham.
- **Movimento secundário:** não há balanço de tecido, das pontas da faixa nem das orelhas, porque não há simulação de pano.
- **Presets de pernas:** são ignorados durante a reprodução (a animação controla as pernas) e voltam ao parar.
- **Parar:** é imediato. Um retorno suave à pose manual pode ser feito com a mesma interpolação das transições.
- **Herdado da 6.1:** perto de 0° e 180°, a pata do lado longe ainda passa por alguns graus de semitransparência ao trocar de camada.
- **Desempenho:** não medi com dois lutadores no jogo.

### Sugestão para a postura de combate (registrada, não implementada)
Para uma guarda mais de jiu-jítsu, preparada para disputar pegadas:
- mãos um pouco mais à frente e mais abertas, na altura do peito do adversário;
- cotovelos mais junto ao corpo;
- queixo levemente para baixo;
- base um pouco menos larga e joelhos um pouco mais flexionados.

Pode ser avaliada numa entrega própria, sem mudar a pose aprovada antes disso.

### Direção de longo prazo: o Rig 2.0 como base do jogo inteiro
Nenhuma decisão desta entrega compromete a migração futura. O que já ajuda nela:
- **Corpo sem tela:** o modelo do corpo é separado do desenho e da janela, e as animações são funções puras do tempo sobre esse modelo. O laço do jogo poderá usar o `Reprodutor` como está.
- **Alvos para as mãos e os pés:** as poses são descritas por ângulos e alvos de IK no espaço do corpo, não por imagens nem por coordenadas da tela. Isso é o que permitirá pegadas e contatos entre dois lutadores, porque as mãos de um viram alvos sobre o kimono do outro.
- **Animações independentes da perspectiva:** a câmera nunca faz parte da animação, então a mesma animação serve para qualquer ângulo, inclusive o espelhamento esquerda/direita do jogo.
- **Medidas relativas:** a caminhada usa medidas relativas ao comprimento da perna.

**Riscos identificados e alternativas propostas** (nenhum bloqueia a 6.2):
1. **Proporções globais:** as medidas (braço 39, antebraço 30, coxa 46,5, canela 46, cabeça 70) ainda são constantes de todo o modelo. Para personagens com outras proporções, antes da integração, sugiro transformá-las num objeto `Proporcoes` por personagem, passado ao modelo, e manter as animações em medidas relativas. Não fiz isso agora para não mexer no modelo aprovado sem necessidade.
2. **Localização do código:** o código vive em `docs/rig2/`, sem pacote, para ficar fora do build. Na integração, ele deve ir para um pacote próprio em `src/` (por exemplo `com.irmaosmiau.rig2`), separando modelo, animação e desenho como já está separado hoje. É uma mudança mecânica.
3. **Posições no chão:** a orientação do corpo hoje é só a guinada mais a inclinação do tronco. Posições de chão, quedas e rolamentos vão exigir uma orientação completa da raiz (rolagem e arfagem). Convém acrescentá-la ao `Corpo` antes de começar essas animações.
4. **Compatibilidade com as mecânicas atuais:** as animações antigas são acionadas pelo motor de combate (pegadas, raspagens, finalizações). Para preservar todas as mecânicas, a integração deve ter, para cada animação antiga, uma equivalente no Rig 2.0 com o mesmo identificador e a mesma duração, chamada pelo mesmo ponto do motor. Assim, as regras e os tempos de reação continuam iguais, e os dois sistemas podem ser comparados lado a lado, com o atual preservado até a validação.

### Próximas evoluções recomendadas
1. Torção do tronco e deslocamento real da raiz na caminhada; depois, parada e partida (transição idle ↔ caminhada).
2. Deslocamentos de combate em pé (passo à frente e para trás, e lateral, na guarda), já sobre o idle de combate.
3. Retorno suave ao parar e mistura (cross-fade) entre quaisquer duas animações, reutilizando `ParametrosPose.interpolar`.
4. O objeto `Proporcoes` por personagem e a orientação completa da raiz, preparando o Miau Preto e as posições de chão.
