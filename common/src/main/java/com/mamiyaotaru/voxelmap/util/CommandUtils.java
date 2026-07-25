package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.GuiAddWaypoint;
import com.mamiyaotaru.voxelmap.gui.GuiSelectPlayer;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

public final class CommandUtils {
    private static final int NEW_WAYPOINT_COMMAND_LENGTH = "newWaypoint ".length();
    private static final int TELEPORT_COMMAND_LENGTH = "ztp ".length();
    private static final String ADD_WAYPOINT_COMMAND = "voxelmapAddWaypoint ";
    private static final String HIGHLIGHT_WAYPOINT_COMMAND = "voxelmapHighlightWaypoint ";
    private static final String XAERO_WAYPOINT_PREFIX = "xaero-waypoint:";
    private static final Random generator = new Random();
    public static final Pattern pattern = Pattern.compile("\\[(\\w+\\s*:\\s*[-#]?[^\\[\\]]+)(,\\s*\\w+\\s*:\\s*[-#]?[^\\[\\]]+)+\\]", Pattern.CASE_INSENSITIVE);
    /** xaero-waypoint:name:initials:x:y:z:color:disabled:type:world, optionally followed by further fields we ignore. */
    public static final Pattern xaeroPattern = Pattern.compile(XAERO_WAYPOINT_PREFIX + "(.*?):([^:]*):(-?\\d+):(-?\\d+):(-?\\d+):(\\d+):(true|false):(\\d+)((?::[^:\\s]*)*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHAT_NAME_PATTERN = Pattern.compile("<([^<>\\s]{1,32})>");

    /** Vanilla translates the dimension names only as a superflat preset and as advancement tabs, those are the names the game shows. */
    private static final Map<Identifier, String> VANILLA_DIMENSION_NAME_KEYS = Map.of(
            Level.OVERWORLD.identifier(), "flat_world_preset.minecraft.overworld",
            Level.NETHER.identifier(), "advancements.nether.root.title",
            Level.END.identifier(), "advancements.end.root.title");

    private static final int SHARE_TEXT_COLOR = 0xC2C2C2;
    private static final int SHARE_NAME_COLOR = 0xA5D6D4;
    private static final int SHARE_ADD_COLOR = 0xA5D6AC;
    private static final int SHARE_HIGHLIGHT_COLOR = 0xD6A5A5;

    private CommandUtils() {
    }

    private record ChatSource(String sender, String comment) {
    }

    private record SharedWaypoint(int start, Waypoint waypoint, String data) {
    }

    public static Component checkForWaypoints(Component chat) {
        String plainMessage = chat.getString();
        List<SharedWaypoint> sharedWaypoints = findSharedWaypoints(plainMessage);

        if (sharedWaypoints.isEmpty()) {
            return chat;
        }

        ChatSource source = findChatSource(chat, sharedWaypoints.getFirst().start());
        MutableComponent finalMessage = Component.empty();

        for (int i = 0; i < sharedWaypoints.size(); i++) {
            if (i > 0) {
                finalMessage.append("\n");
            }

            SharedWaypoint sharedWaypoint = sharedWaypoints.get(i);
            finalMessage.append(buildShareMessage(source == null ? null : source.sender(), sharedWaypoint.waypoint(), sharedWaypoint.data()));
        }

        if (source != null && !source.comment().isEmpty()) {
            finalMessage.append("\n").append(Component.literal(source.comment()).withColor(SHARE_TEXT_COLOR));
        }

        return finalMessage;
    }

    private static List<SharedWaypoint> findSharedWaypoints(String message) {
        List<SharedWaypoint> sharedWaypoints = new ArrayList<>();

        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String waypointPart = matcher.group();
            String dataPart = waypointPart.substring(1, waypointPart.length() - 1);
            Waypoint waypoint = createWaypointFromChat(dataPart);

            if (waypoint != null) {
                sharedWaypoints.add(new SharedWaypoint(matcher.start(), waypoint, dataPart));
            }
        }

        Matcher xaeroMatcher = xaeroPattern.matcher(message);
        while (xaeroMatcher.find()) {
            Waypoint waypoint = createWaypointFromXaero(xaeroMatcher);

            if (waypoint != null) {
                sharedWaypoints.add(new SharedWaypoint(xaeroMatcher.start(), waypoint, xaeroMatcher.group()));
            }
        }

        sharedWaypoints.sort(Comparator.comparingInt(SharedWaypoint::start));
        return sharedWaypoints;
    }

