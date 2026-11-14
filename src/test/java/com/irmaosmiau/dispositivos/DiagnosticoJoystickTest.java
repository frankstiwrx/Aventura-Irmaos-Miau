package com.irmaosmiau.dispositivos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.irmaosmiau.persistencia.PastaDeTeste;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

/** Entrega 5E: diagnostico distingue ausencia fisica de ausencia de mapeamento (sem hardware). */
class DiagnosticoJoystickTest {

    @TempDir(factory = PastaDeTeste.class, cleanup = CleanupMode.NEVER)
    Path tmp;

    private static String texto(ProvedorGamepads p, ArquivoMapeamentos.Status m) {
        return String.join("\n", RelatorioDiagnostico.linhas(p, m));
    }

    @Test
    void nenhumJoystickFisico() {
        String t = texto(new ProvedorGamepadSimulado(), ArquivoMapeamentos.Status.NENHUM);
        assertTrue(t.contains("1. Biblioteca: OK"), t);
        assertTrue(t.contains("2. Joysticks detectados pelo sistema: 0"), t);
        assertTrue(t.contains("3. Gamepads utilizáveis no jogo: 0"), t);
        assertTrue(t.contains("nenhum joystick chegou ao GLFW"), t);
        assertTrue(t.contains("Leitura de gamepad: OK"), "linha da 5D mantida");
    }

    @Test
    void joystickPresenteSemMapeamento() {
        ProvedorGamepadSimulado sim = new ProvedorGamepadSimulado();
        sim.conectarSemMapeamento(0, "Generic USB Joystick", "03000000790000000600000000000000", 4, 12, 1);
        String t = texto(sim, ArquivoMapeamentos.Status.NENHUM);
        assertTrue(t.contains("2. Joysticks detectados pelo sistema: 1"), t);
        assertTrue(t.contains("[porta 0] Generic USB Joystick | GUID 03000000790000000600000000000000"
                + " | 4 eixo(s), 12 botão(ões), 1 direcional(is) | gamepad: NÃO"), t);
        assertTrue(t.contains("-> detectado, mas o GLFW não tem mapeamento de gamepad para o GUID "
                + "03000000790000000600000000000000"), t);
        assertTrue(t.contains("3. Gamepads utilizáveis no jogo: 0"), t);
        assertTrue(t.contains("SEM mapeamento de gamepad: forneça um mapeamento SDL"), t);
        assertTrue(sim.conectados().isEmpty(), "o jogo nao usa joystick sem mapeamento");
    }

    @Test
    void gamepadMapeadoEJoystickSemMapeamentoJuntos() {
        ProvedorGamepadSimulado sim = new ProvedorGamepadSimulado();
        sim.conectar("g1", "030000005e0400008e02000000000000", "Xbox Controller");
        sim.conectarSemMapeamento(1, "Volante", "0300000000000000000000000000ffff", 3, 8, 0);
        String t = texto(sim, ArquivoMapeamentos.Status.NENHUM);
        assertTrue(t.contains("gamepad: SIM (Xbox Controller)"), t);
        assertTrue(t.contains("3. Gamepads utilizáveis no jogo: 1"), t);
        assertTrue(t.contains("1 gamepad(s) prontos; 1 joystick(s) sem mapeamento"), t);
    }

    @Test
    void semBibliotecaOJogoSegueNoTeclado() {
        String t = texto(new ProvedorGamepadsAusente("biblioteca LWJGL não encontrada"), ArquivoMapeamentos.Status.NENHUM);
        assertTrue(t.contains("1. Biblioteca: INDISPONIVEL - biblioteca LWJGL não encontrada"), t);
        assertTrue(t.contains("funciona só com o teclado"), t);
    }

