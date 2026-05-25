package net.fayebeard.bookffamiliars;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MAX_FAMILIARS = BUILDER
            .comment("Maximum number of familiars a player can store.",
                    "WARNING: Higher values may cause performance issues, especially on servers.")
            .defineInRange("maxFamiliars", 10, 1, 100);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_BLACKLIST = BUILDER
            .comment("List of entity types that cannot be stored. Format: [\"minecraft:wolf\", \"minecraft:cat\"]")
            .defineListAllowEmpty("entityBlacklist", List.of(), entry ->
                    entry instanceof String s && s.contains(":") && !s.contains(" "));

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_WHITELIST = BUILDER
            .comment("List of additional entity types that can be stored beyond the default tamed entities.",
                    "Format: [\"minecraft:cow\", \"minecraft:sheep\"]")
            .defineListAllowEmpty("entityWhitelist", List.of(), entry ->
                    entry instanceof String s && s.contains(":") && !s.contains(" "));

    public static final ForgeConfigSpec.IntValue RESURRECTION_COOLDOWN_MINUTES = BUILDER
            .comment("Cooldown in minutes before a released familiar returns to the book after death")
            .defineInRange("resurrectionCooldownMinutes", 20, 0, 1440);

    public static final ForgeConfigSpec.IntValue RESURRECTION_XP_COST = BUILDER
            .comment("Experience levels required to skip the resurrection cooldown.")
            .defineInRange("resurrectionXpCost", 5, 0, 100);

    public static final ForgeConfigSpec.BooleanValue AUTO_REMOVE_INVALID_FAMILIARS = BUILDER
            .comment("Automatically remove familiars from the book if their mod is no longer installed")
            .define("autoRemoveInvalidFamiliars", false);

    public static final ForgeConfigSpec.BooleanValue ENABLE_RESURRECTION = BUILDER
            .comment("Enable the resurrection system. If disabled, familiars will not return to the book after death")
            .define("enableResurrection", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();
}
