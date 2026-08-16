import copy, json, pathlib, re, shutil, zipfile

root=pathlib.Path('emipokemon')
res=root/'src/main/resources'
jar=pathlib.Path('Cobblemon-1.7.3-fabric.jar')

CUSTOM_IDS=(
    'eevee_gatito','snorlax_emi','unown_6','unown_7','unown_67',
    'espurr_emi','meowstic_emi','sprigatito_emi','floragato_emi',
    'meowscarada_emi','zorua_emi','zoroark_emi'
)

def read_json(path):
    return json.load(open(path,encoding='utf-8'))

def write_json(path, doc):
    path.parent.mkdir(parents=True,exist_ok=True)
    with open(path,'w',encoding='utf-8') as f:
        json.dump(doc,f,ensure_ascii=False,indent=2)
        f.write('\n')

# Correct Cobblemon 1.7.3 translation keys.
lang_paths=[res/'assets/emipokemon/lang/en_us.json',res/'assets/emipokemon/lang/es_es.json',res/'assets/emipokemon/lang/es_mx.json']
langs={p:read_json(p) for p in lang_paths}
for sid in CUSTOM_IDS:
    spath=res/f'data/emipokemon/species/custom/{sid}.json'
    species=read_json(spath)
    name=species['name']
    normalized=re.sub(r'[^a-z0-9]','',name.lower())
    new_name_key=f'emipokemon.species.{normalized}.name'
    new_desc_key=f'emipokemon.species.{normalized}.desc'
    legacy_desc=(species.get('pokedex') or [None])[0]
    for p,lang in langs.items():
        lang[new_name_key]=name
        desc=None
        if isinstance(legacy_desc,str):
            desc=lang.get(legacy_desc)
        if not desc:
            desc=('A special Emi variant obtainable from Emi’s Special Gacha.' if p.name=='en_us.json' else 'Una variante especial de Emi obtenible en la Gasha Especial de Emi.')
        lang[new_desc_key]=desc
        lang.setdefault(f'emipokemon.species.{sid}.name',name)
        lang.setdefault(f'emipokemon.species.{sid}.desc',desc)
    if species.get('pokedex') and isinstance(species['pokedex'][0],str) and species['pokedex'][0].startswith('emipokemon.species.'):
        species['pokedex'][0]=new_desc_key
        write_json(spath,species)
for p,lang in langs.items():
    write_json(p,lang)

