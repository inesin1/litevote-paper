package io.nesin.voteplugin;

public enum VoteTarget {
    DAY,
    NIGHT,
    CLEAR,
    RAIN,
    STORM;

    public static VoteTarget from(String target) {
        if (target == null) {
            return null;
        }
        return switch (target.trim().toLowerCase()) {
            case "day" -> DAY;
            case "night" -> NIGHT;
            case "rain" -> RAIN;
            case "clear" -> CLEAR;
            case "storm" -> STORM;
            default -> null;
        };
    }
}