    private static MutableComponent buildShareMessage(String sender, Waypoint waypoint, String dataPart) {
        Component dimension = getSharedDimensionName(waypoint);
        MutableComponent message;

        if (waypoint.name.isEmpty()) {
            message = sender == null
                    ? Component.translatable("minimap.waypointShare.chat.sharedCoordinateUnknown", dimension)
                    : Component.translatable("minimap.waypointShare.chat.sharedCoordinate", Component.literal(sender).withColor(SHARE_NAME_COLOR), dimension);
        } else {
            Component name = Component.literal(waypoint.name).withColor(SHARE_NAME_COLOR);
            message = sender == null
                    ? Component.translatable("minimap.waypointShare.chat.sharedUnknown", name, dimension)
                    : Component.translatable("minimap.waypointShare.chat.shared", Component.literal(sender).withColor(SHARE_NAME_COLOR), name, dimension);
        }

        return message.withColor(SHARE_TEXT_COLOR)
                .append(" ")
                .append(buildShareButton("minimap.waypointShare.chat.add", "minimap.waypointShare.chat.addTooltip", ADD_WAYPOINT_COMMAND + dataPart, SHARE_ADD_COLOR, waypoint))
                .append(" ")
                .append(buildShareButton("minimap.waypointShare.chat.highlight", "minimap.waypointShare.chat.highlightTooltip", HIGHLIGHT_WAYPOINT_COMMAND + dataPart, SHARE_HIGHLIGHT_COLOR, waypoint));
    }

    private static Component buildShareButton(String labelKey, String tooltipKey, String command, int color, Waypoint waypoint) {
        Component tooltip = Component.translatable(tooltipKey).append("\n").append(Component.literal(getSharedCoordinates(waypoint)).withColor(SHARE_TEXT_COLOR));

        return Component.empty()
                .append("[")
                .append(Component.translatable(labelKey))
                .append("]")
                .withStyle(style -> style
                        .withColor(color)
                        .withClickEvent(new ClickEvent.RunCommand("/" + command))
                        .withHoverEvent(new HoverEvent.ShowText(tooltip)));
    }

    private static Component getSharedDimensionName(Waypoint waypoint) {
        MutableComponent dimensionNames = Component.empty();
        boolean firstDimension = true;

        for (DimensionContainer dimension : waypoint.dimensions) {
            if (!firstDimension) {
                dimensionNames.append(", ");
            }

            dimensionNames.append(getDimensionName(dimension));
            firstDimension = false;
        }

        return dimensionNames;
    }

    /**
     * Names the dimension the way the game itself does. Vanilla has no translations for the dimensions as such, so the
     * three vanilla ones borrow the keys their names are shown under, and modded dimensions use the key mods commonly
     * ship for theirs. Whatever has no translation falls back to its prettified identifier.
     */
    private static Component getDimensionName(DimensionContainer dimension) {
        Identifier identifier = dimension.Identifier;
        if (identifier == null) {
            return Component.literal(dimension.getDisplayName());
        }

        String key = VANILLA_DIMENSION_NAME_KEYS.get(identifier);
        if (key == null) {
            key = "dimension." + identifier.getNamespace() + "." + identifier.getPath();
        }

        return Component.translatableWithFallback(key, dimension.getDisplayName());
    }

    private static String getSharedCoordinates(Waypoint waypoint) {
        double dimensionScale = !waypoint.dimensions.isEmpty() && waypoint.dimensions.first().type != null ? waypoint.dimensions.first().type.coordinateScale() : 1.0;
        return (int) (waypoint.x / dimensionScale) + ", " + waypoint.y + ", " + (int) (waypoint.z / dimensionScale);
    }

    private static ChatSource findChatSource(Component chat, int firstWaypointStart) {
        ChatSource source = findChatSourceFromContents(chat);
        if (source != null) {
            return source;
        }

        String prefix = chat.getString().substring(0, firstWaypointStart);
        String sender = findOnlinePlayerName(prefix);
        if (sender == null) {
            Matcher nameMatcher = CHAT_NAME_PATTERN.matcher(prefix);
            sender = nameMatcher.find() ? nameMatcher.group(1) : null;
        }

        return sender == null ? null : new ChatSource(sender, "");
    }

