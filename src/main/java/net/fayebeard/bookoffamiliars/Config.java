package net.fayebeard.bookoffamiliars;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = BookOfFamiliarsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MAX_FAMILIARS = BUILDER
            .comment("Maximum number of familiars a player can store.",
                    "WARNING: Higher values may cause performance issues, especially on servers.")
            .defineInRange("maxFamiliars", 10, 1, 100);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_BLACKLIST = BUILDER
            .comment("List of entity types that cannot be stored. Format: ['minecraft:wolf', 'minecraft:cat']",
                    "Tip: blacklist horses if you don't want players stealing other people's.")
            .defineListAllowEmpty("entityBlacklist", List.of(), entry -> entry instanceof String);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {

    }
}
