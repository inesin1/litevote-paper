package io.nesin.voteplugin;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.UUID;

import static io.nesin.voteplugin.VotePlugin.MM;

public class Vote {
    private final HashMap<UUID, Boolean> votes = new HashMap<>();
    private final VoteTarget target;
    private final World world;

    public Vote(World world, UUID initiatorId, VoteTarget target) {
        this.target = target;
        this.world = world;

        votes.put(initiatorId, true);
    }

    public boolean addVote(UUID playerId, Boolean value) {
        if (votes.containsKey(playerId)) {
            return false;
        }

        votes.put(playerId, value);

        return true;
    }

    public void finish() {
        long yes = votes.values().stream().filter(v -> v).count();
        long no = votes.size() - yes;

        if (yes > no) {
            Bukkit.broadcast(MM.deserialize(
                    "<gray>[<green>LiteVote</green>]</gray>" +
                    "<green>Голосование завершилось, запрос выполнен!</green>"));
            applyResult();
        } else {
            Bukkit.broadcast(MM.deserialize(
                    "<gray>[<green>LiteVote</green>]</gray>" +
                    "<red>Голосование завершено, запрос не выполнен!</red>"));
        }
    }

    private void applyResult() {
        switch (target) {
            case DAY -> world.setTime(0);
            case NIGHT -> world.setTime(13000);
            case CLEAR -> world.setStorm(false);
            case RAIN -> world.setStorm(true);
            case STORM -> {
                world.setStorm(true);
                world.setThundering(true);
            }
        }
    }
}
