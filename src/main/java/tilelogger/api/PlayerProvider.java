package tilelogger.api;

import org.jetbrains.annotations.Nullable;
import tilelogger.PlayerDescriptor;

public interface PlayerProvider {
    @Nullable PlayerDescriptor findPlayer(String str);
    @Nullable PlayerDescriptor findPlayerUuid(String str);
}