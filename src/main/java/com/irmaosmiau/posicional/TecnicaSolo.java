package com.irmaosmiau.posicional;

import java.util.List;
import java.util.Set;

/**
 * Entrega 5P: uma tecnica do combate de quedas e de solo (dados, nao codigo).
 *
 * - origens + papel: onde e por quem ela pode comecar;
 * - comandos: direcao do RS (e se exige o modificador de quedas: RB/Select,
 *   4 no teclado do J1, O/SHIFT no do J2);
 * - janela: ticks de PREPARACAO em que o iniciador fica exposto e o outro pode
 *   responder com uma das {@code contras} (a contra resolve na hora e vale o
 *   SUCESSO dela). 0 = instantanea (ajustes de controle, sem troca de posicao);
 * - criterio: o que precisa ser verdade no fim da janela para dar certo;
 *   senao o desfecho e o {@code defendida} (permanece, com consequencia);
 * - sucesso: posicao, quem fica por cima e quem controla (relativo a quem
 *   iniciou), aplicado aos dois de uma vez pelo MotorSolo.
 */
public record TecnicaSolo(
        String id,
        String nome,
        String rotulo,
        Set<Posicao> origens,
        PapelPosicional papel,
        List<ComandoSolo> comandos,
        double custo,
        int janela,
        Exposicao exposicao,
        Criterio criterio,
        double limiarEstabilidade,
        Desfecho sucesso,
        Set<String> contras,
        Efeito efeito) {

    /** Direcao da tecnica (RS / setas / teclas de pegada). */
    public enum Direcao {
        CIMA, DIREITA, BAIXO, ESQUERDA
    }

    /**
     * 5P.2: a FAMILIA do comando (o modificador segurado com o RS). Nomes no
     * padrao PlayStation (L1 = LB, L2 = LT, R1 = RB/Select, R2 = RT).
     * - RS: fundamentos (avancar, segurar, lados);
     * - R1: tatico / explosivo (quedas em pe, upa, levantar, sair da posicao);
     * - L1: guardas (em pe: entradas; no chao: variacoes) e finalizacoes futuras;
     * - L2: quedas especificas (em pe) / controles de manga e finalizacoes futuras;
     * - L1_L2: especiais (berimbolo) e a terceira familia de finalizacoes.
     */
    public enum Familia {
        RS("RS"), R1("R1"), L1("L1"), L2("L2"), L1_L2("L1+L2");

        private final String rotulo;

        Familia(String rotulo) {
            this.rotulo = rotulo;
        }

        public String getRotulo() {
            return rotulo;
        }
    }

    /** Comando: direcao + familia (5P: o booleano antigo = RS / R1). */
    public record ComandoSolo(Direcao direcao, Familia familia) {

        public ComandoSolo(Direcao direcao, boolean explosivo) {
            this(direcao, explosivo ? Familia.R1 : Familia.RS);
        }

        /** R1 (RB/Select): a familia explosiva da 5P. */
        public boolean explosivo() {
            return familia == Familia.R1;
        }
    }

    /** Vulnerabilidade temporaria (contextual). */
    public enum Exposicao {
        /** Base comprometida: o peso esta passando ou abrindo (raspagens e reversoes entram). */
        BASE,
        /** Controle reduzido: o controlador desloca o peso para progredir (escapes entram). */
        CONTROLE_REDUZIDO,
        /** Postura quebrada dentro da guarda fechada (raspagens entram). */
        POSTURA_QUEBRADA,
        /** Avanco excessivo de um empurrao bloqueado (o outro empurra / derruba). */
        AVANCO,
        /** Entrada nas pernas: o sprawl do outro vence. */
        PERNAS,
        /** 5P.2: pernas presas por quem esta em pe (a retencao com os pes nao funciona). */
        PERNAS_PRESAS
    }

    /** O que precisa ser verdade no fim da janela. */
    public enum Criterio {
        /** Da certo se ninguem responder com uma contra. */
        SE_NAO_CONTRADA,
        /** Controle do outro abaixo do limiar OU o outro exposto. */
        ESTABILIDADE_BAIXA,
        /** O outro nao esta em postura (guarda) OU esta exposto. */
        SEM_POSTURA_DO_OUTRO,
        /** So se o outro estiver exposto. */
        OUTRO_EXPOSTO,
        /** 5P.2: o PROPRIO controle (de quem controla e inicia) no limiar ou acima, e o outro sem postura. */
        CONTROLE_PROPRIO
    }

    /** Efeito das tecnicas instantaneas (ajustes de controle, sem trocar de posicao). */
    public enum Efeito {
        NENHUM,
        /** +25 de estabilidade do controle (consolidar, estabilizar, ajustar ganchos). */
        CONSOLIDAR,
        /** -20 de estabilidade do controle do outro (enquadrar, defender as maos). */
        ENQUADRAR,
        /** Postura dentro da guarda (60 ticks): as raspagens do outro precisam de uma exposicao. */
        POSTURA,
        /** Quebra a postura do outro (expoe POSTURA_QUEBRADA por 40 ticks). */
        QUEBRAR_POSTURA,
        /** Montada alta: +35 de estabilidade, mas a base fica alta (expoe BASE por 20 ticks). */
        MONTADA_ALTA,
        /** 5P.2: desequilibra o outro (expoe BASE por 30 ticks): a raspagem passa pela postura. */
        DESEQUILIBRAR,
        /** 5P.2: em pe, prende as pernas de quem esta no chao (expoe PERNAS_PRESAS por 45 ticks). */
        CONTROLAR_PERNAS,
        /** 5P.2: segura a manga do outro (conexao MANGA, pre-requisito da guarda laco). */
        PEGAR_MANGA,
        /** 5P.2: solta a manga presa pelo outro (perde a conexao; do laco volta para a guarda aberta). */
        LIVRAR_MANGA,
        /** 5P.2: recua / cria distancia (sem vinculo). */
        RECUAR
    }

    public boolean isInstantanea() {
        return janela == 0;
    }

    /** O comando aciona esta tecnica? */
    public boolean aceita(ComandoSolo c) {
        return comandos.contains(c);
    }

    @Override
    public String toString() {
        return id;
    }
}