    private static ChatSource findChatSourceFromContents(Component component) {
        if (component.getContents() instanceof TranslatableContents contents && (contents.getKey().startsWith("chat.type.") || contents.getKey().startsWith("commands.message.display"))) {
            String sender = null;
            String comment = "";

            for (Object argument : contents.getArgs()) {
                if (argument instanceof Component argumentComponent) {
                    if (sender == null) {
                        sender = argumentComponent.getString();
                    } else {
                        comment = stripWaypointData(argumentComponent.getString());
                        break;
                    }
                }
            }

            if (sender != null && !sender.isEmpty()) {
                return new ChatSource(sender, comment);
            }
        }

        for (Component sibling : component.getSiblings()) {
            ChatSource source = findChatSourceFromContents(sibling);
            if (source != null) {
                return source;
            }
        }

        return null;
    }

    private static String findOnlinePlayerName(String text) {
        ClientPacketListener connection = VoxelConstants.getMinecraft().getConnection();
        if (connection == null) {
            return null;
        }

        String longestMatch = null;
        for (PlayerInfo playerInfo : connection.getOnlinePlayers()) {
            String name = playerInfo.getProfile().name();
            if (!name.isEmpty() && text.contains(name) && (longestMatch == null || name.length() > longestMatch.length())) {
                longestMatch = name;
            }
        }

        return longestMatch;
    }

    private static String stripWaypointData(String text) {
        String stripped = pattern.matcher(text).replaceAll("");
        stripped = xaeroPattern.matcher(stripped).replaceAll("");
        return stripped.replaceAll("\\s+", " ").trim();
    }

    private static Waypoint createWaypointFromXaero(String details) {
        Matcher matcher = xaeroPattern.matcher(details);
        return matcher.find() ? createWaypointFromXaero(matcher) : null;
    }

    private static Waypoint createWaypointFromXaero(Matcher matcher) {
        try {
            String name = matcher.group(1).trim();
            int x = Integer.parseInt(matcher.group(3));
            int y = Integer.parseInt(matcher.group(4));
            int z = Integer.parseInt(matcher.group(5));
            boolean enabled = !Boolean.parseBoolean(matcher.group(7));
            float[] color = getXaeroColor(Integer.parseInt(matcher.group(6)));

            TreeSet<DimensionContainer> dimensions = new TreeSet<>();
            DimensionContainer dimension = getXaeroDimension(getXaeroWorldField(matcher.group(9)));
            dimensions.add(dimension != null ? dimension : VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level()));

            double dimensionScale = dimensions.first().type != null ? dimensions.first().type.coordinateScale() : 1.0;
            String world = VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false);

