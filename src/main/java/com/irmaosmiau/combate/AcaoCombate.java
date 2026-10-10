package com.irmaosmiau.combate;

/**
 * Descreve uma tecnica/acao disponivel para o motor de combate.
 *
 * Esta classe contem DADOS da acao. Ela nao le teclado, nao movimenta
 * personagens, nao desenha animacoes e nao decide sozinha o resultado.
 * Essas responsabilidades pertencem a outras camadas do jogo.
 */
public final class AcaoCombate {

    private final String id;
    private final String nome;
    private final CategoriaAcao categoria;

    /*
     * Alcance em unidades locais do rig do ATACANTE.
     *
     * Exemplo: com escala 0.7, alcance 80 representa aproximadamente
     * 56 pixels de distancia entre os eixos de cintura.
     *
     * Isso evita transformar "55 pixels" numa regra eterna do combate.
     */
    private final double alcanceHorizontal;

    private final int diferencaMaximaLinhas;
    private final boolean exigeFrenteParaOponente;
    private final double custoFolego;
    private final double dificuldade;

    private final double pesoTecnica;
    private final double pesoForca;
    private final double pesoAgilidade;
    private final double pesoPesoCorporal;

    public AcaoCombate(
            String id,
            String nome,
            CategoriaAcao categoria,
            double alcanceHorizontal,
            int diferencaMaximaLinhas,
            boolean exigeFrenteParaOponente,
            double custoFolego,
            double dificuldade,
            double pesoTecnica,
            double pesoForca,
            double pesoAgilidade,
            double pesoPesoCorporal
    ) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("A acao precisa de um id.");
        }

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("A acao precisa de um nome.");
        }

        if (categoria == null) {
            throw new IllegalArgumentException("A acao precisa de uma categoria.");
        }

        if (alcanceHorizontal < 0) {
            throw new IllegalArgumentException("O alcance horizontal nao pode ser negativo.");
        }

        if (diferencaMaximaLinhas < 0) {
            throw new IllegalArgumentException("A diferenca maxima de linhas nao pode ser negativa.");
        }

        if (custoFolego < 0) {
            throw new IllegalArgumentException("O custo de folego nao pode ser negativo.");
        }

        if (dificuldade < 0 || dificuldade > 100) {
            throw new IllegalArgumentException("A dificuldade deve ficar entre 0 e 100.");
        }

        validarPeso("tecnica", pesoTecnica);
        validarPeso("forca", pesoForca);
        validarPeso("agilidade", pesoAgilidade);
        validarPeso("peso corporal", pesoPesoCorporal);

        this.id = id.trim().toLowerCase();
        this.nome = nome.trim();
        this.categoria = categoria;
        this.alcanceHorizontal = alcanceHorizontal;
        this.diferencaMaximaLinhas = diferencaMaximaLinhas;
        this.exigeFrenteParaOponente = exigeFrenteParaOponente;
        this.custoFolego = custoFolego;
        this.dificuldade = dificuldade;
        this.pesoTecnica = pesoTecnica;
        this.pesoForca = pesoForca;
        this.pesoAgilidade = pesoAgilidade;
        this.pesoPesoCorporal = pesoPesoCorporal;
    }

    private static void validarPeso(String nome, double valor) {
        if (valor < 0) {
            throw new IllegalArgumentException(
                    "O peso do atributo " + nome + " nao pode ser negativo."
            );
        }
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public CategoriaAcao getCategoria() {
        return categoria;
    }

    public double getAlcanceHorizontal() {
        return alcanceHorizontal;
    }

    public int getDiferencaMaximaLinhas() {
        return diferencaMaximaLinhas;
    }

    public boolean isExigeFrenteParaOponente() {
        return exigeFrenteParaOponente;
    }

    public double getCustoFolego() {
        return custoFolego;
    }

    public double getDificuldade() {
        return dificuldade;
    }

    public double getPesoTecnica() {
        return pesoTecnica;
    }

    public double getPesoForca() {
        return pesoForca;
    }

    public double getPesoAgilidade() {
        return pesoAgilidade;
    }

    public double getPesoPesoCorporal() {
        return pesoPesoCorporal;
    }

    /**
     * Soma dos pesos usados pela acao.
     *
     * Nao obrigamos a soma a ser 1.0 ou 100.0; o futuro resolvedor pode
     * normalizar automaticamente. Isso deixa o cadastro mais confortavel.
     */
    public double getSomaPesosAtributos() {
        return pesoTecnica
                + pesoForca
                + pesoAgilidade
                + pesoPesoCorporal;
    }
}
