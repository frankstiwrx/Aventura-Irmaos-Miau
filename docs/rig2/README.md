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
| `ConceitoRig2.java` | Gerador Java2D isolado. Produz os PNG e os SVG a partir das mesmas formas. Hoje gera a versão 6.0.2. |
| `referencias/` | As referências recebidas: o SVG de perspectivas, quatro capturas do Rig 2.0 experimental e o perfil anatômico de gato usado no refinamento. |

As prévias de `previas/` são as originais da 6.0.1 e não são regeradas; o gerador daquela versão está no commit `e99b380`. Para regenerar as prévias refinadas e os comparativos, com Java 23 e sem compilar o projeto:

```
java docs/rig2/ConceitoRig2.java docs/rig2/previas-refinadas docs/rig2/previas
```

O segundo argumento é opcional e só serve para montar os comparativos. O gerador também confere:
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
