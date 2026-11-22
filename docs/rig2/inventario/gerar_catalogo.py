# -*- coding: utf-8 -*-
"""
Gera docs/rig2/CATALOGO-ANIMACOES.md e docs/rig2/inventario/catalogo-completo.tsv (Superentrega 6.3).

Entradas (todas no repositorio):
  - inventario-jogo.tsv: extraido do jogo compilado por InventarioJogo.java (somente leitura);
  - os arquivos JSON de src/main/resources/conteudo (lidos aqui);
  - MOVIMENTOS_INDIVIDUAIS: movimentos que nao estao em catalogos, com a evidencia no codigo;
  - estado-rig2.tsv: estado de cada item no Rig 2.0 (mantido a mao, com evidencia).

Uso (a partir da raiz do repositorio):  python docs/rig2/inventario/gerar_catalogo.py
"""
import collections, io, json, os, sys

RAIZ = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..'))
INV = os.path.join(RAIZ, 'docs', 'rig2', 'inventario')
CONTEUDO = os.path.join(RAIZ, 'src', 'main', 'resources', 'conteudo')

EM_PE = {'EM_PE_SEPARADOS', 'EM_PE_PEGADA'}
DOMINIO = {'CONTROLE_LATERAL', 'MONTADA', 'COSTAS', 'JOELHO_BARRIGA'}
GUARDAS = {'GUARDA', 'GUARDA_ABERTA', 'MEIA_GUARDA', 'GUARDA_BORBOLETA', 'DE_LA_RIVA', 'GUARDA_LACO', 'GUARDA_ARANHA', 'SINGLE_LEG_X'}
ENROSCADAS = {'DE_LA_RIVA', 'GUARDA_LACO', 'GUARDA_ARANHA', 'SINGLE_LEG_X', 'ASHI_GARAMI'}
TICK_MS = 16  # GamePanel.INTERVALO_GAME_LOOP_MS

