# ATG Engine Verifier

Black-box verification tool for student ATG (Automation: The Game) Engine implementations. Tests your engine against 11 game rule invariants to ensure compliance with the ATG specification.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) installed and running

## Quick Start

```bash
docker run --rm -v $(pwd)/target:/jars \
  ghcr.io/brandeis-cosi-103a/atg-engine-verifier \
  /jars/my-engine.jar com.example.MyEngine
```

Where:
- `/jars/my-engine.jar` — path to your engine JAR inside the container (mount with `-v`)
- `com.example.MyEngine` — fully qualified class name of your `Engine` implementation

## Usage

### Basic Usage

Mount your JAR directory and specify your engine class:

```bash
# From your project directory
docker run --rm -v $(pwd)/target:/jars \
  ghcr.io/brandeis-cosi-103a/atg-engine-verifier \
  /jars/my-engine-1.0.jar com.student.MyGameEngine
```

### Verbose Mode

Get detailed output including game traces and decision logs:

```bash
docker run --rm -v $(pwd)/target:/jars \
  ghcr.io/brandeis-cosi-103a/atg-engine-verifier \
  /jars/my-engine.jar com.example.MyEngine --verbose
```

### Help

```bash
docker run --rm ghcr.io/brandeis-cosi-103a/atg-engine-verifier --help
```

## Exit Codes

| Code | Meaning |
|------|---------|
| 0 | **Compliant** — all invariant checks passed |
| 1 | **Violations detected** — engine does not comply with game rules |
| 2 | **Error** — invalid arguments, JAR not found, class not found, etc. |

## Output Interpretation

### Passing

```
Running verification...
Games completed: 10/10
All invariant checks passed.
```

### Violations

```
Running verification...
Games completed: 10/10

VIOLATIONS FOUND:

[Game 3] Score calculation
  Player "Alice" reported score 12 but ending deck victory cards sum to 10

[Game 7] Legal decisions
  BuyDecision(Framework, cost=8) offered but spendableMoney=6
```

## Invariant Checks

The verifier runs 11 checks against your engine:

### Tier 1 — Core Rules

1. **Score calculation** — Victory card values in ending deck sum to reported score
2. **Results sorted** — Final results are sorted descending by score
3. **Starting hands** — First turn hands contain only Bitcoin and Method cards
4. **Initial supply** — Supply counts match the ATG specification (60 Bitcoin, 40 Ethereum, etc.)
5. **Game termination** — Game ends when Frameworks are depleted
6. **Legal decisions** — All offered decisions are actually legal given the game state
7. **Phase ordering** — Phases follow ACTION → MONEY → BUY → CLEANUP within each turn
8. **EndTurnEvent** — EndTurnEvent is fired between turns

### Tier 2 — Consistency

9. **Card conservation** — Total cards at game end equals total at game start (minus trashed)
10. **Supply depletion** — Supply decreases appropriately after GainCardEvents
11. **Lifecycle events** — GameStartEvent and GameEndEvent fire exactly once each

## Troubleshooting

### "JAR file not found"

Make sure to mount your JAR directory correctly:

```bash
# Correct — mount the directory containing your JAR
docker run --rm -v /absolute/path/to/target:/jars ...

# Wrong — mounting the JAR file directly won't work
docker run --rm -v /path/to/my-engine.jar:/jars/engine.jar ...
```

### "Class not found"

Verify:
1. The class name is fully qualified (includes package): `com.example.MyEngine`
2. Your class implements `edu.brandeis.cosi.atg.engine.Engine`
3. Your JAR includes all dependencies (use maven-shade-plugin for a fat JAR)

### "Method not found" or "AbstractMethodError"

Your engine may not implement all required interface methods. Check that you're compiling against the correct atg-api version.

### "ClassCastException"

Your engine class must have a no-argument constructor and implement `Engine`:

```java
public class MyEngine implements Engine {
    public MyEngine() {
        // no-arg constructor required
    }
    // ... implement all Engine methods
}
```

## Building from Source

```bash
git clone https://github.com/brandeis-cosi-103a/atg-engine-verifier.git
cd atg-engine-verifier
mvn clean package
docker build -t atg-engine-verifier .
```

## License

MIT License — see [LICENSE](LICENSE) for details.
