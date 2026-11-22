import com.irmaosmiau.animacao.AnimacoesTecnicas;
import com.irmaosmiau.combate.EstadoInteracaoCombate;
import com.irmaosmiau.entities.TipoAcrobacia;
import com.irmaosmiau.entities.TransicaoPostura;
import com.irmaosmiau.posicional.*;

import java.lang.reflect.*;
import java.util.*;

/** Extrator SOMENTE LEITURA do inventario de animacoes do jogo (6.3). Saida: TSV no stdout. */
public class InventarioJogo {
    static String j(Collection<?> c) {
        List<String> l = new ArrayList<>();
        for (Object o : c) l.add(String.valueOf(o));
        Collections.sort(l);
        return String.join(",", l);
    }

    static void linha(Object... v) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            if (i > 0) b.append('\t');
            b.append(String.valueOf(v[i]).replace('\t', ' ').replace('\n', ' '));
        }
        System.out.println(b);
    }

    /** Getters sem argumento de um enum (para dump generico). */
    static String getters(Object o) {
        List<String> r = new ArrayList<>();
        for (Method m : o.getClass().getMethods()) {
            if (m.getParameterCount() != 0 || m.getDeclaringClass() == Object.class || m.getDeclaringClass() == Enum.class) continue;
            if (!(m.getName().startsWith("get") || m.getName().startsWith("is"))) continue;
            try { r.add(m.getName() + "=" + m.invoke(o)); } catch (Exception e) { r.add(m.getName() + "=?"); }
        }
        Collections.sort(r);
        return String.join("; ", r);
    }

    public static void main(String[] a) throws Exception {
        // chaves das animacoes proprias (quadros-chave)
        Field f = AnimacoesTecnicas.class.getDeclaredField("ANIMACOES");
        f.setAccessible(true);
        Map<?, ?> anim = (Map<?, ?>) f.get(null);
        Set<String> chaves = new TreeSet<>();
        for (Object k : anim.keySet()) chaves.add(String.valueOf(k));

        CatalogoTecnicasSolo cat = CatalogoTecnicasSolo.referencia();
        for (TecnicaSolo t : cat.todas()) {
            Optional<QuedasExplosivas.Perfil> qe = QuedasExplosivas.perfil(t.id());
            linha("TECNICA", t.id(), t.nome(), t.rotulo(), j(t.origens()), t.papel(), j(t.comandos()), t.janela(),
                    t.exposicao(), t.criterio(), t.efeito(), t.sucesso(), j(t.contras()), j(cat.requisitos(t.id())),
                    cat.posturaExigida(t.id()).map(String::valueOf).orElse("-"), cat.falha(t.id()).map(String::valueOf).orElse("-"),
                    CatalogoTecnicasSolo.animacaoFutura(t.id()), chaves.contains(t.id() + ":ATACA"), chaves.contains(t.id() + ":RECEBE"),
                    qe.isPresent() ? "explosiva:" + qe.get() : "-");
        }
        for (Finalizacao z : CatalogoFinalizacoes.referencia().todas()) {
            linha("FINALIZACAO", z.id(), z.nome(), z.rotulo(), z.categoria(), z.graduacao(), z.posicao(), z.papel(),
                    z.familia(), z.direcao(), z.requisito(), z.janelaEntrada(), z.ganhoPorTick(), z.alvo(), z.tecnicaBase(),
                    z.animacaoFutura(), chaves.contains(z.id() + ":ATACA"), chaves.contains(z.id() + ":RECEBE"), z.escape(), z.falha());
        }
        for (DefinicaoTransicao d : CatalogoTransicoes.referencia().todas()) {
            linha("TRANSICAO", d.getId(), d.getNome(), d.getOrigem(), d.getPapelIniciador(), d.getCadencia(), d.getDuracaoTicks(),
                    j(d.destinosPossiveis()), d.getIdTecnica().orElse("-"));
        }
        // articulacoes realmente movidas por cada animacao de quadros-chave (amostrando os deltas)
        String[] campos = {"cintura", "cintura", "cintura", "tronco", "tronco", "tronco", "rotação global", "cabeça", "cabeça", "cabeça",
            "braço esq.", "braço esq.", "mão esq.", "mão esq.", "braço dir.", "braço dir.", "mão dir.", "mão dir.",
            "joelho esq.", "joelho esq.", "pé esq.", "pé esq.", "joelho dir.", "joelho dir.", "pé dir.", "pé dir."};
        for (String k : chaves) {
            Set<String> partes = new TreeSet<>();
            for (double t = 0; t <= 1.0001; t += .05) {
                double[] d = AnimacoesTecnicas.amostraParaTeste(k, t);
                for (int i = 0; i < d.length; i++) {
                    if (Math.abs(d[i]) < .5) continue;
                    if (i >= 4 && i <= 29) partes.add(campos[i - 4]);
                    else if (i >= 30) partes.add("deslocamento");
                    else partes.add("base(" + i + ")");
                }
            }
            linha("QUADROS", k, String.join(", ", partes));
        }
        for (TransicaoPostura t : TransicaoPostura.values()) linha("POSTURA", t.name(), getters(t));
        for (TipoAcrobacia t : TipoAcrobacia.values()) linha("ACROBACIA", t.name(), getters(t));
        for (Posicao p : Posicao.values()) linha("POSICAO", p.name(), p.getNome(), p.isNoChao(), p.isGuarda(), p.isJogavelNoJogo());
        for (EstadoInteracaoCombate e : EstadoInteracaoCombate.values()) linha("INTERACAO", e.name());
    }
}