# Movimentos fora dos catalogos de tecnicas (evidencia: classe/arquivo e metodo/estado).
MOVIMENTOS_INDIVIDUAIS = [
    # id, nome, categoria, origem, duracao, estado no jogo, animacao no jogo
    ('andar', 'Andar', 'Movimento básico', 'IrmaoMiau.isAndando / AnimadorMovimentos (procedural)', '-', 1, 'própria (procedural)'),
    ('correr', 'Correr', 'Movimento básico', 'IrmaoMiau.isCorrendo', '-', 1, 'própria (procedural)'),
    ('agachar', 'Agachar', 'Movimento básico', 'IrmaoMiau.isAgachado (getProfundidadeAgachamento)', '-', 1, 'própria (procedural)'),
    ('pular', 'Pular', 'Movimento básico', 'IrmaoMiau.isPulando / isCicloSubindo', '-', 1, 'própria (procedural)'),
    ('idle_combate', 'Idle de combate', 'Movimento básico', 'IrmaoMiau.estaEmIdleDeCombate', 'loop', 1, 'própria (procedural)'),
    ('respiracao', 'Respiração / exaustão', 'Movimento básico', 'IrmaoMiau.isRespirando / isExaustaoCritica', 'loop', 1, 'própria (procedural)'),
    ('taunt', 'Provocação (taunt)', 'Expressão', 'IrmaoMiau.estaEmTaunt / IrmaoMiauBranco.desenharCabecaTauntMiauStudio', '-', 1, 'própria'),
    ('pose_apresentacao', 'Pose de apresentação', 'Expressão', 'IrmaoMiau.isPoseDeApresentacao', '-', 1, 'própria'),
    ('guarda_alta', 'Guarda alta', 'Defesa em pé', 'DefesaCorporal.Tipo.GUARDA_ALTA + movimentos/guarda_alta.json', '-', 1, 'JSON (BibliotecaMovimentos)'),
    ('guarda_media', 'Guarda média', 'Defesa em pé', 'DefesaCorporal.Tipo.GUARDA_MEDIA + movimentos/guarda_media.json', '-', 1, 'JSON (BibliotecaMovimentos)'),
    ('guarda_baixa', 'Guarda baixa', 'Defesa em pé', 'DefesaCorporal.Tipo.GUARDA_BAIXA + movimentos/guarda_baixa.json', '-', 1, 'JSON (BibliotecaMovimentos)'),
    ('rolamento_frente', 'Rolamento para a frente', 'Acrobacia / rolamento', 'movimentos/rolamento_frente_5o.json; IrmaoMiau.isRolando', '-', 1, 'JSON'),
    ('rolamento_tras', 'Rolamento para trás', 'Acrobacia / rolamento', 'movimentos/rolamento_tras_5o.json', '-', 1, 'JSON'),
    ('rolamento_lateral', 'Rolamento lateral', 'Acrobacia / rolamento', 'movimentos/rolamento_lateral_5o.json', '-', 1, 'JSON'),
    ('sprawl', 'Sprawl', 'Defesa em pé', 'movimentos/sprawl_5o.json; IrmaoMiau.isFazendoSprawl', '32 ticks (5Q.6)', 1, 'JSON'),
    ('ESTRELA_ESQUERDA', 'Estrela (esquerda)', 'Acrobacia / rolamento', 'TipoAcrobacia + movimentos/estrela_5o.json', '26 ticks', 1, 'JSON'),
    ('ESTRELA_DIREITA', 'Estrela (direita)', 'Acrobacia / rolamento', 'TipoAcrobacia + movimentos/estrela_5o.json', '26 ticks', 1, 'JSON'),
    ('MORTAL_FRENTE', 'Mortal para a frente', 'Acrobacia / rolamento', 'TipoAcrobacia + movimentos/mortal_frente_5o.json', '28 ticks', 1, 'JSON'),
    ('MORTAL_TRAS', 'Mortal para trás', 'Acrobacia / rolamento', 'TipoAcrobacia + movimentos/mortal_tras_5o.json', '30 ticks', 1, 'JSON'),
    ('ajoelhado', 'Pose ajoelhado', 'Movimento no chão', 'individuais/{branco,preto}_ajoelhado.json', '-', 1, 'JSON (AnimacaoIndividual)'),
    ('ajoelhado_movimento', 'Andar de joelhos', 'Movimento no chão', 'individuais/{branco,preto}_ajoelhado_movimento.json', '-', 1, 'JSON (AnimacaoIndividual)'),
    ('movimento_chao', 'Movimento deitado no chão', 'Movimento no chão', 'individuais/{branco,preto}_movimento_chao.json', '-', 1, 'JSON (AnimacaoIndividual)'),
    ('fuga_quadril', 'Fuga de quadril', 'Movimento no chão', 'individuais/{branco,preto}_fuga_quadril.json', '-', 1, 'JSON (AnimacaoIndividual)'),
    ('joelhos_acelerado', 'Deslocamento acelerado de joelhos', 'Movimento no chão', 'AnimadorJoelhosAcelerado (procedural)', '-', 1, 'própria (procedural)'),
    ('deitado_acelerado', 'Deslocamento acelerado deitado', 'Movimento no chão', 'reaproveita individuais/*_fuga_quadril.json', '-', 1, 'reaproveitada'),
    ('bater', 'Bater (rendição)', 'Finalização (reação)', 'AnimadorSolo: BATENDO (40 ticks)', '40 ticks', 1, 'provisória (procedural)'),
]

GENERICOS = [
    ('generica_braco', 'Finalização de braço (pose genérica)', 'AnimadorSolo.aplicarEfeito, categoria BRACO'),
    ('generica_estrangulamento', 'Estrangulamento (pose genérica)', 'AnimadorSolo.aplicarEfeito, categoria ESTRANGULAMENTO'),
    ('generica_perna', 'Finalização de perna (pose genérica)', 'AnimadorSolo.aplicarEfeito, categoria PERNA'),
]


def ler_tsv(caminho):
    with io.open(caminho, encoding='utf-8-sig') as f:
        return [l.rstrip('\n').split('\t') for l in f if l.strip()]


