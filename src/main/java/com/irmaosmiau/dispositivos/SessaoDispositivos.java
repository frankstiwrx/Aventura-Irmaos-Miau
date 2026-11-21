package com.irmaosmiau.dispositivos;

import com.irmaosmiau.controles.CamadaComando;
import com.irmaosmiau.controles.PosicaoComando;
import com.irmaosmiau.controles.SlotJogador;
import com.irmaosmiau.input.InputHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Dispositivos de UMA partida (Entrega 5D): liga gamepads aos slots que
 * escolheram GAMEPAD e cuida do ciclo de vida a cada tick.
 *
 * Regras (explicitas e previsiveis):
 * - slots resolvidos em ordem (Jogador 1, depois Jogador 2); cada um recebe o
 *   primeiro gamepad conectado que NAO esteja com o outro slot: um gamepad
 *   nunca controla os dois jogadores;
 * - enquanto um gamepad controla o slot, o teclado daquele slot fica
 *   desligado (dois dispositivos nao agem no mesmo jogador);
 * - sem gamepad livre (ou sem suporte), o slot usa o teclado e o jogador e avisado;
 * - desconectou: todas as acoes do gamepad sao soltas na hora, o teclado do
 *   slot volta e o jogador e avisado;
 * - reconectou (ou outro gamepad livre apareceu): o slot volta a usar o
 *   gamepad, preferindo o mesmo aparelho; botoes ja segurados nesse momento
 *   ficam travados ate serem soltos;
 * - janela sem foco: o gamepad e ignorado; ao voltar o foco, o que estiver
 *   segurado fica travado ate ser solto.
 */
public final class SessaoDispositivos {

    private final ProvedorGamepads provedor;
    private final MapaGamepad mapa;
    private final InputHandler input;
    private final Map<SlotJogador, FonteGamepad> fontes = new EnumMap<>(SlotJogador.class);
    private final Map<SlotJogador, InfoGamepad> vinculos = new EnumMap<>(SlotJogador.class);
    private final Map<SlotJogador, String> identidadeAnterior = new EnumMap<>(SlotJogador.class);
    private final Set<SlotJogador> avisadoSemGamepad = EnumSet.noneOf(SlotJogador.class);
    private final List<String> mensagens = new ArrayList<>();
    private boolean focado = true;
    private boolean pausaPedida;
    /** 5O.1: teclado-controle por slot (J2 no teclado = um Xbox de teclas). */
    private final Map<SlotJogador, TecladoControle> teclados = new EnumMap<>(SlotJogador.class);
    /** 5O.1: slots no TECLADO cuja fonte e so o teclado-controle (nunca pegam um gamepad). */
    private final Set<SlotJogador> soTeclado = EnumSet.noneOf(SlotJogador.class);
    private boolean pausaPedidaTeclado;

    /**
     * @param emJogo slots que existem nesta partida (o treino e o modo
     *               individual usam so o Jogador 1)
     */
    public SessaoDispositivos(AssociacaoDispositivos associacao, ProvedorGamepads provedor,
            InputHandler input, Collection<SlotJogador> emJogo, MapaGamepad mapa) {
        this.provedor = provedor;
        this.mapa = mapa;
        this.input = input;
        for (SlotJogador s : SlotJogador.values()) {
            if (!emJogo.contains(s)) {
                continue;
            }
            input.tecladoControle(s).ifPresent(t -> teclados.put(s, t));
            boolean gamepad = associacao.tipo(s) == TipoDispositivo.GAMEPAD;
            if (gamepad || teclados.containsKey(s)) {
                FonteGamepad f = new FonteGamepad(s, mapa);
                fontes.put(s, f);
                input.conectarFonte(s, f);
                if (!gamepad) {
                    soTeclado.add(s);
                }
            }
        }
        atualizar();
    }

