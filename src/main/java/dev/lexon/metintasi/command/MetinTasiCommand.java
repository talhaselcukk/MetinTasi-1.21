package dev.lexon.metintasi.command;

import dev.lexon.metintasi.MetinTasiPlugin;
import dev.lexon.metintasi.gui.AdminGUI;
import dev.lexon.metintasi.gui.InputManager;
import dev.lexon.metintasi.manager.FloatingHologramManager;
import dev.lexon.metintasi.model.MetinStone;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MetinTasiCommand implements CommandExecutor, TabCompleter {

    private final MetinTasiPlugin plugin;
    private final AdminGUI adminGUI;

    public MetinTasiCommand(MetinTasiPlugin plugin) {
        this.plugin = plugin;
        this.adminGUI = new AdminGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "reload" -> handleReload(sender);
            case "admin" -> handleAdmin(sender);
            case "spawn" -> handleSpawn(sender, args);
            case "remove" -> handleRemove(sender);
            case "reset" -> handleReset(sender, args);
            case "list" -> handleList(sender);
            case "reward" -> handleReward(sender, args);
            case "hologram" -> handleHologram(sender, args);
            case "stone" -> handleStoneSub(sender, args);
            case "help" -> sendHelp(sender);
            default -> sender.sendMessage(plugin.getConfigManager().getMessage("unknown-command"));
        }

        return true;
    }

    private void handleReward(CommandSender sender, String[] args) {
        if (!sender.hasPermission("metintasi.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("usage-reward-additem"));
            return;
        }
        String a1 = args[1].toLowerCase(Locale.ROOT);
        if (a1.equals("additem")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("only-player"));
                return;
            }
            if (args.length < 3) {
                player.sendMessage(plugin.getConfigManager().getMessage("usage-reward-additem"));
                return;
            }
            try {
                double chance = Double.parseDouble(args[2]);
                plugin.getRewardManager().addItemRewardFromHand(player, chance);
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
            }
            return;
        }
        if (a1.equals("move")) {
            if (args.length < 4) {
                sender.sendMessage(plugin.getConfigManager().getMessage("usage-reward-move"));
                return;
            }
            String id = args[2];
            String dir = args[3].toLowerCase(Locale.ROOT);
            if (plugin.getRewardManager().getRewardById(id) == null) {
                sender.sendMessage(plugin.getConfigManager().getMessage("reward-move-unknown"));
                return;
            }
            if (dir.equals("up")) {
                plugin.getRewardManager().moveReward(id, true);
            } else if (dir.equals("down")) {
                plugin.getRewardManager().moveReward(id, false);
            } else {
                sender.sendMessage(plugin.getConfigManager().getMessage("usage-reward-move"));
                return;
            }
            sender.sendMessage(plugin.getConfigManager().getMessage("reward-moved"));
            return;
        }
        sender.sendMessage(plugin.getConfigManager().getMessage("usage-reward-additem"));
    }

    private void applyAdditemTarget(Player player, double chance, String rawTarget) {
        String t = rawTarget.trim();
        if (t.equalsIgnoreCase("genel")
                || t.equalsIgnoreCase("global")
                || t.equalsIgnoreCase("tum")
                || t.equals("*")) {
            plugin.getRewardManager().addItemRewardFromHand(player, chance, null);
            return;
        }
        MetinStone stone = plugin.getMetinStoneManager().findStoneByDisplayName(t);
        if (stone == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("reward-additem-stone-not-found"));
            return;
        }
        plugin.getRewardManager().addItemRewardFromHand(player, chance, stone);
    }

    private void handleHologram(CommandSender sender, String[] args) {
        if (!sender.hasPermission("metintasi.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("only-player"));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(plugin.getConfigManager().getMessage("usage-hologram-here"));
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        if (sub.equals("here")) {
            String name = joinArgs(args, 2);
            MetinStone stone = plugin.getMetinStoneManager().findStoneByDisplayName(name);
            if (stone == null) {
                String msg = plugin.getConfigManager().getMessage("stone-not-found-name");
                player.sendMessage(msg.replace("<isim>", name));
                return;
            }
            String holoId = FloatingHologramManager.rankingHologramId(stone.getId());
            org.bukkit.Location at = player.getLocation().clone();
            plugin.getFloatingHologramManager().createOrMoveAtFeet(holoId, at, stone);
            player.sendMessage(plugin.getConfigManager().getMessage("hologram-here-set"));
            return;
        }
        if (sub.equals("reset")) {
            if (args.length < 3) {
                player.sendMessage(plugin.getConfigManager().getMessage("usage-hologram-reset"));
                return;
            }
            String name = joinArgs(args, 2);
            MetinStone stone = plugin.getMetinStoneManager().findStoneByDisplayName(name);
            if (stone == null) {
                String msg = plugin.getConfigManager().getMessage("stone-not-found-name");
                player.sendMessage(msg.replace("<isim>", name));
                return;
            }
            String holoId = FloatingHologramManager.rankingHologramId(stone.getId());
            boolean removed = plugin.getFloatingHologramManager().delete(holoId);
            if (removed) {
                player.sendMessage(plugin.getConfigManager().getMessage("hologram-reset"));
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("hologram-reset-none"));
            }
            return;
        }
        player.sendMessage(plugin.getConfigManager().getMessage("usage-hologram-here"));
    }

    private void handleStoneSub(CommandSender sender, String[] args) {
        if (!sender.hasPermission("metintasi.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("health")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("usage-stone-health"));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(plugin.getConfigManager().getMessage("usage-stone-health"));
            return;
        }
        List<String> tail = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            tail.add(args[i]);
        }
        if (tail.size() < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("usage-stone-health"));
            return;
        }

        String last = tail.get(tail.size() - 1);
        if (!isNumber(last)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("usage-stone-health"));
            return;
        }

        double maxHp;
        double curHp;
        String name;
        if (tail.size() >= 3 && isNumber(tail.get(tail.size() - 2))) {
            maxHp = Double.parseDouble(tail.get(tail.size() - 2));
            curHp = Double.parseDouble(last);
            name = joinList(tail, 0, tail.size() - 2);
        } else {
            maxHp = Double.parseDouble(last);
            curHp = maxHp;
            name = joinList(tail, 0, tail.size() - 1);
        }

        MetinStone stone = plugin.getMetinStoneManager().findStoneByDisplayName(name);
        if (stone == null) {
            String msg = plugin.getConfigManager().getMessage("stone-not-found-name");
            sender.sendMessage(msg.replace("<isim>", name));
            return;
        }

        stone.setMaxHealth(maxHp);
        stone.setCurrentHealth(Math.min(curHp, maxHp));
        plugin.getMetinStoneManager().saveStone(stone);
        plugin.getHologramManager().updateHologram(stone);

        String out = plugin.getConfigManager().getMessage("stone-health-set")
                .replace("<isim>", stone.getDisplayName())
                .replace("<max>", String.format("%.1f", maxHp))
                .replace("<simdi>", String.format("%.1f", stone.getCurrentHealth()));
        sender.sendMessage(out);
    }

    private static boolean isNumber(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String joinArgs(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private static String joinList(List<String> parts, int from, int toExclusive) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < toExclusive; i++) {
            if (i > from) sb.append(' ');
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("metintasi.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        plugin.reloadPlugin();
        sender.sendMessage(plugin.getConfigManager().getMessage("plugin-reloaded"));
    }

    private void handleAdmin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("only-player"));
            return;
        }

        if (!player.hasPermission("metintasi.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        adminGUI.openMainMenu(player);
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("only-player"));
            return;
        }

        if (!player.hasPermission("metintasi.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        Material blockType = plugin.getConfigManager().getDefaultBlockType();
        String stoneName = plugin.getConfigManager().getDefaultStoneDisplayName();
        if (args.length >= 2) {
            try {
                blockType = Material.valueOf(args[1].toUpperCase(Locale.ROOT));
                if (!blockType.isBlock()) {
                    player.sendMessage(plugin.getConfigManager().getMessage("not-a-block"));
                    return;
                }
            } catch (IllegalArgumentException e) {
                player.sendMessage(plugin.getConfigManager().getMessage("invalid-block-type"));
                return;
            }
        }
        if (args.length >= 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (i > 2) sb.append(' ');
                sb.append(args[i]);
            }
            stoneName = sb.toString();
        }

        org.bukkit.Location loc = player.getLocation().getBlock().getLocation();
        plugin.getMetinStoneManager().createStone(loc, blockType, stoneName);
        player.sendMessage(plugin.getConfigManager().getMessage("stone-created"));
    }

    private void handleRemove(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("only-player"));
            return;
        }

        if (!player.hasPermission("metintasi.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        org.bukkit.block.Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-target-block"));
            return;
        }

        MetinStone stone = plugin.getMetinStoneManager().getStoneAt(target.getLocation());
        if (stone == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-metin-stone"));
            return;
        }

        plugin.getMetinStoneManager().deleteStone(stone);
        player.sendMessage(plugin.getConfigManager().getMessage("stone-removed"));
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("metintasi.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        if (args.length >= 2 && (args[1].equalsIgnoreCase("all")
                || args[1].equalsIgnoreCase("tum") || args[1].equalsIgnoreCase("hepsi"))) {
            int count = 0;
            for (MetinStone stone : new ArrayList<>(plugin.getMetinStoneManager().getAllStones())) {
                plugin.getMetinStoneManager().resetStone(stone);
                count++;
            }
            sender.sendMessage(plugin.getConfigManager().getMessage("all-stones-reset")
                    .replace("<adet>", String.valueOf(count)));
            return;
        }

        MetinStone stone;
        if (args.length >= 2) {
            String name = joinArgs(args, 1);
            stone = plugin.getMetinStoneManager().findStoneByDisplayName(name);
            if (stone == null) {
                sender.sendMessage(plugin.getConfigManager().getMessage("stone-not-found-name")
                        .replace("<isim>", name));
                return;
            }
        } else if (sender instanceof Player player) {
            org.bukkit.block.Block target = player.getTargetBlockExact(5);
            stone = target == null ? null : plugin.getMetinStoneManager().getStoneAt(target.getLocation());
            if (stone == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("reset-target-required"));
                return;
            }
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessage("usage-reset"));
            return;
        }

        plugin.getMetinStoneManager().resetStone(stone);
        sender.sendMessage(plugin.getConfigManager().getMessage("stone-reset")
                .replace("<isim>", stone.getDisplayName()));
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("metintasi.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        var stones = plugin.getMetinStoneManager().getAllStones();
        if (stones.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-stones"));
            return;
        }

        sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize("&6--- Metin Taşları (" + stones.size() + ") ---"));
        for (MetinStone stone : stones) {
            String status = stone.isBroken() ? "&cKırık" : "&aCan: " + String.format("%.0f", stone.getCurrentHealth());
            sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize(
                    "&7- &f" + stone.getDisplayName() + " &8| &7" + stone.getLocationKey() + " &7| " + status + " &7| &e" + stone.getBlockType().name()
            ));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize("&6&m──────────&r &eMetin Taşı &6&m──────────"));
        sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize("&e/metintasi help &7- Bu menü"));
        sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize("&e/metintasi reload &7- Yapılandırmayı yenile"));
        sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize("&e/metintasi admin &7- Panel (para ödülleri, taşlar)"));
        sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize("&e/metintasi spawn [blok] [isim] &7- Taş oluştur"));
        sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize("&e/metintasi remove &7- Baktığın taşı kaldır"));
        sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize("&e/metintasi reset [taş adı|all] &7- Taşı tam canla yeniden aç"));
        sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize("&e/metintasi list &7- Taşları listele"));
        sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize("&e/metintasi reward additem <şans> [taş|genel] &7- Eşya ödülü; hedef yoksa sohbette sorulur"));
        sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize("&e/metintasi reward move <id> up|down &7- Ödül sırası"));
        sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize("&e/metintasi hologram here <taş adı> &7- Sıralama hologramı (ayak hizası)"));
        sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize("&e/metintasi hologram reset <taş adı> &7- Sıralama hologramını kaldır"));
        sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize("&e/metintasi stone health <taş> <max> [şimdi] &7- Can ayarla"));
        sender.sendMessage(dev.lexon.metintasi.util.ColorUtil.colorize("&6&m────────────────────────────────"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of(
                    "help", "reload", "admin", "spawn", "remove", "reset", "list",
                    "reward", "hologram", "stone"
            ));
            String input = args[0].toLowerCase(Locale.ROOT);
            for (String sub : subs) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            String input = args[1].toUpperCase(Locale.ROOT);
            for (Material mat : Material.values()) {
                if (mat.isBlock() && mat.name().startsWith(input)) {
                    completions.add(mat.name().toLowerCase(Locale.ROOT));
                    if (completions.size() >= 20) break;
                }
            }
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("reset")) {
            if (args.length == 2 && "all".startsWith(args[1].toLowerCase(Locale.ROOT))) completions.add("all");
            addStoneNameCompletions(completions, args[args.length - 1]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reward")) {
            for (String s : List.of("additem", "move")) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) completions.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("reward") && args[1].equalsIgnoreCase("move")) {
            for (var r : plugin.getRewardManager().getRewards()) {
                completions.add(r.getId());
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("reward") && args[1].equalsIgnoreCase("move")) {
            for (String s : List.of("up", "down")) {
                if (s.startsWith(args[3].toLowerCase(Locale.ROOT))) completions.add(s);
            }
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("reward") && args[1].equalsIgnoreCase("additem")) {
            if (args.length == 4) {
                String pfx = args[3].toLowerCase(Locale.ROOT);
                for (String g : List.of("genel", "global", "tum", "*")) {
                    if (pfx.isEmpty() || g.startsWith(pfx)) {
                        completions.add(g);
                    }
                }
                addStoneNameCompletions(completions, args[3]);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("hologram")) {
            for (String s : List.of("here", "reset")) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) completions.add(s);
            }
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("hologram")
                && (args[1].equalsIgnoreCase("here") || args[1].equalsIgnoreCase("reset"))) {
            addStoneNameCompletions(completions, args[args.length - 1]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("stone")) {
            if ("health".startsWith(args[1].toLowerCase(Locale.ROOT))) completions.add("health");
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("stone") && args[1].equalsIgnoreCase("health")) {
            addStoneNameCompletions(completions, args[args.length - 1]);
        }

        return completions;
    }

    private void addStoneNameCompletions(List<String> completions, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        for (MetinStone stone : plugin.getMetinStoneManager().getAllStones()) {
            String n = stone.getDisplayName();
            if (p.isEmpty() || n.toLowerCase(Locale.ROOT).startsWith(p)) {
                completions.add(n);
            }
        }
    }
}
