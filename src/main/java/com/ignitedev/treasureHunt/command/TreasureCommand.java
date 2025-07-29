package com.ignitedev.treasureHunt.command;

import com.ignitedev.treasureHunt.base.Treasure;
import com.ignitedev.treasureHunt.config.BaseConfiguration;
import com.ignitedev.treasureHunt.database.SQLDatabaseManager;
import com.ignitedev.treasureHunt.gui.TreasureGui;
import com.ignitedev.treasureHunt.repository.TreasureRepository;
import com.ignitedev.treasureHunt.util.PlayerSelection;

import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class TreasureCommand implements CommandExecutor, TabCompleter {

  private static final String PERMISSION = "treasurehunt.admin";
  private static final int ITEMS_PER_PAGE = 10;

  private final TreasureRepository treasureRepository;
  private final SQLDatabaseManager sqlDatabaseManager;
  private final BaseConfiguration config;

  @Override
  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String @NotNull [] args) {
    if (!sender.hasPermission(PERMISSION)) {
      return Collections.emptyList();
    }
    if (args.length == 1) {
      List<String> subcommands = Arrays.asList("create", "delete", "completed", "list");

      return subcommands.stream()
          .filter(s -> s.startsWith(args[0].toLowerCase()))
          .collect(Collectors.toList());
    }
    if (args.length == 2) {
      if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("completed")) {
        return treasureRepository.getAllTreasures().stream()
            .map(Treasure::id)
            .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
            .collect(Collectors.toList());
      }
    }
    return Collections.emptyList();
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String @NotNull [] args) {
    if (!sender.hasPermission(PERMISSION)) {
      sender.sendMessage(config.getNoPermissionMessage());
      return false;
    }
    if (args.length == 0) {
      return sendHelp(sender);
    }
    String subCommand = args[0].toLowerCase();

    switch (subCommand) {
      case "create" -> {
        return handleCreate(sender, args);
      }
      case "delete" -> {
        return handleDelete(sender, args);
      }
      case "completed" -> {
        return handleCompleted(sender, args);
      }
      case "list" -> {
        return handleList(sender);
      }
      case "gui" -> {
        return handleGui(sender);
      }
      default -> {
        return sendHelp(sender);
      }
    }
  }

  private boolean handleCreate(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      sendMessage(sender, config.getPlayerOnlyMessage());
      return false;
    }
    if (args.length < 3) {
      return sendHelp(sender, "Usage: /treasure create <id> <command>");
    }
    String treasureId = args[1].toLowerCase();
    String command = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

    if (treasureRepository.getTreasure(treasureId).isPresent()) {
      return sendError(sender, "A treasure with that ID already exists!");
    }
    sendMessage(
        sender,
        Component.text("Please click on a block to set as the treasure location.")
            .color(NamedTextColor.YELLOW));

    PlayerSelection.awaitSelection(
        player,
        location -> {
          if (location == null) {
            sendError(player, "Treasure creation cancelled.");
            return;
          }
          treasureRepository.createTreasure(location, treasureId, List.of(command));
          sendSuccess(sender, "Successfully created treasure: " + treasureId);
        });
    return true;
  }

  private boolean handleDelete(CommandSender sender, String[] args) {
    if (!(sender instanceof Player)) {
      return sendMessage(sender, config.getPlayerOnlyMessage());
    }
    if (args.length < 2) {
      return sendHelp(sender, "Usage: /treasure delete <id> [confirm]");
    }
    String treasureId = args[1].toLowerCase();

    if (treasureRepository.getTreasure(treasureId).isEmpty()) {
      return sendError(sender, "No treasure found with ID: " + treasureId);
    }
    if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
      sendMessage(
          sender,
          Component.text("Are you sure you want to delete treasure '" + treasureId + "'? ")
              .color(NamedTextColor.YELLOW)
              .append(
                  Component.text("[CONFIRM]")
                      .color(NamedTextColor.RED)
                      .decorate(TextDecoration.BOLD)
                      .clickEvent(
                          ClickEvent.runCommand("/treasure delete " + treasureId + " confirm"))
                      .hoverEvent(
                          HoverEvent.showText(Component.text("Click to confirm deletion")))));
      return true;
    }
    treasureRepository.removeTreasure(treasureId);
    sendSuccess(sender, "Successfully deleted treasure: " + treasureId);
    return true;
  }

  private boolean handleCompleted(CommandSender sender, String[] args) {
    if (args.length < 2) {
      return sendHelp(sender, "Usage: /treasure completed <id> [page]");
    }
    String treasureId = args[1].toLowerCase();

    if (treasureRepository.getTreasure(treasureId).isEmpty()) {
      return sendError(sender, "No treasure found with ID: " + treasureId);
    }
    int page = 1;
    if (args.length > 2) {
      try {
        page = Integer.parseInt(args[2]);
        if (page < 1) page = 1;
      } catch (NumberFormatException exception) {
        return sendError(sender, "Invalid page number!");
      }
    }
    int currentPage = page;

    sqlDatabaseManager
        .getPlayersWhoFoundTreasureAsync(treasureId)
        .whenComplete(
            (foundBy, exception) -> {
              if (foundBy == null || foundBy.isEmpty()) {
                sendMessage(
                    sender, "No players have found this treasure yet.", NamedTextColor.YELLOW);
                return;
              }
              int totalPlayers = foundBy.size();
              int totalPages = (int) Math.ceil((double) totalPlayers / ITEMS_PER_PAGE);
              int actualPage = Math.min(currentPage, Math.max(1, totalPages));
              int start = (actualPage - 1) * ITEMS_PER_PAGE;
              int end = Math.min(start + ITEMS_PER_PAGE, totalPlayers);

              if (exception != null) {
                sendMessage(
                    sender, "An error occurred while fetching player data.", NamedTextColor.RED);
                return;
              }
              Component message =
                  Component.empty()
                      .append(
                          Component.text("\n=== Players who found " + treasureId + " ===\n")
                              .color(NamedTextColor.GOLD))
                      .append(
                          Component.text(
                                  "Page "
                                      + actualPage
                                      + "/"
                                      + totalPages
                                      + " (Total: "
                                      + totalPlayers
                                      + ")\n\n")
                              .color(NamedTextColor.GRAY));

              for (int i = start; i < end; i++) {
                UUID playerId = foundBy.get(i);
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
                String playerName = player.getName() != null ? player.getName() : "Unknown";

                message =
                    message
                        .append(
                            Component.text("• " + playerName + " ")
                                .color(
                                    player.isOnline() ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                        .append(Component.newline());
              }

              if (totalPages > 1) {
                message = message.append(Component.newline());

                if (actualPage > 1) {
                  message =
                      message
                          .append(
                              Component.text("[")
                                  .color(NamedTextColor.GRAY)
                                  .append(
                                      Component.text("← Previous")
                                          .color(NamedTextColor.YELLOW)
                                          .clickEvent(
                                              ClickEvent.runCommand(
                                                  "/treasure completed "
                                                      + treasureId
                                                      + " "
                                                      + (actualPage - 1))))
                                  .append(Component.text("]").color(NamedTextColor.GRAY)))
                          .append(Component.space());
                }
                if (actualPage < totalPages) {
                  message =
                      message.append(
                          Component.text("[")
                              .color(NamedTextColor.GRAY)
                              .append(
                                  Component.text("Next →")
                                      .color(NamedTextColor.YELLOW)
                                      .clickEvent(
                                          ClickEvent.runCommand(
                                              "/treasure completed "
                                                  + treasureId
                                                  + " "
                                                  + (actualPage + 1))))
                              .append(Component.text("]").color(NamedTextColor.GRAY)));
                }
              }
              sendMessage(sender, message);
            });
    return true;
  }

  private boolean handleList(CommandSender sender) {
    List<Treasure> treasures = treasureRepository.getAllTreasures();

    if (treasures.isEmpty()) {
      sender.sendMessage(Component.text("No treasures found!").color(NamedTextColor.YELLOW));
      return false;
    }

    sender.sendMessage(Component.text("\n=== Treasures ===\n").color(NamedTextColor.GOLD));

    for (Treasure treasure : treasures) {
      sqlDatabaseManager.getPlayersWhoFoundTreasureAsync(treasure.id())
          .thenAccept(foundBy -> {
            Component message = Component.text("\n- " + treasure.id() + "\n")
                .color(NamedTextColor.YELLOW)
                .append(Component.text("Found: " + foundBy.size() + " times\n")
                    .color(NamedTextColor.WHITE))
                .append(Component.text("Commands: [").color(NamedTextColor.GRAY))
                .append(Component.text(
                    String.join(", ", treasure.rewardCommands()),
                    NamedTextColor.WHITE))
                .append(Component.text("]\n").color(NamedTextColor.GRAY));
            
            sender.sendMessage(message);
          });
    }
    
    return true;
  }

  private boolean handleGui(CommandSender sender) {
    if (!(sender instanceof Player player)) {
      return sendError(sender, "This command can only be used by players.");
    }
    
    if (!player.hasPermission(PERMISSION)) {
      return sendError(sender, "You don't have permission to use this command.");
    }
    player.openInventory(new TreasureGui(player, treasureRepository, config).getInventory());
    return true;
  }

  private boolean addCommand(CommandSender sender, String[] args) {
    return false; // todo extra
  }

  private boolean sendMessage(CommandSender sender, String message, NamedTextColor color) {
    sender.sendMessage(Component.text(message).color(color));
    return true;
  }

  private boolean sendSuccess(CommandSender sender, String message) {
    return sendMessage(sender, " " + message, NamedTextColor.GREEN);
  }

  private boolean sendError(CommandSender sender, String message) {
    return sendMessage(sender, " " + message, NamedTextColor.RED);
  }

  private boolean sendMessage(CommandSender sender, Component message) {
    sender.sendMessage(message);
    return true;
  }

  private boolean sendHelp(CommandSender sender, String error) {
    return sendError(sender, error + "\nUse /treasure for help.");
  }

  private boolean sendHelp(CommandSender sender) {
    Component helpMessage =
        Component.text("\n=== TreasureHunt Help ===\n")
            .color(NamedTextColor.GOLD)
            .append(createCommandHelp("create <id> <command>", "Create a new treasure at your location"))
            .append(Component.text("\n"))
            .append(createCommandHelp("delete <id>", "Delete a treasure"))
            .append(Component.text("\n"))
            .append(createCommandHelp("completed <id>", "Show players who found a treasure"))
            .append(Component.text("\n"))
            .append(createCommandHelp("list [page]", "List all treasures"))
            .append(Component.text("\n"))
            .append(createCommandHelp("gui", "Open treasure management GUI"));
    sender.sendMessage(helpMessage);
    return true;
  }

  private Component createCommandHelp(String command, String description) {
    return Component.text("/treasure " + command)
        .color(NamedTextColor.YELLOW)
        .hoverEvent(HoverEvent.showText(Component.text("Click to suggest command")))
        .clickEvent(ClickEvent.suggestCommand("/treasure " + command + " "))
        .append(Component.text(" - " + description + "\n").color(NamedTextColor.WHITE));
  }
}
