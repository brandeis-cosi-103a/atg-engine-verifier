package edu.brandeis.cosi103a.verifier.strategies;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.state.GameState;
import edu.brandeis.cosi103a.verifier.DecisionStrategy;

import java.util.Optional;
import java.util.Random;

/**
 * Picks a random decision from the offered options. Maximizes code path coverage over many runs.
 */
public class RandomLegalStrategy implements DecisionStrategy {
    private final Random random;

    public RandomLegalStrategy(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public Decision choose(GameState state, ImmutableList<Decision> options, Optional<Event> event) {
        return options.get(random.nextInt(options.size()));
    }
}
