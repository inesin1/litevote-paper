package io.nesin.voteplugin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class VoteManager implements CommandExecutor, TabCompleter {
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

    // Brigadier bridge
    public int startVote(CommandContext<CommandSourceStack> ctx, VoteTarget target) {
        startVote(ctx.getSource().getSender(), target);
        return Command.SINGLE_SUCCESS;
    }

    public int castVote(CommandContext<CommandSourceStack> ctx, boolean value) {
        castVote(ctx.getSource().getSender(), value);
        return Command.SINGLE_SUCCESS;
    }

    public int reload(CommandContext<CommandSourceStack> ctx) {
        reload(ctx.getSource().getSender());
        return Command.SINGLE_SUCCESS;
    }

    public int showHelp(CommandContext<CommandSourceStack> ctx) {
        showHelp(ctx.getSource().getSender());
        return Command.SINGLE_SUCCESS;
    }

    // Core logic
    public void startVote(CommandSender sender, VoteTarget target) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.getMessage("only-players"));
            return;
        }

        if (activeVote != null) {
            player.sendMessage(config.getMessage("vote-already-active"));
            return;
        }

        // Min players check
        int eligiblePlayers = config.isWorldOnly() ? player.getWorld().getPlayerCount() : Bukkit.getOnlinePlayers().size();
        if (eligiblePlayers < config.getMinPlayers()) {
            player.sendMessage(config.getMessage("min-players",
                    Placeholder.unparsed("min_players", String.valueOf(config.getMinPlayers()))
            ));
            return;
        }

        // Cooldown check
        if (!player.hasPermission("litevote.bypass.cooldown")) {
            long remainingSeconds = getRemainingCooldown(player.getUniqueId());
            if (remainingSeconds > 0) {
                player.sendMessage(config.getMessage("cooldown",
                        Placeholder.unparsed("seconds", String.valueOf(remainingSeconds))
                ));
                return;
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
    }

    public void castVote(CommandSender sender, boolean value) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.getMessage("only-players"));
            return;
        }

        if (activeVote == null) {
            player.sendMessage(config.getMessage("no-active-vote"));
            return;
        }

        // If world-only, ensure player is in the same world
        if (config.isWorldOnly() && !player.getWorld().equals(activeVote.getWorld())) {
            player.sendMessage(config.getMessage("no-active-vote"));
            return;
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
    }

    public void reload(CommandSender sender) {
        config.reload();
        sender.sendMessage(config.getMessage("reload-success"));
    }

    public void showHelp(CommandSender sender) {
        sender.sendMessage(config.getRawMessage("help"));
    }

    // Classic Bukkit CommandExecutor fallback for maximum multi-version compatibility
    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!sender.hasPermission("litevote.vote")) {
            sender.sendMessage(config.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "yes" -> castVote(sender, true);
            case "no" -> castVote(sender, false);
            case "reload" -> {
                if (!sender.hasPermission("litevote.admin")) {
                    sender.sendMessage(config.getMessage("no-permission"));
                } else {
                    reload(sender);
                }
            }
            default -> {
                VoteTarget target = VoteTarget.from(sub);
                if (target != null) {
                    startVote(sender, target);
                } else {
                    sender.sendMessage(config.getMessage("unknown-target"));
                }
            }
        }
        return true;
    }

    // Classic Bukkit TabCompleter fallback
    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(List.of("day", "night", "clear", "rain", "storm", "yes", "no"));
            if (sender.hasPermission("litevote.admin")) {
                completions.add("reload");
            }
            String prefix = args[0].toLowerCase();
            return completions.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        return Collections.emptyList();
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
