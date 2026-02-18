package io.nesin.voteplugin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static io.nesin.voteplugin.VotePlugin.MM;

public class VoteManager {
    private final VotePlugin plugin;

    private Vote activeVote;

    public VoteManager(VotePlugin plugin) {
        this.plugin = plugin;
    }

    public int startVote(CommandContext<CommandSourceStack> ctx, VoteTarget target){
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Создавать голосования могут только игроки!");
            return Command.SINGLE_SUCCESS;
        }

        if (activeVote != null) {
            player.sendMessage("Голосование уже идёт!");
            return  Command.SINGLE_SUCCESS;
        }

        activeVote = new Vote(player.getWorld(), player.getUniqueId(), target);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            activeVote.finish();
            activeVote = null;
        }, 600L);

        Bukkit.getScheduler().runTaskLater(plugin, () ->
            Bukkit.broadcast(MM.deserialize(
                    "<gray>[<green>LiteVote</green>]</gray>" +
                            "<gray>До конца голосования осталось 15 секунд</gray>")),
                300L);

        Bukkit.broadcast(
                MM.deserialize(
                        "<gray>[<green>LiteVote</green>]</gray> " +
                                "<green><player></green> начал голосование за <gold><target></gold>!\n" +
                                "<gray>Вы можете проголосовать с помощью <green>/vote yes</green> или <red>/vote no</red> в течение 30 секунд</gray>",
                        Placeholder.unparsed("player", player.getName()),
                        Placeholder.unparsed("target", target.toString())
                )
        );

        return  Command.SINGLE_SUCCESS;
    }

    public int castVote(CommandContext<CommandSourceStack> ctx, boolean value) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Голосовать могут только игроки!");
            return Command.SINGLE_SUCCESS;
        }

        if (activeVote == null) {
            player.sendMessage("Активного голосования нет");
            return Command.SINGLE_SUCCESS;
        }

        boolean isVoteAdded = activeVote.addVote(player.getUniqueId(), value);

        if (isVoteAdded) {
            player.sendMessage("Ваш голос учтен!");
        } else {
            player.sendMessage("Вы уже проголосовали!");
        }

        return Command.SINGLE_SUCCESS;
    }
}
