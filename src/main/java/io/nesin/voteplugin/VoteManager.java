package io.nesin.voteplugin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VoteManager {
    private final VotePlugin plugin;
    private final VoteConfig config;

    private Vote activeVote;
    private BukkitTask voteEndTask;
    private BukkitTask reminderTask;

    private long globalCooldownExpiry = 0;
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();

    public VoteManager(VotePlugin plugin, VoteConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public int startVote(CommandContext<CommandSourceStack> ctx, VoteTarget target) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.getMessage("only-players"));
            return Command.SINGLE_SUCCESS;
        }

        if (activeVote != null) {
            player.sendMessage(config.getMessage("vote-already-active"));
            return Command.SINGLE_SUCCESS;
        }

        // Min players check
        int eligiblePlayers = config.isWorldOnly() ? player.getWorld().getPlayerCount() : Bukkit.getOnlinePlayers().size();
        if (eligiblePlayers < config.getMinPlayers()) {
            player.sendMessage(config.getMessage("min-players",
                    Placeholder.unparsed("min_players", String.valueOf(config.getMinPlayers()))
            ));
            return Command.SINGLE_SUCCESS;
        }

        // Cooldown check
        if (!player.hasPermission("litevote.bypass.cooldown")) {
            long remainingSeconds = getRemainingCooldown(player.getUniqueId());
            if (remainingSeconds > 0) {
                player.sendMessage(config.getMessage("cooldown",
                        Placeholder.unparsed("seconds", String.valueOf(remainingSeconds))
                ));
                return Command.SINGLE_SUCCESS;
            }
        }

        activeVote = new Vote(player.getWorld(), player.getUniqueId(), target);
        int durationSec = config.getVoteDurationSeconds();
        long durationTicks = durationSec * 20L;

        // Schedule vote completion
        voteEndTask = Bukkit.getScheduler().runTaskLater(plugin, this::finishActiveVote, durationTicks);

        // Schedule reminder
        int reminderSec = config.getReminderSeconds();
        if (reminderSec > 0 && reminderSec < durationSec) {
            long reminderDelayTicks = (durationSec - reminderSec) * 20L;
            reminderTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (activeVote != null) {
                    Component reminderMsg = config.getMessage("vote-reminder",
                            Placeholder.unparsed("seconds_left", String.valueOf(reminderSec))
                    );
                    broadcastToAudience(activeVote.getWorld(), reminderMsg, config.getSoundReminder());
                }
            }, reminderDelayTicks);
        }

        // Broadcast start
        Component startMsg = config.getMessage("vote-start",
                Placeholder.unparsed("player", player.getName()),
                Placeholder.unparsed("target", config.getTargetName(target)),
                Placeholder.unparsed("duration", String.valueOf(durationSec))
        );
        broadcastToAudience(player.getWorld(), startMsg, config.getSoundStart());

        return Command.SINGLE_SUCCESS;
    }

    public int castVote(CommandContext<CommandSourceStack> ctx, boolean value) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.getMessage("only-players"));
            return Command.SINGLE_SUCCESS;
        }

        if (activeVote == null) {
            player.sendMessage(config.getMessage("no-active-vote"));
            return Command.SINGLE_SUCCESS;
        }

        // If world-only, ensure player is in the same world
        if (config.isWorldOnly() && !player.getWorld().equals(activeVote.getWorld())) {
            player.sendMessage(config.getMessage("no-active-vote"));
            return Command.SINGLE_SUCCESS;
        }

        boolean added = activeVote.addVote(player.getUniqueId(), value);
        if (added) {
            String msgKey = value ? "vote-cast-yes" : "vote-cast-no";
            player.sendMessage(config.getMessage(msgKey));
            if (config.isSoundsEnabled() && config.getSoundCast() != null) {
                player.playSound(config.getSoundCast());
            }
        } else {
            player.sendMessage(config.getMessage("already-voted"));
        }

        return Command.SINGLE_SUCCESS;
    }

    public int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        config.reload();
        sender.sendMessage(config.getMessage("reload-success"));
        return Command.SINGLE_SUCCESS;
    }

    public int showHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendMessage(config.getRawMessage("help"));
        return Command.SINGLE_SUCCESS;
    }

    private void finishActiveVote() {
        if (activeVote == null) {
            return;
        }

        Vote vote = activeVote;
        activeVote = null;

        boolean passed = vote.calculatePassed(config);
        long yes = vote.getYesCount();
        long no = vote.getNoCount();
        String targetName = config.getTargetName(vote.getTarget());

        if (passed) {
            vote.applyResult(config);
            Component msg = config.getMessage("vote-success",
                    Placeholder.unparsed("target", targetName),
                    Placeholder.unparsed("yes_votes", String.valueOf(yes)),
                    Placeholder.unparsed("no_votes", String.valueOf(no))
            );
            broadcastToAudience(vote.getWorld(), msg, config.getSoundSuccess());
        } else {
            Component msg = config.getMessage("vote-failure",
                    Placeholder.unparsed("target", targetName),
                    Placeholder.unparsed("yes_votes", String.valueOf(yes)),
                    Placeholder.unparsed("no_votes", String.valueOf(no))
            );
            broadcastToAudience(vote.getWorld(), msg, config.getSoundFailure());
        }

        // Apply cooldown
        int cooldownSec = config.getCooldownSeconds();
        if (cooldownSec > 0) {
            long expiryTime = System.currentTimeMillis() + (cooldownSec * 1000L);
            if (config.getCooldownMode() == VoteConfig.CooldownMode.PLAYER) {
                playerCooldowns.put(vote.getInitiatorId(), expiryTime);
            } else {
                globalCooldownExpiry = expiryTime;
            }
        }

        cancelTasks();
    }

    public void cancelActiveVote() {
        cancelTasks();
        activeVote = null;
    }

    private void cancelTasks() {
        if (voteEndTask != null) {
            voteEndTask.cancel();
            voteEndTask = null;
        }
        if (reminderTask != null) {
            reminderTask.cancel();
            reminderTask = null;
        }
    }

    private long getRemainingCooldown(UUID playerId) {
        long now = System.currentTimeMillis();
        long expiry = (config.getCooldownMode() == VoteConfig.CooldownMode.PLAYER)
                ? playerCooldowns.getOrDefault(playerId, 0L)
                : globalCooldownExpiry;

        if (now >= expiry) {
            return 0;
        }
        return (expiry - now + 999) / 1000;
    }

    private void broadcastToAudience(World world, Component message, Sound sound) {
        if (config.isWorldOnly() && world != null) {
            for (Player p : world.getPlayers()) {
                p.sendMessage(message);
                if (sound != null && config.isSoundsEnabled()) {
                    p.playSound(sound);
                }
            }
        } else {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(message);
                if (sound != null && config.isSoundsEnabled()) {
                    p.playSound(sound);
                }
            }
        }
    }
}
