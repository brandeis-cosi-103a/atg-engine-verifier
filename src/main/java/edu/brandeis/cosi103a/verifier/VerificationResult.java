package edu.brandeis.cosi103a.verifier;

import java.util.List;

/**
 * The result of verifying a set of games against invariants.
 */
public record VerificationResult(
        int gamesPlayed,
        int gamesPassed,
        List<Violation> violations) {

    public boolean isCompliant() {
        return violations.isEmpty();
    }

    /**
     * Formats a human-readable report.
     */
    public String formatReport(String engineClassName) {
        var sb = new StringBuilder();
        sb.append("Engine: ").append(engineClassName).append('\n');
        sb.append("Games: ").append(gamesPlayed).append(" played, ")
                .append(gamesPassed).append(" passed");
        if (gamesPlayed != gamesPassed) {
            sb.append(", ").append(gamesPlayed - gamesPassed).append(" failed");
        }
        sb.append('\n');

        if (isCompliant()) {
            sb.append("Result: FULLY COMPLIANT\n");
        } else {
            sb.append("Result: NON-COMPLIANT (").append(violations.size()).append(" violation");
            if (violations.size() != 1) sb.append('s');
            sb.append(")\n\n");
            for (Violation v : violations) {
                sb.append("--- ").append(v).append('\n');
            }
        }
        return sb.toString();
    }
}
