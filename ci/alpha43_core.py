from pathlib import Path

root=Path('.')

def replace_once(path,old,new,label):
    p=root/path
    s=p.read_text()
    if old not in s:
        raise SystemExit(f'missing alpha43 anchor: {label}')
    p.write_text(s.replace(old,new,1))

replace_once('gradle.properties','mod_version=0.4.0-alpha.42','mod_version=0.4.0-alpha.43','gradle version')
replace_once('src/main/java/com/emipokemon/Emipokemon.java','0.4.0-alpha.42','0.4.0-alpha.43','core version')

(root/'CHANGELOG-0.4.0-alpha.43.md').write_text('''# Emipokemon 0.4.0-alpha.43\n\nInteractive roulette GUI foundation.\n\n- Starts from alpha.42 models and server-authoritative multiplayer casino sessions.\n- Rebuilds the casino screen around a game-first layout instead of a generic form.\n- Roulette receives a clickable 0-36 betting cloth, red/black, even/odd, 1-18/19-36, dozens and 2:1 columns.\n- Adds client-side animated roulette wheel that settles visually on the shared server result.\n- Displays the player's selected bet as a physical chip marker on the corresponding table region.\n- Adds 2:1 column bets to the server with the same authoritative reservation, payout and anti-duplicate path as existing roulette bets.\n- Keeps non-roulette games functional in the new shared casino shell while their dedicated game views are developed next.\n- Preserves alpha.42 models, holograms, economy, networking authority and multiplayer session safety.\n- Does not modify EmiProtecciones or Arlight.\n\nReal Cobbleverse visual and two-client validation is required before this candidate is considered finished.\n''')
