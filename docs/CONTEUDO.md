# Como adicionar conteúdo

Guia prático da Entrega 2. Todo conteúdo novo entra por **arquivos** e
**registros**, sem copiar arrays para o Java e sem mexer nos renderers.

```
src/main/resources/conteudo/
  miaustudio/PEGADA_1.json      projeto original do MiauStudio (nunca editado à mão)
  animacoes/animacoes.json      catálogo de animações conjuntas
  tecnicas/tecnicas.json        catálogo de técnicas
```

## 1. Nova animação conjunta (MiauStudio → jogo)

1. No MiauStudio, modo **Conjunta**, desenhe os quadros (1 a 60), escolha o
   aplicador e salve o projeto (`Ctrl+S`), por exemplo `PEGADA_2.json`.
2. Copie o arquivo **sem editar** para `src/main/resources/conteudo/miaustudio/`.
3. Cadastre no `animacoes/animacoes.json`:

```json
{
  "id": "pegada_2_entrada",
  "formato": "MIAUSTUDIO_PROJECT_V2",
  "arquivo": "/conteudo/miaustudio/PEGADA_2.json",
  "duracaoFrames": 36,
  "duracoesQuadros": [6, 12, 18],
  "descricao": "Opcional."
}
```

| Campo | Obrigatório | Significado |
|---|---|---|
| `id` | sim | ID único (maiúsculas/espaços são normalizados) |
| `formato` | sim | Hoje só `MIAUSTUDIO_PROJECT_V2` |
| `arquivo` | sim | `/...` = caminho no classpath; sem `/` = relativo à pasta do catálogo |
| `duracaoFrames` | não | Duração total em frames (60 ≈ 1 s) |
| `duracoesQuadros` | não | Frames de cada quadro; sem este campo os quadros dividem o tempo igualmente |

Se os dois campos de tempo existirem, a soma de `duracoesQuadros` deve ser igual a `duracaoFrames`.

**O que o conversor lê do projeto:** `tipo` = `MIAUSTUDIO_PROJECT`, `versao` = 2,
`nomeAnimacao`, `aplicadorConjunto` (`Branco`/`Preto`), `centroInteracaoX`,
`linhaInteracao`, `offsetVisualAnimacaoX/Y` e `timelineConjunta`. Cada quadro precisa
ter as duas poses (`branco` e `preto`) com **todos** os campos: `offsetCentroX`,
`offsetLinha`, `escalaX10`, `viradoDireita` e as 26 articulações. Diferente do editor,
o jogo não completa campos ausentes com zero: quadro incompleto = erro com o caminho
exato (ex.: `[/conteudo/miaustudio/PEGADA_2.json] timelineConjunta[2].preto.offMaoEsqY: campo obrigatorio ausente`).

O centro de autoria (`centroInteracaoX`/`linhaInteracao`) é guardado só como
metadado; no jogo valem as posições **relativas** (`offsetCentroX`/`offsetLinha`),
e o motor usa apenas a variação em relação ao quadro 1.

## 2. Nova técnica

Em `tecnicas/tecnicas.json`:

```json
{
  "id": "pegada_2",
  "nome": "Pegada 2",
  "categoria": "PEGADA",
  "disponibilidade": "JOGAVEL",
  "descricao": "Inversão das mãos.",
  "estadoInteracaoAoSucesso": "PEGADA_2",
  "animacao": "pegada_2_entrada",
  "alcanceHorizontal": 125.0,
  "diferencaMaximaLinhas": 1,
  "exigeFrenteParaOponente": true,
  "custoFolego": 3.0,
  "dificuldade": 20.0,
  "pesos": { "tecnica": 0.5, "forca": 0.3, "agilidade": 0.2, "pesoCorporal": 0.0 }
}
```

- `disponibilidade`: `JOGAVEL` ou `PENDENTE`. Técnica **pendente** precisa só de
  `id`, `nome`, `categoria` e `disponibilidade`, e o motor sempre a trata como
  `INDISPONIVEL` (sem custo, sem estado, sem animação).
- Técnica **jogável** exige alcance, linhas, frente, custo, dificuldade e pesos: nenhuma
  regra vem de um valor padrão silencioso.
- `duracaoAnimacaoFrames` é opcional quando a animação define `duracaoFrames`; se os dois
  existirem, precisam ser iguais.
