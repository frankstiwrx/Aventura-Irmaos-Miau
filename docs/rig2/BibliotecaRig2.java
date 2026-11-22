import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * SUPERENTREGA 6.3 - Registro central da biblioteca de animacoes do Rig 2.0.
 *
 * Lista, num unico lugar e com metadados, todas as animacoes executaveis (de um ou de dois lutadores) e, a partir do
 * catalogo mestre (inventario/catalogo-completo.tsv), os movimentos do jogo ainda PENDENTES no Rig 2.0, que aparecem
 * como tal (sem execucao). Nenhuma regra de combate mora aqui: os eventos so marcam instantes para o motor.
 */
public class BibliotecaRig2 {

    /** Instante relevante para o motor (contato, janela, resolucao, decolagem...). */
    record Evento(String nome, double tempo) {}

    record Metadados(String id, String nome, String categoria, int participantes, double duracao, boolean loop,
                     String poseInicial, String poseFinal, String articulacoes, List<Evento> eventos, List<String> contatos,
                     String perspectivas, String estado, String idJogo, boolean executavel) {}

    static final String HOMOLOGADA = "Homologada (laboratório 6.2)", AGUARDANDO = "Testada automaticamente · aguardando homologação visual";
    static final String CONTINUAS = "contínuas de 0° a 180° (e orientação própria de cada lutador)";

    static Metadados individual(String id, String categoria, String ini, String fim, String articulacoes, String estado, String idJogo,
                                List<Evento> eventos) {
        AnimacoesRig2.Animacao a = AnimacoesRig2.criar(id, null);
        return new Metadados(id, a.nome(), categoria, 1, a.duracao(), a.emLoop(), ini, fim, articulacoes, eventos, List.of(), CONTINUAS,
                estado, idJogo, true);
    }

    static Metadados dupla(String id, String categoria, String ini, String fim, String estado, String idJogo) {
        DuelosRig2.AnimacaoDupla a = DuelosRig2.criar(id);
        List<String> contatos = new ArrayList<>();
        for (DuelosRig2.Pegada g : a.amostrar(0).pegadas)
            contatos.add("mão " + (g.mao() == 0 ? "direita" : "esquerda") + " de A → " + g.ponto() + " de B");
        return new Metadados(id, a.nome(), categoria, 2, a.duracao(), a.emLoop(), ini, fim, "corpo inteiro dos dois; mãos por alvo de contato",
                a.eventos(), contatos, CONTINUAS, estado, idJogo, true);
    }

