package edu.brandeis.cosi103a.verifier.strategies;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.*;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.state.GameState;
import edu.brandeis.cosi103a.verifier.DecisionStrategy;

import java.util.Optional;

/**
 * Buys Framework if affordable, otherwise highest money card. Plays all money. Ends action phase.
 */
public class BigMoneyStrategy implements DecisionStrategy {
    @Override
    public Decision choose(GameState state, ImmutableList<Decision> options, Optional<Event> event) {
        return switch (state.phase()) {
            case ACTION, REACTION, DISCARD, CLEANUP -> findEndPhaseOrFirst(options);
            case MONEY -> playMoneyOrEnd(options);
            case BUY -> buyBest(options);
            case GAIN -> gainHighestCost(options);
        };
    }

    private Decision playMoneyOrEnd(ImmutableList<Decision> options) {
        for (Decision d : options) {
            if (d instanceof PlayCardDecision p && p.card().category() == Card.Type.Category.MONEY) {
                return d;
            }
        }
        return findEndPhaseOrFirst(options);
    }

    private Decision buyBest(ImmutableList<Decision> options) {
        BuyDecision frameworkBuy = null;
        BuyDecision bestMoney = null;
        int bestValue = -1;
        for (Decision d : options) {
            if (d instanceof BuyDecision b) {
                if (b.cardType() == Card.Type.FRAMEWORK) frameworkBuy = b;
                if (b.cardType().category() == Card.Type.Category.MONEY && b.cardType().value() > bestValue) {
                    bestValue = b.cardType().value();
                    bestMoney = b;
                }
            }
        }
        if (frameworkBuy != null) return frameworkBuy;
        if (bestMoney != null) return bestMoney;
        return findEndPhaseOrFirst(options);
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

    static Decision findEndPhaseOrFirst(ImmutableList<Decision> options) {
        for (Decision d : options) {
            if (d instanceof EndPhaseDecision) return d;
        }
        return options.get(0);
    }
}
