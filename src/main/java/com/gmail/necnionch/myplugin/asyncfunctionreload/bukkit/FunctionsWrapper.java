package com.gmail.necnionch.myplugin.asyncfunctionreload.bukkit;

import com.google.common.collect.Lists;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FunctionsWrapper {

    private final IResourceManager resourceManager;
    private final MinecraftServer server;
    private final net.minecraft.server.v1_15_R1.CustomFunctionData functionData;
    private final Map<MinecraftKey, CustomFunction> g;
    private final List<CustomFunction> l;
    private final Field m;
    private final MinecraftKey d;
    private final MinecraftKey e;
    private final Tags<CustomFunction> k;
    private final AsyncFunctionReload owner;

    public FunctionsWrapper(AsyncFunctionReload owner, MinecraftServer server, IResourceManager resourceManager) throws IllegalAccessException {
        this.owner = owner;
        this.server = server;
        this.resourceManager = resourceManager;
        functionData = server.getFunctionData();

        Class<net.minecraft.server.v1_15_R1.CustomFunctionData> clazz = net.minecraft.server.v1_15_R1.CustomFunctionData.class;

        try {
            Field field = clazz.getDeclaredField("g");
            field.setAccessible(true);
            //noinspection unchecked
            g = (Map<MinecraftKey, CustomFunction>) field.get(functionData);
        } catch (ReflectiveOperationException e) {
            IllegalAccessException e2 = new IllegalAccessException("reflection failed");
            e2.addSuppressed(e);
            throw e2;
        }

        try {
            Field field = clazz.getDeclaredField("l");
            field.setAccessible(true);
            //noinspection unchecked
            l = ((List<CustomFunction>) field.get(functionData));
        } catch (ReflectiveOperationException e) {
            IllegalAccessException e2 = new IllegalAccessException("reflection failed");
            e2.addSuppressed(e);
            throw e2;
        }

        try {
            m = clazz.getDeclaredField("m");
            m.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            IllegalAccessException e2 = new IllegalAccessException("reflection failed");
            e2.addSuppressed(e);
            throw e2;
        }

        try {
            Field field = clazz.getDeclaredField("d");
            field.setAccessible(true);
            d = ((MinecraftKey) field.get(null));
        } catch (ReflectiveOperationException e) {
            IllegalAccessException e2 = new IllegalAccessException("reflection failed");
            e2.addSuppressed(e);
            throw e2;
        }

        try {
            Field field = clazz.getDeclaredField("e");
            field.setAccessible(true);
            e = ((MinecraftKey) field.get(null));
        } catch (ReflectiveOperationException e) {
            IllegalAccessException e2 = new IllegalAccessException("reflection failed");
            e2.addSuppressed(e);
            throw e2;
        }

        try {
            Field field = clazz.getDeclaredField("k");
            field.setAccessible(true);
            //noinspection unchecked
            k = ((Tags<CustomFunction>) field.get(functionData));
        } catch (ReflectiveOperationException e) {
            IllegalAccessException e2 = new IllegalAccessException("reflection failed");
            e2.addSuppressed(e);
            throw e2;
        }




    }


    private List<String> readLines(IResourceManager resourceManager, MinecraftKey key) {
        try (IResource resource = resourceManager.a(key)) {
            return IOUtils.readLines(resource.b(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new CompletionException(e);
        }

    }


    public void reloadAll(Consumer<FunctionReload.Result> onResult) {
        // async
        owner.getServer().getScheduler().runTaskAsynchronously(owner, () -> {
            FunctionReload reload = new FunctionReload();

            Collection<MinecraftKey> functionKeys = resourceManager.a("functions", s -> s.endsWith(".mcfunction"));
            List<CompletableFuture<CustomFunction>> list = Lists.newArrayList();
            List<CustomFunction> functions = Lists.newArrayList();

            for (MinecraftKey key : functionKeys) {
                String keyName = key.getKey();
                MinecraftKey key2 = new MinecraftKey(key.getNamespace(), keyName.substring("functions/".length(), keyName.length() - ".mcfunction".length()));

                list.add(CompletableFuture.supplyAsync(() -> readLines(resourceManager, key), Resource.a)
                        .thenApplyAsync(list1 -> CustomFunction.a(key2, functionData, list1), this.server.aX())
                        .handle((function, throwable) -> {
                            if (throwable != null) {
                                reload.addFail(key, throwable);
                                return null;
                            } else {
                                reload.counter.incrementAndGet();
                                functions.add(function);
                                return function;
                            }}));
            }

            CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).join();

            int count = reload.counter.get();
            if (count > 0)
                owner.getLogger().info("Loaded " + count + " custom command functions");

//            this.k.a(this.k.a(resourceManager, this.server.aX()).join());

            // main-thread
            owner.getServer().getScheduler().runTask(owner, () -> {
                this.g.clear();
                this.l.clear();

                this.g.putAll(functions.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(CustomFunction::a, f -> f)));
                this.l.addAll(this.k.b(d).a());

                try {
                    this.m.set(functionData, true);
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }

                onResult.accept(reload.complete());

            });
        });
    }

    public void reload(Consumer<FunctionReload.Result> onResult, String namespace) {
        // async
        owner.getServer().getScheduler().runTaskAsynchronously(owner, () -> {
            FunctionReload reload = new FunctionReload();

            Collection<MinecraftKey> functionKeys = resourceManager.a("functions", s -> s.endsWith(".mcfunction"));
            List<CompletableFuture<CustomFunction>> list = Lists.newArrayList();
            List<CustomFunction> functions = Lists.newArrayList();

            for (MinecraftKey key : functionKeys) {
                if (!key.getNamespace().equalsIgnoreCase(namespace))
                    continue;

                String keyName = key.getKey();
                MinecraftKey key2 = new MinecraftKey(key.getNamespace(), keyName.substring("functions/".length(), keyName.length() - ".mcfunction".length()));

                list.add(CompletableFuture.supplyAsync(() -> readLines(resourceManager, key), Resource.a)
                        .thenApplyAsync(list1 -> CustomFunction.a(key2, functionData, list1), this.server.aX())
                        .handle((function, throwable) -> {
                            if (throwable != null) {
                                reload.addFail(key, throwable);
                                return null;
                            } else {
                                reload.counter.incrementAndGet();
                                functions.add(function);
                                return function;
                            }}));
            }

            CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).join();

            int count = reload.counter.get();
            if (count > 0)
                owner.getLogger().info("Loaded " + count + " custom command functions");

//            this.k.a(this.k.a(resourceManager, this.server.aX()).join());

            // main-thread
            owner.getServer().getScheduler().runTask(owner, () -> {
                this.g.keySet().removeIf(k -> k.getNamespace().equalsIgnoreCase(namespace));
                this.l.removeIf(k -> k.getMinecraftKey().getNamespace().equalsIgnoreCase(namespace));

                this.g.putAll(functions.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(CustomFunction::a, f -> f)));
                this.l.addAll(this.k.b(d).a());

                try {
                    this.m.set(functionData, true);
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }

                onResult.accept(reload.complete());

            });
        });
    }


    public Set<String> getNamespaces() {
        return g.values().stream()
                .map(f -> f.a().getNamespace())
                .collect(Collectors.toSet());
    }



}
