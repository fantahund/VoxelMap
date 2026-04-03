package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.GuiAddWaypoint;
import com.mamiyaotaru.voxelmap.gui.GuiSelectPlayer;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Optional;
import java.util.Random;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

public final class CommandUtils {
    private static final int NEW_WAYPOINT_COMMAND_LENGTH = "newWaypoint ".length();
    private static final int TELEPORT_COMMAND_LENGTH = "ztp ".length();
    private static final Random generator = new Random();
    public static final Pattern pattern = Pattern.compile("\\[(\\w+\\s*:\\s*[-#]?[^\\[\\]]+)(,\\s*\\w+\\s*:\\s*[-#]?[^\\[\\]]+)+\\]", Pattern.CASE_INSENSITIVE);

    private CommandUtils() {
    }

    public static Component checkForWaypoints(Component chat) {
        if (!pattern.matcher(chat.getString()).find()) {
            return chat;
        }

        MutableComponent finalMessage = Component.empty();
        chat.visit((style, textPart) -> {
            Matcher matcher = pattern.matcher(textPart);
            int lastEnd = 0;

            while (matcher.find()) {
                String waypointPart = matcher.group();
                String dataPart = waypointPart.substring(1, waypointPart.length() - 1);

                if (createWaypointFromChat(dataPart) == null) {
                    continue;
                }

                if (matcher.start() > lastEnd) {
                    String prefixPart = textPart.substring(lastEnd, matcher.start());
                    finalMessage.append(Component.literal(prefixPart).withStyle(style));
                }

                lastEnd = matcher.end();

                String command = "/newWaypoint " + dataPart;
                Component tooltip = Component.empty()
                        .append(Component.translatable("minimap.waypointShare.tooltip1"))
                        .append("\n")
                        .append(Component.translatable("minimap.waypointShare.tooltip2"));

                MutableComponent clickableWaypoint = Component.literal(waypointPart).withStyle(style2 -> style2
                        .withClickEvent(new ClickEvent.RunCommand(command))
                        .withColor(ChatFormatting.AQUA)
                        .withHoverEvent(new HoverEvent.ShowText(tooltip))
                );
                finalMessage.append(clickableWaypoint);
            }

            if (lastEnd < textPart.length()) {
                String suffixPart = textPart.substring(lastEnd);
                finalMessage.append(Component.literal(suffixPart).withStyle(style));
            }

            return Optional.empty();
        }, Style.EMPTY);

        return finalMessage;
    }

    private static Waypoint createWaypointFromChat(String details) {
        Waypoint waypoint = null;
        String[] pairs = details.split(",");

        try {
            String name = "";
            Integer x = null;
            Integer z = null;
            int y = 64;
            boolean enabled = true;
            float red = generator.nextFloat();
            float green = generator.nextFloat();
            float blue = generator.nextFloat();
            String suffix = "";
            String world = "";
            TreeSet<DimensionContainer> dimensions = new TreeSet<>();

            for (String pair : pairs) {
                int splitIndex = pair.indexOf(':');
                if (splitIndex != -1) {
                    String key = pair.substring(0, splitIndex).toLowerCase().trim();
                    String value = pair.substring(splitIndex + 1).trim();
                    if (key.equals("name")) {
                        name = TextUtils.descrubName(value);
                    } else if (key.equals("x")) {
                        x = Integer.parseInt(value);
                    } else if (key.equals("z")) {
                        z = Integer.parseInt(value);
                    } else if (key.equals("y")) {
                        y = Integer.parseInt(value);
                    } else if (key.equals("enabled")) {
                        enabled = Boolean.parseBoolean(value);
                    } else if (key.equals("red")) {
                        red = Float.parseFloat(value);
                    } else if (key.equals("green")) {
                        green = Float.parseFloat(value);
                    } else if (key.equals("blue")) {
                        blue = Float.parseFloat(value);
                    } else if (key.equals("color")) {
                        int color = Integer.decode(value);
                        red = (color >> 16 & 0xFF) / 255.0F;
                        green = (color >> 8 & 0xFF) / 255.0F;
                        blue = (color & 0xFF) / 255.0F;
                    } else if (!key.equals("suffix") && !key.equals("icon")) {
                        switch (key) {
                            case "world" -> world = TextUtils.descrubName(value);
                            case "dimensions" -> {
                                String[] dimensionStrings = value.split("#");
                                for (String dimensionString : dimensionStrings) {
                                    dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByIdentifier(dimensionString));
                                }
                            }
                            case "dimension", "dim" ->
                                    dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByIdentifier(value));
                        }
                    } else {
                        suffix = value;
                    }
                }
            }

