# Persistência: configurações e perfis (Entrega 4B)

Infraestrutura da 4B, ligada aos menus na 4C e **jogável na 4D** (perfis escolhidos na seleção e no treino).

## Onde ficam os dados

```
~/.aventura-irmaos-miau/            (Windows: C:\Users\<voce>\.aventura-irmaos-miau\)
  configuracoes.json
  perfis/
    <id-do-perfil>.json
```

- Outra pasta: `-Dirmaosmiau.dados=<pasta>` ou `new DiretorioDados(Path)`.
- As pastas só são criadas na **primeira gravação**.
- Testes usam `target/dados-testes/` (apagada por `mvn clean`), nunca a pasta pessoal.

## Regras de segurança

| Situação | Comportamento |
|---|---|
| Arquivo não existe | Configurações: valores padrão. Perfil: `Optional.empty()` |
| JSON corrompido | `PersistenciaException` `ARQUIVO_CORROMPIDO`; o arquivo **não** é alterado |
| Outro `tipo`, `versaoEsquema` ausente, maior que a suportada ou < 1 | `VERSAO_INCOMPATIVEL` |
| Valores fora das regras, campo desconhecido ou de tipo errado | `DADOS_INVALIDOS` (campos desconhecidos são recusados para não serem apagados ao salvar de novo) |
| Criar perfil que já existe / atualizar perfil inexistente | `CONFLITO` |
| Falha de disco | `ERRO_DE_ARQUIVO`; o arquivo anterior fica intacto |
| `carregarOuPadrao()` com arquivo ruim | Renomeia para `configuracoes.json.corrompido-<data-hora>`, usa padrões e devolve **avisos** (nunca silencioso) |
| `listar()` perfis com arquivos ruins | Lista os bons; os ruins aparecem nos avisos e **não** são apagados |

**Gravação segura:** JSON montado em memória → arquivo temporário na mesma pasta →
`force` no disco → troca atômica sobre o arquivo final (com novas tentativas se o Windows
negar acesso por um instante). Falhou em qualquer passo: o arquivo antigo permanece e o temporário é apagado.

Só dados simples são gravados (`EscritorJson` recusa qualquer objeto que não seja
Map/List/String/Number/Boolean/null: nada de Swing, cores ou objetos do jogo).

## Formatos (versão de esquema 1)

```json
{
  "tipo": "CONFIGURACOES_IRMAOS_MIAU",
  "versaoEsquema": 1,
  "audio": { "musicaAtiva": true, "efeitosAtivos": true, "volumeMusica": 0.6, "volumeEfeitos": 0.8 },
  "visual": { "cenarioPreferido": "dojo" }
}
```

```json
{
  "tipo": "PERFIL_LUTADOR_IRMAOS_MIAU",
  "versaoEsquema": 1,
  "id": "branco_forte",
  "nome": "Branco Forte",
  "personagemBase": "miau_branco",
  "atributos": {
    "condicionamento": 35.0, "folego": 25.0, "forca": 90.0, "velocidade": 22.0,
    "agilidade": 25.0, "tecnica": 25.0, "recuperacao": 22.0, "peso": 80.0
  }
}
```

Os padrões de configuração são os mesmos que o jogo já usa hoje (`ServicoAudio`: música e
efeitos ligados, volumes 0,6 e 0,8; cenário `dojo`); um teste garante que continuem iguais.

## Limites de atributos (provisórios)

`AtributosLutador` não tinha limites. A regra provisória fica num lugar só, `personagens/LimitesAtributos`:

| Atributo | Mínimo | Máximo |
|---|---|---|
| condicionamento, folego, forca, velocidade, agilidade, tecnica, recuperacao | 1 | 100 |
| peso (kg) | 40 | 120 |

Base: as fórmulas do jogo usam escala 0–100 (ex.: `velocidade / 100.0`), fôlego e condicionamento
precisam ser > 0, e os oficiais (Branco 35/25/42/22/25/25/22/72, Preto 25/40/18/45/45/28/38/68) cabem.
**Confirmar com o criador** antes da tela de edição.

## Usando pelo código

