package net.fayebeard.bookoffamiliars;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;


public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_FAMILIARS = BUILDER
            .comment("Maximum number of familiars a player can store.",
                    "WARNING: Higher values may cause performance issues, especially on servers.")
            .translation("bookoffamiliars.configuration.maxFamiliars")
            .defineInRange("maxFamiliars", 10, 1, 100);

    @SuppressWarnings("deprecation")
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_BLACKLIST = BUILDER
            .comment("List of entity types that cannot be stored. Format: [\"minecraft:wolf\", \"minecraft:cat\"]")
            .translation("bookoffamiliars.configuration.entityBlacklist")
            .defineListAllowEmpty("entityBlacklist", List.of(), entry ->
                    entry instanceof String s && s.contains(":") && !s.contains(" "));

    @SuppressWarnings("deprecation")
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_WHITELIST = BUILDER
            .comment("List of additional entity types that can be stored beyond the default tamed entities.",
                    "Format: [\"minecraft:cow\", \"minecraft:sheep\"]")
            .translation("bookoffamiliars.configuration.entityWhitelist")
            .defineListAllowEmpty("entityWhitelist", List.of(), entry ->
                    entry instanceof String s && s.contains(":") && !s.contains(" "));

    public static final ModConfigSpec.IntValue RESURRECTION_COOLDOWN_MINUTES = BUILDER
            .comment("Cooldown in minutes before a released familiar returns to the book after death")
            .translation("bookoffamiliars.configuration.resurrectionCooldownMinutes")
            .defineInRange("resurrectionCooldownMinutes", 20, 0, 1440);

    public static final ModConfigSpec.IntValue RESURRECTION_XP_COST = BUILDER
            .comment("Experience levels required to skip the resurrection cooldown.")
            .translation("bookoffamiliars.configuration.resurrectionXpCost")
            .defineInRange("resurrectionXpCost", 5, 0, 100);

    public static final ModConfigSpec.BooleanValue REMOVE_INVALID_FAMILIARS = BUILDER
            .comment("Automatically remove familiars from the book if their mod is no longer installed.")
            .translation("bookoffamiliars.configuration.removeInvalidFamiliars")
            .define("removeInvalidFamiliars", false);

    public static final ModConfigSpec.BooleanValue ENABLE_RESURRECTION = BUILDER
            .comment("Enable the resurrection system. If disabled, familiars will not return to the book after death")
            .translation("bookoffamiliars.configuration.enableResurrection")
            .define("enableResurrection", true);

    static final ModConfigSpec SPEC = BUILDER.build();

}
