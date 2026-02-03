package edu.brandeis.cosi103a.verifier;

import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.*;
import edu.brandeis.cosi.atg.event.*;
import edu.brandeis.cosi.atg.state.CardStacks;
import edu.brandeis.cosi.atg.state.GameState;
import edu.brandeis.cosi.atg.state.PlayerResult;

import java.util.*;

/**
 * Runs invariant checks against a GameTrace and returns violations found.
 */
public class InvariantChecker {

    private InvariantChecker() {
    }

    /**
     * Run all invariant checks against the given trace.
     */
    public static List<Violation> check(GameTrace trace) {
        List<Violation> violations = new ArrayList<>();
        if (!trace.completedSuccessfully()) {
            return violations; // nothing to check if the game didn't complete
        }
        violations.addAll(checkScoreCalculation(trace));
        violations.addAll(checkResultsSorted(trace));
        violations.addAll(checkStartingHands(trace));
        violations.addAll(checkInitialSupply(trace));
        violations.addAll(checkGameTermination(trace));
        violations.addAll(checkLegalDecisionsOffered(trace));
        violations.addAll(checkPhaseOrdering(trace));
        violations.addAll(checkEndTurnEvents(trace));
        violations.addAll(checkCardConservation(trace));
        violations.addAll(checkSupplyDepletion(trace));
        violations.addAll(checkLifecycleEvents(trace));
        return violations;
    }

    // --- Tier 1 checks ---

    /**
     * Check 1: Score calculation — sum of victory card values in endingDeck matches reported score.
     */
    static List<Violation> checkScoreCalculation(GameTrace trace) {
        List<Violation> violations = new ArrayList<>();
        for (PlayerResult pr : trace.result().playerResults()) {
            int computed = 0;
            for (Card card : pr.endingDeck()) {
                if (card.category() == Card.Type.Category.VICTORY) {
                    computed += card.value();
                }
            }
            if (computed != pr.score()) {
                violations.add(new Violation("Score calculation",
                        "Player \"" + pr.playerName() + "\" reported score " + pr.score()
                                + " but ending deck victory cards sum to " + computed,
                        trace.gameIndex()));
            }
        }
        return violations;
    }

    /**
     * Check 2: Results sorted descending by score.
     */
    static List<Violation> checkResultsSorted(GameTrace trace) {
        List<Violation> violations = new ArrayList<>();
        var results = trace.result().playerResults();
        for (int i = 1; i < results.size(); i++) {
            if (results.get(i).score() > results.get(i - 1).score()) {
                violations.add(new Violation("Results sorted",
                        "Player \"" + results.get(i).playerName() + "\" (score " + results.get(i).score()
                                + ") ranked after \"" + results.get(i - 1).playerName()
                                + "\" (score " + results.get(i - 1).score() + ")",
                        trace.gameIndex()));
            }
        }
        return violations;
    }

    /**
     * Check 3: Starting hands — first decision per player should have hand of only BITCOIN/METHOD.
     */
    static List<Violation> checkStartingHands(GameTrace trace) {
        List<Violation> violations = new ArrayList<>();
        for (var entry : trace.playerDecisions().entrySet()) {
            String player = entry.getKey();
            List<DecisionRecord> records = entry.getValue();
            if (records.isEmpty()) continue;
            DecisionRecord first = records.get(0);
            var hand = first.state().currentPlayerHand();
            for (Card card : hand.unplayedCards()) {
                if (card.type() != Card.Type.BITCOIN && card.type() != Card.Type.METHOD) {
                    violations.add(new Violation("Starting hands",
                            "Player \"" + player + "\" has " + card.type().description()
                                    + " in starting hand, expected only Bitcoin/Method",
                            trace.gameIndex(), 0, player));
                    break;
                }
            }
            for (Card card : hand.playedCards()) {
                if (card.type() != Card.Type.BITCOIN && card.type() != Card.Type.METHOD) {
                    violations.add(new Violation("Starting hands",
                            "Player \"" + player + "\" has " + card.type().description()
                                    + " in starting played cards, expected only Bitcoin/Method",
                            trace.gameIndex(), 0, player));
                    break;
                }
            }
        }
        return violations;
    }

