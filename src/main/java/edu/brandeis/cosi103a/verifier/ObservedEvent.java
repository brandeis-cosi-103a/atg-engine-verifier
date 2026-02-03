package edu.brandeis.cosi103a.verifier;

import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.state.GameState;

/**
 * A single event captured by the observer, paired with the game state at that moment.
 */
public record ObservedEvent(GameState state, Event event) {
}
