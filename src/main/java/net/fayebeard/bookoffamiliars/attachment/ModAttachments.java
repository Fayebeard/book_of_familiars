package net.fayebeard.bookoffamiliars.attachment;

import net.fayebeard.bookoffamiliars.BookOfFamiliarsMod;
import net.fayebeard.bookoffamiliars.data.FamiliarBookData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BookOfFamiliarsMod.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FamiliarBookData>> FAMILIAR_DATA =
            ATTACHMENT_TYPES.register("familiar_data", () ->
                    AttachmentType.builder(FamiliarBookData::new)
                            .serialize(FamiliarBookData.CODEC)
                            .build());
}
