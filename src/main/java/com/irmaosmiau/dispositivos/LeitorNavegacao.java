package com.irmaosmiau.dispositivos;

import java.util.ArrayList;
import java.util.List;

/**
 * Entrega 5I (complemento): leitura de UM gamepad para navegar em menus.
 *
 * - D-pad ou analogico esquerdo movem (zona morta 0,5; volta ao neutro abaixo
 *   de 0,3, com histerese); vertical tem prioridade sobre horizontal.
 * - Segurar repete: primeiro passo na hora, depois de {@link #ATRASO_MS} um
 *   passo a cada {@link #REPETICAO_MS}.
 * - A confirma e B volta, uma vez por toque.
 * - Comeca DESARMADO e so arma depois de ver tudo solto: o botao que abriu
 *   ou fechou uma tela nunca vale na tela seguinte ({@link #desarmar()}).
 *
 * Sem Swing: o NavegadorMenus aplica os comandos aos componentes.
 */
public final class LeitorNavegacao {

    /** O que a tela deve fazer. */
    public enum Comando { ANTERIOR, PROXIMO, ESQUERDA, DIREITA, CONFIRMAR, VOLTAR,
        /** X / Quadrado: so na selecao de lutadores (abre os perfis); os outros leitores nunca emitem. */
        PERFIS }

    public static final float ZONA_MORTA = 0.5f;
    public static final float NEUTRO = 0.3f;
    public static final long ATRASO_MS = 400;
    public static final long REPETICAO_MS = 140;

    private boolean armado;
    private Comando direcao;
    private long proximoPasso;
    private boolean confirmarAntes;
    private boolean voltarAntes;
    /** Le o X / Quadrado como PERFIS (so a selecao de lutadores liga). */
    private final boolean comPerfis;
    private boolean perfisAntes;

    public LeitorNavegacao() {
        this(false);
    }

    /** @param comPerfis true = o X / Quadrado vira {@link Comando#PERFIS} (uma vez por toque) */
    public LeitorNavegacao(boolean comPerfis) {
        this.comPerfis = comPerfis;
    }

    /** Exige soltar tudo antes do proximo comando (troca de tela, pausa, foco). */
    public void desarmar() {
        armado = false;
        direcao = null;
    }

    public boolean isArmado() {
        return armado;
    }

    /**
     * @param estado estado atual (null = desconectado: desarma)
     * @param agoraMs relogio em milissegundos
     */
    public List<Comando> ler(EstadoGamepad estado, long agoraMs) {
        List<Comando> saida = new ArrayList<>();
        if (estado == null) {
            desarmar();
            confirmarAntes = false;
            voltarAntes = false;
            perfisAntes = false;
            return saida;
        }
        Comando d = direcao(estado);
        boolean confirmar = estado.pressionado(BotaoGamepad.SUL);
        boolean voltar = estado.pressionado(BotaoGamepad.LESTE);
        boolean perfis = comPerfis && estado.pressionado(BotaoGamepad.OESTE);

        if (!armado) {
            if (d == null && !confirmar && !voltar && !perfis) {
                armado = true;
            }
            direcao = null;
            confirmarAntes = confirmar;
            voltarAntes = voltar;
            perfisAntes = perfis;
            return saida;
        }

        if (d == null) {
            direcao = null;
        } else if (d != direcao) {
            direcao = d;
            saida.add(d);
            proximoPasso = agoraMs + ATRASO_MS;
        } else if (agoraMs >= proximoPasso) {
            saida.add(d);
            proximoPasso = agoraMs + REPETICAO_MS;
        }
        if (confirmar && !confirmarAntes) {
            saida.add(Comando.CONFIRMAR);
        }
        if (voltar && !voltarAntes) {
            saida.add(Comando.VOLTAR);
        }
        if (perfis && !perfisAntes) {
            saida.add(Comando.PERFIS);
        }
        confirmarAntes = confirmar;
        voltarAntes = voltar;
        perfisAntes = perfis;
        return saida;
    }

    private Comando direcao(EstadoGamepad e) {
        if (e.pressionado(BotaoGamepad.DPAD_CIMA)) {
            return Comando.ANTERIOR;
        }
        if (e.pressionado(BotaoGamepad.DPAD_BAIXO)) {
            return Comando.PROXIMO;
        }
        if (e.pressionado(BotaoGamepad.DPAD_ESQUERDA)) {
            return Comando.ESQUERDA;
        }
        if (e.pressionado(BotaoGamepad.DPAD_DIREITA)) {
            return Comando.DIREITA;
        }
        float x = e.eixo(EixoGamepad.ESQ_X);
        float y = e.eixo(EixoGamepad.ESQ_Y);
        // Histerese: a direcao atual continua ate o analogico voltar abaixo do neutro.
        if (direcao != null) {
            float mantido = switch (direcao) {
                case ANTERIOR -> -y;
                case PROXIMO -> y;
                case ESQUERDA -> -x;
                case DIREITA -> x;
                default -> 0f;
            };
            if (mantido > NEUTRO && Math.abs(mantido) >= Math.max(Math.abs(x), Math.abs(y)) - 1e-6f) {
                return direcao;
            }
        }
        if (Math.abs(y) >= ZONA_MORTA && Math.abs(y) >= Math.abs(x)) {
            return y < 0 ? Comando.ANTERIOR : Comando.PROXIMO;
        }
        if (Math.abs(x) >= ZONA_MORTA) {
            return x < 0 ? Comando.ESQUERDA : Comando.DIREITA;
        }
        return null;
    }
}
