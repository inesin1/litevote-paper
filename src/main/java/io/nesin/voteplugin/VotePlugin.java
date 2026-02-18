package io.nesin.voteplugin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import static io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS;

public class VotePlugin extends JavaPlugin implements Listener {
    public static final MiniMessage MM = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        VoteManager voteManager = new VoteManager(this);

        LiteralArgumentBuilder<CommandSourceStack> vote =
                Commands.literal("vote")
                        .then(Commands.literal("day").executes(ctx -> voteManager.startVote(ctx, VoteTarget.DAY)))
                        .then(Commands.literal("night").executes(ctx -> voteManager.startVote(ctx, VoteTarget.NIGHT)))
                        .then(Commands.literal("rain").executes(ctx -> voteManager.startVote(ctx, VoteTarget.RAIN)))
                        .then(Commands.literal("clear").executes(ctx -> voteManager.startVote(ctx, VoteTarget.CLEAR)))
                        .then(Commands.literal("storm").executes(ctx -> voteManager.startVote(ctx, VoteTarget.STORM)))
                        .then(Commands.literal("yes").executes(ctx -> voteManager.castVote(ctx, true)))
                        .then(Commands.literal("no").executes(ctx -> voteManager.castVote(ctx, false)));

    this.getLifecycleManager().registerEventHandler(COMMANDS, event -> {
        var registrar = event.registrar();

        registrar.register(vote.build(), "Голосование за смену времени/погоды");
    });
    }
}