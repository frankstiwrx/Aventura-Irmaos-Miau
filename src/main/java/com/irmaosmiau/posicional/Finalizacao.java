package com.irmaosmiau.posicional;

import com.irmaosmiau.posicional.TecnicaSolo.Direcao;
import com.irmaosmiau.posicional.TecnicaSolo.Familia;
import com.irmaosmiau.regras.Graduacao;

/**
 * Entrega 5Q: uma finalizacao de uma POSICAO (a mesma tecnica pode aparecer em
 * varias posicoes com entradas diferentes; {@code tecnicaBase} as agrupa).
 *
 * Fluxo no MotorSolo: requisitos (posicao, papel, graduacao, aprendida, modo,
 * controle, folego) -> ENTRADA (janela, quem ataca fica exposto, o outro pode
 * cortar a entrada) -> CONTROLE/DISPUTA (pressao 0-100: quem ataca aperta, quem
 * defende defende, escapa ou bate) -> desfecho.
 *
 * @param requisito     o que precisa ser verdade para comecar (alem da posicao/papel)
 * @param limiarControle controle minimo (estabilidade) quando o requisito pede controle
 * @param janelaEntrada ticks de entrada (antes de virar ameaca)
 * @param ganhoPorTick  pressao que sobe sozinha por tick na disputa (ajustada pelo controle/cansaco)
 * @param escape        posicao depois de um escape (relativa a quem atacou)
 * @param falha         posicao quando a defesa zera a pressao (relativa a quem atacou)
 */
public record Finalizacao(
        String id,
        String tecnicaBase,
        String nome,
        String rotulo,
        CategoriaFinalizacao categoria,
        Graduacao graduacao,
        Posicao posicao,
        PapelPosicional papel,
        Familia familia,
        Direcao direcao,
        Requisito requisito,
        double limiarControle,
        double custo,
        int janelaEntrada,
        double ganhoPorTick,
        Desfecho escape,
        Desfecho falha) {

    /** Pre-condicao propria da tecnica (o "braco isolado", a "manga", o controle). */
    public enum Requisito {
        /** Nada alem da posicao e do papel. */
        NENHUM("-"),
        /** O outro sem postura dentro da guarda (braco/pescoco alcancaveis). */
        OUTRO_SEM_POSTURA("O OUTRO EM POSTURA"),
        /** O outro exposto (postura quebrada, base comprometida...). */
        OUTRO_EXPOSTO("SEM ABERTURA"),
        /** Controle da posicao (estabilidade) no limiar ou acima. */
        CONTROLE("CONTROLE BAIXO"),
        /** Conexao de manga (guarda aranha / laco). */
        MANGA("SEM A MANGA");

        private final String motivo;

        Requisito(String motivo) {
            this.motivo = motivo;
        }

        public String getMotivo() {
            return motivo;
        }
    }

    /** O comando aciona esta finalizacao? */
    public boolean aceita(TecnicaSolo.ComandoSolo c) {
        return c != null && c.familia() == familia && c.direcao() == direcao;
    }

    /** 5Q.6: o que a finalizacao ataca (braco, ombro, punho, pescoco, tornozelo, pe, joelho...). */
    public AlvoFinalizacao alvo() {
        return AlvoFinalizacao.de(this);
    }

    /** 5Q.6: rotulo da defesa tecnica pelo alvo (antes so pela categoria). */
    public String defesa() {
        return alvo().getDefesa();
    }

    /** Identificador futuro da animacao (contrato). */
    public String animacaoFutura() {
        return "finalizacao/" + id;
    }
}
