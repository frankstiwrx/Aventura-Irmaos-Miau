package com.irmaosmiau.treino;

import com.irmaosmiau.combate.SistemaCombate;
import com.irmaosmiau.entities.IrmaoMiau;
import com.irmaosmiau.posicional.Finalizacao;
import com.irmaosmiau.posicional.Lado;
import com.irmaosmiau.posicional.MotorSolo;
import com.irmaosmiau.posicional.NarradorSolo;
import com.irmaosmiau.posicional.TecnicaSolo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Entrega 5Q.2: LABORATORIO do modo treino. Escolhe uma situacao (posicao real
 * do MotorSolo) e quem controla; aplica pelo inicializador do motor (nada de
 * setters espalhados) e responde as consultas do HUD tecnico e do catalogo
 * sempre pelas regras do proprio motor (motivoFinalizacao), sem recalcular nada.
 *
 * So existe no treino: a partida normal nunca cria um laboratorio.
 */
public final class LaboratorioTreino {

    private SituacaoTreino situacao = SituacaoTreino.MONTADA;
    private Lado ativo = Lado.A;
    private boolean parceiroControlado;
    /** A situacao foi iniciada (F5 passa a reiniciar a SITUACAO). */
    private boolean ativa;

    public SituacaoTreino getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoTreino s) {
        situacao = s == null ? SituacaoTreino.MONTADA : s;
    }

    /** Proxima/anterior situacao (circular), para o menu. */
    public void proximaSituacao(int passo) {
        SituacaoTreino[] v = SituacaoTreino.values();
        situacao = v[Math.floorMod(situacao.ordinal() + passo, v.length)];
    }

    /** Lado que controla (A = J1, B = J2). */
    public Lado getAtivo() {
        return ativo;
    }

    public void setAtivo(Lado l) {
        ativo = l == null ? Lado.A : l;
    }

    public void inverterPapeis() {
        ativo = ativo.outro();
    }

    /** J2 controlado por um segundo jogador (so no laboratorio); padrao: parceiro passivo. */
    public boolean isParceiroControlado() {
        return parceiroControlado;
    }

    public void setParceiroControlado(boolean v) {
        parceiroControlado = v;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void desativar() {
        ativa = false;
    }

    /**
     * Aplica a situacao aos lutadores RECEM-CRIADOS da sessao (folego, graduacao e
     * kimono ja vem das escolhas). Em pe com pegada usa o SistemaCombate de verdade:
     * aproxima a dupla ate o alcance e tenta a PEGADA_1 (estabelece em ~30 ticks).
     */
    public void aplicar(MotorSolo motor, SistemaCombate sistema, IrmaoMiau j1, IrmaoMiau j2) {
        Lado cima = situacao.porCima(ativo);
        Lado ctrl = situacao.controlador(ativo);
        // mesma linha e frente a frente antes de tudo
        while (j2.getLinhaAtual() > j1.getLinhaAtual()) {
            j2.subirLinha();
        }
        while (j2.getLinhaAtual() < j1.getLinhaAtual()) {
            j2.descerLinha();
        }
        motor.iniciarSituacao(situacao.getPosicao(), cima, ctrl, situacao.exigeManga());
        if (situacao == SituacaoTreino.EM_PE_PEGADA && sistema != null) {
            IrmaoMiau ap = ativo == Lado.A ? j1 : j2;
            IrmaoMiau rec = ap == j1 ? j2 : j1;
            if (sistema.estaEmInteracao()) {
                sistema.encerrarInteracao();
            }
            int lado = rec.getX() >= ap.getX() ? 1 : -1;
            for (int d = 140; d >= 30 && !sistema.podeTentarAcao(ap, rec, "pegada_1"); d -= 2) {
                rec.encostarNaBorda(ap.getX() + lado * d - rec.getX());
                ap.setViradoParaDireita(lado > 0);
                rec.setViradoParaDireita(lado < 0);
            }
            sistema.tentarAcao(ap, rec, "pegada_1");
        }
        ativa = true;
    }

    // =====================================================
    // CONSULTAS (HUD tecnico e catalogo): sempre pelo motor
    // =====================================================
    private static String sigla(Lado l) {
        return l == Lado.A ? "J1" : "J2";
    }

    /** Linhas curtas do HUD tecnico (posicao, controle, graduacoes, finalizacao, ultimo desfecho, bloqueio). */
    public List<String> linhasHud(MotorSolo m, IrmaoMiau j1, IrmaoMiau j2, String bloqueioJ1, String bloqueioJ2) {
        List<String> l = new ArrayList<>();
        String ctrl = m.getEstado().getControlador().map(LaboratorioTreino::sigla).orElse("—");
        l.add("LAB · " + m.getPosicao().getNome().toUpperCase(Locale.ROOT) + " · CONTROLA " + ctrl);
        l.add("J1 FAIXA " + j1.getGraduacao().name() + " · J2 FAIXA " + j2.getGraduacao().name());
        m.getAtaque().ifPresentOrElse(a -> l.add(sigla(a.atacante()) + ": "
                        + a.finalizacao().nome().toUpperCase(Locale.ROOT) + " · "
                        + (a.fase() == MotorSolo.FaseFinalizacao.ENTRADA ? "ENTRADA"
                                : "DISPUTA " + Math.round(a.pressao()) + "%")),
                () -> m.getTentativa().ifPresentOrElse(t -> l.add(sigla(t.iniciador()) + ": "
                        + t.tecnica().nome().toUpperCase(Locale.ROOT) + " · JANELA"),
                        () -> {
                            MotorSolo.Evento e = m.getUltimoEvento();
                            String frase = e == null ? null : NarradorSolo.mensagem(e);
                            l.add(frase == null ? "—" : "ÚLTIMO: " + frase);
                        }));
        String b = bloqueioJ1 != null && !"-".equals(bloqueioJ1) ? "J1 " + bloqueioJ1
                : bloqueioJ2 != null && !"-".equals(bloqueioJ2) ? "J2 " + bloqueioJ2 : null;
        if (b != null) {
            l.add("BLOQUEIO: " + b);
        }
        return l;
    }

    /** Status de uma finalizacao no catalogo consultavel. */
    public enum Status {
        LIBERADA, GRADUACAO, NAO_APRENDIDA, PROIBIDA_NO_MODO, INDISPONIVEL
    }

    /** Uma linha do catalogo. */
    public record Item(Lado lado, Finalizacao finalizacao, Status status, String motivo) {
        public String texto() {
            String cmd = finalizacao.familia().getRotulo() + " " + seta(finalizacao.direcao());
            String st = switch (status) {
                case LIBERADA -> "LIBERADA";
                case GRADUACAO -> "FAIXA " + finalizacao.graduacao().name();
                case NAO_APRENDIDA -> "NÃO APRENDIDA";
                case PROIBIDA_NO_MODO -> "PROIBIDA NO MODO";
                case INDISPONIVEL -> "AGORA NÃO: " + motivo;
            };
            return sigla(lado) + " " + cmd + " " + finalizacao.nome() + " — " + st;
        }
    }

    private static String seta(TecnicaSolo.Direcao d) {
        return switch (d) {
            case CIMA -> "↑";
            case DIREITA -> "→";
            case BAIXO -> "↓";
            case ESQUERDA -> "←";
        };
    }

    /** As finalizacoes REAIS da posicao atual, para cada lado que tem o papel delas, com o status do motor. */
    public List<Item> catalogo(MotorSolo m) {
        List<Item> itens = new ArrayList<>();
        for (Finalizacao f : m.getCatalogoFinalizacoes().todas()) {
            if (f.posicao() != m.getPosicao()) {
                continue;
            }
            for (Lado l : Lado.values()) {
                if (!m.getEstado().temPapel(l, f.papel())) {
                    continue;
                }
                String mot = m.motivoFinalizacao(l, f);
                Status st = mot == null ? Status.LIBERADA
                        : mot.startsWith("GRADUAÇÃO") ? Status.GRADUACAO
                        : mot.equals("NÃO APRENDIDA") ? Status.NAO_APRENDIDA
                        : mot.equals("PROIBIDA NO MODO") ? Status.PROIBIDA_NO_MODO : Status.INDISPONIVEL;
                itens.add(new Item(l, f, st, mot));
            }
        }
        return itens;
    }
}
