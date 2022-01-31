package com.gmail.necnionch.myplugin.asyncfunctionreload.bukkit;

import com.gmail.necnionch.myplugin.asyncfunctionreload.bukkit.commands.ReloadCommand;
import net.minecraft.server.v1_15_R1.IReloadableResourceManager;
import net.minecraft.server.v1_15_R1.MinecraftServer;
import org.bukkit.craftbukkit.v1_15_R1.CraftServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

// SAVE_PLAYERS
// RESOURCE_PACK_REPOSITORY
// THIS.B(WorldData)
// PLAYER_LIST_RELOAD
// THIS.BB (BlockRegistry)

// LOCAL
// 9 > 7   > 627  > 22 > 53

// LO
// 0 > 450 > 5924 > 0  > 43
// 0 > 288 > 2152 > 0  > 27
// 0 > 1   > 264  > 0  > 33

public final class AsyncFunctionReload extends JavaPlugin {

    private FunctionsWrapper functionsWrapper;

    @Override
    public void onLoad() {
        getLogger().info("reflection initializing...");
        MinecraftServer server = ((CraftServer) getServer()).getServer();
        IReloadableResourceManager resourceManager = server.getResourceManager();
        try {
            functionsWrapper = new FunctionsWrapper(this, server, resourceManager);
        } catch (ReflectiveOperationException e) {
            IllegalArgumentException e2 = new IllegalArgumentException("reflection failed");
            e2.addSuppressed(e);
            throw e2;
        }
    }

    @Override
    public void onEnable() {
        Objects.requireNonNull(getCommand("functionsreload")).setExecutor(new ReloadCommand(functionsWrapper));

    }
}
