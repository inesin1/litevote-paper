package io.nesin.voteplugin;

public enum VoteTarget {
    DAY,
    NIGHT,
    CLEAR,
    RAIN,
    STORM,;

    public static VoteTarget from(String target) {
        return switch (target) {
            case "day" -> DAY;
            case "night" -> NIGHT;
            case "rain" -> RAIN;
            case "clear" -> CLEAR;
            case "storm" -> STORM;
            default -> null;
        };
    }
}
