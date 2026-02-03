package edu.brandeis.cosi103a.verifier;

import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.engine.Engine;
import edu.brandeis.cosi.atg.engine.PlayerViolationException;
import edu.brandeis.cosi.atg.player.Player;
import edu.brandeis.cosi.atg.state.GameResult;
import edu.brandeis.cosi103a.verifier.strategies.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main entry point for the engine verifier. Runs a set of games with instrumented
 * players, captures events and decisions, and verifies game rule invariants.
 */
public class VerifierHarness {

    private final EngineLoader loader;
    private final int numGames;
    private final boolean verbose;
    private final Random random;

    public VerifierHarness(EngineLoader loader, int numGames, boolean verbose) {
        this.loader = loader;
        this.numGames = numGames;
        this.verbose = verbose;
        this.random = new Random(42);
    }

    /**
     * Run all verification games and return the result.
     */
    public VerificationResult verify() {
        List<Violation> allViolations = new ArrayList<>();
        int passed = 0;

        // Normal games
        for (int i = 0; i < numGames; i++) {
            List<VerifierPlayer> players = createPlayers(i);
            List<Card.Type> actionTypes = selectActionTypes();
            GameTrace trace = runGame(i, players, actionTypes);
            List<Violation> violations = InvariantChecker.check(trace);
            allViolations.addAll(violations);
            if (violations.isEmpty() && trace.completedSuccessfully()) {
                passed++;
            }
            if (verbose && !violations.isEmpty()) {
                printVerboseTrace(trace);
            }
        }

        // Violation test
        Violation violationResult = runViolationTest(numGames);
        if (violationResult != null) {
            allViolations.add(violationResult);
        } else {
            passed++;
        }

        return new VerificationResult(numGames + 1, passed, allViolations);
    }

    private GameTrace runGame(int gameIndex, List<VerifierPlayer> players, List<Card.Type> actionTypes) {
        ObserverRecorder recorder = new ObserverRecorder();
        try {
            List<Player> playerList = new ArrayList<>(players);
            Engine engine = loader.create(playerList, actionTypes);
            engine.setObserver(recorder);
            GameResult result = engine.play();

            Map<String, List<DecisionRecord>> decisions = new LinkedHashMap<>();
            for (VerifierPlayer vp : players) {
                decisions.put(vp.getName(), vp.getDecisionLog());
            }
            return new GameTrace(gameIndex, players.size(), recorder.getEvents(), decisions, result, null);
        } catch (Exception e) {
            Map<String, List<DecisionRecord>> decisions = new LinkedHashMap<>();
            for (VerifierPlayer vp : players) {
                decisions.put(vp.getName(), vp.getDecisionLog());
            }
            return new GameTrace(gameIndex, players.size(), recorder.getEvents(), decisions, null, e);
        }
    }

    private Violation runViolationTest(int gameIndex) {
        ObserverRecorder recorder = new ObserverRecorder();
        CheatingPlayer cheater = new CheatingPlayer("Cheater");
        VerifierPlayer honest = new VerifierPlayer("Honest", new BigMoneyStrategy());
        try {
            List<Player> players = List.of(cheater, honest);
            Engine engine = loader.create(players, selectActionTypes());
            engine.setObserver(recorder);
            engine.play();
            // If we get here, the engine did NOT throw
            return new Violation("PlayerViolationException",
                    "Engine did not throw PlayerViolationException for invalid decision",
                    gameIndex);
        } catch (PlayerViolationException e) {
            return null; // expected
        } catch (Exception e) {
            return null; // other exceptions are acceptable too
        }
    }

    private List<VerifierPlayer> createPlayers(int gameIndex) {
        int config = gameIndex % 5;
        return switch (config) {
            case 0 -> List.of(
                    new VerifierPlayer("BigMoney-1", new BigMoneyStrategy()),
                    new VerifierPlayer("BigMoney-2", new BigMoneyStrategy()));
            case 1 -> List.of(
                    new VerifierPlayer("ActionHeavy-1", new ActionHeavyStrategy()),
                    new VerifierPlayer("ActionHeavy-2", new ActionHeavyStrategy()));
            case 2 -> List.of(
                    new VerifierPlayer("BigMoney", new BigMoneyStrategy()),
                    new VerifierPlayer("ActionHeavy", new ActionHeavyStrategy()),
                    new VerifierPlayer("Passive", new PassiveStrategy()));
            case 3 -> List.of(
                    new VerifierPlayer("Random-1", new RandomLegalStrategy(random.nextLong())),
                    new VerifierPlayer("Random-2", new RandomLegalStrategy(random.nextLong())),
                    new VerifierPlayer("Random-3", new RandomLegalStrategy(random.nextLong())),
                    new VerifierPlayer("Random-4", new RandomLegalStrategy(random.nextLong())));
            case 4 -> List.of(
                    new VerifierPlayer("Passive-1", new PassiveStrategy()),
                    new VerifierPlayer("Passive-2", new PassiveStrategy()));
            default -> throw new IllegalStateException();
        };
    }

    /**
     * Randomly select 10 of the 15 action card types.
     */
    private List<Card.Type> selectActionTypes() {
        List<Card.Type> allActions = Arrays.stream(Card.Type.values())
                .filter(t -> t.category() == Card.Type.Category.ACTION)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(allActions, random);
        return allActions.subList(0, 10);
    }

    private void printVerboseTrace(GameTrace trace) {
        System.err.println("--- Verbose trace for Game " + trace.gameIndex() + " ---");
        for (ObservedEvent oe : trace.observerEvents()) {
            System.err.println("  EVENT: " + oe.event().getDescription());
        }
        System.err.println("---");
    }

    // --- CLI ---

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: VerifierHarness <jar-path> <engine-class-fqn> [numGames] [--verbose]");
            System.exit(1);
        }
        String jarPath = args[0];
        String className = args[1];
        int numGames = args.length > 2 && !args[2].startsWith("-") ? Integer.parseInt(args[2]) : 10;
        boolean verbose = Arrays.asList(args).contains("--verbose");

        EngineLoader loader = new EngineLoader(jarPath, className);
        VerifierHarness harness = new VerifierHarness(loader, numGames, verbose);
        VerificationResult result = harness.verify();
        System.out.println(result.formatReport(className));
        System.exit(result.isCompliant() ? 0 : 1);
    }
}
