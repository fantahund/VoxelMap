package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.multiloader.PackRegistrar;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;

public class NeoForgePackRegistrar implements PackRegistrar {
    private static final ArrayList<PackInfo> PACKS_TO_REGISTER = new ArrayList<>();

    @Override
    public void registerPack(Identifier location, Component displayName) {
        PACKS_TO_REGISTER.add(new PackInfo(location, displayName));
    }

    public static void registerPacks(final AddPackFindersEvent event) {
        for (PackInfo packInfo : PACKS_TO_REGISTER) {
            event.addPackFinders(packInfo.location().withPrefix("resourcepacks/"), PackType.CLIENT_RESOURCES, packInfo.displayName(), PackSource.BUILT_IN, false, Pack.Position.TOP);
        }
    }

    static record PackInfo(Identifier location, Component displayName) {
    }
}