    /** Uma vez por tick do jogo (inclusive pausado, para o botao de pausa). */
    public void atualizar() {
        if (fontes.isEmpty()) {
            return;
        }
        // 1) Quem perdeu o gamepad.
        for (SlotJogador s : fontes.keySet()) {
            if (soTeclado.contains(s)) {
                continue;
            }
            InfoGamepad v = vinculos.get(s);
            if (v != null && provedor.estado(v.chave()).isEmpty()) {
                vinculos.remove(s);
                identidadeAnterior.put(s, v.identidade());
                FonteGamepad f = fontes.get(s);
                f.atualizar(null);
                f.travarNaProximaLeitura();
                input.setTecladoAtivo(s, true);
                // O aviso de desconexao ja diz que o teclado assumiu; nao repetir "nenhum gamepad livre".
                avisadoSemGamepad.add(s);
                mensagens.add(s.getSigla() + ": GAMEPAD DESCONECTADO · teclado ativo");
            }
        }
        // 2) Quem esta sem gamepad tenta um livre.
        List<InfoGamepad> conectados = provedor.conectados();
        for (SlotJogador s : fontes.keySet()) {
            if (vinculos.containsKey(s) || soTeclado.contains(s)) {
                continue;
            }
            Optional<InfoGamepad> livre = escolherLivre(s, conectados);
            if (livre.isPresent()) {
                vincular(s, livre.get());
            } else if (avisadoSemGamepad.add(s)) {
                mensagens.add(s.getSigla() + ": nenhum gamepad livre · usando o teclado");
            }
        }
        // 3) Leitura.
        for (Map.Entry<SlotJogador, FonteGamepad> e : fontes.entrySet()) {
            InfoGamepad v = vinculos.get(e.getKey());
            TecladoControle t = teclados.get(e.getKey());
            if (v == null && t == null) {
                continue;
            }
            // 5O.1: sem gamepad vinculado, o teclado-controle do slot alimenta a mesma fonte.
            EstadoGamepad estado = !focado ? null
                    : v != null ? provedor.estado(v.chave()).orElse(null) : t.estado();
            e.getValue().atualizar(estado);
            if (e.getValue().consumirPausa()) {
                if (v != null) {
                    pausaPedida = true;
                } else {
                    pausaPedidaTeclado = true;
                }
            }
        }
    }

    private Optional<InfoGamepad> escolherLivre(SlotJogador slot, List<InfoGamepad> conectados) {
        List<InfoGamepad> livres = new ArrayList<>();
        for (InfoGamepad g : conectados) {
            boolean ocupado = vinculos.values().stream().anyMatch(v -> v.chave().equals(g.chave()));
            if (!ocupado) {
                livres.add(g);
            }
        }
        String anterior = identidadeAnterior.get(slot);
        if (anterior != null) {
            for (InfoGamepad g : livres) {
                if (anterior.equals(g.identidade())) {
                    return Optional.of(g);
                }
            }
        }
        return livres.stream().findFirst();
    }

    private void vincular(SlotJogador slot, InfoGamepad gamepad) {
        boolean reconexao = identidadeAnterior.containsKey(slot) || avisadoSemGamepad.contains(slot);
        vinculos.put(slot, gamepad);
        avisadoSemGamepad.remove(slot);
        FonteGamepad f = fontes.get(slot);
        f.travarNaProximaLeitura();
        input.setTecladoAtivo(slot, false);
        mensagens.add(slot.getSigla() + ": " + (reconexao ? "GAMEPAD CONECTADO" : "GAMEPAD") + " · " + gamepad.nome());
    }

    /** Janela ganhou/perdeu o foco. */
    public void setFocado(boolean agora) {
        if (agora == focado) {
            return;
        }
        focado = agora;
        for (FonteGamepad f : fontes.values()) {
            if (agora) {
                f.travarNaProximaLeitura();
            } else {
                f.atualizar(null);
                f.liberarTodas();
            }
        }
    }

    /** true uma vez por toque no botao de pausa de qualquer gamepad em uso. */
    public boolean consumirPausa() {
        boolean p = pausaPedida;
        pausaPedida = false;
        return p;
    }

    /**
     * 5O.1: ENTER (Start do teclado-controle) uma vez. So ABRE a pausa: com a
     * pausa aberta, o ENTER confirma o item do menu (nao pode alternar duas vezes).
     */
    public boolean consumirPausaTeclado() {
        boolean p = pausaPedidaTeclado;
        pausaPedidaTeclado = false;
        return p;
    }

    // =====================================================
    // ROTA (treino: F11 alterna o personagem do dispositivo principal)
    // =====================================================
    private com.irmaosmiau.controles.RotaControles rota = new com.irmaosmiau.controles.RotaControles();

    public void setRota(com.irmaosmiau.controles.RotaControles r) {
        rota = r == null ? new com.irmaosmiau.controles.RotaControles() : r;
    }

    /** O slot cujos dispositivos comandam este personagem (null = nenhum). */
    private SlotJogador origem(SlotJogador alvo) {
        return rota.origem(alvo);
    }

    private FonteGamepad fonteDe(SlotJogador alvo) {
        SlotJogador o = origem(alvo);
        return o == null ? null : fontes.get(o);
    }

