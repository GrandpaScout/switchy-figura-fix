package folk.sisby.switchy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import folk.sisby.switchy.api.SwitchyPlayer;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import folk.sisby.switchy.api.presets.SwitchyPreset;
import folk.sisby.switchy.api.presets.SwitchyPresets;
import net.minecraft.command.CommandBuildContext;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.quiltmc.qsl.command.api.CommandRegistrationCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import static folk.sisby.switchy.Switchy.LOGGER;
import static folk.sisby.switchy.util.Command.*;
import static folk.sisby.switchy.util.Feedback.*;

public class SwitchyCommands implements CommandRegistrationCallback {
	public static final Map<UUID, String> HISTORY = new HashMap<>();

	@Override
	public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandBuildContext buildContext, CommandManager.RegistrationEnvironment environment) {
		dispatcher.register(
				CommandManager.literal("switchy")
						.then(CommandManager.literal("help")
								.executes(c -> execute(c, SwitchyCommands::displayHelp))
						)
						.then(CommandManager.literal("list")
								.executes(c -> execute(c, SwitchyCommands::listPresets))
						)
						.then(CommandManager.literal("new")
								.then(CommandManager.argument("preset", StringArgumentType.word())
										.executes(c -> execute(c, (player, presets) -> newPreset(player, presets, c.getArgument("preset", String.class))))
								)
						)
						.then(CommandManager.literal("set")
								.then(CommandManager.argument("preset", StringArgumentType.word())
										.suggests((c, b) -> suggestPresets(c, b, false))
										.executes(c -> execute(c, (player, presets) -> setPreset(player, presets, c.getArgument("preset", String.class))))
								)
						)
						.then(CommandManager.literal("delete")
								.then(CommandManager.argument("preset", StringArgumentType.word())
										.suggests((c, b) -> suggestPresets(c, b, false))
										.executes(c -> execute(c, (player, presets) -> deletePreset(player, presets, c.getArgument("preset", String.class))))
								)
						)
						.then(CommandManager.literal("rename")
								.then(CommandManager.argument("preset", StringArgumentType.word())
										.suggests((c, b) -> suggestPresets(c, b, true))
										.then(CommandManager.argument("name", StringArgumentType.word())
												.executes(c -> execute(c, (player, presets) -> renamePreset(player, presets, c.getArgument("preset", String.class), c.getArgument("name", String.class))))
										)
								)
						)
						.then(CommandManager.literal("module")
								.then(CommandManager.literal("enable")
										.then(CommandManager.argument("module", IdentifierArgumentType.identifier())
												.suggests((c, b) -> suggestModules(c, b, false))
												.executes(c -> execute(c, (player, presets) -> enableModule(player, presets, c.getArgument("module", Identifier.class)))))
								)
								.then(CommandManager.literal("disable")
										.then(CommandManager.argument("module", IdentifierArgumentType.identifier())
												.suggests((c, b) -> suggestModules(c, b, true))
												.executes(c -> execute(c, (player, presets) -> disableModule(player, presets, c.getArgument("module", Identifier.class)))))
								)
						)
		);

		dispatcher.register(
				CommandManager.literal("switch")
						.then(CommandManager.argument("preset", StringArgumentType.word())
								.suggests((c, b) -> suggestPresets(c, b, false))
								.executes(c -> execute(c, (player, presets) -> setPreset(player, presets, c.getArgument("preset", String.class))))
						)
		);
	}

	private static void displayHelp(ServerPlayerEntity player, SwitchyPresets presets) {
		tellHelp(player, "commands.switchy.help.help", "commands.switchy.help.command");
		tellHelp(player, "commands.switchy.list.help", "commands.switchy.list.command");
		tellHelp(player, "commands.switchy.new.help", "commands.switchy.new.command", "commands.switchy.help.placeholder.preset");
		tellHelp(player, "commands.switchy.set.help", "commands.switchy.set.command", "commands.switchy.help.placeholder.preset");
		tellHelp(player, "commands.switch.help", "commands.switch.command", "commands.switchy.help.placeholder.preset");
		tellHelp(player, "commands.switchy.delete.help", "commands.switchy.delete.command", "commands.switchy.help.placeholder.preset");
		tellHelp(player, "commands.switchy.rename.help", "commands.switchy.rename.command", "commands.switchy.help.placeholder.preset", "commands.switchy.help.placeholder.preset");
		tellHelp(player, "commands.switchy.module.enable.help", "commands.switchy.module.enable.command", "commands.switchy.help.placeholder.module");
		tellHelp(player, "commands.switchy.module.disable.help", "commands.switchy.module.disable.command", "commands.switchy.help.placeholder.module");
		tellHelp(player, "commands.switchy.export.help", "commands.switchy.export.command");
		tellHelp(player, "commands.switchy.import.help", "commands.switchy.import.command", "commands.switchy.help.placeholder.file");
	}

	private static void listPresets(ServerPlayerEntity player, SwitchyPresets presets) {
		sendMessage(player, translatableWithArgs("commands.switchy.list.presets", FORMAT_INFO, literal(presets.toString())));
		sendMessage(player, translatableWithArgs("commands.switchy.list.modules", FORMAT_INFO, presets.getEnabledModuleText()));
		sendMessage(player, translatableWithArgs("commands.switchy.list.current", FORMAT_INFO, literal(presets.getCurrentPreset().toString())));
	}

	private static void newPreset(ServerPlayerEntity player, SwitchyPresets presets, String name) {
		try {
			SwitchyPreset newPreset = presets.newPreset(name);
			tellSuccess(player, "commands.switchy.new.success", literal(newPreset.getName()));
		} catch (IllegalStateException ignored) {
			tellInvalidTry(player, "commands.switchy.new.fail.exists", "commands.switchy.set.command", literal(name));
		}
	}

	public static void setPreset(ServerPlayerEntity player, SwitchyPresets presets, String name) {
		String oldName = presets.getCurrentPreset().toString();
		try {
			String newName = presets.switchCurrentPreset(player, name);
			LOGGER.info("[Switchy] Player switch: '" + oldName + "' -> '" + newName + "' [" + player.getGameProfile().getName() + "]");
			tellSuccess(player, "commands.switchy.set.success", literal(oldName), literal(newName));
		} catch (IllegalArgumentException ignored) {
			tellInvalidTry(player, "commands.switchy.set.fail.missing", "commands.switchy.list.command");
		} catch (IllegalStateException ignored) {
			tellInvalidTry(player, "commands.switchy.set.fail.current", "commands.switchy.list.command");
		}
	}

	private static void renamePreset(ServerPlayerEntity player, SwitchyPresets presets, String name, String newName) {
		try {
			presets.renamePreset(name, newName);
			tellSuccess(player, "commands.switchy.rename.success", literal(name), literal(newName));
		} catch (IllegalArgumentException ignored) {
			tellInvalidTry(player, "commands.switchy.rename.fail.missing", "commands.switchy.list.command");
		} catch (IllegalStateException ignored) {
			tellInvalidTry(player, "commands.switchy.rename.fail.exists", "commands.switchy.list.command");
		}
	}

	private static void deletePreset(ServerPlayerEntity player, SwitchyPresets presets, String name) {
		try {
			presets.deletePreset(name, true);
		} catch (IllegalArgumentException ignored) {
			tellInvalidTry(player, "commands.switchy.delete.fail.missing", "commands.switchy.list.command");
		} catch (IllegalStateException ignored) {
			tellInvalidTry(player, "commands.switchy.delete.fail.current", "commands.switchy.rename.command", literal(""), literal(""));
		}

		if (!HISTORY.getOrDefault(player.getUuid(), "").equalsIgnoreCase(command("switchy delete " + name))) {
			tellWarn(player, "commands.switchy.delete.warn");
			tellWarn(player, "commands.switchy.list.modules", presets.getEnabledModuleText());
			tellInvalidTry(player, "commands.switchy.delete.confirmation", "commands.switchy.delete.command", literal(name));
		} else {
			presets.deletePreset(name); // Unsure if we can rectify having both confirmation and throw-errors
			tellSuccess(player, "commands.switchy.delete.success", literal(name));
		}
	}

	private static void disableModule(ServerPlayerEntity player, SwitchyPresets presets, Identifier id) {
		try {
			presets.disableModule(id, true);
		} catch (IllegalArgumentException ignored) {
			tellInvalid(player, "commands.switchy.module.disable.fail.missing", literal(id.toString()));
		} catch (IllegalStateException ignored) {
			tellInvalid(player, "commands.switchy.module.disable.fail.disabled", literal(id.toString()));
		}

		if (!HISTORY.getOrDefault(player.getUuid(), "").equalsIgnoreCase(command("switchy module disable " + id))) {
			sendMessage(player, SwitchyModuleRegistry.getDisableConfirmation(id).setStyle(FORMAT_WARN.getLeft()));
			tellInvalidTry(player, "commands.switchy.module.disable.confirmation", "commands.switchy.module.disable.command", literal(id.toString()));
		} else {
			presets.disableModule(id);
			tellSuccess(player, "commands.switchy.module.disable.success", literal(id.toString()));
		}
	}

	private static void enableModule(ServerPlayerEntity player, SwitchyPresets presets, Identifier id) {
		try {
			presets.enableModule(id);
			tellSuccess(player, "commands.switchy.module.enable.success", literal(id.toString()));
		} catch (IllegalArgumentException ignored) {
			tellInvalid(player, "commands.switchy.module.enable.fail.missing", literal(id.toString()));
		} catch (IllegalStateException ignored) {
			tellInvalid(player, "commands.switchy.module.enable.fail.enabled", literal(id.toString()));
		}
	}

	public static boolean confirmAndImportPresets(ServerPlayerEntity player, Map<String, SwitchyPreset> importedPresets, List<Identifier> modules, String command) {
		SwitchyPresets presets = ((SwitchyPlayer) player).switchy$getPresets();

		// Print info and stop if confirmation is required.
		if (!HISTORY.getOrDefault(player.getUuid(), "").equalsIgnoreCase(command(command))) {
			tellWarn(player, "commands.switchy.import.warn.info", literal(String.valueOf(importedPresets.size())), literal(String.valueOf(modules.size())));
			tellWarn(player, "commands.switchy.list.presets", getHighlightedListText(importedPresets.keySet().stream().sorted().toList(), List.of(new Pair<>(presets.getPresetNames()::contains, Formatting.DARK_RED))));
			tellWarn(player, "commands.switchy.import.warn.collision");
			tellWarn(player, "commands.switchy.list.modules", getIdListText(modules));
			sendMessage(player, translatableWithArgs("commands.switchy.import.confirmation", FORMAT_INVALID, literal("/" + command)));
			HISTORY.put(player.getUuid(), command(command));
			return false;
		}

		// Import
		presets.importFromOther(player, importedPresets);
		tellSuccess(player, "commands.switchy.import.success", literal(String.valueOf(importedPresets.keySet().stream().filter(Predicate.not(presets.getPresetNames()::contains)).toList().size())), literal(String.valueOf(importedPresets.keySet().stream().filter(presets.getPresetNames()::contains).toList().size())));
		return true;
	}
}
