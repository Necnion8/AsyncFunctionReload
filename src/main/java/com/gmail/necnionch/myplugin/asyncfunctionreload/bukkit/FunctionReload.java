package com.gmail.necnionch.myplugin.asyncfunctionreload.bukkit;

import com.google.common.collect.Lists;
import net.minecraft.server.v1_15_R1.MinecraftKey;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FunctionReload {
    public final AtomicInteger counter = new AtomicInteger();
    public final long startAt = System.currentTimeMillis();
    public final List<Fail> fails = Lists.newArrayList();

    public FunctionReload() {}

    public Result complete() {
        return new Result();
    }

    public void addFail(MinecraftKey key, Throwable throwable) {
        fails.add(new Fail(key, throwable));
    }




    public static class Fail {

        private final Throwable error;
        private final MinecraftKey functionKey;

        public Fail(MinecraftKey functionKey, Throwable throwable) {
            this.functionKey = functionKey;
            error = throwable;
        }

        public MinecraftKey getFunctionKey() {
            return functionKey;
        }

        public Throwable getError() {
            return error;
        }

    }

    public class Result {
        private final long endAt = System.currentTimeMillis();

        public int getFunctionCount() {
            return counter.get();
        }

        public long getProcessingTime() {
            return endAt - startAt;
        }

        public Fail[] getFails() {
            return fails.toArray(new Fail[0]);
        }

    }

}