    /**
     * Troca de personagem: esquece comandos pendentes (RS, D-pad, escolha de familia) de todas as fontes e
     * trava o que estiver segurado ate ser solto, para nada passar de um personagem para o outro.
     */
    public void prepararTroca() {
        for (FonteGamepad f : fontes.values()) {
            f.limparContextual();
            f.travarNaProximaLeitura();
        }
    }

    /** 5O.1: o slot esta no teclado-controle agora (sem gamepad vinculado)? */
    public boolean usaTecladoControleAgora(SlotJogador slot) {
        SlotJogador o = origem(slot);
        return o != null && teclados.containsKey(o) && fontes.containsKey(o) && !vinculos.containsKey(o);
    }

    /** 5O.1: o slot e lido como CONTROLE agora (gamepad real ou teclado-controle). */
    public boolean usaControleAgora(SlotJogador slot) {
        return usaGamepadAgora(slot) || usaTecladoControleAgora(slot);
    }

    /** Avisos novos (conexao/desconexao) para o HUD; a lista e esvaziada. */
    public List<String> consumirMensagens() {
        List<String> m = List.copyOf(mensagens);
        mensagens.clear();
        return m;
    }

    /** Gamepad controlando o slot agora (vazio = teclado). */
    public Optional<InfoGamepad> gamepadDo(SlotJogador slot) {
        SlotJogador o = origem(slot);
        return o == null ? Optional.empty() : Optional.ofNullable(vinculos.get(o));
    }

    public boolean usaGamepadAgora(SlotJogador slot) {
        SlotJogador o = origem(slot);
        return o != null && vinculos.containsKey(o);
    }

    public Optional<FonteGamepad> fonte(SlotJogador slot) {
        return Optional.ofNullable(fonteDe(slot));
    }

    // =====================================================
    // COMANDOS CONTEXTUAIS (Entrega 5F)
    // =====================================================
    /** O slot esta num gamepad AGORA e no esquema CONTEXTUAL. */
    public boolean usaContextualAgora(SlotJogador slot) {
        SlotJogador o = origem(slot);
        return o != null && usaControleAgora(slot) && mapa.esquema(o) == EsquemaGamepad.CONTEXTUAL;
    }

    /** Comando das 12 posicoes deste toque (vazio se nada, ou se o slot nao esta no contextual). */
    public Optional<PosicaoComando> consumirComandoContextual(SlotJogador slot) {
        FonteGamepad f = fonteDe(slot);
        if (f == null || !usaContextualAgora(slot)) {
            if (f != null) {
                f.consumirComandoContextual();
            }
            return Optional.empty();
        }
        return f.consumirComandoContextual();
    }

    /** Entrega 5L: RS do esquema CLASSICO (vazio fora do gamepad ou no Contextual). */
    public Optional<com.irmaosmiau.controles.DirecaoTecnica> consumirPegadaClassica(SlotJogador slot) {
        FonteGamepad f = fonteDe(slot);
        if (f == null || !usaControleAgora(slot)) {
            return Optional.empty();
        }
        return f.consumirPegadaClassica();
    }

    /** 5O: familia escolhida pelos modificadores no gamepad do slot (PEGADAS sem gamepad). */
    public com.irmaosmiau.controles.FamiliaTecnica familiaGamepad(SlotJogador slot) {
        FonteGamepad f = fonteDe(slot);
        return f == null || !usaControleAgora(slot) ? com.irmaosmiau.controles.FamiliaTecnica.PEGADAS
                : f.familiaAtiva();
    }

    /** 5O: direcao do RS segurada na familia (null se nenhuma). */
    public com.irmaosmiau.controles.DirecaoTecnica direcaoFamiliaGamepad(SlotJogador slot) {
        FonteGamepad f = fonteDe(slot);
        return f == null || !usaControleAgora(slot) ? null : f.direcaoFamiliaSegurada();
    }

    /** 5O: escolha (familia + direcao) do RS desde a ultima consulta. */
    public Optional<com.irmaosmiau.controles.FamiliaTecnica.Escolha> consumirEscolhaFamilia(SlotJogador slot) {
        FonteGamepad f = fonteDe(slot);
        if (f == null || !usaControleAgora(slot)) {
            return Optional.empty();
        }
        return f.consumirEscolhaFamilia();
    }

    public CamadaComando camadaAtiva(SlotJogador slot) {
        FonteGamepad f = fonteDe(slot);
        return f == null ? CamadaComando.UM : f.camadaAtiva();
    }

    /** Pausa/fim: nada pendente; o D-pad precisa ser solto antes do proximo comando. */
    public void limparContextual() {
        for (FonteGamepad f : fontes.values()) {
            f.limparContextual();
        }
    }

    public MapaGamepad getMapa() {
        return mapa;
    }
}
