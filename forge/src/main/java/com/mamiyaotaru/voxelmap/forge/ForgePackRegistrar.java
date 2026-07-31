package com.mamiyaotaru.voxelmap.forge;

import com.mamiyaotaru.voxelmap.PackRegistrar;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.versions.forge.ForgeVersion;

public class ForgePackRegistrar implements PackRegistrar {
    private static final ArrayList<PackInfo> PACKS_TO_REGISTER = new ArrayList<>();

    @Override
    public void registerPack(Identifier location, Component displayName) {
        PACKS_TO_REGISTER.add(new PackInfo(location, displayName));
    }

    public static void registerPacks(final AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

        IModFileInfo modFileInfo = ModList.getModFileById(VoxelConstants.MOD_ID);
        if (modFileInfo == null) return;

        String forgeId = ForgeVersion.MOD_ID;
        String forgeVersion = ForgeVersion.getVersion();

        for (PackInfo packInfo : PACKS_TO_REGISTER) {
            Identifier packLocation = packInfo.location().withPrefix("resourcepacks/");

            String packId = "mod/" + packLocation;
            PackLocationInfo info = new PackLocationInfo(
                    packId,
                    packInfo.displayName(),
                    PackSource.BUILT_IN,
                    Optional.of(new KnownPack(forgeId, packId, forgeVersion))
            );

            Path packPath = modFileInfo.getFile().findResource(packLocation.getPath());
            Pack pack = Pack.readMetaAndCreate(
                    info,
                    new PathPackResources.PathResourcesSupplier(packPath),
                    PackType.CLIENT_RESOURCES,
                    new PackSelectionConfig(false, Pack.Position.TOP, false)
            );

            if (pack != null) {
                event.addRepositorySource((packConsumer) -> packConsumer.accept(pack));
            }
        }
    }

    static record PackInfo(Identifier location, Component displayName) {
    }
}
