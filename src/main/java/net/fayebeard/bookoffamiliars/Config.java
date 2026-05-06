package net.fayebeard.bookoffamiliars;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class Config {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_FAMILIARS = BUILDER
            .comment("Maximum number of familiars a player can store.",
                    "WARNING: Higher values may cause performance issues, especially on servers.")
            .translation("bookoffamiliars.configuration.maxFamiliars")
            .defineInRange("maxFamiliars", 10, 1, 100);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ENTITY_BLACKLIST = BUILDER
            .comment("List of entity types that cannot be stored. Format: [\"minecraft:wolf\", \"minecraft:cat\"]",
                    "Tip: blacklist horses if you don't want players stealing other people's.")
            .translation("bookoffamiliars.configuration.entityBlacklist")
            .defineListAllowEmpty("entityBlacklist", List.of(), entry -> entry instanceof String);

    static final ModConfigSpec SPEC = BUILDER.build();
}
