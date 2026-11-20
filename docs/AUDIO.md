# Trilha sonora por contexto (Entrega 5Q.6)

Cada tela/modo do jogo tem o **próprio mapeamento de música**. Para trocar uma música basta
colocar um arquivo com o nome certo na pasta externa: **não é preciso editar código nem
recompilar o JAR**.

## Onde ficam os arquivos

Pasta externa (procurada nesta ordem):

1. `-Dirmaosmiau.musicas=<pasta>` na linha de comando (ex.: `java -Dirmaosmiau.musicas=D:\minhas_musicas -jar ...`);
2. `audio/musicas/` **ao lado do JAR** (ex.: `target/audio/musicas/` ou a pasta onde o JAR for copiado);
3. `audio/musicas/` na pasta de onde o jogo foi aberto (no NetBeans: a pasta do projeto — o modelo
   já está em [`audio/musicas/`](../audio/musicas/LEIAME.md)).

Estrutura esperada:

```
audio/
    musicas/
        menus/
            menu_inicial.wav         tela de título
            menu_principal.wav       menu principal
            selecao_personagens.wav  JOGAR: personagens, faixas, kimonos, mapa e tempo
            menu_treinamento.wav     seleção do treino
            criacao_personagens.wav  PERSONAGENS (perfis)
            configuracoes.wav        CONFIGURAÇÕES e CONTROLES
            resultados.wav           fim da luta (vitória, tempo esgotado)
        treinamento/
            treinamento.wav          treino em execução
            laboratorio_tecnico.wav  laboratório técnico (situações do treino)
        combate/
            combate_01.wav ... combate_06.wav   lutas (rodízio)
```

As telas "menu Jogar", "menu de luta" e "seleção de cenários" do pedido não existem
separadamente no jogo (JOGAR abre direto a seleção, que já tem o mapa); por isso não ganharam
arquivo próprio.

## Formatos

**WAV** (recomendado: PCM 16 bits, 22050 ou 44100 Hz, mono ou estéreo). Também são aceitos
**AIFF** (`.aiff`/`.aif`) e **AU** (`.au`) — o que o Java Sound lê sem bibliotecas extras.
MP3/OGG **não** são lidos: converta para WAV.

## Como funciona

- **Menus, treino, laboratório, resultados:** a música do contexto toca em loop. Voltar para a
  mesma tela não reinicia a música.
- **Combate:** ao começar a luta é sorteada uma das faixas `combate_01..06`; quando ela termina,
  outra é sorteada, **sem repetir a anterior** (quando existe outra diferente). Pausar, fazer
  técnicas, cair, trocar de posição, trocar o cenário (F4) ou reiniciar a luta (F5) **não
  reiniciam** a música.
- **Fallback:** sem o arquivo externo, o jogo procura `/audio/musica/<id>.wav` dentro do JAR e,
  sem ele, usa as músicas internas provisórias (hoje só existem duas músicas próprias,
  `menu.wav` e `treino.wav`: os menus usam a primeira, treino e laboratório a segunda e as seis
  faixas de combate alternam as duas). Arquivo ausente, corrompido ou num formato incompatível
  é pulado (aviso `[audio]` no console) e o jogo segue.
- **Uma música por vez:** a anterior sempre para antes da próxima. O arquivo é lido numa
  thread própria (a tela não trava); se o jogador trocar de tela no meio, só a última pedida toca.
- **Volume e silêncio:** valem as opções de CONFIGURAÇÕES (música ligada/desligada, volume) e o
  F9. Os efeitos sonoros (pegada, rompimento) continuam independentes.
- **Fechar o jogo** para a música e libera o áudio.

## Trocar as seis músicas de combate

1. Converta as músicas para WAV.
2. Salve como `audio/musicas/combate/combate_01.wav` ... `combate_06.wav` (pode usar menos: as
   que faltarem usam a provisória).
3. Abra o jogo. Pronto.

## Adicionar músicas no futuro

- Para outra tela: um valor novo em `ContextoMusical` (pasta, id e a provisória) e uma chamada
  `AudioJogo.trilhas().tocar(ContextoMusical.<NOVO>)` quando a tela abrir.
- Para mais faixas de combate: novos `COMBATE_07...` em `ContextoMusical` (o rodízio usa todas
  as de `ContextoMusical.combate()`).

## Direitos autorais

Use só músicas próprias ou com licença que permita o uso. **Não** coloque músicas comerciais.
As duas músicas internas foram geradas pelo próprio projeto (`ferramentas/gerar_audio_provisorio.py`).

## Código

`audio/ContextoMusical` (mapeamento), `audio/BibliotecaMusical` (pasta externa → interno →
provisória), `audio/TrilhaSonora` (troca por tela, rodízio do combate, carregamento fora da EDT),
`ServicoAudio.tocarSom` (uma música por vez, volume/mudo), `CarregadorSom.carregarArquivo`
(arquivos do disco). Testes: `audio/Trilhas5Q6Test`.

Pendência: transição suave (fade) entre músicas — hoje a troca é direta.
