package com.irmaosmiau.dispositivos;

import com.irmaosmiau.controles.AcaoJogador;
import com.irmaosmiau.controles.SlotJogador;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Diagnostico de gamepad pelo terminal, sem abrir o jogo (Entrega 5D).
 *
 * <pre>
 * java -cp target/AventuraIrmaosMiau-1.0-SNAPSHOT.jar com.irmaosmiau.dispositivos.DiagnosticoGamepad [segundos]
 * </pre>
 *
 * Mostra se a leitura fisica iniciou, os gamepads conectados e, durante os
 * segundos pedidos, os botoes apertados e as acoes que eles produzem para o
 * Jogador 1 e o Jogador 2 (esquema experimental). Saida 0 = leitura
 * disponivel; 2 = indisponivel (o jogo seguiria so com o teclado).
 */
public final class DiagnosticoGamepad {

    private DiagnosticoGamepad() {
    }

    public static void main(String[] args) throws InterruptedException {
        int segundos = args.length > 0 ? Integer.parseInt(args[0]) : 0;
        ProvedorGamepads p = ServicoGamepads.iniciarFisico();
        int codigo = p.isDisponivel() ? 0 : 2;
        try {
            System.out.println("Leitura de gamepad: " + (p.isDisponivel() ? "OK" : "INDISPONIVEL") + " - " + p.descricao());
            List<InfoGamepad> lista = p.conectados();
            System.out.println("Gamepads conectados: " + lista.size());
            for (InfoGamepad g : lista) {
                System.out.println("  " + g.chave() + " | " + g.nome() + " | " + g.identidade());
            }
            MapaGamepad mapa = MapaGamepad.experimental();
            String ultimo = "";
            long fim = System.currentTimeMillis() + segundos * 1000L;
            while (System.currentTimeMillis() < fim) {
                StringBuilder sb = new StringBuilder();
                for (InfoGamepad g : p.conectados()) {
                    Optional<EstadoGamepad> e = p.estado(g.chave());
                    if (e.isEmpty()) {
                        continue;
                    }
                    sb.append(g.chave()).append(": ");
                    for (BotaoGamepad b : BotaoGamepad.values()) {
                        if (e.get().pressionado(b)) {
                            sb.append(b.getRotulo()).append(' ');
                        }
                    }
                    for (SlotJogador s : SlotJogador.values()) {
                        Set<AcaoJogador> a = mapa.acoesAtivas(e.get(), s,
                                EnumSet.noneOf(AcaoJogador.class), EnumSet.noneOf(AcaoJogador.class));
                        sb.append("| ").append(s.getSigla()).append(' ').append(a).append(' ');
                    }
                }
                String linha = sb.toString();
                if (!linha.equals(ultimo)) {
                    System.out.println(linha);
                    ultimo = linha;
                }
                Thread.sleep(50);
            }
        } finally {
            ServicoGamepads.encerrar();
        }
        System.exit(codigo);
    }
}
