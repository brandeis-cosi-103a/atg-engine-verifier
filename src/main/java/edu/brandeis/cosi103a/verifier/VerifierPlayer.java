package edu.brandeis.cosi103a.verifier;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.event.GameObserver;
import edu.brandeis.cosi.atg.player.Player;
import edu.brandeis.cosi.atg.state.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * An instrumented Player that captures all makeDecision calls and delegates
 * actual decision-making to a pluggable DecisionStrategy.
 */
public class VerifierPlayer implements Player {
    private final String name;
    private final DecisionStrategy strategy;
    private final List<DecisionRecord> decisionLog = new ArrayList<>();

    public VerifierPlayer(String name, DecisionStrategy strategy) {
        this.name = name;
        this.strategy = strategy;
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
        Decision chosen = strategy.choose(state, options, event);
        decisionLog.add(new DecisionRecord(state, options, chosen, event));
        return chosen;
    }

    public List<DecisionRecord> getDecisionLog() {
        return Collections.unmodifiableList(decisionLog);
    }
}