            if (world.isEmpty()) {
                world = VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false);
            }

            if (dimensions.isEmpty()) {
                dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level()));
            }

            if (x != null && z != null) {
                if (dimensions.size() == 1 && (dimensions.first()).type.coordinateScale() != 1.0) {
                    double dimensionScale = (dimensions.first()).type.coordinateScale();
                    x = (int) ((double) x * dimensionScale);
                    z = (int) ((double) z * dimensionScale);
                }

                waypoint = new Waypoint(name, x, z, y, enabled, red, green, blue, suffix, world, dimensions);
            }
        } catch (Exception ignored) {
        }

        return waypoint;
    }

    public static void waypointClicked(String command) {
        boolean control = InputConstants.isKeyDown(VoxelConstants.getMinecraft().getWindow(), InputConstants.getKey("key.keyboard.left.control").getValue())
                || InputConstants.isKeyDown(VoxelConstants.getMinecraft().getWindow(), InputConstants.getKey("key.keyboard.right.control").getValue());
        String details = command.substring(NEW_WAYPOINT_COMMAND_LENGTH);
        Waypoint newWaypoint = createWaypointFromChat(details);
        if (newWaypoint != null) {
            for (Waypoint existingWaypoint : VoxelConstants.getVoxelMapInstance().getWaypointManager().getWaypoints()) {
                if (newWaypoint.getX() == existingWaypoint.getX() && newWaypoint.getZ() == existingWaypoint.getZ()) {
                    if (control) {
                        VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(null, existingWaypoint, true));
                    } else {
                        VoxelConstants.getVoxelMapInstance().getWaypointManager().setHighlightedWaypoint(existingWaypoint, false);
                    }

                    return;
                }
            }

            if (control) {
                VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(null, newWaypoint, false));
            } else {
                VoxelConstants.getVoxelMapInstance().getWaypointManager().setHighlightedWaypoint(newWaypoint, false);
            }
        }

    }

    public static void sendWaypoint(Waypoint waypoint) {
        Identifier Identifier = VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level()).Identifier;
        int color = ((int) (waypoint.red * 255.0F) & 0xFF) << 16 | ((int) (waypoint.green * 255.0F) & 0xFF) << 8 | (int) (waypoint.blue * 255.0F) & 0xFF;
        StringBuilder hexColor = new StringBuilder(Integer.toHexString(color));

        while (hexColor.length() < 6) {
            hexColor.insert(0, "0");
        }

        hexColor.insert(0, "#");
        String world = VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false);
        if (waypoint.world != null && !waypoint.world.isEmpty()) {
            world = waypoint.world;
        }

        String suffix = waypoint.imageSuffix;
        Object[] args = {TextUtils.scrubNameRegex(waypoint.name), waypoint.getX(), waypoint.getY(), waypoint.getZ(), Identifier.toString()};
        String message = String.format("[name:%s, x:%s, y:%s, z:%s, dim:%s", args);
        if (world != null && !world.isEmpty()) {
            message = message + ", world:" + world;
        }

        if (suffix != null && !suffix.isEmpty()) {
            message = message + ", icon:" + suffix;
        }

        message = message + "]";
        VoxelConstants.getMinecraft().setScreen(new GuiSelectPlayer(null, message, true));
    }

    public static void sendCoordinate(int x, int y, int z) {
        String message = String.format("[x:%s, y:%s, z:%s]", x, y, z);
        VoxelConstants.getMinecraft().setScreen(new GuiSelectPlayer(null, message, false));
    }

    public static void teleport(String command) {
        String details = command.substring(TELEPORT_COMMAND_LENGTH);

        for (Waypoint wp : VoxelConstants.getVoxelMapInstance().getWaypointManager().getWaypoints()) {
            if (wp.name.equalsIgnoreCase(details) && wp.inDimension && wp.inWorld) {
                int y = wp.getY() > VoxelConstants.getPlayer().level().getMinY() ? wp.getY() : (!VoxelConstants.getPlayer().level().dimensionType().hasCeiling() ? VoxelConstants.getPlayer().level().getMaxY() : 64);
                VoxelConstants.playerRunTeleportCommand(wp.getX(), y, wp.getZ());
                return;
            }
        }
    }

    public static int getSafeHeight(int x, int y, int z, Level worldObj) {
        boolean inNetherDimension = worldObj.dimensionType().hasCeiling();
        BlockPos blockPos = new BlockPos(x, y, z);
        worldObj.getChunk(blockPos);
        worldObj.getChunkSource().getChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4, ChunkStatus.FULL, true);
        if (inNetherDimension) {
            int safeY = -1;

            for (int t = worldObj.getMinY(); t < worldObj.getMaxY(); ++t) {
                if (y + t < worldObj.getMaxY() && isBlockStandable(worldObj, x, y + t, z) && isBlockOpen(worldObj, x, y + t + 1, z) && isBlockOpen(worldObj, x, y + t + 2, z)) {
                    safeY = y + t + 1;
                    t = worldObj.getMaxY();
                }

                if (y - t > worldObj.getMinY() && isBlockStandable(worldObj, x, y - t, z) && isBlockOpen(worldObj, x, y - t + 1, z) && isBlockOpen(worldObj, x, y - t + 2, z)) {
                    safeY = y - t + 1;
                    t = worldObj.getMaxY();
                }
            }

            y = safeY;
        } else if (y <= worldObj.getMinY()) {
            y = worldObj.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
        }

        return y;
    }

    private static boolean isBlockStandable(Level worldObj, int par1, int par2, int par3) {
        BlockPos blockPos = new BlockPos(par1, par2, par3);
        BlockState blockState = worldObj.getBlockState(blockPos);
        Block block = blockState.getBlock();
        return block != null && !blockState.getCollisionShape(worldObj, blockPos).isEmpty();
    }

    private static boolean isBlockOpen(Level worldObj, int par1, int par2, int par3) {
        BlockPos blockPos = new BlockPos(par1, par2, par3);
        BlockState blockState = worldObj.getBlockState(blockPos);
        Block block = blockState.getBlock();
        return block == null || !blockState.isSuffocating(worldObj, blockPos);
    }
}
