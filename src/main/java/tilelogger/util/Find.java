package tilelogger.util;

import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.Administration;

import static arc.util.Strings.*;
import static mindustry.Vars.netServer;

public class Find {
    public static Player player(String nameOrId) {
        Player player = playerById(nameOrId);
        return (player != null) ? player : playerByName(nameOrId);
    }

    public static Player playerById(String id) {
        return id.startsWith("#") ? Groups.player.getByID(parseInt(id.substring(1))) : null;
    }

    public static Player playerByName(String name) {
        return Groups.player.find(player -> deepEquals(player.name, name));
    }

    public static Player playerByUuid(String uuid) {
        return Groups.player.find(player -> player.uuid().equals(uuid));
    }

    public static boolean deepEquals(String first, String second) {
        first = stripColors(stripGlyphs(first));
        second = stripColors(stripGlyphs(second));
        return first.equalsIgnoreCase(second) || first.toLowerCase().contains(second.toLowerCase());
    }
}
