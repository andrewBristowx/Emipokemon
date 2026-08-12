from pathlib import Path

root = Path(".")
screen = (root / "src/client/java/com/emipokemon/client/casino/CasinoScreen.java").read_text(encoding="utf-8")
server = (root / "src/main/java/com/emipokemon/casino/CasinoTableService.java").read_text(encoding="utf-8")

required_screen = [
    "gameX = panelX",
    "sideX = gameX + gameW",
    "sideW = panelX + panelW - sideX",
    "contentTop -= rouletteOverlapH",
    "amountField.visible = false",
    "drawFittedCenteredUiText",
    "Últimos resultados",
    "renderPlayerCount(context, players.size())",
]
for marker in required_screen:
    assert marker in screen, marker

assert "renderRouletteEdgeCleanup" not in screen
assert "MAX_ROULETTE_PLAYERS = 8" in server
assert server.index("session.participants.size() >= MAX_ROULETTE_PLAYERS") < server.index("if (!reserve(player, amount")
