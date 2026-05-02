package net.fayebeard.bookoffamiliars;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ALLOW_HORSE_STORAGE = BUILDER
            .comment("Allow players to store horses in the familiar book.",
                    "Disable this on servers if you're worried about the potential stealing of horses.")
            .define("allowHorseStorage", true);

    public static final ModConfigSpec.IntValue MAX_FAMILIARS = BUILDER
            .comment("Maximum number of familiars a player can store in their familiar book.",
                    "WARNING: Setting this very high may cause performance issues. Maximum is 100.")
            .defineInRange("maxfamiliars", 10, 1, 100);

    static final ModConfigSpec SPEC = BUILDER.build();
}