            return new Waypoint(name, (int) (x * dimensionScale), (int) (z * dimensionScale), y, enabled, color[0], color[1], color[2], "", world, dimensions);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static float[] getXaeroColor(int colorIndex) {
        ChatFormatting[] formattings = ChatFormatting.values();
        TextColor color = colorIndex >= 0 && colorIndex < formattings.length ? TextColor.fromLegacyFormat(formattings[colorIndex]) : null;

        if (color == null) {
            return new float[]{generator.nextFloat(), generator.nextFloat(), generator.nextFloat()};
        }

        int rgb = color.getValue();
        return new float[]{(rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F};
    }

    private static DimensionContainer getXaeroDimension(String world) {
        String descriptor = normalizeXaeroWorld(world);
        if (descriptor.isEmpty()) {
            return null;
        }

        DimensionManager dimensionManager = VoxelConstants.getVoxelMapInstance().getDimensionManager();
        Identifier identifier = Identifier.tryParse(descriptor);
        if (identifier != null) {
            DimensionContainer dimension = dimensionManager.getDimensionContainerByIdentifier(identifier);
            if (dimension != null) {
                return dimension;
            }
        }

        for (DimensionContainer dimension : dimensionManager.getDimensions()) {
            if (dimension.Identifier != null && (descriptor.equals(dimension.Identifier.getPath()) || descriptor.endsWith("_" + dimension.Identifier.getPath()))) {
                return dimension;
            }
        }

        if (descriptor.contains("nether") || descriptor.equals("_1")) {
            return dimensionManager.getDimensionContainerByIdentifier(Level.NETHER.identifier());
        }

        if (descriptor.contains("end") || descriptor.equals("1")) {
            return dimensionManager.getDimensionContainerByIdentifier(Level.END.identifier());
        }

        if (descriptor.contains("overworld") || descriptor.equals("0")) {
            return dimensionManager.getDimensionContainerByIdentifier(Level.OVERWORLD.identifier());
        }

        return null;
    }

    private static String getXaeroWorldField(String trailingFields) {
        for (String field : trailingFields.split(":")) {
            if (!field.isEmpty()) {
                return field;
            }
        }

        return "";
    }

    private static String normalizeXaeroWorld(String world) {
        String descriptor = world.toLowerCase(Locale.ROOT).trim();

        int dimensionIndex = descriptor.indexOf("dim%");
        if (dimensionIndex != -1) {
            String[] parts = descriptor.substring(dimensionIndex + "dim%".length()).split("%", 2);
            descriptor = parts.length == 2 ? parts[0] + ":" + parts[1] : parts[0];
        }

        int pathIndex = descriptor.indexOf('/');
        if (pathIndex != -1) {
            descriptor = descriptor.substring(0, pathIndex);
        }

        descriptor = descriptor.replace("internal-", "").replace("internal_", "").replace("multiplayer_", "");
        descriptor = descriptor.replace("-waypoints", "").replace("_waypoints", "");
        return descriptor.replace('-', '_');
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

    public static boolean handleSharedWaypointCommand(String command) {
        if (command.startsWith(ADD_WAYPOINT_COMMAND)) {
            addSharedWaypoint(command.substring(ADD_WAYPOINT_COMMAND.length()));
            return true;
        }

        if (command.startsWith(HIGHLIGHT_WAYPOINT_COMMAND)) {
            highlightSharedWaypoint(command.substring(HIGHLIGHT_WAYPOINT_COMMAND.length()));
            return true;
        }

        return false;
    }

    private static Waypoint parseSharedWaypoint(String details) {
        return details.toLowerCase(Locale.ROOT).startsWith(XAERO_WAYPOINT_PREFIX) ? createWaypointFromXaero(details) : createWaypointFromChat(details);
    }

    private static void addSharedWaypoint(String details) {
        Waypoint sharedWaypoint = parseSharedWaypoint(details);
        if (sharedWaypoint == null) {
            return;
        }

        Waypoint existingWaypoint = findWaypointAt(sharedWaypoint);
        if (existingWaypoint != null) {
            VoxelConstants.getMinecraft().gui.setScreen(new GuiAddWaypoint(null, existingWaypoint, true));
        } else {
            VoxelConstants.getMinecraft().gui.setScreen(new GuiAddWaypoint(null, sharedWaypoint, false));
        }
    }

    private static void highlightSharedWaypoint(String details) {
        Waypoint sharedWaypoint = parseSharedWaypoint(details);
        if (sharedWaypoint == null) {
            return;
        }

        Waypoint existingWaypoint = findWaypointAt(sharedWaypoint);
        VoxelConstants.getVoxelMapInstance().getWaypointManager().setHighlightedWaypoint(existingWaypoint != null ? existingWaypoint : sharedWaypoint, false);
    }

    private static Waypoint findWaypointAt(Waypoint waypoint) {
        for (Waypoint existingWaypoint : VoxelConstants.getVoxelMapInstance().getWaypointManager().getWaypoints()) {
            if (waypoint.getX() == existingWaypoint.getX() && waypoint.getZ() == existingWaypoint.getZ()) {
                return existingWaypoint;
            }
        }

        return null;
    }

    public static void waypointClicked(String command) {
        boolean control = InputConstants.isKeyDown(VoxelConstants.getMinecraft().getWindow(), InputConstants.getKey("key.keyboard.left.control").getValue())
                || InputConstants.isKeyDown(VoxelConstants.getMinecraft().getWindow(), InputConstants.getKey("key.keyboard.right.control").getValue());
        String details = command.substring(NEW_WAYPOINT_COMMAND_LENGTH);

        if (control) {
            addSharedWaypoint(details);
        } else {
            highlightSharedWaypoint(details);
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
        VoxelConstants.getMinecraft().gui.setScreen(new GuiSelectPlayer(null, message, true));
    }

    public static void sendCoordinate(int x, int y, int z) {
        String message = String.format("[x:%s, y:%s, z:%s]", x, y, z);
        VoxelConstants.getMinecraft().gui.setScreen(new GuiSelectPlayer(null, message, false));
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