- Campos com erro de digitação (`"alcence"`) são rejeitados.
- `categoria` e `estadoInteracaoAoSucesso` usam os enums atuais (`CategoriaAcao`,
  `EstadoInteracaoCombate`). Um estado de interação novo ainda exige editar o enum
  (está previsto passar a ser por ID na Entrega 3).

Em Java, o mesmo cadastro pode ser feito com o Builder:

```java
AcaoCombate acao = AcaoCombate.builder("pegada_2", "Pegada 2", CategoriaAcao.PEGADA)
        .disponibilidade(DisponibilidadeAcao.PENDENTE)
        .descricao("Inversão das mãos.")
        .build();
```

## 3. Usar o conteúdo carregado

```java
ConteudoCombate conteudo = CarregadorConteudoCombate.carregarPadrao();
SistemaCombate sistema = conteudo.criarSistema(branco, preto);
```

O `SistemaCombate` se recusa a ser criado se uma técnica jogável apontar para uma
animação que não existe.

> **Estado atual (Entrega 3A):** o `GamePanel` usa `CarregadorConteudoCombate.carregarParaJogo()`.
> Tudo que estiver nos catálogos entra no jogo; os arrays legados ficam só como referência de regressão.

## 4. Novo personagem

```java
CatalogoPersonagens catalogo = CatalogoPersonagens.padrao();   // Branco + Preto

catalogo.registrar(new DefinicaoPersonagem(
        "miau_cinza",                    // id único
        "Irmão Miau Cinza",              // nome
        "Judô",                          // estilo de luta
        "Descrição opcional.",
        new AtributosLutador(30, 30, 30, 30, 30, 30, 30, 70),
        new ReferenciasVisuais(Color.GRAY, Color.WHITE, Color.BLACK,
                "/assets/sprites/miau-cinza.png"),
        (x, y, escala) -> /* instância jogável, ex.: */ new IrmaoMiauCinza(x, y, escala)
));

IrmaoMiau lutador = catalogo.exigir("miau_cinza").criarLutador(220, 150, 0.7);
```

A `FabricaLutador` é um lambda: o catálogo não exige uma subclasse por personagem e
não conhece classes concretas. Hoje uma aparência nova ainda precisa de um renderer
(uma classe que implemente `desenhar`), porque cores e atributos do Branco/Preto são
definidos nos seus construtores. Os construtores alternativos aprovados para a
Entrega 4 permitirão reaproveitar os renderers atuais com atributos/cores vindos da definição.

O catálogo ainda não está ligado aos menus nem ao `GamePanel`.

## 5. Nova música ou efeito (WAV)

Detalhes e licença em `src/main/resources/audio/LEIAME.md`.

1. Converta para **WAV** (PCM 16 bits; 22050 ou 44100 Hz).
2. Copie para `src/main/resources/audio/musica/<id>.wav` (músicas, tocam em loop) ou
   `src/main/resources/audio/efeitos/<id>.wav` (efeitos).
3. Ids já usados pelo jogo, sem mudar código: `menu`, `treino`, `combate` (músicas), `pegada`,
   `rompimento` (efeitos). Ex.: salvar `combate.wav` já faz a luta normal ter música.
4. Id novo: `AudioJogo.servico().tocarMusica("<id>")` ou `tocarEfeito("<id>")`.

Arquivo ausente ou inválido não trava nada: aparece `[audio] ... seguindo em silencio` no console.
**Não use músicas comerciais** nem arquivos sem licença.
Para rodar sem som: `-Dirmaosmiau.audio=desligado`.

## 6. Novo cenário

```java
public final class CenarioPraia implements Cenario {
    public String getId() { return "praia"; }
    public String getNome() { return "Praia"; }
    public void desenharFundo(Graphics2D g, int largura, int altura) { /* céu, mar... */ }
    public void desenharChao(Graphics2D g, int largura, int altura)  { /* areia a partir de y ~ 296 */ }
    // desenharPrimeiroPlano é opcional: não cubra a área dos personagens (y 56..410)
}
```

Registre em `RegistroCenarios.padrao()` (`r.registrar(new CenarioPraia());`). Ele entra na rotação do F4
e pode ser usado no treino com `new ConfiguracaoTreino(..., "praia", ...)` ou `painel.usarCenario("praia")`.
Para desenhos pesados, renderize uma vez numa `BufferedImage` (como o `CenarioDojo`).
Referências úteis: pés dos lutadores em `CenarioDojo.yCentroLinha(linha)`.