def classificar(t):
    """Categoria da tecnica pela ESTRUTURA (origem, papel, destino, janela, efeito), nao pelo nome."""
    origens = set(t['origens'].split(',')) if t['origens'] else set()
    destino = t['destino']
    if t['janela'] == '0' or t['efeito'] != 'NENHUM':
        return 'Ajuste de controle / postura'
    if origens <= EM_PE:
        if destino in GUARDAS and t['por_cima'] == 'OUTRO':
            return 'Puxada de guarda'
        if destino not in EM_PE:
            return 'Queda / entrada'
        return 'Em pé (outra)'
    if t['papel'] in ('POR_BAIXO', 'CONTROLADOR') and origens & (GUARDAS | {'ASHI_GARAMI'}) and t['por_cima'] == 'INICIADOR'             and destino not in GUARDAS:
        return 'Raspagem / reversão'
    if 'EM_PE_CONTRA_SOLO' in origens and destino in DOMINIO and t['papel'] in ('POR_CIMA', 'QUALQUER'):
        return 'Passagem de guarda'
    if destino in EM_PE or destino == 'EM_PE_CONTRA_SOLO':
        return 'Levantada / saída de posição'
    if t['papel'] in ('POR_BAIXO', 'CONTROLADOR') and origens & (GUARDAS | {'ASHI_GARAMI'}) and t['por_cima'] == 'INICIADOR':
        return 'Raspagem / reversão'
    if t['papel'] in ('POR_CIMA', 'CONTROLADO') and origens & GUARDAS and destino in DOMINIO:
        return 'Passagem de guarda'
    if t['papel'] in ('CONTROLADO', 'POR_BAIXO') and origens & DOMINIO:
        return 'Escape / reposição'
    if origens & DOMINIO and destino in DOMINIO:
        return 'Transição de controle'
    if destino == 'ASHI_GARAMI' or origens == {'ASHI_GARAMI'}:
        return 'Controle de perna (ashi)'
    if destino in GUARDAS and origens & (GUARDAS | {'SOLO_DISPUTADO', 'EM_PE_CONTRA_SOLO'}):
        return 'Troca / entrada de guarda'
    return 'Chão (outra)'


def complexidade(cat, origens, destino, dois, chao):
    if not dois:
        return 'média' if chao else 'baixa'
    if (origens | {destino}) & ENROSCADAS:
        return 'muito alta'
    if chao or cat in ('Queda / entrada', 'Puxada de guarda'):
        return 'alta'
    return 'média'


def requisitos_rig(dois, chao, origens, destino, cat):
    r = []
    if dois:
        r.append('dois personagens')
    if chao or cat in ('Queda / entrada', 'Puxada de guarda', 'Levantada'):
        r.append('orientação da raiz (deitar/cair)')
    if (origens | {destino}) & ENROSCADAS:
        r.append('camadas por membro (pernas enroscadas)')
    return r


