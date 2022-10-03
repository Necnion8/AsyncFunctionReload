package com.gmail.necnionch.myplugin.asyncfunctionreload.bukkit;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.CustomFunction;
import net.minecraft.commands.ICommandListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.CustomFunctionManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.IResource;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.tags.TagDataPack;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class FunctionsWrapper {

    private static final int c = "functions/".length();
    private static final int d = ".mcfunction".length();
    private final IResourceManager resourceManager;
    private final Executor asyncExecutor;
    private final Executor syncExecutor;
    private final CustomFunctionManager inst;
    private final int h;
    private final CommandDispatcher<CommandListenerWrapper> i;
    private final Field e;  // Map<MinecraftKey, CustomFunction>
    private final TagDataPack<CustomFunction> f;
    private final Field g;  // Map<MinecraftKey, Collection<CustomFunction>>


    @SuppressWarnings("unchecked")
    public FunctionsWrapper(AsyncFunctionReload owner, MinecraftServer server) throws ReflectiveOperationException {
        this.resourceManager = server.aZ();
        this.inst = server.at.b().a();
        Class<CustomFunctionManager> clazz = CustomFunctionManager.class;

        Field field = clazz.getDeclaredField("h");
        field.setAccessible(true);
        h = ((int) field.get(inst));

        field = clazz.getDeclaredField("i");
        field.setAccessible(true);
        i = ((CommandDispatcher<CommandListenerWrapper>) field.get(inst));

        e = clazz.getDeclaredField("e");
        e.setAccessible(true);

        field = clazz.getDeclaredField("f");
        field.setAccessible(true);
        f = ((TagDataPack<CustomFunction>) field.get(inst));

        g = clazz.getDeclaredField("g");
        g.setAccessible(true);

        asyncExecutor = command -> owner.getServer().getScheduler().runTaskAsynchronously(owner, command);
        syncExecutor = command -> owner.getServer().getScheduler().runTask(owner, command);
    }


    public static FunctionsWrapper init(AsyncFunctionReload owner, Server server) {
        try {
            Method method = server.getClass().getDeclaredMethod("getServer");
            MinecraftServer mc = ((MinecraftServer) method.invoke(server));
            return new FunctionsWrapper(owner, mc);
        } catch (ReflectiveOperationException e) {
            IllegalArgumentException e2 = new IllegalArgumentException("reflection failed");
            e2.addSuppressed(e);
            throw e2;
        }
    }


    private static List<String> readLines(IResource var0) {
        try (BufferedReader br = var0.c()) {
            return br.lines().toList();
        } catch (IOException var6) {
            throw new CompletionException(var6);
        }
    }

    public CompletableFuture<FunctionReload.Result> reloadAll() {
        return a(resourceManager);
    }

    public CompletableFuture<FunctionReload.Result> a(IResourceManager resourceManager) {
        FunctionReload result = new FunctionReload();

        CompletableFuture<Map<MinecraftKey, List<net.minecraft.tags.TagDataPack.a>>> first = CompletableFuture.supplyAsync(
                () -> this.f.a(resourceManager),
                asyncExecutor
        );

        CompletableFuture<Map<MinecraftKey, CompletableFuture<CustomFunction>>> second = CompletableFuture.supplyAsync(
                () -> resourceManager.b("functions", (key) -> key.a().endsWith(".mcfunction")),
                asyncExecutor

        ).thenCompose((var1x) -> {
            Map<MinecraftKey, CompletableFuture<CustomFunction>> var2 = Maps.newHashMap();
            CommandListenerWrapper var3 = new CommandListenerWrapper(ICommandListener.a, Vec3D.b, Vec2F.a, null, this.h, "", CommonComponents.a, null, null);

            for (Map.Entry<MinecraftKey, IResource> var5 : var1x.entrySet()) {
                MinecraftKey var6_ = var5.getKey();
                String var7_ = var6_.a();
                MinecraftKey var8 = new MinecraftKey(var6_.b(), var7_.substring(c, var7_.length() - d));
                var2.put(var8, CompletableFuture.supplyAsync(() -> {
                    List<String> var3x = readLines(var5.getValue());
                    return CustomFunction.a(var8, this.i, var3, var3x);
                }, asyncExecutor));
            }

            CompletableFuture<?>[] var4x = (CompletableFuture<?>[])var2.values().toArray(new CompletableFuture[0]);
            return CompletableFuture.allOf(var4x).handle((var1_, var2x) -> var2);
        });

        return first
                .thenCombine(second, Pair::of)
                .thenAcceptAsync((var0x) -> {
                    Map<MinecraftKey, CompletableFuture<CustomFunction>> var1_ = var0x.getSecond();
                    ImmutableMap.Builder<MinecraftKey, CustomFunction> var2 = ImmutableMap.builder();
                    var1_.forEach((key, var2x) -> {
                        var2x.handle((customFunction, error) -> {
                            if (error != null) {
//                                a.error("Failed to load function {}", key, error);
                                result.addFail(key, error);
                            } else {
                                result.counter.incrementAndGet();
                                var2.put(key, customFunction);
                            }
                            return null;
                        }).join();
                    });

                    try {
                        this.e.set(inst, var2.build());
                        this.g.set(inst, this.f.a(var0x.getFirst()));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }, syncExecutor)
                .thenApply((v) -> result.complete());

    }

    public Set<String> getNamespaces() {
        try {
            //noinspection unchecked
            return ((Map<MinecraftKey, CustomFunction>) e.get(inst)).keySet().stream()
                    .map(MinecraftKey::b)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IllegalAccessException ex) {
            return Collections.emptySet();
        }
    }



}
