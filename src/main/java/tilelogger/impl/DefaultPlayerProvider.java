package tilelogger.impl;

import org.jetbrains.annotations.Nullable;
import tilelogger.PlayerDescriptor;
import tilelogger.api.PlayerProvider;
import tilelogger.util.Find;

import static mindustry.Vars.netServer;

/*
 * A default player provider implementation
 * only for players currently online on the server.
 */
public class DefaultPlayerProvider implements PlayerProvider {
    @Override
    public @Nullable PlayerDescriptor findPlayer(String str) {
        var player = Find.player(str);

        return player == null ? null : new PlayerDescriptor(player.name, player.uuid(), -1);
    }

    @Override
    public @Nullable PlayerDescriptor findPlayerUuid(String str) {
        var info = netServer.admins.getInfoOptional(str);

        return info == null ? null : new PlayerDescriptor(info.lastName, info.id,  -1);
    }
}
