package ca.fxco.encodedchat.utils.command;

import ca.fxco.encodedchat.EncodedChat;
import ca.fxco.encodedchat.actions.ParsedArguments;
import ca.fxco.encodedchat.encodingSets.EncodingSet;
import ca.fxco.encodedchat.actions.EncodingAction;
import ca.fxco.encodedchat.actions.EncodingActions;
import ca.fxco.encodedchat.utils.EncodingUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class EncodedChatCommand {

    // TODO: Make the feedback message actually useful

    public static final String PREFIX = "encodedchat";

    public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal(PREFIX);
        builder.then(ClientCommandManager.literal("player")
                .then(ClientCommandManager.argument("player", ClientPlayerArgumentType.player())
                        .executes(EncodedChatCommand::getPlayerActions)
                        .then(ClientCommandManager.literal("get").executes(EncodedChatCommand::getPlayerActions))
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("type", StringArgumentType.word())
                                        .suggests((context, builderx) -> CommandSource.suggestMatching(
                                                EncodedChat.getEncodingSetNames(),
                                                builderx
                                        ))
                                        .executes(c -> addPlayerAction(c, false))
                                        .then(ClientCommandManager.argument("arguments", StringArgumentType.greedyString())
                                                .executes(c -> addPlayerAction(c, true)))))
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("id", IntegerArgumentType.integer(0))
                                        .executes(EncodedChatCommand::removePlayerAction)))
                        .then(ClientCommandManager.literal("clear")
                                .executes(EncodedChatCommand::clearPlayerActions))
                ));
        dispatcher.register(builder);
    }

    private static int clearPlayerActions(CommandContext<FabricClientCommandSource> context) {
        PlayerEntity player = context.getArgument("player", PlayerEntity.class);
        if (player == null) {
            context.getSource().sendError(Text.of("Player was not found!"));
            return 0;
        }
        EncodingActions encodingActions = EncodedChat.PLAYER_ACTIONS.getPlayerAction(player.getUuid());
        if (encodingActions == null) {
            context.getSource().sendFeedback(Text.of("Player does not have any actions to clear!"));
            return 1;
        }
        encodingActions.clearActions();
        context.getSource().sendFeedback(Text.of("Cleared all actions from player!"));
        return 1;
    }

    private static int removePlayerAction(CommandContext<FabricClientCommandSource> context) {
        PlayerEntity player = context.getArgument("player", PlayerEntity.class);
        if (player == null) {
            context.getSource().sendError(Text.of("Player was not found!"));
            return 0;
        }
        EncodingActions encodingActions = EncodedChat.PLAYER_ACTIONS.getPlayerAction(player.getUuid());
        if (encodingActions == null) {
            context.getSource().sendFeedback(Text.of("Player does not have any actions to remove!"));
            return 1;
        }
        int id = IntegerArgumentType.getInteger(context,"id");
        if (encodingActions.getEncodingActions().size() <= id) {
            context.getSource().sendError(Text.of("The value `"+id+"` does not exist!"));
            return 0;
        }
        if (encodingActions.getEncodingActions().size() == 1) {
            EncodedChat.PLAYER_ACTIONS.removePlayerAction(player.getUuid());
            context.getSource().sendFeedback(Text.of("Removed action from player!"));
            return 1;
        }
        encodingActions.getEncodingActions().remove(id);
        context.getSource().sendFeedback(Text.of("Removed action from player!"));
        return 1;
    }

    private static int addPlayerAction(CommandContext<FabricClientCommandSource> context, boolean hasArguments) {
        PlayerEntity player = context.getArgument("player", PlayerEntity.class);
        if (player == null) {
            context.getSource().sendError(Text.of("Player was not found!"));
            return 0;
        }
        EncodingActions encodingActions = EncodedChat.PLAYER_ACTIONS.PLAYER_ENCODING_ACTIONS.getOrDefault(player.getUuid(), new EncodingActions());
        String type = StringArgumentType.getString(context,"type");
        if (EncodingUtils.isInvalidSetType(type)) {
            context.getSource().sendError(Text.of("Type `"+type+"` does not exist!"));
            return 0;
        }
        EncodingSet set = EncodedChat.ENCODING_SETS.get(type);
        String[] arguments = hasArguments ?
                StringArgumentType.getString(context, "arguments").split(" ") :
                null;
        ParsedArguments parsedArguments = set.createArguments(arguments);
        if (!parsedArguments.validateArguments()) {
            context.getSource().sendError(Text.of("Arguments used are not valid for this type!"));
            return 0;
        }
        encodingActions.add(new EncodingAction(set, parsedArguments.parseArguments()));
        context.getSource().sendFeedback(Text.of("Action has been added successfully!"));
        return 1;
    }

    private static int getPlayerActions(CommandContext<FabricClientCommandSource> context) {
        PlayerEntity player = context.getArgument("player", PlayerEntity.class);
        if (player == null) {
            context.getSource().sendError(Text.of("Player was not found!"));
            return 0;
        }
        EncodingActions encodingActions = EncodedChat.PLAYER_ACTIONS.getPlayerAction(player.getUuid());
        context.getSource().sendFeedback(Text.of("Player " + player.getEntityName() + (
                encodingActions == null ?
                        " does not have any set actions" :
                        " " + encodingActions)));
        return 1;
    }
}