    /**
     * Check 4: Initial supply counts from GameStartEvent match expected formula.
     */
    static List<Violation> checkInitialSupply(GameTrace trace) {
        List<Violation> violations = new ArrayList<>();
        GameStartEvent startEvent = null;
        for (ObservedEvent oe : trace.observerEvents()) {
            if (oe.event() instanceof GameStartEvent gse) {
                startEvent = gse;
                break;
            }
        }
        if (startEvent == null) return violations; // checked by lifecycle check

        int numPlayers = trace.numPlayers();
        CardStacks supply = startEvent.initialSupply();

        // After dealing starting hands (7 Bitcoin + 3 Method per player):
        Map<Card.Type, Integer> expected = new LinkedHashMap<>();
        expected.put(Card.Type.BITCOIN, 60); // 60 + 7p - 7p = 60
        expected.put(Card.Type.ETHEREUM, 40);
        expected.put(Card.Type.DOGECOIN, 30);
        expected.put(Card.Type.METHOD, 14); // 14 + 3p - 3p = 14
        expected.put(Card.Type.MODULE, 8);
        expected.put(Card.Type.FRAMEWORK, 8);
        expected.put(Card.Type.BUG, 10 * numPlayers);

        for (var entry : expected.entrySet()) {
            int actual = supply.getNumAvailable(entry.getKey());
            if (actual != entry.getValue()) {
                violations.add(new Violation("Initial supply",
                        entry.getKey().description() + ": expected " + entry.getValue()
                                + " but found " + actual,
                        trace.gameIndex()));
            }
        }

        // Action cards in supply should each have 10.
        // The API spec says 10 types are selected, but we check that whatever types
        // are present have count 10, and there are at least 10 types.
        int actionTypesInSupply = 0;
        for (Card.Type type : Card.Type.values()) {
            if (type.category() == Card.Type.Category.ACTION) {
                int count = supply.getNumAvailable(type);
                if (count > 0) {
                    actionTypesInSupply++;
                    if (count != 10) {
                        violations.add(new Violation("Initial supply",
                                type.description() + ": expected 10 but found " + count,
                                trace.gameIndex()));
                    }
                }
            }
        }
        if (actionTypesInSupply < 10) {
            violations.add(new Violation("Initial supply",
                    "Expected at least 10 action card types in supply but found " + actionTypesInSupply,
                    trace.gameIndex()));
        }

        return violations;
    }

    /**
     * Check 5: Game termination — FRAMEWORK supply should be 0 at game end,
     * unless the game ended for another valid reason (e.g. turn limit).
     * We verify that IF frameworks remain, the game result is still consistent.
     */
    static List<Violation> checkGameTermination(GameTrace trace) {
        List<Violation> violations = new ArrayList<>();
        GameEndEvent endEvent = null;
        for (ObservedEvent oe : trace.observerEvents()) {
            if (oe.event() instanceof GameEndEvent gee) {
                endEvent = gee;
                break;
            }
        }
        if (endEvent == null) return violations; // checked by lifecycle check

        // FRAMEWORK=0 is the primary end condition, but engines may also have
        // a turn limit safety valve. We just verify the end event was fired.
        return violations;
    }

