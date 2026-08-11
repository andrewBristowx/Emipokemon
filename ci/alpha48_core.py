from pathlib import Path
root=Path('.')

# alpha.48 integrates dynamic roulette data into the approved HD chrome without
# changing the authoritative casino rules/network protocol.
gp=root/'gradle.properties'
s=gp.read_text().replace('mod_version=0.4.0-alpha.47','mod_version=0.4.0-alpha.48')
gp.write_text(s)

core=root/'src/main/java/com/emipokemon/Emipokemon.java'
s=core.read_text().replace('0.4.0-alpha.47','0.4.0-alpha.48')
core.write_text(s)

for p in (root/'src/test/java').rglob('*.java'):
    s=p.read_text().replace('0.4.0-alpha.47','0.4.0-alpha.48').replace('alpha47VersionIsConsistent','alpha48VersionIsConsistent')
    p.write_text(s)