    // =====================================================
    // ARQUIVO DE MAPEAMENTOS (formato SDL, somente leitura)
    // =====================================================
    @Test
    void validacaoDoArquivoDeMapeamentos() {
        ArquivoMapeamentos.Validacao v = ArquivoMapeamentos.validar("""
                # comentario
                030000005e0400008e02000000000000,Xbox,a:b0,b:b1,leftx:a0,lefty:a1,platform:Windows,

                xyz,Curto,a:b0
                030000005e0400008e02000000000000,,a:b0
                030000005e0400008e02000000000000,SemNada,platform:Windows,
                030000005e0400008e02000000000000,Errado,a-b0
                so uma coisa
                """);
        assertEquals(1, v.validas().size());
        assertEquals(5, v.problemas().size(), v.problemas().toString());
        assertTrue(v.problemas().get(0).startsWith("linha 4: GUID"), v.problemas().get(0));
        assertTrue(v.textoParaGlfw().startsWith("030000005e0400008e02000000000000,Xbox"));
        assertFalse(ArquivoMapeamentos.validar("lixo\n").temValidas());
        assertFalse(ArquivoMapeamentos.validar(null).temValidas());
    }

    @Test
    void arquivoInvalidoNaoQuebraALeituraNemEAlterado() throws Exception {
        Path arquivo = tmp.resolve("gamecontrollerdb.txt");
        Files.writeString(arquivo, "isto nao e um mapeamento\n");
        String antes = Files.readString(arquivo);
        String anterior = System.getProperty(ServicoGamepads.PROPRIEDADE_MAPEAMENTOS);
        System.setProperty(ServicoGamepads.PROPRIEDADE_MAPEAMENTOS, arquivo.toString());
        try {
            assertEquals(arquivo, ServicoGamepads.localizarMapeamentos());
            ProvedorGamepads p = ServicoGamepads.criarFisico();
            try {
                ArquivoMapeamentos.Status s = ServicoGamepads.statusMapeamentos();
                assertEquals(0, s.validas());
                assertTrue(s.problemas().stream().anyMatch(x -> x.contains("nenhuma linha válida")), s.toString());
                assertTrue(p.mapeamentosAceitos().isEmpty(), "nada invalido e enviado ao GLFW");
                if (System.getProperty("os.name", "").toLowerCase().contains("windows")) {
                    assertTrue(p.isDisponivel(), "a leitura continua: " + p.descricao());
                }
            } finally {
                p.encerrar();
            }
            assertEquals(antes, Files.readString(arquivo), "o jogo so le o arquivo");

            System.setProperty(ServicoGamepads.PROPRIEDADE_MAPEAMENTOS, tmp.resolve("nao_existe.txt").toString());
            ProvedorGamepads p2 = ServicoGamepads.criarFisico();
            p2.encerrar();
            assertTrue(ServicoGamepads.statusMapeamentos().problemas().get(0).contains("não foi possível ler"));
        } finally {
            if (anterior == null) {
                System.clearProperty(ServicoGamepads.PROPRIEDADE_MAPEAMENTOS);
            } else {
                System.setProperty(ServicoGamepads.PROPRIEDADE_MAPEAMENTOS, anterior);
            }
        }
    }

    @Test
    void mapeamentoValidoEAceitoPeloGlfwReal() throws Exception {
        if (!System.getProperty("os.name", "").toLowerCase().contains("windows")) {
            return;                                     // nativos so de Windows no projeto
        }
        Path arquivo = tmp.resolve("gamecontrollerdb.txt");
        Files.writeString(arquivo,
                "030000005e0400008e02000000000000,Controle Teste,a:b0,b:b1,leftx:a0,lefty:a1,platform:Windows,\n");
        String anterior = System.getProperty(ServicoGamepads.PROPRIEDADE_MAPEAMENTOS);
        System.setProperty(ServicoGamepads.PROPRIEDADE_MAPEAMENTOS, arquivo.toString());
        try {
            ProvedorGamepads p = ServicoGamepads.criarFisico();
            try {
                assertEquals(java.util.Optional.of(true), p.mapeamentosAceitos());
                assertEquals(1, ServicoGamepads.statusMapeamentos().validas());
                assertTrue(ServicoGamepads.statusMapeamentos().descricao().contains("aceito pelo GLFW"));
                List<String> linhas = RelatorioDiagnostico.linhas(p, ServicoGamepads.statusMapeamentos());
                assertTrue(linhas.stream().anyMatch(l -> l.startsWith("4. Mapeamentos extras: ")));
            } finally {
                p.encerrar();
            }
        } finally {
            if (anterior == null) {
                System.clearProperty(ServicoGamepads.PROPRIEDADE_MAPEAMENTOS);
            } else {
                System.setProperty(ServicoGamepads.PROPRIEDADE_MAPEAMENTOS, anterior);
            }
        }
    }
}
