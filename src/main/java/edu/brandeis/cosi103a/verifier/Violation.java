package edu.brandeis.cosi103a.verifier;

/**
 * A single invariant violation found during verification.
 *
 * @param checkName   the name of the invariant check that failed
 * @param description human-readable description of the violation
 * @param gameIndex   which game (0-based) the violation occurred in, or -1 for cross-game checks
 * @param turn        which turn the violation occurred in, or -1 if not turn-specific
 * @param playerName  the player involved, or null if not player-specific
 * @param context     additional context (e.g. game state details) for debugging
 */
public record Violation(
        String checkName,
        String description,
        int gameIndex,
        int turn,
        String playerName,
        String context) {

    public Violation(String checkName, String description, int gameIndex) {
        this(checkName, description, gameIndex, -1, null, null);
    }

    public Violation(String checkName, String description, int gameIndex, int turn, String playerName) {
        this(checkName, description, gameIndex, turn, playerName, null);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("[Game ").append(gameIndex);
        if (turn >= 0) sb.append(", Turn ").append(turn);
        if (playerName != null) sb.append(", Player: ").append(playerName);
        sb.append("] ");
        sb.append(checkName).append(": ").append(description);
        if (context != null) sb.append("\n  ").append(context);
        return sb.toString();
    }
}
