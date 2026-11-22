# Rig 2.0: versões homologadas

Este é o registro dos marcos aprovados manualmente pela equipe. As versões históricas não são alteradas; o laboratório ativo (`docs/rig2/`) segue evoluindo.

| Marco | O que foi aprovado | Commits (local) | Onde ver |
|---|---|---|---|
| **6.0.4** | Conceito visual: cinco perspectivas, kimono, cabeça, focinho de perfil, pescoço curto; braços e antebraços 145%, pernas 120% | `3d8db58` (gerador e prévias), `f52094a` (documentação) | `docs/rig2/previas-6.0.4/` |
| **6.1** | Laboratório interativo: rotação contínua de 0° a 180°, giro da cabeça, braços, presets de pernas, esqueleto, profundidade | `5932f67`, `5dbf241`, `3e5b42b`, `8f37aca` | `docs/rig2/laboratorio-6.1/` |
| **6.2** | Animações: idle neutro (respiração), idle de combate, caminhada, transições neutra ↔ combate | `036f247`, `84816ac`, `775a029`, `e32d2b7`, `679ff7b` | `docs/rig2/laboratorio-6.2/` e `historico/rig2-6.2-aprovado.zip` |

| 6.3 | *Em avaliação (não homologada):* inventário, catálogo, corrida, agachamento, pulo, dois lutadores, pegadas 1 a 3, derrubada forçada defendida e laboratório de combate | ver `git log` (`2c47fe0` em diante) | `docs/rig2/laboratorio-6.3/` |

## Cópia executável da 6.2
O arquivo `rig2-6.2-aprovado.zip` contém os três arquivos da 6.2 exatamente como estão no commit `679ff7b` (`ConceitoRig2.java`, `AnimacoesRig2.java` e `LaboratorioRig2.java`) e um `LEIA-ME.txt`.
- **SHA-256:** `F06AAA502CE97E495AE469D3E00B4B749BB1CC4F683103E8446EA56BE000694A`
- **Conferência feita na 6.3:** o zip foi extraído numa pasta temporária fora do repositório. Lá, `java AnimacoesRig2.java` passou todas as verificações da 6.2 e `java LaboratorioRig2.java --capturas-6.2 <pasta>` gerou as capturas e exercitou os controles sem exceções. Isso confirma que a cópia não depende dos arquivos da 6.3.
- Para abrir, extraia o zip e, dentro de `rig2-6.2-aprovado/`, rode `java LaboratorioRig2.java`. É preciso Java 22 ou mais novo.

## Como recuperar a 6.0.4 e a 6.1 pelo git
As versões anteriores estão nos commits. Para extrair uma cópia sem mexer na pasta de trabalho (exemplo da 6.1):

```
git archive --format=zip -o rig2-6.1.zip 8f37aca:docs/rig2 ConceitoRig2.java LaboratorioRig2.java
```

Para a 6.0.4, o gerador está em `3d8db58:docs/rig2/ConceitoRig2.java`, e as prévias, em `docs/rig2/previas-6.0.4/`, nunca foram regeradas.
