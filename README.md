**SolverViewModel State Machine**

**Game State Machine**
![Game State Machine](GameViewModel-State-Machine.bmp)

**Solver State Machine**
![Solver State Machine](SolverViewModel-State-Machine.bmp)

Solver Game UI Cpntrol

| UI Control Object                    | NoMoveAvailable | Thinking  | ReadyToFindSolution | HasWinningMoveWaitingToMove |
|--------------------------------------|-----------------|-----------|---------------------|-----------------------------|
| "Find Solution" button               | Disabled        | Disabled  | Enabled             | Disabled                    |
| "Move and find next Solution" button | Disabled        | Disabled  | Disabled            | Enabled                     |
| "Reset" button                       | Enable          | Disabled  | Enabled             | Enabled                     |
| Toggle position game board           | Enable          | Disabled  | Enabled             | Enabled                     |
| Swipe action on game board           | Blocked         | Blocked   | Blocked             | Blocked                     |
| Exit from screen activity            | Enabled         | Disabled  | Enabled             | Enabled                     |