from pathlib import Path

root=Path('.')

def replace_once(path,old,new,label):
    p=root/path
    s=p.read_text()
    if old not in s:
        raise SystemExit(f'missing alpha43 server anchor: {label}')
    p.write_text(s.replace(old,new,1))

p='src/main/java/com/emipokemon/casino/CasinoTableService.java'
s=(root/p).read_text()
old='''            long multiplier = participant.action.startsWith("number:") ? 36L : participant.action.startsWith("dozen") ? 3L : 2L;'''
new='''            long multiplier = participant.action.startsWith("number:") ? 36L\n                    : (participant.action.startsWith("dozen") || participant.action.startsWith("column")) ? 3L : 2L;'''
if old not in s: raise SystemExit('missing alpha43 server anchor: roulette multiplier')
s=s.replace(old,new,1)
old='''        if (Set.of("red", "black", "even", "odd", "low", "high", "dozen1", "dozen2", "dozen3").contains(action)) return true;'''
new='''        if (Set.of("red", "black", "even", "odd", "low", "high", "dozen1", "dozen2", "dozen3",\n                "column1", "column2", "column3").contains(action)) return true;'''
if old not in s: raise SystemExit('missing alpha43 server anchor: valid roulette set')
s=s.replace(old,new,1)
old='''            case "dozen1" -> number >= 1 && number <= 12;\n            case "dozen2" -> number >= 13 && number <= 24;\n            case "dozen3" -> number >= 25;\n            default -> false;'''
new='''            case "dozen1" -> number >= 1 && number <= 12;\n            case "dozen2" -> number >= 13 && number <= 24;\n            case "dozen3" -> number >= 25;\n            case "column1" -> number > 0 && (number - 1) % 3 == 0;\n            case "column2" -> number > 0 && (number - 2) % 3 == 0;\n            case "column3" -> number > 0 && number % 3 == 0;\n            default -> false;'''
if old not in s: raise SystemExit('missing alpha43 server anchor: roulette win switch')
s=s.replace(old,new,1)
(root/p).write_text(s)
