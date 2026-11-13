package com.irmaosmiau.preferencias;

import com.irmaosmiau.audio.AudioJogo;
import com.irmaosmiau.audio.ServicoAudio;
import com.irmaosmiau.cenario.RegistroCenarios;
import com.irmaosmiau.persistencia.ConfiguracoesJogo;
import com.irmaosmiau.persistencia.PersistenciaException;
import com.irmaosmiau.persistencia.RepositorioConfiguracoes;
import com.irmaosmiau.persistencia.ResultadoCarga;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ponto UNICO de verdade das preferencias durante a execucao.
 *
 * Menu de configuracoes, atalhos F4/F9/F10 e o inicio do jogo passam por
 * aqui; nenhuma outra classe muda o ServicoAudio "por fora". Assim o que
 * esta tocando, o que a tela mostra e o que esta salvo nao divergem.
 *
 * Duas formas:
 * - persistente ({@link #carregar}): le/grava configuracoes.json;
 * - em memoria ({@link #emMemoria}): nunca toca no disco (testes, protótipo).
 *
 * Regras de seguranca:
 * - nada e gravado so por abrir o jogo;
 * - arquivo invalido nao e sobrescrito: na primeira gravacao ele e guardado
 *   como "configuracoes.json.corrompido-..." e o jogador e avisado.
 */
public final class PreferenciasJogo {

    private static PreferenciasJogo global;

    private final RepositorioConfiguracoes repositorio;   // null = em memoria
    private final ServicoAudio audio;

    private ConfiguracoesJogo atual;
    private ConfiguracoesJogo salva;
    private boolean arquivoProblematico;
    private final List<String> avisos = new ArrayList<>();

    private PreferenciasJogo(RepositorioConfiguracoes repositorio, ServicoAudio audio) {
        if (audio == null) {
            throw new IllegalArgumentException("O servico de audio e obrigatorio.");
        }
        this.repositorio = repositorio;
        this.audio = audio;
    }

    // =====================================================
    // CRIACAO
    // =====================================================
    /**
     * Carrega do disco e aplica no audio. Arquivo ausente = padroes (nada e
     * criado). Arquivo com problema = padroes + aviso (o arquivo fica
     * intacto ate a primeira gravacao).
     */
    public static PreferenciasJogo carregar(RepositorioConfiguracoes repositorio, ServicoAudio audio) {
        if (repositorio == null) {
            throw new IllegalArgumentException("O repositorio e obrigatorio.");
        }
        PreferenciasJogo p = new PreferenciasJogo(repositorio, audio);
        ConfiguracoesJogo lida;
        try {
            lida = repositorio.carregar();
        } catch (PersistenciaException ex) {
            lida = ConfiguracoesJogo.padrao();
            p.arquivoProblematico = ex.getMotivo() != PersistenciaException.Motivo.ERRO_DE_ARQUIVO;
            p.avisos.add("Não foi possível usar as configurações salvas; valores padrão em uso. "
                    + (p.arquivoProblematico
                            ? "O arquivo atual será guardado como cópia quando você salvar. "
                            : "")
                    + "Detalhe: " + ex.getMessage());
        }
        p.atual = lida;
        p.salva = lida;
        p.aplicarNoAudio();
        return p;
    }

    /** Preferencias que nunca leem nem gravam arquivos. */
    public static PreferenciasJogo emMemoria(ConfiguracoesJogo inicial, ServicoAudio audio) {
        PreferenciasJogo p = new PreferenciasJogo(null, audio);
        p.atual = inicial == null ? ConfiguracoesJogo.padrao() : inicial;
        p.salva = p.atual;
        p.aplicarNoAudio();
        return p;
    }

    /** Instancia usada pelo jogo (Main instala a persistente). */
    public static synchronized PreferenciasJogo global() {
        if (global == null) {
            global = emMemoria(ConfiguracoesJogo.padrao(), AudioJogo.servico());
        }
        return global;
    }

    public static synchronized void instalarGlobal(PreferenciasJogo preferencias) {
        global = preferencias;
    }

    // =====================================================
    // CONSULTAS
    // =====================================================
    public ConfiguracoesJogo getAtual() {
        return atual;
    }

    /** Ultima versao gravada (ou carregada). */
    public ConfiguracoesJogo getSalva() {
        return salva;
    }

    public boolean temAlteracoesNaoSalvas() {
        return !atual.equals(salva);
    }

    public boolean isPersistente() {
        return repositorio != null;
    }

    public List<String> getAvisos() {
        return List.copyOf(avisos);
    }

    /**
     * Cenario preferido se ainda estiver cadastrado; senao o padrao, com
     * aviso (a preferencia salva NAO e apagada).
     */
    public String cenarioValido(RegistroCenarios registro) {
        String id = atual.getCenarioPreferido();
        if (registro.contem(id)) {
            return id;
        }
        String aviso = "O cenário salvo \"" + id + "\" não existe mais; usando \""
                + RegistroCenarios.ID_PADRAO + "\".";
        if (!avisos.contains(aviso)) {
            avisos.add(aviso);
        }
        return RegistroCenarios.ID_PADRAO;
    }

    // =====================================================
    // ALTERACOES
    // =====================================================
    /** Aplica no audio AGORA, sem gravar (previa da tela de configuracoes). */
    public void aplicar(ConfiguracoesJogo nova) {
        if (nova == null) {
            throw new IllegalArgumentException("As configuracoes sao obrigatorias.");
        }
        atual = nova;
        aplicarNoAudio();
    }

    /** Volta para a ultima versao salva (descartar alteracoes). */
    public void descartarAlteracoes() {
        aplicar(salva);
    }

    /**
     * Aplica e grava.
     *
     * @throws PersistenciaException se nao for possivel gravar (o estado
     *         aplicado continua valendo; nada salvo e perdido)
     */
    public void salvar(ConfiguracoesJogo nova) {
        aplicar(nova);
        if (repositorio == null) {
            salva = atual;
            return;
        }
        if (arquivoProblematico) {
            // Preserva o arquivo ruim como copia antes de gravar o novo.
            ResultadoCarga<ConfiguracoesJogo> r = repositorio.carregarOuPadrao();
            avisos.addAll(r.getAvisos());
            arquivoProblematico = false;
        }
        repositorio.salvar(atual);
        salva = atual;
    }

    /** F9: liga/desliga a musica e grava. @return erro de gravacao, se houver */
    public Optional<String> alternarMusica() {
        return salvarSilenciandoErro(atual.comMusicaAtiva(!atual.isMusicaAtiva()));
    }

    /** F10: liga/desliga os efeitos e grava. */
    public Optional<String> alternarEfeitos() {
        return salvarSilenciandoErro(atual.comEfeitosAtivos(!atual.isEfeitosAtivos()));
    }

    /** F4 / tela: troca o cenario preferido e grava. */
    public Optional<String> definirCenarioPreferido(String idCenario) {
        return salvarSilenciandoErro(atual.comCenarioPreferido(idCenario));
    }

    /**
     * Para atalhos durante o jogo: a mudanca vale imediatamente mesmo que a
     * gravacao falhe; o erro volta como texto para ser mostrado no HUD.
     */
    private Optional<String> salvarSilenciandoErro(ConfiguracoesJogo nova) {
        try {
            salvar(nova);
            return Optional.empty();
        } catch (PersistenciaException ex) {
            return Optional.of("não foi possível salvar: " + ex.getMessage());
        }
    }

    private void aplicarNoAudio() {
        audio.setVolumeMusica(atual.getVolumeMusica());
        audio.setVolumeEfeitos(atual.getVolumeEfeitos());
        audio.setMusicaAtiva(atual.isMusicaAtiva());
        audio.setEfeitosAtivos(atual.isEfeitosAtivos());
    }
}
