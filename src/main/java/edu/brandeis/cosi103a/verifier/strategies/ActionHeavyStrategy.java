package edu.brandeis.cosi103a.verifier.strategies;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.*;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.state.GameState;
import edu.brandeis.cosi103a.verifier.DecisionStrategy;

import java.util.Optional;

/**
 * Prioritizes buying and playing action cards to exercise action, reaction,
 * gain, discard, and trash phases.
 */
public class ActionHeavyStrategy implements DecisionStrategy {
    @Override
    public Decision choose(GameState state, ImmutableList<Decision> options, Optional<Event> event) {
        return switch (state.phase()) {
            case ACTION -> playActionOrEnd(options);
            case MONEY -> playMoneyOrEnd(options);
            case BUY -> buyActionOrFramework(options);
            case GAIN -> gainHighestCost(options);
            case DISCARD -> discardFirst(options);
            case REACTION, CLEANUP -> BigMoneyStrategy.findEndPhaseOrFirst(options);
        };
    }

    private Decision playActionOrEnd(ImmutableList<Decision> options) {
        for (Decision d : options) {
            if (d instanceof PlayCardDecision p && p.card().category() == Card.Type.Category.ACTION) {
                return d;
            }
        }
        return BigMoneyStrategy.findEndPhaseOrFirst(options);
    }

    private Decision playMoneyOrEnd(ImmutableList<Decision> options) {
        for (Decision d : options) {
            if (d instanceof PlayCardDecision p && p.card().category() == Card.Type.Category.MONEY) {
                return d;
            }
        }
        return BigMoneyStrategy.findEndPhaseOrFirst(options);
    }

    private Decision buyActionOrFramework(ImmutableList<Decision> options) {
        BuyDecision bestAction = null;
        int bestCost = -1;
        BuyDecision frameworkBuy = null;
        for (Decision d : options) {
            if (d instanceof BuyDecision b) {
                if (b.cardType() == Card.Type.FRAMEWORK) frameworkBuy = b;
                if (b.cardType().category() == Card.Type.Category.ACTION && b.cardType().cost() > bestCost) {
                    bestCost = b.cardType().cost();
                    bestAction = b;
                }
            }
        }
        if (bestAction != null) return bestAction;
        if (frameworkBuy != null) return frameworkBuy;
        return BigMoneyStrategy.findEndPhaseOrFirst(options);
    }

    private Decision gainHighestCost(ImmutableList<Decision> options) {
        GainCardDecision best = null;
        int bestCost = -1;
        for (Decision d : options) {
            if (d instanceof GainCardDecision g && g.cardType().cost() > bestCost) {
                bestCost = g.cardType().cost();
                best = g;
            }
        }
        return best != null ? best : options.get(0);
    }

    private Decision discardFirst(ImmutableList<Decision> options) {
        for (Decision d : options) {
            if (d instanceof DiscardCardDecision) return d;
        }
        return BigMoneyStrategy.findEndPhaseOrFirst(options);
    }
}