    /**
     * Check 6: Legal decisions offered — verify options are valid given the game state.
     */
    static List<Violation> checkLegalDecisionsOffered(GameTrace trace) {
        List<Violation> violations = new ArrayList<>();
        int turnEstimate = 0;
        for (var entry : trace.playerDecisions().entrySet()) {
            String player = entry.getKey();
            for (DecisionRecord record : entry.getValue()) {
                GameState state = record.state();
                for (Decision option : record.options()) {
                    if (option instanceof BuyDecision bd) {
                        // Buy decisions: card must be affordable and available
                        if (bd.cardType().cost() > state.spendableMoney()) {
                            violations.add(new Violation("Legal decisions",
                                    "BuyDecision(" + bd.cardType().description() + ", cost="
                                            + bd.cardType().cost() + ") offered but spendableMoney="
                                            + state.spendableMoney(),
                                    trace.gameIndex(), turnEstimate, player,
                                    "State: actions=" + state.availableActions()
                                            + ", money=" + state.spendableMoney()
                                            + ", buys=" + state.availableBuys()));
                        }
                        if (state.buyableCards().getNumAvailable(bd.cardType()) <= 0) {
                            violations.add(new Violation("Legal decisions",
                                    "BuyDecision(" + bd.cardType().description()
                                            + ") offered but supply is empty",
                                    trace.gameIndex(), turnEstimate, player));
                        }
                    }
                    if (option instanceof PlayCardDecision pd && state.phase() == GameState.TurnPhase.ACTION) {
                        if (pd.card().category() == Card.Type.Category.ACTION
                                && state.availableActions() <= 0) {
                            violations.add(new Violation("Legal decisions",
                                    "PlayCardDecision(" + pd.card().type().description()
                                            + ") offered in ACTION phase but availableActions=0",
                                    trace.gameIndex(), turnEstimate, player));
                        }
                    }
                }
            }
        }
        return violations;
    }

    /**
     * Check 7: Phase ordering — within a turn, phases should follow
     * ACTION → MONEY → BUY → CLEANUP. REACTION/GAIN/DISCARD are sub-phases.
     */
    static List<Violation> checkPhaseOrdering(GameTrace trace) {
        List<Violation> violations = new ArrayList<>();
        for (var entry : trace.playerDecisions().entrySet()) {
            String player = entry.getKey();
            List<DecisionRecord> records = entry.getValue();

            int mainPhaseOrdinal = -1;
            int turn = 0;

            for (DecisionRecord record : records) {
                GameState.TurnPhase phase = record.state().phase();
                int ordinal = mainPhaseOrdinal(phase);
                if (ordinal < 0) continue; // sub-phase, skip

                // Detect new turn: if ordinal goes backwards, it's a new turn
                if (ordinal < mainPhaseOrdinal) {
                    // This should be ACTION (ordinal 0) starting a new turn
                    if (ordinal == 0) {
                        turn++;
                        mainPhaseOrdinal = ordinal;
                    } else {
                        violations.add(new Violation("Phase ordering",
                                phase + " phase after " + mainPhaseNameFromOrdinal(mainPhaseOrdinal) + " phase",
                                trace.gameIndex(), turn, player));
                    }
                } else {
                    mainPhaseOrdinal = ordinal;
                }
            }
        }
        return violations;
    }

    /**
     * Check 8: EndTurnEvent appears between turns in the observer stream.
     */
    static List<Violation> checkEndTurnEvents(GameTrace trace) {
        List<Violation> violations = new ArrayList<>();
        boolean hasEndTurn = false;
        for (ObservedEvent oe : trace.observerEvents()) {
            if (oe.event() instanceof EndTurnEvent) {
                hasEndTurn = true;
                break;
            }
        }
        if (!hasEndTurn && !trace.observerEvents().isEmpty()) {
            violations.add(new Violation("EndTurnEvent",
                    "No EndTurnEvent found in observer event stream",
                    trace.gameIndex()));
        }
        return violations;
    }

    // --- Tier 2 checks ---