    /** Animacoes executaveis registradas. */
    static List<Metadados> executaveis() {
        List<Metadados> r = new ArrayList<>();
        String corpo = "tronco, ombros, braços, quadril, pernas, pés, cabeça";
        r.add(individual(AnimacoesRig2.IDLE_NEUTRO, "Movimento básico", "neutra", "neutra (loop)", corpo, HOMOLOGADA, "respiracao", List.of()));
        r.add(individual(AnimacoesRig2.IDLE_COMBATE, "Movimento básico", "combate", "combate (loop)", corpo, HOMOLOGADA, "idle_combate", List.of()));
        r.add(individual(AnimacoesRig2.CAMINHADA, "Movimento básico", "caminhada", "caminhada (loop)", corpo, HOMOLOGADA, "andar", List.of()));
        r.add(individual(AnimacoesRig2.NEUTRA_PARA_COMBATE, "Transição de postura", "qualquer (pose mostrada)", "idle de combate", corpo, HOMOLOGADA, "-",
                List.of()));
        r.add(individual(AnimacoesRig2.COMBATE_PARA_NEUTRA, "Transição de postura", "qualquer (pose mostrada)", "idle neutro", corpo, HOMOLOGADA, "-",
                List.of()));
        r.add(individual(MovimentosRig2.CORRIDA, "Movimento básico", "corrida", "corrida (loop)", corpo, AGUARDANDO, "correr",
                List.of(new Evento("suspensão 1", .35 * .66), new Evento("suspensão 2", .85 * .66))));
        r.add(individual(MovimentosRig2.AGACHAR, "Movimento básico", "neutra", "agachado", corpo, AGUARDANDO, "agachar", List.of()));
        r.add(individual(MovimentosRig2.AGACHADO, "Movimento básico", "agachado", "agachado (loop)", corpo, AGUARDANDO, "agachar", List.of()));
        r.add(individual(MovimentosRig2.LEVANTAR_AGACHAMENTO, "Movimento básico", "agachado", "idle neutro", corpo, AGUARDANDO, "agachar", List.of()));
        r.add(individual(MovimentosRig2.PULO, "Movimento básico", "neutra", "idle neutro", corpo, AGUARDANDO, "pular",
                List.of(new Evento("decolagem", MovimentosRig2.Pulo.DECOLAGEM), new Evento("ápice", (MovimentosRig2.Pulo.DECOLAGEM + MovimentosRig2.Pulo.ATERRISSAGEM) / 2),
                        new Evento("aterrissagem", MovimentosRig2.Pulo.ATERRISSAGEM))));
        for (DuelosRig2.DefPegada d : DuelosRig2.PEGADAS) {
            if (!DuelosRig2.executavel(d)) continue;
            r.add(dupla(d.id(), "Pegada", "em pé, separados (combate)", d.id() + " mantida", AGUARDANDO, d.id()));
            r.add(dupla(d.id() + "-mantida", "Pegada", d.id(), d.id() + " (loop)", AGUARDANDO, d.id()));
        }
        r.add(dupla("derrubada_forcada", "Queda / entrada (desfecho defendido)", "pegada_1 mantida", "pegada_1 mantida", AGUARDANDO, "derrubada_forcada"));
        return r;
    }

    /** Movimentos do catalogo mestre ainda sem animacao no Rig 2.0 (lidos do TSV gerado). */
    static List<Metadados> pendentes() {
        Path tsv = Paths.get("docs", "rig2", "inventario", "catalogo-completo.tsv");
        if (!Files.exists(tsv)) tsv = Paths.get("inventario", "catalogo-completo.tsv");
        List<Metadados> r = new ArrayList<>();
        if (!Files.exists(tsv)) return r;
        Set<String> feitos = new HashSet<>();
        for (Metadados m : executaveis()) feitos.add(m.idJogo());
        try {
            List<String> linhas = Files.readAllLines(tsv, StandardCharsets.UTF_8);
            List<String> cab = Arrays.asList(linhas.get(0).split("\t", -1));
            int iId = cab.indexOf("id"), iNome = cab.indexOf("nome"), iCat = cab.indexOf("categoria"), iEst = cab.indexOf("rig2_estado"),
                    iDois = cab.indexOf("dois"), iIni = cab.indexOf("inicial"), iFim = cab.indexOf("final"), iReq = cab.indexOf("requisitos");
            for (String l : linhas.subList(1, linhas.size())) {
                String[] c = l.split("\t", -1);
                if (feitos.contains(c[iId])) continue;
                r.add(new Metadados("jogo/" + c[iId], c[iNome], c[iCat], c[iDois].equals("sim") ? 2 : 1, 0, false, c[iIni], c[iFim],
                        "requisitos no Rig 2.0: " + c[iReq], List.of(), List.of(), "-", c[iEst], c[iId], false));
            }
        } catch (IOException e) {
            return r;
        }
        return r;
    }

    static List<Metadados> todas() {
        List<Metadados> r = new ArrayList<>(executaveis());
        r.addAll(pendentes());
        return r;
    }

    static Optional<Metadados> buscar(String id) {
        return todas().stream().filter(m -> m.id().equals(id)).findFirst();
    }

    public static void main(String[] args) {
        List<Metadados> ex = executaveis(), pe = pendentes();
        System.out.println("executaveis: " + ex.size() + " · pendentes do catalogo: " + pe.size());
        for (Metadados m : ex) System.out.printf(Locale.ROOT, "  %-26s %-40s %d lutador(es)  %.2f s%s  [%s]%n", m.id(), m.categoria(),
                m.participantes(), m.duracao(), m.loop() ? " loop" : "", m.estado());
    }
}
