package edu.brandeis.cosi103a.verifier;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.state.GameState;

import java.util.Optional;

/**
 * A single makeDecision call captured by a VerifierPlayer.
 */
public record DecisionRecord(
        GameState state,
        ImmutableList<Decision> options,
        Decision chosen,
        Optional<Event> triggeringEvent) {
}