def main():
    linhas = ler_tsv(os.path.join(INV, 'inventario-jogo.tsv'))
    estado = {}
    caminho_estado = os.path.join(INV, 'estado-rig2.tsv')
    if os.path.exists(caminho_estado):
        for l in ler_tsv(caminho_estado):
            if l[0].startswith('#'):
                continue
            estado[l[0]] = (l[1], l[2] if len(l) > 2 else '', l[3] if len(l) > 3 else '')
    quadros = {l[1]: l[2] if len(l) > 2 else '' for l in linhas if l[0] == 'QUADROS'}

    itens = []  # dicionarios com todas as colunas

    def novo(**kw):
        base = dict(id='', nome='', categoria='', origem='', condicoes='', inicial='', final='', duracao='', articulacoes='',
                    dependencias='', eventos='', dois='não', chao='não', miaustudio='não', situacao='', animacao_jogo='',
                    complexidade='', requisitos='', rig2_estado='Não iniciada', rig2_animacao='', rig2_evidencia='', chave='')
        base.update(kw)
        itens.append(base)

    # 1) tecnicas (catalogo do MotorSolo)
    for l in linhas:
        if l[0] != 'TECNICA':
            continue
        _, id_, nome, rotulo, origens, papel, comandos, janela, expo, crit, efeito, sucesso, contras, reqs, postura, falha, futura, ataca, recebe, expl = l[:20]
        destino = sucesso.split('destino=')[1].split(',')[0] if 'destino=' in sucesso else '?'
        por_cima = sucesso.split('porCima=')[1].split(',')[0] if 'porCima=' in sucesso else '?'
        t = dict(origens=origens, destino=destino, papel=papel, janela=janela, efeito=efeito, por_cima=por_cima)
        cat = classificar(t)
        oset = set(origens.split(',')) if origens else set()
        chao = bool(oset - EM_PE) or destino not in EM_PE
        art = sorted(set(filter(None, (quadros.get(id_ + ':ATACA', '') + ', ' + quadros.get(id_ + ':RECEBE', '')).split(', '))))
        propria = ataca == 'true' or recebe == 'true'
        eventos = f'janela {janela} ticks; exposição {expo}; critério {crit}; efeito {efeito}' + (f'; contras: {contras}' if contras else '') \
            + ('; ' + expl if expl != '-' else '')
        novo(id=id_, nome=f'{nome} [{rotulo}]', categoria=cat, origem='CatalogoTecnicasSolo / MotorSolo', chave='solo/' + id_,
             condicoes=f'papel {papel}; comandos {comandos}' + (f'; requisitos {reqs}' if reqs else '') + (f'; postura {postura}' if postura != '-' else ''),
             inicial=origens.replace(',', ', '), final=f'{destino} (por cima: {por_cima})' + (f'; falha: {falha}' if falha != '-' else ''),
             duracao=('instantânea' if janela == '0' else f'janela de {janela} ticks (~{int(janela) * TICK_MS} ms)'),
             articulacoes=', '.join(art) if art else 'não determinado (pose provisória genérica)',
             dependencias=contras or '-', eventos=eventos, dois='sim', chao='sim' if chao else 'não',
             situacao='1 implementado e funcional', animacao_jogo='própria (quadros-chave)' if propria else 'provisória (pose genérica)',
             complexidade=complexidade(cat, oset, destino, True, chao), requisitos=', '.join(requisitos_rig(True, chao, oset, destino, cat)))

    # 2) finalizacoes
    for l in linhas:
        if l[0] != 'FINALIZACAO':
            continue
        _, id_, nome, rotulo, cat, grad, pos, papel, fam, dire, req, jan, ganho, alvo, base, futura, ataca, recebe, escape, falha = l[:20]
        art = sorted(set(filter(None, (quadros.get(id_ + ':ATACA', '') + ', ' + quadros.get(id_ + ':RECEBE', '')).split(', '))))
        propria = ataca == 'true' or recebe == 'true'
        novo(id=id_, nome=f'{nome} [{rotulo}]', categoria='Finalização: ' + cat.lower(), origem='CatalogoFinalizacoes / MotorSolo',
             chave=futura, condicoes=f'papel {papel}; {fam} {dire}; requisito {req}; graduação {grad}', inicial=pos,
             final=f'rendição (bater); escape → {escape}; falha → {falha}', duracao=f'entrada {jan} ticks; pressão +{ganho}/tick',
             articulacoes=', '.join(art) if art else 'pose genérica da categoria ' + cat, dependencias=base, eventos=f'alvo {alvo}; pressão, defesa, escape, rendição',
             dois='sim', chao='sim', situacao='1 implementado e funcional',
             animacao_jogo='própria (quadros-chave)' if propria else f'provisória (pose genérica {cat})',
             complexidade='muito alta' if pos in ENROSCADAS or cat == 'PERNA' else 'alta',
             requisitos=', '.join(requisitos_rig(True, True, {pos}, pos, 'fin')))

    # 3) transicoes do motor posicional antigo (CatalogoTransicoes)
    for l in linhas:
        if l[0] != 'TRANSICAO':
            continue
        _, id_, nome, origem, papel, cad, dur, destinos, tec = l[:9]
        chao = origem not in EM_PE or any(d not in EM_PE for d in destinos.split(','))
        novo(id='transicao/' + id_, nome=nome, categoria='Transição posicional (motor legado)', origem='CatalogoTransicoes / MotorPosicional',
             condicoes=f'papel {papel}; cadência {cad}', inicial=origem, final=destinos.replace(',', ', '),
             duracao=f'{dur} ticks (~{int(dur) * TICK_MS} ms)', dependencias=tec, dois='sim', chao='sim' if chao else 'não',
             situacao='2 implementado parcialmente (motor posicional anterior ao MotorSolo)', animacao_jogo='sem animação própria',
             complexidade='alta' if chao else 'média', requisitos=', '.join(requisitos_rig(True, chao, {origem}, origem, '')))

    # 4) quadros-chave que nao correspondem a uma tecnica (reacoes e estados)
    ids = {i['id'] for i in itens}
    reacoes = sorted({k.split(':')[0] for k in quadros if k.split(':')[0] not in ids})
    for r in reacoes:
        papeis = [k.split(':')[1] for k in quadros if k.split(':')[0] == r and ':' in k]
        art = sorted(set(filter(None, ', '.join(quadros[k] for k in quadros if k.split(':')[0] == r).split(', '))))
        novo(id=r, nome=r.replace('_', ' '), categoria='Reação / estado (quadros-chave)', origem='AnimacoesTecnicas',
             condicoes='disparada pelo MotorSolo (efeito visual)', duracao='relativa ao efeito', articulacoes=', '.join(art),
             eventos='papéis: ' + ', '.join(papeis), dois='sim', situacao='1 implementado e funcional', animacao_jogo='própria (quadros-chave)',
             complexidade='média', requisitos='dois personagens')

    # 5) transicoes de postura e acrobacias (enums)
    for l in linhas:
        if l[0] == 'POSTURA':
            kv = dict(p.split('=', 1) for p in l[2].split('; '))
            novo(id=l[1], nome=l[1].replace('_', ' ').lower(), categoria='Transição de postura', origem='TransicaoPostura / TransicoesPostura',
                 inicial=kv.get('getOrigem', '?'), final=kv.get('getDestino', '?'), duracao=f"{kv.get('getDuracaoTicks')} ticks",
                 eventos=f"custo de fôlego {kv.get('getCusto')}; rápida={kv.get('isRapida')}", chao='sim', situacao='1 implementado e funcional',
                 animacao_jogo='própria (interpolação de poses)', complexidade='média', requisitos='orientação da raiz (deitar/cair)')

    # 6) movimentos individuais
    for id_, nome, cat, origem, dur, sit, anim in MOVIMENTOS_INDIVIDUAIS:
        if id_.isupper() and any(i['id'] == id_ for i in itens):
            continue
        chao = cat in ('Movimento no chão', 'Acrobacia / rolamento')
        novo(id=id_, nome=nome, categoria=cat, origem=origem, duracao=dur, situacao=f'{sit} implementado e funcional',
             animacao_jogo=anim, chao='sim' if chao else 'não', complexidade='média' if chao else 'baixa',
             requisitos='orientação da raiz (deitar/cair)' if chao else '-')
    for id_, nome, origem in GENERICOS:
        novo(id=id_, nome=nome, categoria='Finalização (pose genérica)', origem=origem, dois='sim', chao='sim',
             situacao='2 implementado parcialmente (provisória)', animacao_jogo='provisória', complexidade='alta',
             requisitos='dois personagens, orientação da raiz (deitar/cair)')

    # 7) pegadas (dados) e arquivos do MiauStudio
    tec_json = json.load(io.open(os.path.join(CONTEUDO, 'tecnicas', 'tecnicas.json'), encoding='utf-8'))
    anim_json = json.load(io.open(os.path.join(CONTEUDO, 'animacoes', 'animacoes.json'), encoding='utf-8'))
    variantes = collections.defaultdict(list)
    for a in anim_json['animacoes']:
        variantes[a.get('varianteDe', a['id'])].append(a)
    for t in tec_json['tecnicas']:
        vs = variantes.get(t['animacao'], [])
        exp = sum(1 for v in vs if v.get('experimental'))
        novo(id=t['id'], nome=t['nome'] + ' — ' + t['descricao'], categoria='Pegada', origem='tecnicas.json + animacoes.json + MiauStudio',
             condicoes=f"alcance {t['alcanceHorizontal']} px; diferença máx. de linhas {t['diferencaMaximaLinhas']}; frente para o oponente; dificuldade {t['dificuldade']}",
             inicial='EM_PE_SEPARADOS', final=f"estado {t['estadoInteracaoAoSucesso']}", duracao=f"{t['duracaoAnimacaoFrames']} quadros",
             articulacoes='corpo inteiro dos dois (pose MiauStudio de 30 valores)', eventos=f"custo de fôlego {t['custoFolego']}; disponibilidade {t['disponibilidade']}",
             dois='sim', miaustudio=f'sim ({len(vs)} arquivos: aplicador Branco/Preto' + (f', {exp} experimentais' if exp else '') + ')',
             situacao='1 implementado e funcional', animacao_jogo='JSON MiauStudio + reação do receptor (receptor/' + t['id'].replace('pegada', 'receptor_pegada') + '.json)',
             complexidade='média', requisitos='dois personagens, mãos (pegada)')

    # estado no Rig 2.0
    for i in itens:
        if i['id'] in estado:
            i['rig2_estado'], i['rig2_animacao'], i['rig2_evidencia'] = estado[i['id']]

    # ---------------------------------------------------------------- saida TSV completa
    colunas = list(itens[0].keys())
    with io.open(os.path.join(INV, 'catalogo-completo.tsv'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('\t'.join(colunas) + '\n')
        for i in itens:
            f.write('\t'.join(str(i[c]).replace('\t', ' ') for c in colunas) + '\n')

    # ---------------------------------------------------------------- markdown
    por_cat = collections.OrderedDict()
    for i in itens:
        por_cat.setdefault(i['categoria'], []).append(i)
    total = len(itens)
    no_rig = [i for i in itens if i['rig2_estado'] not in ('Não iniciada', 'Em análise')]
    pend = total - len(no_rig)
    dois = sum(1 for i in itens if i['dois'] == 'sim')
    chao = sum(1 for i in itens if i['chao'] == 'sim')
    dep_raiz = sum(1 for i in itens if 'orientação da raiz' in i['requisitos'])
    dep_camadas = sum(1 for i in itens if 'camadas por membro' in i['requisitos'])
    propria = sum(1 for i in itens if i['animacao_jogo'].startswith('própria') or i['animacao_jogo'].startswith('JSON'))
    estados = collections.Counter(i['rig2_estado'] for i in itens)

    o = []
    o.append('# Catálogo mestre de animações (Rig 2.0)\n')
    o.append('Arquivo **gerado** por `docs/rig2/inventario/gerar_catalogo.py`: não editar à mão. O estado no Rig 2.0 vem de '
             '`docs/rig2/inventario/estado-rig2.tsv`, e a lista completa, com todas as colunas, está em `docs/rig2/inventario/catalogo-completo.tsv`.\n')
    o.append('**Fontes (evidência):**')
    o.append('- `InventarioJogo.java` lê o jogo compilado sem modificá-lo: catálogos de técnicas, finalizações e transições, as chaves de quadros-chave de '
             '`AnimacoesTecnicas` (e as articulações que cada uma move de fato), transições de postura, acrobacias e posições.')
    o.append('- Os JSON de `src/main/resources/conteudo`.')
    o.append('- Uma lista de movimentos individuais, com a classe ou o arquivo de origem de cada um.\n')
    o.append('Um tick do jogo = 16 ms (`GamePanel.INTERVALO_GAME_LOOP_MS`).\n')
    o.append('**Situação no jogo atual:**')
    o.append('- 1 = implementado e funcional;')
    o.append('- 2 = implementado parcialmente;')
    o.append('- 3 = presente só em dados ou documentação;')
    o.append('- 4 = planejado.\n')
    o.append('**Estado no Rig 2.0:** Não iniciada · Em análise · Em implementação · Implementada experimentalmente · Testada automaticamente · '
             'Aguardando homologação visual · Homologada · Integrada ao jogo principal.\n')
    o.append('## Resumo (calculado do inventário)\n')
    o.append('| Medida | Quantidade |')
    o.append('|---|---|')
    o.append(f'| Movimentos inventariados | **{total}** |')
    o.append(f'| Com animação própria no jogo atual (quadros-chave, JSON ou procedural dedicado) | {propria} |')
    o.append(f'| Já reproduzidos no Rig 2.0 (algum estado além de "Não iniciada") | {len(no_rig)} |')
    o.append(f'| Pendentes no Rig 2.0 | {pend} |')
    o.append(f'| Exigem dois personagens | {dois} |')
    o.append(f'| Exigem movimentação no chão | {chao} |')
    o.append(f'| Dependem de orientação completa da raiz (deitar, cair, rolar), ainda inexistente no Rig 2.0 | {dep_raiz} |')
    o.append(f'| Dependem de camadas por membro (pernas enroscadas no adversário), ainda inexistentes | {dep_camadas} |')
    o.append('')
    o.append('| Categoria | Quantidade | No Rig 2.0 |')
    o.append('|---|---|---|')
    for c, l in sorted(por_cat.items(), key=lambda kv: -len(kv[1])):
        o.append(f'| {c} | {len(l)} | {sum(1 for i in l if i in no_rig)} |')
    o.append('')
    o.append('| Estado no Rig 2.0 | Quantidade |')
    o.append('|---|---|')
    for e, n in estados.most_common():
        o.append(f'| {e} | {n} |')
    o.append('')
    o.append('## Movimentos já reproduzidos no Rig 2.0\n')
    o.append('| ID no jogo | Nome | Estado | Animação no Rig 2.0 | Evidência |')
    o.append('|---|---|---|---|---|')
    for i in no_rig:
        o.append(f"| `{i['id']}` | {i['nome']} | {i['rig2_estado']} | {i['rig2_animacao']} | {i['rig2_evidencia']} |")
    o.append('')
    o.append('## Catálogo por categoria\n')
    o.append('**Como ler as colunas:**')
    o.append('- **Duração:** janela de preparação do motor ou duração da transição.')
    o.append('- **Articulações:** as que os quadros-chave do jogo movem de fato.')
    o.append('- **2P:** exige dois personagens.')
    o.append('- **Rig 2.0:** estado atual.')
    o.append('- **Requisitos Rig 2.0:** recursos que o Rig precisa ter para reproduzir o movimento.\n')
    for c, l in sorted(por_cat.items(), key=lambda kv: -len(kv[1])):
        o.append(f'### {c} ({len(l)})\n')
        o.append('| ID | Nome | Início → fim | Duração | Animação no jogo | Articulações | 2P | Complexidade | Rig 2.0 | Requisitos Rig 2.0 |')
        o.append('|---|---|---|---|---|---|---|---|---|---|')
        for i in l:
            ini_fim = (i['inicial'] or '-') + ' → ' + (i['final'] or '-')
            o.append(f"| `{i['id']}` | {i['nome']} | {ini_fim} | {i['duracao'] or '-'} | {i['animacao_jogo']} | {i['articulacoes'] or '-'} | "
                     f"{i['dois']} | {i['complexidade']} | {i['rig2_estado']} | {i['requisitos'] or '-'} |")
        o.append('')
    with io.open(os.path.join(RAIZ, 'docs', 'rig2', 'CATALOGO-ANIMACOES.md'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(o))
    print(f'catalogo: {total} itens, {len(por_cat)} categorias, {len(no_rig)} no Rig 2.0, {pend} pendentes')
    for c, l in sorted(por_cat.items(), key=lambda kv: -len(kv[1])):
        print(f'  {len(l):4d}  {c}')


if __name__ == '__main__':
    main()
