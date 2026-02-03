package edu.brandeis.cosi103a.verifier;

import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.event.GameObserver;
import edu.brandeis.cosi.atg.state.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A GameObserver that records all events for later analysis.
 */
public class ObserverRecorder implements GameObserver {
    private final List<ObservedEvent> events = new ArrayList<>();

    @Override
    public void notifyEvent(GameState state, Event event) {
        events.add(new ObservedEvent(state, event));
    }

    public List<ObservedEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }
}
