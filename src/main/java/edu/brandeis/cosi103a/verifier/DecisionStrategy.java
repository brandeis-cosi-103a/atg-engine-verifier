package edu.brandeis.cosi103a.verifier;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.state.GameState;

import java.util.Optional;

/**
 * Strategy for choosing decisions in a VerifierPlayer.
 * Implementations should always return a decision from the provided options.
 */
public interface DecisionStrategy {
    Decision choose(GameState state, ImmutableList<Decision> options, Optional<Event> event);
}
