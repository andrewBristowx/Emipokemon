from pathlib import Path
root=Path('.')

# Version bump without touching gameplay/networking semantics.
gp=root/'gradle.properties'
s=gp.read_text()
s=s.replace('mod_version=0.4.0-alpha.46','mod_version=0.4.0-alpha.47')
gp.write_text(s)

# Keep the runtime version marker aligned with gradle.properties. Historical casino
# regression tests deliberately check both values to catch mismatched release builds.
core=root/'src/main/java/com/emipokemon/Emipokemon.java'
s=core.read_text().replace('0.4.0-alpha.46','0.4.0-alpha.47')
core.write_text(s)

screen=root/'src/client/java/com/emipokemon/client/casino/CasinoScreen.java'
s=screen.read_text()
# Native-HD source dimensions. The logical layout stays the same; only texture detail increases.
s=s.replace('private static final int ROULETTE_HEADER_TEX_W = 540;','private static final int ROULETTE_HEADER_TEX_W = 1535;')
s=s.replace('private static final int ROULETTE_HEADER_TEX_H = 38;','private static final int ROULETTE_HEADER_TEX_H = 146;')
s=s.replace('private static final int ROULETTE_LEFT_TEX_W = 182;','private static final int ROULETTE_LEFT_TEX_W = 1052;')
s=s.replace('private static final int ROULETTE_LEFT_TEX_H = 143;','private static final int ROULETTE_LEFT_TEX_H = 828;')
s=s.replace('private static final int ROULETTE_SIDE_TEX_W = 152;','private static final int ROULETTE_SIDE_TEX_W = 440;')
s=s.replace('private static final int ROULETTE_SIDE_TEX_H = 286;','private static final int ROULETTE_SIDE_TEX_H = 828;')
s=s.replace('private static final int ROULETTE_HEADER_H = 76;','private static final int ROULETTE_HEADER_H = 103;\n    private static final int ROULETTE_WHEEL_TEX_SIZE = 410;\n    private static final int ROULETTE_MEDALLION_TEX_SIZE = 200;')
# Header height now follows the complete uncropped source band rather than forcing a stretched 76px slice.
s=s.replace('    private int contentTop;\n', '    private int contentTop;\n    private int rouletteHeaderH;\n')
s=s.replace('        if (isRoulette()) {\n            contentTop = panelY + 76;\n            initRoulette();\n', '        if (isRoulette()) {\n            rouletteHeaderH = Math.max(52, Math.min(ROULETTE_HEADER_H, Math.round(panelW * (ROULETTE_HEADER_TEX_H / (float)ROULETTE_HEADER_TEX_W))));\n            contentTop = panelY + rouletteHeaderH;\n            initRoulette();\n')
s=s.replace('        rouletteContentH = Math.max(360, panelH - 84);','        rouletteContentH = Math.max(340, panelH - rouletteHeaderH);')
s=s.replace('            drawAsset(context, ROULETTE_HEADER, panelX, panelY, panelW, 76, ROULETTE_HEADER_TEX_W, ROULETTE_HEADER_TEX_H);','            drawAsset(context, ROULETTE_HEADER, panelX, panelY, panelW, rouletteHeaderH, ROULETTE_HEADER_TEX_W, ROULETTE_HEADER_TEX_H);')
s=s.replace('drawAsset(context, ROULETTE_WHEEL_OUTER, wheelCx - outerRadius, wheelCy - outerRadius, trimSize, trimSize, 128, 128);','drawAsset(context, ROULETTE_WHEEL_OUTER, wheelCx - outerRadius, wheelCy - outerRadius, trimSize, trimSize, ROULETTE_WHEEL_TEX_SIZE, ROULETTE_WHEEL_TEX_SIZE);')
s=s.replace('drawAsset(context, ROULETTE_MEDALLION, wheelCx - medSize / 2, wheelCy - medSize / 2, medSize, medSize, 64, 64);','drawAsset(context, ROULETTE_MEDALLION, wheelCx - medSize / 2, wheelCy - medSize / 2, medSize, medSize, ROULETTE_MEDALLION_TEX_SIZE, ROULETTE_MEDALLION_TEX_SIZE);')
s=s.replace('0.4.0-alpha.46','0.4.0-alpha.47')
screen.write_text(s)

# Keep version-regression tests aligned with the new candidate.
for p in (root/'src/test/java').rglob('*.java'):
    t=p.read_text().replace('0.4.0-alpha.46','0.4.0-alpha.47').replace('alpha46VersionIsConsistent','alpha47VersionIsConsistent')
    p.write_text(t)
