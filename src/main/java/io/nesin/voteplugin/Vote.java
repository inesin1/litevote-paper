package io.nesin.voteplugin;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Vote {
    private final Map<UUID, Boolean> votes = new HashMap<>();
    private final VoteTarget target;
    private final World world;
    private final UUID initiatorId;

    public Vote(World world, UUID initiatorId, VoteTarget target) {
        this.target = target;
        this.world = world;
        this.initiatorId = initiatorId;

        // Initiator automatically votes YES
        votes.put(initiatorId, true);
    }

    public boolean addVote(UUID playerId, boolean value) {
        if (votes.containsKey(playerId)) {
            return false;
        }
        votes.put(playerId, value);
        return true;
    }

    public boolean hasVoted(UUID playerId) {
        return votes.containsKey(playerId);
    }

    public long getYesCount() {
        return votes.values().stream().filter(v -> v).count();
    }

    public long getNoCount() {
        return votes.size() - getYesCount();
    }

    public boolean calculatePassed(VoteConfig config) {
        long yes = getYesCount();
        long no = getNoCount();

        if (config.getRequirementMode() == VoteConfig.RequirementMode.PERCENTAGE) {
            int eligiblePlayers = config.isWorldOnly() ? world.getPlayerCount() : Bukkit.getOnlinePlayers().size();
            if (eligiblePlayers <= 0) eligiblePlayers = 1;
            double percentage = ((double) yes / eligiblePlayers) * 100.0;
            return percentage >= config.getRequiredPercentage();
        } else {
            return yes > no;
        }
    }

    public void applyResult(VoteConfig config) {
        if (config.isWorldOnly()) {
            applyToWorld(this.world);
        } else {
            for (World w : Bukkit.getWorlds()) {
                applyToWorld(w);
            }
        }
    }

    private void applyToWorld(World targetWorld) {
        switch (target) {
            case DAY -> targetWorld.setTime(1000);
            case NIGHT -> targetWorld.setTime(13000);
            case CLEAR -> {
                targetWorld.setStorm(false);
                targetWorld.setThundering(false);
            }
            case RAIN -> {
                targetWorld.setStorm(true);
                targetWorld.setThundering(false);
            }
            case STORM -> {
                targetWorld.setStorm(true);
                targetWorld.setThundering(true);
            }
        }
    }

    public VoteTarget getTarget() {
        return target;
    }

    public World getWorld() {
        return world;
    }

    public UUID getInitiatorId() {
        return initiatorId;
    }
}