with zipfile.ZipFile(jar) as z:
    names=z.namelist()

    def resolver_for(species):
        matches=[]
        for n in names:
            low=n.lower()
            if '/bedrock/pokemon/resolvers/' not in low or not low.endswith('.json'):
                continue
            try:
                d=json.loads(z.read(n))
            except Exception:
                continue
            raw=d.get('species')
            vals=raw if isinstance(raw,list) else [raw]
            if any(v in (species,f'cobblemon:{species}') for v in vals):
                matches.append((n,d))
        if not matches:
            raise RuntimeError(f'Official resolver not found for {species}')
        matches.sort(key=lambda x:(len(x[0]),x[0]))
        print('Official resolver',species,'=>',matches[0][0])
        return matches[0][1]

    def pick_variation(doc, required=(), forbidden=()):
        candidates=[]
        for i,v in enumerate(doc.get('variations',[])):
            aspects=set(v.get('aspects',[]))
            if all(x in aspects for x in required) and all(x not in aspects for x in forbidden):
                candidates.append((len(aspects),i,v))
        if not candidates:
            return None
        candidates.sort(key=lambda x:(x[0],x[1]))
        return candidates[0][2]

    def first_field(doc,key):
        for v in doc.get('variations',[]):
            if v.get(key): return v[key]
        return None

    def model_geometry(model_ref):
        if not model_ref:
            raise RuntimeError('Missing model reference')
        rel=model_ref.split(':',1)[-1]
        suffix='/bedrock/pokemon/models/'+rel+'.json'
        candidates=[n for n in names if n.endswith(suffix)]
        if not candidates:
            base=pathlib.PurePosixPath(rel).name+'.json'
            candidates=[n for n in names if '/bedrock/pokemon/models/' in n and n.endswith('/'+base)]
        if not candidates:
            raise RuntimeError(f'Cannot locate model file for {model_ref}')
        candidates.sort(key=lambda n:(len(n),n))
        doc=json.loads(z.read(candidates[0]))
        geoms=doc.get('minecraft:geometry',[])
        if not geoms:
            raise RuntimeError(f'No geometry in {candidates[0]}')
        print('Official model',model_ref,'=>',candidates[0],geoms[0].get('description',{}).get('identifier'))
        return copy.deepcopy(geoms[0]), doc.get('format_version','1.12.0')

    def official_base(species):
        r=resolver_for(species)
        base=pick_variation(r,forbidden=('shiny','female')) or r['variations'][0]
        model=base.get('model') or first_field(r,'model')
        poser=base.get('poser') or first_field(r,'poser')
        geom,fmt=model_geometry(model)
        return r,base,model,poser,geom,fmt

    # Exact official skeleton + only accepted Emi accessory bones.
    for species in ('sprigatito','floragato'):
        custom_path=res/f'assets/cobblemon/bedrock/pokemon/models/{species}_emi/{species}_emi.geo.json'
        custom_doc=read_json(custom_path)
        custom_geom=custom_doc['minecraft:geometry'][0]
        extras=[copy.deepcopy(b) for b in custom_geom.get('bones',[]) if b.get('name','').startswith('emi_')]
        resolver,base,model_ref,poser,official,fmt=official_base(species)
        official_names={b.get('name') for b in official.get('bones',[])}
        extra_names={b.get('name') for b in extras}
        for b in extras:
            parent=b.get('parent')
            if parent and parent not in official_names and parent not in extra_names:
                raise RuntimeError(f'{species} Emi accessory parent missing: {b.get("name")} -> {parent}')
        official['bones']=copy.deepcopy(official.get('bones',[]))+extras
        official.setdefault('description',{})['identifier']=f'geometry.{species}_emi'
        official['description']['texture_width']=max(official['description'].get('texture_width',0),custom_geom.get('description',{}).get('texture_width',0))
        official['description']['texture_height']=max(official['description'].get('texture_height',0),custom_geom.get('description',{}).get('texture_height',0))
        write_json(custom_path,{'format_version':fmt,'minecraft:geometry':[official]})

        rp=res/f'assets/cobblemon/bedrock/pokemon/resolvers/{species}_emi/0_{species}_emi_base.json'
        ours=read_json(rp)
        ours['variations'][0]['poser']=poser
        ours['variations'][0]['model']=f'cobblemon:{species}_emi.geo'
        write_json(rp,ours)

        om={b.get('name'):b for b in official['bones'] if not b.get('name','').startswith('emi_')}
        source_geom,_=model_geometry(model_ref)
        sm={b.get('name'):b for b in source_geom.get('bones',[])}
        for bone in ('body','head') + (('arm_right','arm_left','forearm_right','forearm_left') if species=='floragato' else ()):
            if bone in sm:
                assert om[bone].get('pivot')==sm[bone].get('pivot'), (species,bone,'pivot')
                assert om[bone].get('rotation')==sm[bone].get('rotation'), (species,bone,'rotation')
        print('Rebased exact official skeleton for',species,'with',len(extras),'Emi bones')

    # Meowstic Emi uses official male/female model + poser with Emi textures.
    r=resolver_for('meowstic')
    base=pick_variation(r,forbidden=('female','shiny')) or r['variations'][0]
    female=pick_variation(r,required=('female',),forbidden=('shiny',))
    base_model=base.get('model') or first_field(r,'model')
    base_poser=base.get('poser') or first_field(r,'poser')
    female_model=(female or {}).get('model') or base_model
    female_poser=(female or {}).get('poser') or base_poser
    if not base_model or not base_poser:
        raise RuntimeError('Could not resolve official Meowstic model/poser')
    mrp=res/'assets/cobblemon/bedrock/pokemon/resolvers/meowstic_emi/0_meowstic_emi_base.json'
    meow={'species':'emipokemon:meowstic_emi','order':0,'variations':[
        {'aspects':[],'poser':base_poser,'model':base_model,'texture':'cobblemon:textures/pokemon/meowstic_emi/meowstic_emi_male.png','layers':[]},
        {'aspects':['female'],'poser':female_poser,'model':female_model,'texture':'cobblemon:textures/pokemon/meowstic_emi/meowstic_emi_female.png','layers':[]},
        {'aspects':['shiny'],'poser':base_poser,'model':base_model,'texture':'cobblemon:textures/pokemon/meowstic_emi/meowstic_emi_male_shiny.png','layers':[]},
        {'aspects':['female','shiny'],'poser':female_poser,'model':female_model,'texture':'cobblemon:textures/pokemon/meowstic_emi/meowstic_emi_female_shiny.png','layers':[]}
    ]}
    write_json(mrp,meow)
    shutil.rmtree(res/'assets/cobblemon/bedrock/pokemon/models/meowstic_emi',ignore_errors=True)
    msp=res/'data/emipokemon/species/custom/meowstic_emi.json'
    md=read_json(msp)
    md['maleRatio']=0.5
    write_json(msp,md)
    print('Meowstic Emi now uses official models:',base_model,female_model,'poser:',base_poser,female_poser)

    # Numeric Unown keep approved numeral geometry but use official root + poser.
    ur=resolver_for('unown')
    uposer=first_field(ur,'poser')
    umodel=first_field(ur,'model')
    if not uposer or not umodel:
        raise RuntimeError('Could not resolve official Unown poser/model')
    ugeom,_=model_geometry(umodel)
    roots=[b.get('name') for b in ugeom.get('bones',[]) if not b.get('parent')]
    if not roots:
        raise RuntimeError('Official Unown root not found')
    official_root=roots[0]
    print('Official Unown root=',official_root,'poser=',uposer)

    for sid in ('unown_6','unown_7','unown_67'):
        mp=res/f'assets/cobblemon/bedrock/pokemon/models/{sid}/{sid}.geo.json'
        doc=read_json(mp)
        geom=doc['minecraft:geometry'][0]
        bones=geom.get('bones',[])
        old_roots=[b for b in bones if not b.get('parent')]
        if len(old_roots)!=1:
            raise RuntimeError(f'{sid}: expected one root, got {len(old_roots)}')
        old=old_roots[0]['name']
        old_roots[0]['name']=official_root
        for b in bones:
            if b.get('parent')==old:
                b['parent']=official_root

        nameset={b.get('name') for b in bones}
        if 'body' not in nameset:
            body={'name':'body','parent':official_root,'pivot':[0,9.5,0]}
            bones.insert(1,body)
            for b in bones:
                if b.get('name') in ('fusion_limbs','left_body','right_body') and b.get('parent')==official_root:
                    b['parent']='body'
        desc=geom.setdefault('description',{})
        desc['visible_bounds_width']=max(float(desc.get('visible_bounds_width',0)),4.5)
        desc['visible_bounds_height']=max(float(desc.get('visible_bounds_height',0)),4.5)
        desc['visible_bounds_offset']=[0,1.0,0]
        write_json(mp,doc)

        rp=res/f'assets/cobblemon/bedrock/pokemon/resolvers/{sid}/0_{sid}_base.json'
        rd=read_json(rp)
        rd['variations'][0]['poser']=uposer
        rd['variations'][0]['model']=f'cobblemon:{sid}.geo'
        write_json(rp,rd)
        print('Repaired numeric Unown',sid,'root',old,'=>',official_root)

for sid in CUSTOM_IDS:
    sp=read_json(res/f'data/emipokemon/species/custom/{sid}.json')
    norm=re.sub(r'[^a-z0-9]','',sp['name'].lower())
    for p in lang_paths:
        lang=read_json(p)
        assert lang.get(f'emipokemon.species.{norm}.name')==sp['name'], (sid,p)

bad=[]
for p in (res/'assets/cobblemon/bedrock/pokemon/models').rglob('*.geo.json'):
    try: doc=read_json(p)
    except Exception: continue
    for g in doc.get('minecraft:geometry',[]):
        for b in g.get('bones',[]):
            for c in b.get('cubes',[]):
                if isinstance(c.get('uv'),dict): bad.append(f'{p}:{b.get("name")}')
if bad:
    raise RuntimeError('Per-face UV remains:\n'+'\n'.join(bad[:30]))
print('All alpha.15 visual resource validations passed')
