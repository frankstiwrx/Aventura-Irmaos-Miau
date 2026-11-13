# Áudio do jogo

Todos os arquivos são **WAV** (PCM; 16 bits recomendado) e são lidos pelo classpath,
tanto no NetBeans quanto no JAR.

```
audio/
  musica/   músicas em loop   -> id = nome do arquivo sem .wav
    menu.wav       menu principal           (provisória)
    treino.wav     modo treino              (provisória)
    combate.wav    lutas normais            (AINDA NÃO EXISTE: o jogo segue em silêncio)
  efeitos/  efeitos curtos
    pegada.wav      PEGADA_1 bem-sucedida   (provisório)
    rompimento.wav  pegada desfeita         (provisório)
```

## Origem e licença

Os quatro arquivos atuais são **originais**, sintetizados pelo script
`ferramentas/gerar_audio_provisorio.py` (senoides, envelopes e ruído com semente fixa;
nenhuma amostra ou música de terceiros). Podem ser usados livremente neste projeto.

**Não adicione músicas comerciais** nem arquivos sem licença que permita o uso.
Ao incluir um arquivo de terceiros com licença livre, registre aqui: arquivo, autor,
fonte e licença.

## Adicionar ou trocar uma música

1. Converta para WAV (ex.: 22050 ou 44100 Hz, 16 bits, mono ou estéreo).
2. Salve como `audio/musica/<id>.wav` (para a luta normal: `combate.wav`).
3. Rode o jogo: não é preciso mudar código para os ids já usados
   (`menu`, `treino`, `combate`, `pegada`, `rompimento`).
4. Um id novo exige só chamar `AudioJogo.servico().tocarMusica("<id>")` no ponto desejado.

Arquivo ausente ou inválido não interrompe o jogo: aparece um aviso `[audio]` no console
e aquele som fica em silêncio.
