"""
Gera os sons PROVISORIOS do jogo (originais, sintetizados aqui mesmo).

Nao usa amostras, musicas ou bibliotecas de terceiros: apenas senoides,
envelopes e ruido com semente fixa. O resultado e reproduzivel e pode
ser usado livremente no projeto. Substitua os arquivos por musicas
definitivas quando houver (ver src/main/resources/audio/LEIAME.md).

Uso (na raiz do projeto):
    python ferramentas/gerar_audio_provisorio.py
"""
import math
import os
import random
import struct
import wave

TAXA = 22050
RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                    "src", "main", "resources", "audio")


def nota(midi):
    return 440.0 * 2 ** ((midi - 69) / 12.0)


def salvar(caminho, amostras):
    os.makedirs(os.path.dirname(caminho), exist_ok=True)
    pico = max(1e-9, max(abs(a) for a in amostras))
    escala = 0.9 / pico if pico > 0.9 else 1.0
    with wave.open(caminho, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(TAXA)
        w.writeframes(b"".join(
            struct.pack("<h", int(max(-1.0, min(1.0, a * escala)) * 32767)) for a in amostras))


def tom(buf, inicio_s, dur_s, freq, vol, ataque=0.01, brilho=0.25):
    i0 = int(inicio_s * TAXA)
    n = int(dur_s * TAXA)
    for i in range(n):
        if i0 + i >= len(buf):
            break
        t = i / TAXA
        env = min(1.0, t / ataque) * math.exp(-3.2 * t / dur_s)
        s = math.sin(2 * math.pi * freq * t) + brilho * math.sin(4 * math.pi * freq * t)
        buf[i0 + i] += vol * env * s


def bumbo(buf, inicio_s, vol):
    i0 = int(inicio_s * TAXA)
    for i in range(int(0.18 * TAXA)):
        if i0 + i >= len(buf):
            break
        t = i / TAXA
        f = 110 * math.exp(-t * 18) + 45
        buf[i0 + i] += vol * math.exp(-t * 22) * math.sin(2 * math.pi * f * t)


def chiado(buf, inicio_s, dur_s, vol, rnd):
    i0 = int(inicio_s * TAXA)
    anterior = 0.0
    for i in range(int(dur_s * TAXA)):
        if i0 + i >= len(buf):
            break
        t = i / TAXA
        x = rnd.uniform(-1, 1)
        anterior = 0.6 * anterior + 0.4 * x       # ruido suavizado
        buf[i0 + i] += vol * math.exp(-t / (dur_s * 0.35)) * anterior


def musica_menu():
    """Loop calmo em pentatonica maior (~10 s)."""
    bpm = 96
    batida = 60.0 / bpm
    compassos = 4
    buf = [0.0] * int(compassos * 4 * batida * TAXA)
    melodia = [72, 74, 76, 79, 76, 74, 72, 69,
               72, 76, 79, 81, 79, 76, 74, 72]
    baixos = [48, 45, 41, 43]
    for c in range(compassos):
        tom(buf, c * 4 * batida, 3.6 * batida, nota(baixos[c]), 0.30, ataque=0.03, brilho=0.1)
        tom(buf, c * 4 * batida + 2 * batida, 1.8 * batida, nota(baixos[c] + 7), 0.15, ataque=0.03, brilho=0.1)
    for k, m in enumerate(melodia):
        tom(buf, k * batida, 0.9 * batida, nota(m), 0.16, brilho=0.35)
    return buf


def musica_treino():
    """Loop ritmado em pentatonica menor com percussao leve (~8,7 s)."""
    bpm = 110
    batida = 60.0 / bpm
    compassos = 4
    rnd = random.Random(7)
    buf = [0.0] * int(compassos * 4 * batida * TAXA)
    baixos = [45, 45, 43, 40]
    padrao = [69, 72, 74, 72, 76, 74, 72, 67]
    for c in range(compassos):
        base = c * 4 * batida
        for b in range(4):
            bumbo(buf, base + b * batida, 0.45 if b % 2 == 0 else 0.25)
            chiado(buf, base + b * batida + batida / 2, 0.05, 0.06, rnd)
        tom(buf, base, 1.8 * batida, nota(baixos[c]), 0.28, ataque=0.01, brilho=0.2)
        tom(buf, base + 2 * batida, 1.8 * batida, nota(baixos[c]), 0.22, ataque=0.01, brilho=0.2)
        for k in range(8):
            if (c + k) % 3 != 2:
                tom(buf, base + k * batida / 2, 0.45 * batida, nota(padrao[k] - (0 if c < 2 else 2)), 0.10)
    return buf


def efeito_pegada():
    """Batida curta de tecido sendo agarrado (~0,18 s)."""
    rnd = random.Random(11)
    buf = [0.0] * int(0.18 * TAXA)
    bumbo(buf, 0.0, 0.7)
    chiado(buf, 0.0, 0.12, 0.35, rnd)
    return buf


def efeito_rompimento():
    """Deslize curto descendente (~0,25 s)."""
    rnd = random.Random(13)
    buf = [0.0] * int(0.25 * TAXA)
    chiado(buf, 0.0, 0.22, 0.4, rnd)
    for i in range(len(buf)):
        t = i / TAXA
        f = 520 - 1400 * t
        buf[i] += 0.18 * math.exp(-t * 9) * math.sin(2 * math.pi * f * t)
    return buf


if __name__ == "__main__":
    salvar(os.path.join(RAIZ, "musica", "menu.wav"), musica_menu())
    salvar(os.path.join(RAIZ, "musica", "treino.wav"), musica_treino())
    salvar(os.path.join(RAIZ, "efeitos", "pegada.wav"), efeito_pegada())
    salvar(os.path.join(RAIZ, "efeitos", "rompimento.wav"), efeito_rompimento())
    print("Sons provisorios gerados em", os.path.normpath(RAIZ))
