from pathlib import Path
root=Path('.')

def replace_once(path, old, new, label):
    p=root/path
    s=p.read_text()
    if old not in s:
        raise SystemExit(f'missing alpha45 server anchor: {label}')
    p.write_text(s.replace(old,new,1))

p='src/main/java/com/emipokemon/casino/CasinoNetworking.java'
s=(root/p).read_text()
old='''                List.of(player.getGameProfile().getName()), "Operación individual.", "");'''
new='''                List.of(player.getGameProfile().getName()), "Operación individual.", "", List.of());'''
if old not in s: raise SystemExit('missing alpha45 server anchor: single state')
s=s.replace(old,new,1)
old='''                              List<String> players, String tableState, String privateState) { }'''
new='''                              List<String> players, String tableState, String privateState,\n                              List<Integer> recentResults) { }'''
if old not in s: raise SystemExit('missing alpha45 server anchor: state record')
s=s.replace(old,new,1)
(root/p).write_text(s)

p='src/main/java/com/emipokemon/casino/CasinoTableService.java'
s=(root/p).read_text()
old='''        machine.activate();\n        result(session, "Ruleta: salió " + number + " " + color + ".");'''
new='''        machine.activate();\n        session.rouletteHistory.addFirst(number);\n        while (session.rouletteHistory.size() > 5) session.rouletteHistory.removeLast();\n        result(session, "Ruleta: salió " + number + " " + color + ".");'''
if old not in s: raise SystemExit('missing alpha45 server anchor: roulette history update')
s=s.replace(old,new,1)
old='''                effectiveMessage, phaseId(session.phase), session.roundId, session.deadline, players, publicState, privateState);'''
new='''                effectiveMessage, phaseId(session.phase), session.roundId, session.deadline, players, publicState, privateState,\n                List.copyOf(session.rouletteHistory));'''
if old not in s: raise SystemExit('missing alpha45 server anchor: state history')
s=s.replace(old,new,1)
old='''        final List<Integer> board = new ArrayList<>();\n        Phase phase = Phase.IDLE;'''
new='''        final List<Integer> board = new ArrayList<>();\n        final java.util.ArrayDeque<Integer> rouletteHistory = new java.util.ArrayDeque<>();\n        Phase phase = Phase.IDLE;'''
if old not in s: raise SystemExit('missing alpha45 server anchor: session history')
s=s.replace(old,new,1)
(root/p).write_text(s)
