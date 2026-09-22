package io.nesin.voteplugin;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

public class VoteConfig {
    public enum RequirementMode {
        SIMPLE_MAJORITY,
        PERCENTAGE
    }

    public enum CooldownMode {
        GLOBAL,
        PLAYER
    }

    private final VotePlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<VoteTarget, String> targetNames = new EnumMap<>(VoteTarget.class);

    private int voteDurationSeconds;
    private int reminderSeconds;
    private int cooldownSeconds;
    private CooldownMode cooldownMode;
    private RequirementMode requirementMode;
    private double requiredPercentage;
    private boolean worldOnly;
    private int minPlayers;

    private boolean soundsEnabled;
    private Sound soundStart;
    private Sound soundReminder;
    private Sound soundSuccess;
    private Sound soundFailure;
    private Sound soundCast;

    private String prefix;

    public VoteConfig(VotePlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() {
        plugin.reloadConfig();
        load();
    }

    private void load() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        Logger logger = plugin.getLogger();

        // Settings
        this.voteDurationSeconds = config.getInt("settings.vote-duration", 30);
        this.reminderSeconds = config.getInt("settings.reminder-seconds", 15);
        this.cooldownSeconds = config.getInt("settings.cooldown-seconds", 60);

        String cMode = config.getString("settings.cooldown-mode", "global");
        this.cooldownMode = "player".equalsIgnoreCase(cMode) ? CooldownMode.PLAYER : CooldownMode.GLOBAL;

        String rMode = config.getString("settings.requirement-mode", "simple_majority");
        this.requirementMode = "percentage".equalsIgnoreCase(rMode) ? RequirementMode.PERCENTAGE : RequirementMode.SIMPLE_MAJORITY;

        this.requiredPercentage = config.getDouble("settings.required-percentage", 50.0);
        this.worldOnly = config.getBoolean("settings.world-only", true);
        this.minPlayers = config.getInt("settings.min-players", 1);

        // Sounds
        this.soundsEnabled = config.getBoolean("sounds.enabled", true);
        this.soundStart = parseSound(config.getString("sounds.vote-start"));
        this.soundReminder = parseSound(config.getString("sounds.vote-reminder"));
        this.soundSuccess = parseSound(config.getString("sounds.vote-success"));
        this.soundFailure = parseSound(config.getString("sounds.vote-failure"));
        this.soundCast = parseSound(config.getString("sounds.vote-cast"));

        // Target Names
        targetNames.put(VoteTarget.DAY, config.getString("targets.day", "День"));
        targetNames.put(VoteTarget.NIGHT, config.getString("targets.night", "Ночь"));
        targetNames.put(VoteTarget.CLEAR, config.getString("targets.clear", "Ясно"));
        targetNames.put(VoteTarget.RAIN, config.getString("targets.rain", "Дождь"));
        targetNames.put(VoteTarget.STORM, config.getString("targets.storm", "Гроза"));

        // Prefix
        this.prefix = config.getString("messages.prefix", "<gray>[<green>LiteVote</green>]</gray> ");
    }

    private Sound parseSound(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            return Sound.sound(Key.key(key.trim().toLowerCase()), Sound.Source.MASTER, 1.0f, 1.0f);
        } catch (Exception e) {
            plugin.getLogger().warning("Некорректный ключ звука в config.yml: " + key);
            return null;
        }
    }

    public Component getMessage(String key, TagResolver... resolvers) {
        String template = plugin.getConfig().getString("messages." + key, "");
        if (template.isEmpty()) {
            return Component.empty();
        }
        String fullTemplate = prefix + template;
        return miniMessage.deserialize(fullTemplate, resolvers);
    }

    public Component getRawMessage(String key, TagResolver... resolvers) {
        String template = plugin.getConfig().getString("messages." + key, "");
        if (template.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(template, resolvers);
    }

    public String getTargetName(VoteTarget target) {
        return targetNames.getOrDefault(target, target.name());
    }

    public int getVoteDurationSeconds() {
        return voteDurationSeconds;
    }

    public int getReminderSeconds() {
        return reminderSeconds;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public CooldownMode getCooldownMode() {
        return cooldownMode;
    }

    public RequirementMode getRequirementMode() {
        return requirementMode;
    }

    public double getRequiredPercentage() {
        return requiredPercentage;
    }

    public boolean isWorldOnly() {
        return worldOnly;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    public Sound getSoundStart() {
        return soundStart;
    }

    public Sound getSoundReminder() {
        return soundReminder;
    }

    public Sound getSoundSuccess() {
        return soundSuccess;
    }

    public Sound getSoundFailure() {
        return soundFailure;
    }

    public Sound getSoundCast() {
        return soundCast;
    }
}
