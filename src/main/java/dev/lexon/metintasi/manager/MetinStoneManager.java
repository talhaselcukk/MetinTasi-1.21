package dev.lexon.metintasi.manager;

import dev.lexon.metintasi.MetinTasiPlugin;
import dev.lexon.metintasi.model.MetinStone;
import dev.lexon.metintasi.model.PendingStoneChat;
import dev.lexon.metintasi.util.ReactivationAnnouncements;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MetinStoneManager {

    private static final ZoneId TURKEY_ZONE = ZoneId.of("Europe/Istanbul");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final MetinTasiPlugin plugin;
    private final Map<String, MetinStone> stones;
    private final Map<UUID, PendingStoneChat> pendingChats;
    private int timeoutTaskId = -1;
    private int reactivationTaskId = -1;
    private int brokenHoloTaskId = -1;
    private final Set<String> reactivationTriggeredToday = new HashSet<>();
    private String reactivationLastDate = "";

    public MetinStoneManager(MetinTasiPlugin plugin) {
        this.plugin = plugin;
        this.stones = new ConcurrentHashMap<>();
        this.pendingChats = new ConcurrentHashMap<>();
    }

    private File getStonesDirectory() {
        File dir = new File(plugin.getDataFolder(), "stones");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private File getStoneFile(UUID id) {
        return new File(getStonesDirectory(), id.toString() + ".yml");
    }

    public void startTasks() {
        startTimeoutChecker();
        startReactivationChecker();
        startBrokenHologramTicker();
    }

    public void stopTasks() {
        if (timeoutTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(timeoutTaskId);
            timeoutTaskId = -1;
        }
        if (reactivationTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(reactivationTaskId);
            reactivationTaskId = -1;
        }
        if (brokenHoloTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(brokenHoloTaskId);
            brokenHoloTaskId = -1;
        }
    }

    /**
     * Tüm taşlar arasında kalıcı numara (UUID sıralı), hologram &lt;tas_no&gt; için.
     */
    public int getStableStoneNumber(MetinStone stone) {
        if (stone == null) return 0;
        List<MetinStone> all = new ArrayList<>(stones.values());
        all.sort(Comparator.comparing(s -> s.getId().toString()));
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(stone.getId())) {
                return i + 1;
            }
        }
        return 0;
    }

    public void addPlayerToChatMode(UUID playerId, Location placement) {
        pendingChats.put(playerId, new PendingStoneChat(placement.clone()));
    }

    public boolean isPlayerInChatMode(UUID playerId) {
        return pendingChats.containsKey(playerId);
    }

    public void removePlayerFromChatMode(UUID playerId) {
        pendingChats.remove(playerId);
    }

    public PendingStoneChat getPendingChat(UUID playerId) {
        return pendingChats.get(playerId);
    }

    public boolean isMetinStoneBlock(Location location) {
        return getStoneAt(location) != null;
    }

    public MetinStone createStone(Location location, Material blockType, String displayName) {
        return createStone(location, blockType, displayName, plugin.getConfigManager().getDefaultHealth());
    }

    public MetinStone createStone(Location location, Material blockType, String displayName, double maxHealth) {
        double health = maxHealth > 0 ? maxHealth : plugin.getConfigManager().getDefaultHealth();
        String name = displayName;
        if (name == null || name.isBlank()) {
            name = plugin.getConfigManager().getDefaultStoneDisplayName();
        }
        MetinStone stone = new MetinStone(location, blockType, health, name);

        double defHours = plugin.getConfigManager().getDefaultReactivationHoursForNewStones();
        if (defHours > 0) {
            stone.setReactivationIntervalMinutes((int) Math.round(defHours * 60.0));
        }

        location.getBlock().setType(blockType);

        stones.put(stone.getLocationKey(), stone);

        plugin.getHologramManager().createHologram(stone);
        saveStone(stone);

        plugin.debug("Metin Taşı oluşturuldu: " + stone.getLocationKey());

        Location l = stone.getLocation();
        String wn = l.getWorld() != null ? l.getWorld().getName() : "?";
        plugin.getWebhookManager().stoneSpawned(stone.getDisplayName(), wn,
                l.getBlockX(), l.getBlockY(), l.getBlockZ());

        return stone;
    }

    public void spawnAtConfigLocation() {
        if (!plugin.getConfigManager().isSpawnEnabled()) return;
        World world = Bukkit.getWorld(plugin.getConfigManager().getSpawnWorld());
        if (world == null) {
            plugin.getLogger().warning("Spawn dünyası bulunamadı: " + plugin.getConfigManager().getSpawnWorld());
            return;
        }
        Location loc = new Location(world,
                plugin.getConfigManager().getSpawnX(),
                plugin.getConfigManager().getSpawnY(),
                plugin.getConfigManager().getSpawnZ());
        createStone(loc.getBlock().getLocation(),
                plugin.getConfigManager().getDefaultBlockType(),
                plugin.getConfigManager().getDefaultStoneDisplayName());
    }

    public MetinStone getStoneAt(Location location) {
        if (location == null || location.getWorld() == null) return null;
        String key = location.getWorld().getName() + ":" +
                location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
        return stones.get(key);
    }

    public MetinStone getStoneById(UUID id) {
        if (id == null) return null;
        for (MetinStone s : stones.values()) {
            if (s.getId().equals(id)) return s;
        }
        return null;
    }

    public MetinStone findStoneByDisplayName(String name) {
        if (name == null || name.isBlank()) return null;
        String t = name.trim();
        for (MetinStone s : stones.values()) {
            if (s.getDisplayName().equalsIgnoreCase(t)) return s;
        }
        return null;
    }

    public Collection<MetinStone> getAllStones() {
        return stones.values();
    }

    public void markBroken(MetinStone stone, UUID breakerId) {
        if (stone == null) return;
        stone.setLastBreakerId(breakerId);
        stone.recordBreak(breakerId);
        saveStone(stone);
        plugin.getHologramManager().updateHologram(stone);
        if (plugin.getFloatingHologramManager() != null) {
            plugin.getFloatingHologramManager().refreshForStone(stone.getId());
        }
        Location l = stone.getLocation();
        String wn = l.getWorld() != null ? l.getWorld().getName() : "?";
        String breakerName = breakerId != null ? Optional.ofNullable(Bukkit.getOfflinePlayer(breakerId).getName()).orElse("?") : "?";
        plugin.getWebhookManager().stoneBroken(stone.getDisplayName(), breakerName, wn,
                l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    public void reactivateStone(MetinStone stone) {
        if (stone == null || !stone.isBroken()) return;
        stone.reactivate();
        if (stone.getLocation().getWorld() != null) {
            stone.getLocation().getBlock().setType(stone.getBlockType());
        }
        saveStone(stone);
        plugin.getHologramManager().updateHologram(stone);
        if (plugin.getFloatingHologramManager() != null) {
            plugin.getFloatingHologramManager().refreshForStone(stone.getId());
        }
        Location l = stone.getLocation();
        String wn = l.getWorld() != null ? l.getWorld().getName() : "?";
        plugin.getWebhookManager().stoneReactivated(stone.getDisplayName(), wn,
                l.getBlockX(), l.getBlockY(), l.getBlockZ());

        ReactivationAnnouncements.broadcast(plugin, stone);
    }

    /** Yönetici sıfırlaması: taş kırık veya aktif olsa da tam canla yeniden açar. */
    public void resetStone(MetinStone stone) {
        if (stone == null) return;
        stone.reactivate();
        if (stone.getLocation().getWorld() != null) {
            stone.getLocation().getBlock().setType(stone.getBlockType());
        }
        saveStone(stone);
        plugin.getHologramManager().updateHologram(stone);
        if (plugin.getFloatingHologramManager() != null) {
            plugin.getFloatingHologramManager().refreshForStone(stone.getId());
        }
    }

    public void deleteStone(MetinStone stone) {
        if (stone == null) return;
        stones.remove(stone.getLocationKey());
        plugin.getHologramManager().removeHologram(stone);
        if (stone.getLocation().getWorld() != null) {
            stone.getLocation().getBlock().setType(Material.AIR);
        }
        File f = getStoneFile(stone.getId());
        if (f.exists() && !f.delete()) {
            plugin.getLogger().warning("Taş dosyası silinemedi: " + f.getName());
        }
        plugin.debug("Metin Taşı tamamen silindi: " + stone.getLocationKey());
        if (plugin.getFloatingHologramManager() != null) {
            plugin.getFloatingHologramManager().removeBoundToStone(stone.getId());
        }
    }

    public void loadStones() {
        stones.clear();
        File dir = getStonesDirectory();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    MetinStone stone = loadStoneFromFile(file);
                    if (stone != null) {
                        stones.put(stone.getLocationKey(), stone);
                        if (stone.getLocation().getWorld() != null && !stone.isBroken()) {
                            stone.getLocation().getBlock().setType(stone.getBlockType());
                        }
                        if (stone.isBroken()) {
                            plugin.getHologramManager().createBrokenHologram(stone);
                        } else {
                            plugin.getHologramManager().createHologram(stone);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Taş yüklenemedi " + file.getName() + ": " + e.getMessage());
                }
            }
        }
        tryLoadLegacyStonesYml();
        plugin.getLogger().info(stones.size() + " Metin Taşı yüklendi.");
    }

    private void tryLoadLegacyStonesYml() {
        if (!stones.isEmpty()) return;
        File legacy = new File(plugin.getDataFolder(), "stones.yml");
        if (!legacy.exists()) return;
        plugin.getLogger().info("Eski stones.yml bulundu; stones/ boş, legacy okunuyor.");
        FileConfiguration c = YamlConfiguration.loadConfiguration(legacy);
        ConfigurationSection root = c.getConfigurationSection("stones");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                ConfigurationSection sec = root.getConfigurationSection(key);
                if (sec == null) continue;
                MetinStone stone = loadStoneFromSection(sec, key);
                if (stone != null) {
                    stones.put(stone.getLocationKey(), stone);
                    if (stone.getLocation().getWorld() != null && !stone.isBroken()) {
                        stone.getLocation().getBlock().setType(stone.getBlockType());
                    }
                    if (stone.isBroken()) {
                        plugin.getHologramManager().createBrokenHologram(stone);
                    } else {
                        plugin.getHologramManager().createHologram(stone);
                    }
                    saveStone(stone);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Legacy taş atlandı: " + key + " " + e.getMessage());
            }
        }
    }

    private MetinStone loadStoneFromFile(File file) throws Exception {
        FileConfiguration c = YamlConfiguration.loadConfiguration(file);
        UUID id;
        try {
            String idStr = c.getString("id");
            id = idStr != null ? UUID.fromString(idStr) : UUID.fromString(file.getName().replace(".yml", ""));
        } catch (IllegalArgumentException e) {
            return null;
        }
        return buildStoneFromConfig(c, id);
    }

    private MetinStone loadStoneFromSection(ConfigurationSection sec, String key) {
        UUID id;
        try {
            id = UUID.fromString(sec.getString("id", key));
        } catch (IllegalArgumentException e) {
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
        return buildStoneFromConfig(sec, id);
    }

    private MetinStone buildStoneFromConfig(ConfigurationSection c, UUID id) {
        String worldName = c.getString("world");
        World world = worldName != null ? Bukkit.getWorld(worldName) : null;
        if (world == null) {
            plugin.getLogger().warning("Dünya yok, taş atlanıyor: " + worldName);
            return null;
        }
        int x = c.getInt("x");
        int y = c.getInt("y");
        int z = c.getInt("z");
        Location loc = new Location(world, x, y, z);

        Material blockType = Material.STONE;
        String bt = c.getString("block-type", "STONE");
        try {
            blockType = Material.valueOf(bt.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
        }

        double maxHealth = c.getDouble("max-health", plugin.getConfigManager().getDefaultHealth());
        double currentHealth = c.getDouble("current-health", maxHealth);
        boolean broken = c.getBoolean("broken", false);
        long brokenTime = c.getLong("broken-time", 0);
        String displayName = c.getString("display-name", plugin.getConfigManager().getDefaultStoneDisplayName());

        List<String> holoOverride = c.getStringList("hologram-lines");
        List<String> brokenHoloOverride = c.getStringList("broken-hologram-lines");
        boolean lb = c.getBoolean("leaderboard-enabled", true);

        Map<UUID, Integer> breakCounts = new HashMap<>();
        ConfigurationSection bc = c.getConfigurationSection("break-counts");
        if (bc != null) {
            for (String bk : bc.getKeys(false)) {
                try {
                    breakCounts.put(UUID.fromString(bk), bc.getInt(bk));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        MetinStone stone = new MetinStone(id, loc, blockType, maxHealth, currentHealth, broken, brokenTime,
                displayName,
                holoOverride.isEmpty() ? null : holoOverride,
                brokenHoloOverride.isEmpty() ? null : brokenHoloOverride,
                lb, breakCounts);

        if (c.isDouble("hologram-offset-y") || c.isInt("hologram-offset-y")) {
            stone.setHologramOffsetYOverride(c.getDouble("hologram-offset-y"));
        }

        plugin.getRewardManager().loadStoneRewardsFromSection(c.getConfigurationSection("stone-rewards"), stone.getStoneRewards());

        String lastBreakerStr = c.getString("last-breaker");
        if (lastBreakerStr != null && !lastBreakerStr.isBlank()) {
            try {
                stone.setLastBreakerId(UUID.fromString(lastBreakerStr.trim()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        double reacHours = c.getDouble("reactivation-interval-hours", 0);
        if (reacHours > 0) {
            stone.setReactivationIntervalMinutes((int) Math.round(reacHours * 60.0));
        } else if (c.contains("reactivation-interval-minutes")) {
            int rm = c.getInt("reactivation-interval-minutes");
            if (rm > 0) {
                stone.setReactivationIntervalMinutes(rm);
            }
        }

        return stone;
    }

    public void reloadStones() {
        plugin.getHologramManager().removeAll();
        loadStones();
    }

    public void saveAllStones() {
        for (MetinStone stone : stones.values()) {
            saveStone(stone);
        }
    }

    public void saveStone(MetinStone stone) {
        if (stone == null) return;
        File f = getStoneFile(stone.getId());
        YamlConfiguration c = new YamlConfiguration();
        Location l = stone.getLocation();
        c.set("id", stone.getId().toString());
        c.set("world", l.getWorld() != null ? l.getWorld().getName() : "world");
        c.set("x", l.getBlockX());
        c.set("y", l.getBlockY());
        c.set("z", l.getBlockZ());
        c.set("block-type", stone.getBlockType().name());
        c.set("max-health", stone.getMaxHealth());
        c.set("current-health", stone.getCurrentHealth());
        c.set("broken", stone.isBroken());
        c.set("broken-time", stone.getBrokenTime());
        c.set("display-name", stone.getDisplayName());
        c.set("leaderboard-enabled", stone.isLeaderboardEnabled());
        if (stone.getHologramLinesOverride() != null) {
            c.set("hologram-lines", stone.getHologramLinesOverride());
        }
        if (stone.getBrokenHologramLinesOverride() != null) {
            c.set("broken-hologram-lines", stone.getBrokenHologramLinesOverride());
        }
        if (stone.getHologramOffsetYOverride() != null) {
            c.set("hologram-offset-y", stone.getHologramOffsetYOverride());
        }
        for (Map.Entry<UUID, Integer> e : stone.getBreakCounts().entrySet()) {
            c.set("break-counts." + e.getKey().toString(), e.getValue());
        }
        if (stone.getLastBreakerId() != null) {
            c.set("last-breaker", stone.getLastBreakerId().toString());
        } else {
            c.set("last-breaker", null);
        }
        c.set("reactivation-interval-hours", null);
        c.set("reactivation-interval-minutes", null);
        if (stone.getReactivationIntervalMinutes() != null && stone.getReactivationIntervalMinutes() > 0) {
            int m = stone.getReactivationIntervalMinutes();
            if (m % 60 == 0) {
                c.set("reactivation-interval-hours", m / 60);
            } else {
                c.set("reactivation-interval-minutes", m);
            }
        }
        plugin.getRewardManager().writeStoneRewardsToConfig(c, stone);
        try {
            c.save(f);
        } catch (IOException e) {
            plugin.getLogger().severe("Taş kaydedilemedi " + f.getName() + ": " + e.getMessage());
        }
    }

    private void startTimeoutChecker() {
        timeoutTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            long timeoutMs = plugin.getConfigManager().getBreakerTimeoutSeconds() * 1000L;
            for (MetinStone stone : stones.values()) {
                if (stone.isBroken()) continue;
                if (stone.getCurrentBreaker() != null && stone.hasBreakerTimedOut(timeoutMs)) {
                    stone.releaseBreaker();
                    plugin.getHologramManager().updateHologram(stone);
                    plugin.debug("Kıran süresi doldu: " + stone.getLocationKey());
                }
            }
        }, 20L, 20L);
    }

    private void startReactivationChecker() {
        reactivationTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            ZonedDateTime now = ZonedDateTime.now(TURKEY_ZONE);
            String currentDate = now.toLocalDate().toString();
            String currentTime = now.format(TIME_FORMAT);
            if (!currentDate.equals(reactivationLastDate)) {
                reactivationTriggeredToday.clear();
                reactivationLastDate = currentDate;
            }

            String mode = plugin.getConfigManager().getReactivationMode();
            if (mode.equalsIgnoreCase("interval")) {
                long t = System.currentTimeMillis();
                for (MetinStone stone : new ArrayList<>(stones.values())) {
                    if (!stone.isBroken()) continue;
                    if (stone.getBrokenTime() <= 0) continue;
                    long intervalMs = plugin.getConfigManager().getReactivationIntervalMinutesForStone(stone) * 60L * 1000L;
                    if (intervalMs <= 0) continue;
                    if (t - stone.getBrokenTime() >= intervalMs) {
                        reactivateStone(stone);
                    }
                }
            } else {
                List<String> times = plugin.getConfigManager().getReactivationTimes();
                for (String time : times) {
                    String trimmed = time.trim();
                    if (trimmed.equals(currentTime) && !reactivationTriggeredToday.contains(trimmed)) {
                        reactivationTriggeredToday.add(trimmed);
                        for (MetinStone stone : new ArrayList<>(stones.values())) {
                            if (stone.isBroken()) {
                                reactivateStone(stone);
                            }
                        }
                        break;
                    }
                }
            }
        }, 20L, 20L);
    }

    private void startBrokenHologramTicker() {
        if (brokenHoloTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(brokenHoloTaskId);
            brokenHoloTaskId = -1;
        }
        int period = plugin.getConfigManager().getBrokenHologramRefreshTicks();
        brokenHoloTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin,
                () -> plugin.getHologramManager().tickBrokenHolograms(),
                period, period);
    }

}
