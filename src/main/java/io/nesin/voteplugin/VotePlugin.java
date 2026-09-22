package io.nesin.voteplugin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import static io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS;

public class VotePlugin extends JavaPlugin implements Listener {
    public static final MiniMessage MM = MiniMessage.miniMessage();

    private VoteConfig voteConfig;
    private VoteManager voteManager;

    @Override
    public void onEnable() {
        this.voteConfig = new VoteConfig(this);
        this.voteManager = new VoteManager(this, this.voteConfig);

        // 1. Universal Bukkit command registration (works across all Spigot / Paper 1.20 - 26.x versions)
        PluginCommand bukkitCmd = getCommand("vote");
        if (bukkitCmd != null) {
            bukkitCmd.setExecutor(voteManager);
            bukkitCmd.setTabCompleter(voteManager);
        }

        // 2. Modern Paper Brigadier lifecycle registration (for Paper 1.20.6+, 1.21.x, and Paper 26.x)
        try {
            LiteralArgumentBuilder<CommandSourceStack> vote = Commands.literal("vote")
                    .requires(source -> source.getSender().hasPermission("litevote.vote"))
                    .executes(voteManager::showHelp)
                    .then(Commands.literal("day").executes(ctx -> voteManager.startVote(ctx, VoteTarget.DAY)))
                    .then(Commands.literal("night").executes(ctx -> voteManager.startVote(ctx, VoteTarget.NIGHT)))
                    .then(Commands.literal("rain").executes(ctx -> voteManager.startVote(ctx, VoteTarget.RAIN)))
                    .then(Commands.literal("clear").executes(ctx -> voteManager.startVote(ctx, VoteTarget.CLEAR)))
                    .then(Commands.literal("storm").executes(ctx -> voteManager.startVote(ctx, VoteTarget.STORM)))
                    .then(Commands.literal("yes").executes(ctx -> voteManager.castVote(ctx, true)))
                    .then(Commands.literal("no").executes(ctx -> voteManager.castVote(ctx, false)))
                    .then(Commands.literal("reload")
                            .requires(source -> source.getSender().hasPermission("litevote.admin"))
                            .executes(voteManager::reload));

            this.getLifecycleManager().registerEventHandler(COMMANDS, event -> {
                event.registrar().register(vote.build(), "LiteVote - голосование за смену времени и погоды");
            });
        } catch (Throwable t) {
            getLogger().info("Используется стандартный Bukkit CommandExecutor.");
        }

        getLogger().info("LiteVotePlugin v" + getPluginMeta().getVersion() + " успешно запущен!");
    }

    @Override
    public void onDisable() {
        if (voteManager != null) {
            voteManager.cancelActiveVote();
        }
        getLogger().info("LiteVotePlugin выключен.");
    }

    public VoteConfig getVoteConfig() {
        return voteConfig;
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }
}