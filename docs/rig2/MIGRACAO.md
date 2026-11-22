# Rig 2.0: arquitetura da biblioteca, compatibilidade e plano de migração

Documento da Superentrega 6.3. O catálogo de movimentos está em `CATALOGO-ANIMACOES.md`, e os marcos homologados, em `historico/VERSOES.md`.

## 1. Arquitetura da biblioteca (laboratório `docs/rig2/`)

| Camada | Arquivo | Responsabilidade | O que **não** faz |
|---|---|---|---|
| Modelo do corpo e desenho | `ConceitoRig2.java` | Esqueleto 3D leve, IK de dois ossos com comprimentos fixos, câmera (guinada), kimono, cabeça, aparência por personagem, alvos e fechamento das mãos, pontos de contato | Tempo, animação, regras |
| Animações de um lutador | `AnimacoesRig2.java` (6.2, homologadas) e `MovimentosRig2.java` (6.3) | Animações como funções puras do tempo, `amostrar(t) → ParametrosPose`; componentes reutilizáveis (respirar, transferir peso, flexionar joelhos, inclinar, posicionar braço, elevar, preparar impulso, recuperar, passo); `Reprodutor` (relógio) | Decidir resultados de combate |
| Animações de dois lutadores | `DuelosRig2.java` | `QuadroDuplo` com as duas poses, posições, orientações e pegadas no **mesmo** instante; resolução das mãos sobre os contatos do adversário; desenho com ordem de profundidade; `ReprodutorDuplo` | Decidir se a técnica entra ou é defendida |
| Registro central | `BibliotecaRig2.java` | Metadados de cada animação executável: id, nome, categoria, participantes, duração, loop, poses inicial e final, articulações, eventos, contatos, perspectivas, estado e id no jogo. Também os itens pendentes do catálogo | — |
| Interfaces | `LaboratorioRig2.java` (6.1/6.2) e `LaboratorioCombateRig2.java` (6.3) | Pedir animações, avançar o relógio com o tempo real e mostrar | Calcular movimento |
| Inventário | `inventario/` | Extrator do jogo (somente leitura), dados extraídos, gerador do catálogo e estado no Rig 2.0 | — |

**Princípio:** a animação **descreve o movimento e marca instantes** (eventos), e o motor de combate continua decidindo tudo. Nenhuma regra, colisão ou fôlego do jogo foi copiada para o laboratório.

## 2. Matriz de compatibilidade (jogo atual × Rig 2.0)

| Aspecto | Jogo atual (evidência no código) | Rig 2.0 hoje | Situação |
|---|---|---|---|
| Tempo | ticks fixos de 16 ms (`GamePanel.INTERVALO_GAME_LOOP_MS`); janelas e durações em ticks | segundos; `Reprodutor.avancar(dt)` | Compatível: 1 tick = 0,016 s. As durações da 6.3 já foram convertidas (entrada da pegada: 30 quadros = 0,48 s; janela da derrubada: 16 ticks = 0,256 s) |
| Pose | 26 valores 2D da vista lateral (MiauStudio) e deltas de 32 valores (`AnimacoesTecnicas`) | parâmetros 3D (ângulos dos braços, alvos dos pés e das mãos, tronco, quadril) | Precisa de conversão ou recriação; a 6.3 recria sem converter |
| Identificadores | `TecnicaSolo.id`, `Finalizacao.id`, `tecnicas.json` | o id no jogo de cada animação fica em `Metadados.idJogo` | Rastreável pelo catálogo |
| Eventos de combate | `MotorSolo.EfeitoVisual` (técnica, progresso, marco, pulso da defesa) | `BibliotecaRig2.Evento` (nome, instante) | Mapeamento a fazer: marco do motor ↔ evento da animação |
| Dois lutadores | `AnimacaoConjuntaQuadros` e pares ATACA/RECEBE em `AnimacoesTecnicas` | `QuadroDuplo` com linha do tempo única e mãos presas aos contatos | Estrutura pronta (experimental) |
| Contato e pegada | pose da pegada vinda do MiauStudio; estado `PEGADA_1..4` | contatos nomeados (gola, manga, nuca, faixa, membros) e alvos de IK | Pegadas 1 a 3 reproduzidas; a 4 está em implementação |
| Hitboxes e hurtboxes | sistema de colisões de produção | não existem no Rig 2.0 | Os pontos de contato podem alimentá-las no futuro; nada foi alterado |
| Fôlego e resistência | `IrmaoMiau`, `CustosFolego` | só respiração visual | Separado, de propósito |
| Ordem de desenho | `GamePanel.frenteNoSolo` (por posição) | profundidade da raiz e camadas por lado (perto/longe) | Em pé, resolvido; no chão, pendente |
| Espelhamento esquerda/direita | `virado` (espelho 2D) | guinada da câmera (inclusive negativa) | Compatível |
| Chão (deitar, cair, rolar) | poses 2D próprias | **não suportado**: a raiz só gira em torno do eixo vertical | Bloqueio principal (241 movimentos) |
| Pernas enroscadas no adversário | uma camada por lutador | uma camada por lado do lutador | Bloqueio secundário (69 movimentos) |
| Personagens com outras proporções | duas classes (`IrmaoMiauBranco`, `IrmaoMiauPreto`) | uma aparência por personagem; proporções ainda globais | Falta um objeto de proporções por personagem |

## 3. Limitações e dependências identificadas
- **Orientação da raiz (deitar, cair, rolar):** é a dependência de 241 dos 267 movimentos. Exige girar a raiz em outros eixos e uma silhueta do tronco que funcione deitada; a silhueta atual é montada por fatias horizontais.
- **Camadas por membro:** triângulo, laço, aranha, De La Riva, SLX, ashi, ou seja, pernas por dentro e por fora do adversário.
- **Proporções do Miau:** a cabeça tem raio 35 contra braços de 69, e isso limita contatos perto da cabeça. A pegada 4 (gola e nuca) não fecha sem o braço atravessar a cabeça do adversário. Também foi preciso uma postura mais reta na disputa de pegada para as cabeças não se sobreporem.
- **Giro do tronco** (ombros contra quadril): ainda não existe.
- **Mão:** tem só aberta e fechada, sem dedos.

## 4. Plano de migração futura (proposta, sem nada integrado)
1. **Capacidades corporais:** primeiro a orientação completa da raiz e a silhueta independente de orientação; depois as camadas por membro e as proporções por personagem.
2. **Movimentos em pé que faltam:** as 18 quedas e entradas (agora que há dois lutadores e contatos), a pegada 4, os empurrões e as reações do receptor.
3. **Chão, pela ordem do catálogo:** transições de postura, puxadas de guarda, guardas e controles, passagens, raspagens, escapes, ajustes de controle e finalizações.
4. **Adaptador no jogo:** um renderizador do Rig 2.0 **ao lado** do atual, ativado só num modo de comparação (como o F6 já faz com as poses experimentais). O motor continua decidindo; o adaptador traduz `EfeitoVisual`, com técnica, progresso e marco, para a animação de mesmo id e duração equivalente.
5. **Regressão por técnica:** o motor tem que tomar as mesmas decisões com o Rig 2.0 ligado ou desligado. Antes de trocar o padrão, comparar lado a lado os eventos (contato, janela, resolução, rendição).
6. **Troca do padrão:** só depois da homologação visual de cada categoria, mantendo o desenho atual disponível até o fim.

Ter o mesmo nome e a mesma duração **não** garante compatibilidade. Cada técnica precisa conferir:
- instantes de contato e janelas de interrupção;
- hitboxes;
- estados de resistência;
- sincronização entre os participantes;
- condições de vitória.
