package edu.brandeis.cosi103a.verifier;

import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.engine.Engine;
import edu.brandeis.cosi.atg.player.Player;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Loads a student Engine class from a JAR file using reflection.
 * The parent classloader provides the atg-api classes so both the verifier
 * and the student engine share the same interface types.
 */
public class EngineLoader {

    private final Class<?> engineClass;

    public EngineLoader(String jarPath, String className) throws Exception {
        URLClassLoader loader = new URLClassLoader(
                new URL[]{new File(jarPath).toURI().toURL()},
                EngineLoader.class.getClassLoader()
        );
        this.engineClass = loader.loadClass(className);
        // Verify it implements Engine
        if (!Engine.class.isAssignableFrom(engineClass)) {
            throw new IllegalArgumentException(className + " does not implement Engine");
        }
    }

    /**
     * For use with an Engine class already on the classpath (e.g. in tests).
     */
    public EngineLoader(Class<? extends Engine> engineClass) {
        this.engineClass = engineClass;
    }

    /**
     * Creates a new Engine instance with the given players and action card types.
     * Tries the 2-arg constructor (List, List) first, falls back to 1-arg (List).
     */
    public Engine create(List<Player> players, List<Card.Type> actionCardTypes) throws Exception {
        try {
            Constructor<?> ctor = engineClass.getConstructor(List.class, List.class);
            return (Engine) ctor.newInstance(players, actionCardTypes);
        } catch (NoSuchMethodException e) {
            // Fall back to 1-arg constructor (e.g. reference engine)
            Constructor<?> ctor = engineClass.getConstructor(List.class);
            return (Engine) ctor.newInstance(players);
        }
    }
}
