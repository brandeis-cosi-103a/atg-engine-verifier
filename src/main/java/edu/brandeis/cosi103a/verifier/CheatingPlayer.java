package edu.brandeis.cosi103a.verifier;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.BuyDecision;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.event.GameObserver;
import edu.brandeis.cosi.atg.player.Player;
import edu.brandeis.cosi.atg.state.GameState;

import java.util.Optional;

/**
 * A player that returns an invalid decision (not from the offered options)
 * to verify the engine throws PlayerViolationException.
 */
public class CheatingPlayer implements Player {
    private final String name;
    private int callCount = 0;

    public CheatingPlayer(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Optional<GameObserver> getObserver() {
        return Optional.empty();
    }

    @Override
    public Decision makeDecision(GameState state, ImmutableList<Decision> options, Optional<Event> event) {
        callCount++;
        // First few calls: play normally so the game starts
        if (callCount <= 2) {
            return options.get(0);
        }
        // Then return a decision NOT in the options list
        return new BuyDecision(Card.Type.FRAMEWORK);
    }
}
