# ChessV3
Chess Project (Version 3)

```
powershell -Command "& '{PATH_TO_JRE}\java.exe' -jar '{PATH_TO_JAR}\ChessV3-5x04-STABLE.jar' -Xmx2048M"
```

```
JVM Arguments:                               | Default values:
  -Board.Size={INTEGER}                      |   600
  -FEN={FEN_STRING}                          |   rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
  -PlayAs={BOOLEAN}                          |   true
  -Game.Opponent={COMPUTER|RANDOM|PLAYER}    |   COMPUTER
  -Game.Self={COMPUTER|RANDOM|PLAYER}        |   PLAYER
  -Engine.Time={INTEGER}                     |   3000
  -Engine.PV={INTEGER}                       |   1
  -Engine.Cores={INTEGER}                    |
```
Features:
  - Forsyth-Edwards-Notation full integration (import + export)
  - Customizable sides (Player types: Computer, Random Moves, Player)
  - Stockfish 16 integration including full UCI parser and evaluation module
  - Functional GUI
  - Implements checkmate, stalemate, draw by 50-move-rule, draw by three-fold repetition, and draw by insufficient material
  - Graphical evaluation window with NNUE + Classical evaluation, and win percentage calculations

$$
Advantage = 50 + 50 * (2 / (1 + (exp(-0.004 * a))) - 1))
$$
<p align="center">
  <img width="600" height="500" src="https://github.com/vlink102/ChessV3/assets/93732189/a0f51d0f-178f-4aa4-86f8-62bbf0ce8ba3">
</p>

  - Will detect ambiguous move notation
  - Castle and En-passant features
  - Implements 7-piece Syzygy table bases (14 terabyte database)

<p align="center">
  <img width="450" height="650" src="https://github.com/vlink102/ChessV3/assets/93732189/6b5e28de-8ea1-426d-8f73-d1127de72f4d">
  <img width="800" height="650" src="https://github.com/vlink102/ChessV3/assets/93732189/fef24357-d31a-4c17-969c-9bc3faf58fb2">
  <img width="600" height="650" src="https://github.com/vlink102/ChessV3/assets/93732189/4ccaad33-05d4-4476-aec1-369db8f931c7">
  <img width="600" height="650" src="https://github.com/vlink102/ChessV3/assets/93732189/d41f405d-4cb4-47ce-b1ea-11279355dd10">
</p>



