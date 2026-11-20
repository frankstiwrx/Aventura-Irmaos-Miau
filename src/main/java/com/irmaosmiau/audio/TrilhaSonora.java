package com.irmaosmiau.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Entrega 5Q.6: trilha sonora por CONTEXTO (tela/modo), sobre o ServicoAudio existente
 * (uma musica por vez, volume e mudo das configuracoes, efeitos independentes).
 *
 * - Menus/treino: a musica do contexto em loop; pedir o mesmo contexto nao a reinicia.
 * - Combate: rodizio aleatorio das faixas combate_01..06; quando uma termina, outra comeca,
 *   sem repetir a anterior quando ha outra diferente; pausar, mudar de posicao, tecnica ou
 *   cenario (F4) e reiniciar a luta (F5) NAO reiniciam a musica.
 * - O arquivo e lido FORA da thread do Swing; trocas rapidas de tela descartam o que ficou
 *   para tras (so a ultima pedida toca: nunca duas musicas juntas).
 */
public final class TrilhaSonora {

    /** Uma faixa so e considerada terminada depois deste tempo tocando (o Clip demora a ficar ativo). */
    static final long MINIMO_TOCANDO_MS = 1500;
    static final int VERIFICAR_A_CADA_MS = 500;

    private final ServicoAudio servico;
    private final BibliotecaMusical biblioteca;
    private final Executor carregador;
    private final Random sorteio;
    private final AtomicLong geracao = new AtomicLong();
    private final javax.swing.Timer verificador;

    private ContextoMusical contexto;
    private boolean rodizioCombate;
    private volatile String origemAtual;
    private volatile long inicioMs;
    private final List<ContextoMusical> historico = new ArrayList<>();

    public TrilhaSonora(ServicoAudio servico, BibliotecaMusical biblioteca, Executor carregador, Random sorteio,
            boolean verificarSozinho) {
        this.servico = servico;
        this.biblioteca = biblioteca;
        this.carregador = carregador;
        this.sorteio = sorteio;
        this.verificador = new javax.swing.Timer(VERIFICAR_A_CADA_MS, e -> verificar());
        this.verificador.setRepeats(true);
        if (verificarSozinho) {
            verificador.start();
        }
    }

    /** A trilha do jogo: carregamento numa thread propria (daemon), verificacao pelo Timer do Swing. */
    public static TrilhaSonora padrao(ServicoAudio servico) {
        Executor ex = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "carregar-musica");
            t.setDaemon(true);
            return t;
        });
        return new TrilhaSonora(servico, BibliotecaMusical.padrao(), ex, new Random(), true);
    }

    /** Musica de uma tela/modo (loop). O mesmo contexto ja tocando continua. */
    public synchronized void tocar(ContextoMusical c) {
        if (c == null) {
            return;
        }
        if (c.isCombate()) {
            tocarCombate();
            return;
        }
        if (c == contexto && !rodizioCombate) {
            return;
        }
        rodizioCombate = false;
        contexto = c;
        carregarETocar(c, true);
    }

    /** Combate: comeca o rodizio (se ja estiver no combate, a faixa atual continua). */
    public synchronized void tocarCombate() {
        if (rodizioCombate) {
            return;
        }
        rodizioCombate = true;
        proximaDoCombate();
    }

    /** Para a musica (ex.: sair do jogo). */
    public synchronized void parar() {
        geracao.incrementAndGet();
        rodizioCombate = false;
        contexto = null;
        origemAtual = null;
        servico.pararMusica();
    }

    /** Chamado pelo Timer: a faixa do combate terminou? Toca a proxima. */
    public synchronized void verificar() {
        if (!rodizioCombate || servico.isSilencioso() || !servico.isMusicaAtiva() || origemAtual == null) {
            return;
        }
        if (!servico.isTocandoMusica() && System.currentTimeMillis() - inicioMs >= MINIMO_TOCANDO_MS) {
            proximaDoCombate();
        }
    }

    /** Escolhe outra faixa do combate (diferente da atual quando houver outra distinta). */
    synchronized void proximaDoCombate() {
        List<ContextoMusical> todas = ContextoMusical.combate();
        String atual = origemAtual;
        List<ContextoMusical> outras = new ArrayList<>();
        for (ContextoMusical c : todas) {
            if (c != contexto && (atual == null || !primeiraOrigem(c).equals(atual))) {
                outras.add(c);
            }
        }
        if (outras.isEmpty()) {
            for (ContextoMusical c : todas) {
                if (c != contexto) {
                    outras.add(c);
                }
            }
        }
        ContextoMusical escolhida = outras.get(sorteio.nextInt(outras.size()));
        contexto = escolhida;
        carregarETocar(escolhida, false);
    }

    private String primeiraOrigem(ContextoMusical c) {
        List<String> cs = biblioteca.candidatos(c);
        for (String d : cs) {
            if (!d.startsWith(BibliotecaMusical.PREFIXO_INTERNO) || recursoExiste(d)) {
                return d;
            }
        }
        return cs.get(cs.size() - 1);
    }

    private static boolean recursoExiste(String descritor) {
        return TrilhaSonora.class.getResource(descritor.substring(BibliotecaMusical.PREFIXO_INTERNO.length())) != null;
    }

    private void carregarETocar(ContextoMusical c, boolean loop) {
        long minha = geracao.incrementAndGet();
        historico.add(c);
        carregador.execute(() -> {
            String origem = null;
            Optional<SomCarregado> som = Optional.empty();
            for (String d : biblioteca.candidatos(c)) {
                som = biblioteca.carregar(d);
                if (som.isPresent()) {
                    origem = d;
                    break;
                }
            }
            synchronized (this) {
                if (minha != geracao.get()) {
                    return;                                    // outra tela ja pediu outra musica
                }
                origemAtual = origem;
                inicioMs = System.currentTimeMillis();
                servico.tocarSom(c.getId(), som, loop);
            }
        });
    }

    // ---------------- consultas (HUD/testes)
    public synchronized Optional<ContextoMusical> getContexto() {
        return Optional.ofNullable(contexto);
    }

    public synchronized boolean isRodizioCombate() {
        return rodizioCombate;
    }

    /** De onde veio a musica que esta tocando ("arquivo:..." externo ou "interno:..."). */
    public String getOrigemAtual() {
        return origemAtual;
    }

    public synchronized List<ContextoMusical> historico() {
        return new ArrayList<>(historico);
    }

    /** Para o Timer (fim do jogo). */
    public void fechar() {
        verificador.stop();
        parar();
    }
}
