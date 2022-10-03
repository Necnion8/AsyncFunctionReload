package com.gmail.necnionch.myplugin.asyncfunctionreload.bukkit;

import com.gmail.necnionch.myplugin.asyncfunctionreload.bukkit.commands.ReloadCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;


public final class AsyncFunctionReload extends JavaPlugin {
    private FunctionsWrapper functionsWrapper;

    @Override
    public void onLoad() {
        getLogger().info("Reflection initializing...");
        try {
            functionsWrapper = FunctionsWrapper.init(this, Bukkit.getServer());
        } catch (Throwable e) {
            e.printStackTrace();
            getLogger().severe("Failed reflection. disabling... X(");
            setEnabled(false);
            return;
        }
    }

    @Override
    public void onEnable() {
        Objects.requireNonNull(getCommand("functionsreload"))
                .setExecutor(new ReloadCommand(functionsWrapper));
    }

    @Override
    public void onDisable() {
        functionsWrapper = null;
    }

}
