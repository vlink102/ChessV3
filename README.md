# ChessV3

## Features:
  - [Forsyth-Edwards-Notation](https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation) full integration (import + export)
> [!NOTE]
> ~~Malformed FEN strings will crash the program.~~ (Fixed in [c489405](https://github.com/vlink102/ChessV3/commit/c4894051c88b84fd15332ec980ee48112bd977e0))
> 
> A fen string should follow the format:
> ```
> [Position] [Turn] (Castling Availability) (En-passant square) {Half-Move Clock} {Full-Move Clock}
> ```
  - Customizable sides (Player types: Computer, Random Moves, Player)
  - [Stockfish 16](https://disservin.github.io/stockfish-docs/pages/Home.html) integration including full [UCI](https://en.wikipedia.org/wiki/Universal_Chess_Interface) parser and evaluation module

<details>
  <summary>Evaluation module</summary>

  - The upper of the 2 evaluation bars is the win-chance, where 0 is at the far left and 1 is at the far right. An evaluation of 0.5 is equal.
  - The lower is the NNUE evaluation, where the centre is 0 (equal)
  
  <p align="center">
    <img width="300" height="500" src="https://github.com/vlink102/ChessV3/assets/93732189/6b5e28de-8ea1-426d-8f73-d1127de72f4d">
    <img width="350" height="300" src="https://github.com/vlink102/ChessV3/assets/93732189/65fbe7a6-eaa1-40c8-a5ae-346254bb38cd">
    <img width="300" height="500" src="https://github.com/vlink102/ChessV3/assets/93732189/72b36130-ad92-41a1-a52a-d13bbe018675">
  </p>

  |Evaluation Type|Description|Example|
  |---|---|---|
  |Evaluation|Normal game evaluation|+0.47|
  |Depth to mate (DTM)|The game can only be won by checkmate.|M3|
  |Depth to conversion (DTC)|The game can be won by checkmate, capturing material or promoting a pawn. For example, in KQKR, conversion occurs when White captures the Black rook.|DTC=5|
  |Depth to zeroing (DTZ)|The game can be won by checkmate, capturing material or moving a pawn. For example, in KRPKR, zeroing occurs when White moves his pawn closer to the eighth rank.|DTZ=12
</details>

  - Functional GUI interface
  - Dynamic menu bar with configurable options
  - Implements checkmate, stalemate, draw by 50-move-rule, draw by three-fold repetition, and draw by insufficient material

<details>
  <summary>Insufficient Material Rules</summary>

  |Player 1|Player 2|
  |---|---|
  |King                          |   King|
  |King                          |   King   +   Knight|
  |King                          |   King   +   Bishop|
  |King + Light-Squared bishop   |   King   +   Dark-Squared bishop|

</details>

  - Graphical evaluation window with [NNUE](https://en.wikipedia.org/wiki/Efficiently_updatable_neural_network#:~:text=An%20efficiently%20updatable%20neural%20network,of%20the%20alpha%E2%80%93beta%20tree.) + Classical evaluation, and win percentage calculations

<details>
  <summary>Equation and example graph</summary>
  
  The non-linear equation allows the engine to determine a good move based on the current positional evaluation.
  For example, a good move in a bad position will not be brilliant, despite it increasing their winning chances, as they are still losing.

  
  The function $f(a)$ is the winning chance given as a scale over $0≤x≤100$, where the parameter a is the current positional evaluation in [centipawns](https://chess.fandom.com/wiki/Centipawn#:~:text=The%20centipawn%20is%20the%20unit,in%20order%20to%20evaluate%20positions.).

$$f(a)=50+(50*(\frac{2}{1+e^{-0.004a}}-1))$$

  <p align="center">
    <img width="300" height="250" src="https://github.com/vlink102/ChessV3/assets/93732189/a0f51d0f-178f-4aa4-86f8-62bbf0ce8ba3">
    <img width="300" height="250" src="https://github.com/vlink102/ChessV3/assets/93732189/83979b9e-12ca-480e-9781-3abb651078a9">
  </p>
</details>


  - Will rewrite ambiguous moves in proper algebraic notation
  - Castle and [En-passant](https://en.wikipedia.org/wiki/En_passant) features
  - Asynchronous Evaluation Graphing
  - Implements 7-piece [Syzygy](https://syzygy-tables.info/) [table bases](https://en.wikipedia.org/wiki/Endgame_tablebase) (14 terabyte database)

<details>
  <summary>Testing and results</summary>

  |Position|Outcome|Info|
  |---|---|---|
  |8/5P2/8/8/p5r1/1p6/3R4/k1K5 w - - 0 1|1-0|White zeroes in 1, White mates in 34|
  | 4k3/8/8/8/8/8/8/4KBN1 w - - 0 1|1-0|White mates in 54|
  | 8/8/7B/K7/8/7p/4Bkp1/6Rb w - - 0 1|1/2-1/2|Insufficient Material|
  | 5q2/n2P1k2/2b5/8/8/3N4/4BK2/6Q1 w - - 0 1|1-0|Black zeroes in 6, White zeroes in 4, White mates in 19|
  | 8/1p6/1P1p4/1K1p2B1/P2P4/6pp/1P6/5k2 w - - 0 1|1/2-1/2|Stalemate|
  | 8/8/5k2/8/p7/8/1PK5/8 w - - 0 1|1-0|White mates in 48|
  | 4k2r/8/8/7P/7P/6KP/7P/7R w k - 0 1|1/2-1/2|Insufficient Material|
  | 7k/r6P/6K1/7R/8/8/P7/8 w - - 0 1|1/2-1/2|Stalemate|
  | r2qk3/8/8/8/8/8/8/3QK2R w Kq - 0 1|1-0|White mates in 32|
  | 8/pQp2p1k/7p/6pK/6P1/6P1/8/5q2 b - - 0 1|0-1|Black mates in 1|
  | 1q6/p1p2Q2/8/3p4/4p3/3qk3/8/B5K1 w - - 0 1|1-0|White mates in 1|
  | 6k1/P3Q3/6K1/8/8/8/5pq1/7q w - - 0 1|0-1|Black zeroes in 2, White mates in 2|
  | 1k6/8/pp6/6B1/3P4/2P5/r3r2P/1KR4R b - - 0 1|0-1|Black mates in 1|
  | 8/8/1rk5/KR6/8/8/1P6/8 w - - 0 1|1-0|White mates in 23|
  | 8/8/8/8/2N5/6p1/k1K3N1/8 w - - 0 1|1-0|White mates in 33|
  | 8/8/8/2kpp3/8/8/1K1NN3/8 w - - 0 1|1-0|White zeroes in 122, White mates in 156|
  | 6k1/8/1r2n3/2b5/K7/8/8/1N5Q w - - 0 1|1/2-1/2|White zeroes in 717|
  | 8/3r4/8/6n1/3K1k2/1b6/7N/7Q w - - 0 1|1/2-1/2|White zeroes in 1033|
  | 8/8/8/8/6k1/6P1/r4PK1/1R6 w - - 0 1|1-0|White zeroes in 9, Black zeroes in 16, White mates in 12|
  | 5qk1/6p1/6P1/8/PP6/KP6/8/QRRRRRRR b - - 0 1|1/2-1/2|Stalemate|
  | 1N6/1RK5/5n2/8/8/8/5n2/6k1 w - - 0 1|1/2-1/2|Black zeroes in 483, White zeroes in 479|
  | 8/4N3/8/8/3pN3/1p6/p2R4/k5K1 w - - 0 1|1-0|Black zeroes in 6, White mates in 30|
  
</details>

<p align="center">
  <img width="400" height="325" src="https://github.com/vlink102/ChessV3/assets/93732189/fef24357-d31a-4c17-969c-9bc3faf58fb2">
  <img width="300" height="325" src="https://github.com/vlink102/ChessV3/assets/93732189/4ccaad33-05d4-4476-aec1-369db8f931c7">
  <img width="300" height="325" src="https://github.com/vlink102/ChessV3/assets/93732189/d41f405d-4cb4-47ce-b1ea-11279355dd10">
</p>

## JVM Startup Arguments

> [!NOTE]
> Arguments with spaces should be encased in quotes:
> ```
> '-FEN=rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'
> ```

|Argument|Default values|Data Type|
|---|---|---|
|-Board.Size=``VALUE``|600|Integer|
|-FEN=``VALUE``|rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1|String|
|-PlayAs=``VALUE``|true|Boolean|
|-Game.Opponent=``VALUE``|COMPUTER|COMPUTER, PLAYER, RANDOM|
|-Game.Self=``VALUE``|PLAYER|COMPUTER, PLAYER, RANDOM|
|-Engine.Time=``VALUE``|3000|Integer|
|-Engine.PV=``VALUE``|1|Integer|
|-Engine.Cores=``VALUE``|``Based on your CPU``|Integer|
|--nogui|``Disables console GUI``||
|--noerr|``Disables error GUI``||
|--generate-bat|``Opens the executable generator``||

> [!TIP]
> The default cores will be set to your physical processor count.
>
> For optimal performance, consider using the following:
> ```
> -Engine.Cores={LOGICAL_PROCESSOR_COUNT}
> ```
> For more info, see [hyper threading](https://en.wikipedia.org/wiki/Hyper-threading)

## Batch file generator (Added in [644c4e9](https://github.com/vlink102/ChessV3/commit/644c4e99b3aef0ccca0712a9ab161be13a19a3de) [#20](https://github.com/vlink102/ChessV3/issues/20)) 
To start the batch file generator, *only* use the ``--generate-bat`` JVM argument:
```cmd
Microsoft Windows [Version xx.x.xxxxx.xxxx]
(c) Microsoft Corporation. All rights reserved.

C:\Users\vlink102>java -jar ChessV3-vx.x.x.jar --generate-bat
```
> [!WARNING]
> The batch file will run the same parameters every time.
>
> This also means that if the JDK path or .jar file are either deleted or moved, the batch script will no longer function.
>
> To generate another, simply re-run the generator.

> [!NOTE]
> The generator will not allow the user to set the Engine.Cores parameter higher than the logical cores of the computer.
>
> Several other restrictions are also in place to prevent accidental inputs.

## Windows Powershell Batch Script (Start generator)
```shell
& "{PATH_TO_JRE}\java.exe" -jar "{PATH_TO_JAR}\ChessV3-{VERSION}.jar" --generate-bat
```