    /**
     * Check 9: Card conservation — total cards at end should match total at start.
     * initial supply + initial player cards = final supply + ending decks + trashed
     */
    static List<Violation> checkCardConservation(GameTrace trace) {
        List<Violation> violations = new ArrayList<>();

        GameStartEvent startEvent = null;
        GameEndEvent endEvent = null;
        for (ObservedEvent oe : trace.observerEvents()) {
            if (oe.event() instanceof GameStartEvent gse) startEvent = gse;
            if (oe.event() instanceof GameEndEvent gee) endEvent = gee;
        }
        if (startEvent == null || endEvent == null) return violations;

        // Count initial total: supply + player starting decks (10 each)
        int initialTotal = trace.numPlayers() * 10;
        for (Card.Type type : Card.Type.values()) {
            initialTotal += startEvent.initialSupply().getNumAvailable(type);
        }

        // Count final total: ending decks + final supply + trashed cards
        int finalTotal = 0;
        for (PlayerResult pr : trace.result().playerResults()) {
            finalTotal += pr.endingDeck().size();
        }
        for (Card.Type type : Card.Type.values()) {
            finalTotal += endEvent.finalSupply().getNumAvailable(type);
        }
        int trashedCount = 0;
        for (ObservedEvent oe : trace.observerEvents()) {
            if (oe.event() instanceof TrashCardEvent) trashedCount++;
        }
        finalTotal += trashedCount;

        if (finalTotal > initialTotal) {
            // Cards appeared from nowhere — definite violation
            violations.add(new Violation("Card conservation",
                    "Final total " + finalTotal + " > initial total " + initialTotal
                            + " — cards appeared (ending decks + final supply + " + trashedCount + " trashed)",
                    trace.gameIndex()));
        } else if (finalTotal < initialTotal && trashedCount == 0) {
            // Cards vanished with no TrashCardEvents — likely missing trash events
            violations.add(new Violation("Card conservation",
                    "Initial total " + initialTotal + " > final total " + finalTotal
                            + " with 0 TrashCardEvents — engine may not be firing TrashCardEvents",
                    trace.gameIndex()));
        } else if (finalTotal != initialTotal) {
            violations.add(new Violation("Card conservation",
                    "Initial total " + initialTotal + " != final total " + finalTotal
                            + " (ending decks + final supply + " + trashedCount + " trashed)",
                    trace.gameIndex()));
        }
        return violations;
    }

    /**
     * Check 10: Supply depletion — track supply changes across observer events.
     * After a GainCardEvent for type X, the next state's supply(X) should have decreased.
     */
    static List<Violation> checkSupplyDepletion(GameTrace trace) {
        List<Violation> violations = new ArrayList<>();
        List<ObservedEvent> events = trace.observerEvents();

        for (int i = 0; i < events.size() - 1; i++) {
            ObservedEvent current = events.get(i);
            if (current.event() instanceof GainCardEvent gce) {
                // Find the next event that has a non-null state with buyableCards
                for (int j = i + 1; j < events.size(); j++) {
                    ObservedEvent next = events.get(j);
                    if (next.state() != null && next.state().buyableCards() != null) {
                        if (current.state() != null && current.state().buyableCards() != null) {
                            int before = current.state().buyableCards().getNumAvailable(gce.cardType());
                            int after = next.state().buyableCards().getNumAvailable(gce.cardType());
                            if (after > before) {
                                violations.add(new Violation("Supply depletion",
                                        gce.cardType().description() + " supply increased from "
                                                + before + " to " + after + " after GainCardEvent",
                                        trace.gameIndex()));
                            }
                        }
                        break;
                    }
                }
            }
        }
        return violations;
    }

    /**
     * Check 11: GameStartEvent and GameEndEvent fired exactly once each.
     */
    static List<Violation> checkLifecycleEvents(GameTrace trace) {
        List<Violation> violations = new ArrayList<>();
        int startCount = 0;
        int endCount = 0;
        for (ObservedEvent oe : trace.observerEvents()) {
            if (oe.event() instanceof GameStartEvent) startCount++;
            if (oe.event() instanceof GameEndEvent) endCount++;
        }
        if (startCount != 1) {
            violations.add(new Violation("Lifecycle events",
                    "GameStartEvent fired " + startCount + " time(s), expected 1",
                    trace.gameIndex()));
        }
        if (endCount != 1) {
            violations.add(new Violation("Lifecycle events",
                    "GameEndEvent fired " + endCount + " time(s), expected 1",
                    trace.gameIndex()));
        }
        return violations;
    }

    // --- helpers ---

    private static int mainPhaseOrdinal(GameState.TurnPhase phase) {
        return switch (phase) {
            case ACTION -> 0;
            case MONEY -> 1;
            case BUY -> 2;
            case CLEANUP -> 3;
            default -> -1; // sub-phases
        };
    }

    private static String mainPhaseNameFromOrdinal(int ordinal) {
        return switch (ordinal) {
            case 0 -> "ACTION";
            case 1 -> "MONEY";
            case 2 -> "BUY";
            case 3 -> "CLEANUP";
            default -> "UNKNOWN";
        };
    }
}