```java
ServicoPerfis perfis = ServicoPerfis.padrao();   // pasta do usuário + catálogo oficial

// Criar a partir de um oficial (copia os atributos padrão)
PerfilLutador meu = perfis.criarAPartirDoPersonagem("meu_branco", "Meu Branco", "miau_branco");

// Ajustar atributos (validados; o oficial nunca muda)
perfis.atualizarAtributos("meu_branco", new AtributosLutador(35, 25, 50, 22, 25, 25, 22, 75));

// Recuperar, listar, excluir
PerfilLutador lido = perfis.carregar("meu_branco").orElseThrow();
ResultadoCarga<List<PerfilLutador>> todos = perfis.listar();   // todos.getAvisos() p/ mostrar ao jogador
perfis.excluir("meu_branco");

// Configurações
RepositorioConfiguracoes cfg = new RepositorioConfiguracoes(DiretorioDados.doUsuario());
ResultadoCarga<ConfiguracoesJogo> r = cfg.carregarOuPadrao();
cfg.salvar(r.getValor().comVolumeMusica(0.3).comCenarioPreferido("original"));
```

Ids de perfil: 3 a 40 caracteres `[a-z0-9_-]`, começando por letra ou número (seguros como nome de
arquivo); ids de personagens oficiais (`miau_branco`, `miau_preto`) são reservados.

## Integração futura (não feita nesta entrega, por não poder tocar arquivos da 4A)

1. **Áudio:** na inicialização (`Main`), `carregarOuPadrao()` e aplicar em `AudioJogo.servico()`
   (`setMusicaAtiva`, `setEfeitosAtivos`, `setVolumeMusica`, `setVolumeEfeitos`); salvar quando F9/F10
   ou uma futura tela de opções mudarem algo. Mostrar `getAvisos()` numa mensagem.
2. **Cenário:** `GamePanel.usarCenario(cfg.getCenarioPreferido())`, conferindo antes
   `RegistroCenarios.contem(...)` (um cenário salvo pode deixar de existir).
3. **Perfis em jogo:** precisa dos construtores alternativos de Branco/Preto que recebem
   `AtributosLutador` (já autorizados pelo criador para uma entrega futura). Com eles:
   `DefinicaoPersonagem base = perfis.baseDe(perfil)` e uma `FabricaLutador` que crie o renderer da
   base com `perfil.getAtributos()`. Seleção de personagem e treino passam a listar oficiais + perfis.
4. **Tela de edição:** usar `LimitesAtributos.todos()` (nome, mínimo, máximo) para montar os controles e
   `LimitesAtributos.problemas(...)` para mostrar erros antes de salvar.
5. **Migrações:** quando houver `versaoEsquema` 2, tratar `versao < suportada` em `ArmazenamentoJson.ler`.

## Entrega 4C: o que foi ligado

| Item | Estado |
|---|---|
| Configurações carregadas ao iniciar e aplicadas no áudio | ✅ `Main.prepararPreferencias()` |
| Tela CONFIGURAÇÕES (música, efeitos, volumes, cenário, restaurar padrões) | ✅ prévia imediata; grava em SALVAR |
| F4 / F9 / F10 gravando a preferência | ✅ via `PreferenciasJogo` |
| Cenário preferido usado nas arenas e no treino | ✅ (se não existir mais: dojo + aviso, preferência preservada) |
| Tela PERSONAGENS PERSONALIZADOS (listar, criar a partir de Branco/Preto, editar 8 atributos, salvar, excluir com confirmação) | ✅ |
| Usar um perfil como lutador | ✅ Entrega 4D (seleção COMEÇAR e TREINO; ver ARQUITETURA.md) |

Regras da tela de perfis:
- O ID é gerado do nome ao salvar ("Branco Forte" → `branco_forte`; repetido → `_2`, `_3`).
  IDs oficiais e IDs de arquivos existentes (até corrompidos) nunca são reaproveitados.
- Validação 100% da 4B (`LimitesAtributos`, `PerfilLutador`, `ServicoPerfis`); valores digitados fora do
  limite marcam o atributo em vermelho e nada é gravado.
- Trocar de perfil, criar outro ou VOLTAR com mudanças pendentes pergunta: salvar / descartar / cancelar.
- "PADRÃO DA BASE" copia os valores do oficial para o editor; o oficial nunca é alterado.
- Arquivos de perfil com problema são listados como aviso e preservados.
