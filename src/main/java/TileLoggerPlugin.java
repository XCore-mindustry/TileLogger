package main.java;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.gen.RotateBlockCallPacket;
import mindustry.io.JsonIO;
import mindustry.mod.Plugin;
import mindustry.net.Administration.PlayerInfo;
import mindustry.world.Tile;
import org.xcore.plugin.modules.Database;
import org.xcore.plugin.modules.votes.VoteKick;
import org.xcore.plugin.utils.models.PlayerData;

import static org.xcore.plugin.commands.ClientCommands.register;
import static useful.Bundle.bundled;

@SuppressWarnings("unused")
public class TileLoggerPlugin extends Plugin {
    @Override
    public void init() {
        VoteKick.setOnKick(player -> {/*TODO: rollback on kick */});

        Events.on(EventType.BlockBuildBeginEvent.class, event -> event.tile.block().iterateTaken(event.tile.x, event.tile.y, (x, y) ->
                TileLogger.destroy(Vars.world.tile(x, y), TileLogger.unitToPlayerInfo(event.unit))));

        Events.on(EventType.BlockBuildEndEvent.class, event -> {
            if (event.breaking) return; // already handled by BlockBuildBeginEvent
            TileLogger.build(event.tile, TileLogger.unitToPlayerInfo(event.unit));
        });

        Events.on(EventType.ConfigEvent.class, event -> TileLogger.build(event.tile.tile, event.player == null ? null : event.player.getInfo()));

        Events.on(EventType.PickupEvent.class, event -> {
            if (event.build == null) return; // payload is unit
            TileLogger.destroy(event.build.tile, TileLogger.unitToPlayerInfo(event.carrier));
        });

        Events.on(EventType.PayloadDropEvent.class, event -> {
            if (event.build == null) return; // payload is unit
            TileLogger.build(event.build.tile, TileLogger.unitToPlayerInfo(event.carrier));
        });

        Events.on(EventType.BlockDestroyEvent.class, event -> TileLogger.destroy(event.tile, null));

        Vars.net.handleServer(RotateBlockCallPacket.class, (con, packet) -> {
            packet.handled();
            packet.handleServer(con);
            TileLogger.build(packet.build.tile, con.player.getInfo());
        });

        Events.on(EventType.WorldLoadEvent.class, event -> TileLogger.reset());
        Events.on(EventType.TapEvent.class, event -> {
            if (event.tile == null) return;

            PlayerData data = Database.getCached(event.player.uuid());

            if (data.historySize <= 0 && data.adminMod && !event.player.con.mobile) {
                Call.clientPacketUnreliable("take_history_infov2",
                        JsonIO.write(TileLogger.getTileStatePacket(event.tile, 100L)));
                return;
            }

            if (data.historySize > 0) {
                TileLogger.showHistory(event.tile.x, event.tile.y, data.historySize, event.player);
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        register("tilelogger", (args, player) ->
                TileLogger.showInfo(player));
        register("history", (args, player) -> {
            var data = Database.getCached(player.uuid());

            if (args.length == 3) {
                long size = Math.abs(Strings.parseLong(args[0], 0));
                short x = Short.parseShort(args[1]);
                short y = Short.parseShort(args[2]);

                TileLogger.showHistory(x, y, size, player);
                return;
            }

            if (args.length > 0) {
                data.historySize = Math.abs(Strings.parseLong(args[0], 0));
            } else if (data.historySize == 0) data.historySize = 6L;
            else data.historySize = 0L;

            bundled(player, "commands.history.success", data.historySize);
            Database.setCached(data);
        });

//        handler.<Player>register("history", "<size> [x] [y]", "Shows tile history.", (args, player) -> {
//            try {
//                long size = Long.parseLong(args[0]);
//                if (args.length == 3) {
//                    TileLogger.showHistory(Short.parseShort(args[1]), Short.parseShort(args[2]), size, player);
//                }
//                else {
//                    if (size > 0) {
//                        history_active.put(player.uuid(), size);
//                        player.sendMessage("History inspector size set to: " + size);
//                    }
//                    else {
//                        history_active.remove(player.uuid());
//                        player.sendMessage("History inspector disabled.");
//                    }
//                }
//            }
//            catch (NumberFormatException e) {
//                player.sendMessage("Failed to parse parameters.");
//            }
//        });

        handler.<Player>register("rollback", "<uuid> [time] [x1] [y1] [x2] [y2]", "Rolls back tiles.", (args, player) -> {
            if (!player.admin) {
                player.sendMessage("Access denied.");
                return;
            }
            try {
                short x1 = 0, y1 = 0, x2 = (short) (Vars.world.width() - 1), y2 = (short) (Vars.world.height() - 1);
                int time = args.length > 1 ? Integer.parseInt(args[1]) : 0;
                String uuid = args[0].equals("all") ? "" : args[0];
                if (uuid.equals("") && time == 0) {
                    player.sendMessage("All players with 0 time are not allowed.");
                    return;
                }
                if (args.length > 2) {
                    if (args.length < 6) {
                        player.sendMessage("Not enought position params.");
                        return;
                    }
                    x1 = Short.parseShort(args[2]);
                    y1 = Short.parseShort(args[3]);
                    x2 = Short.parseShort(args[4]);
                    y2 = Short.parseShort(args[5]);
                }
                PlayerInfo target = uuid.equals("self") ? player.getInfo() : Vars.netServer.admins.getInfoOptional(uuid);
                if (!uuid.equals("") && target == null) {
                    player.sendMessage("Player not found.");
                    return;
                }
                TileLogger.rollback(player, target, -1, time, x1, y1, x2, y2);
            } catch (NumberFormatException e) {
                player.sendMessage("Failed to parse parameters.");
            }
        });
    }
}
