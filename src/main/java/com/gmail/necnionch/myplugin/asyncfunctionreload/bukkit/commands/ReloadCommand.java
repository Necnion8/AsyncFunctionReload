package com.gmail.necnionch.myplugin.asyncfunctionreload.bukkit.commands;

import com.gmail.necnionch.myplugin.asyncfunctionreload.bukkit.FunctionReload;
import com.gmail.necnionch.myplugin.asyncfunctionreload.bukkit.FunctionsWrapper;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.minecraft.server.v1_15_R1.MinecraftKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReloadCommand implements CommandExecutor, TabCompleter {

    private final FunctionsWrapper functions;

    public ReloadCommand(FunctionsWrapper functions) {
        this.functions = functions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            functions.reloadAll((result) -> sendResult(sender, result, null));
        } else {
            String namespace = args[0].toLowerCase(Locale.ROOT);
            if (functions.getNamespaces().contains(namespace)) {
                functions.reload((result) -> sendResult(sender, result, namespace), namespace);
            } else {
                sender.sendMessage(ChatColor.RED + "不明なIDです");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return functions.getNamespaces().stream().filter(n -> n.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private void sendResult(CommandSender sender, FunctionReload.Result result, String namespace) {
        sender.sendMessage(ChatColor.GRAY + "RELOADING!");

        int functions = result.getFunctionCount();
        long time = result.getProcessingTime();
        FunctionReload.Fail[] fails = result.getFails();

        ChatColor color = (fails.length > 0) ? ChatColor.GOLD : ChatColor.DARK_GREEN;
        String message;
        if (namespace != null) {
            message = "" + color + ChatColor.ITALIC + "COMPLETED " + namespace + " FUNCTIONS! " + ChatColor.GRAY + ChatColor.ITALIC + "(" + time + "ms, " + functions + " functions";
        } else {
            message = "" + color + ChatColor.ITALIC + "COMPLETED! " + ChatColor.GRAY + ChatColor.ITALIC + "(" + time + "ms, " + functions + " functions";
        }
        message = message + ((fails.length > 0) ? ", " + fails.length + " errors)" : ")");
        Command.broadcastCommandMessage(sender, message);

        Pattern linePattern = Pattern.compile(" on line (\\d+)");
        Pattern posPattern = Pattern.compile(" at position (\\d+)");

        for (FunctionReload.Fail fail : fails) {
            MinecraftKey key = fail.getFunctionKey();

            Throwable error = fail.getError();
            while (error.getCause() != null) {
                error = error.getCause();
            }

            String info = "";
            Matcher m = linePattern.matcher(error.getMessage());
            if (m.find()) {
                String lineNumber = m.group(1);
                m = posPattern.matcher(error.getMessage());
                if (m.find()) {
                    String posNumber = m.group(1);
                    info = " (L" + lineNumber + ", P" + posNumber + ")";
                } else {
                    info = " (L" + lineNumber + ")";
                }
            }

            sender.spigot().sendMessage(new ComponentBuilder("- " + key + info)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(error.getMessage()).color(ChatColor.RED).create()))
                    .color(ChatColor.RED)
                    .create());
        }

    }


}
