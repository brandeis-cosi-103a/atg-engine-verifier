package edu.brandeis.cosi103a.verifier;

import edu.brandeis.cosi.atg.state.GameResult;

import java.util.List;
import java.util.Map;

/**
 * Complete trace of a single game execution, aggregating all observation data.
 */
public record GameTrace(
        int gameIndex,
        int numPlayers,
        List<ObservedEvent> observerEvents,
        Map<String, List<DecisionRecord>> playerDecisions,
        GameResult result,
        Exception exception) {

    /**
     * Whether the game completed successfully (no exception thrown).
     */
    public boolean completedSuccessfully() {
        return exception == null && result != null;
    }
}
