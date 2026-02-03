package edu.brandeis.cosi103a.verifier.strategies;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.state.GameState;
import edu.brandeis.cosi103a.verifier.DecisionStrategy;

import java.util.Optional;

/**
 * Always ends the phase immediately. Tests degenerate case where players take no actions.
 */
public class PassiveStrategy implements DecisionStrategy {
    @Override
    public Decision choose(GameState state, ImmutableList<Decision> options, Optional<Event> event) {
        return BigMoneyStrategy.findEndPhaseOrFirst(options);
    }
}
