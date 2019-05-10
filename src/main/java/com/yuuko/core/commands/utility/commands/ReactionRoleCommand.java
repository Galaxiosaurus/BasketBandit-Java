package com.yuuko.core.commands.utility.commands;

import com.yuuko.core.MessageHandler;
import com.yuuko.core.commands.Command;
import com.yuuko.core.commands.utility.UtilityModule;
import com.yuuko.core.database.ReactionRoleFunctions;
import com.yuuko.core.events.extensions.MessageEvent;
import com.yuuko.core.utilities.Sanitiser;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;

import java.util.HashMap;

public class ReactionRoleCommand extends Command {

    private static final HashMap<String, Message> selectedMessages = new HashMap<>();

    public ReactionRoleCommand() {
        super("reactrole", UtilityModule.class, 1, new String[]{"-reactrole select", "-reactrole select <Message ID>", "-reactrole add <:emote:> <@role>", "-reactrole rem <:emote:> <@role>"}, false, new Permission[]{Permission.MANAGE_ROLES});
    }

    @Override
    public void onCommand(MessageEvent e) {
        final String[] parameters = e.getCommand()[1].split("\\s+");
        final Role highestSelfRole = e.getGuild().getSelfMember().getRoles().get(0);
        final String action = parameters[0].toLowerCase();
        Message selectedMessage = selectedMessages.get(e.getAuthor().getId());

        // Params length less than 2 are considered `select` for latest message.
        if(parameters.length < 2 && action.equals("select")) {
            selectedMessage = e.getTextChannel().getHistoryBefore(e.getTextChannel().getLatestMessageId(), 1).complete().getRetrievedHistory().get(0);
            selectedMessages.remove(e.getAuthor().getId());
            selectedMessages.put(e.getAuthor().getId(), selectedMessage);

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Reaction Role")
                    .setDescription(selectedMessage.toString() + " has been selected.")
                    .addField("Options", e.getPrefix() + "reactrole add <:emote:> <@role>\n" + e.getPrefix() + "reactrole rem <:emote:> <@role>", true);
            MessageHandler.sendMessage(e, embed.build());
            return;
        }

        // Params length less than 3 (but greater than 1) are considered `select` but with given message id.
        if(parameters.length < 3 && action.equals("select")) {
            if(!Sanitiser.isNumber(parameters[1])) {
                EmbedBuilder embed = new EmbedBuilder().setTitle("Invalid Input").setDescription("**" + parameters[1] + "** isn't a valid message id.");
                MessageHandler.sendMessage(e, embed.build());
                return;
            }

            selectedMessage = e.getTextChannel().getMessageById(parameters[1]).complete();
            selectedMessages.remove(e.getAuthor().getId());
            selectedMessages.put(e.getAuthor().getId(), selectedMessage);

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Reaction Role")
                    .setDescription(selectedMessage.toString() + " has been selected.")
                    .addField("Options", e.getPrefix() + "reactrole add <:emote:> <@role>\n" + e.getPrefix() + "reactrole rem <:emote:> <@role>", true);
            MessageHandler.sendMessage(e, embed.build());
            return;
        }

        // Equivalent to a null check for selectedMessage
        if(!selectedMessages.containsKey(e.getChannel().getId())) {
            EmbedBuilder embed = new EmbedBuilder().setTitle("No Selection").setDescription("You need to select a message using `" + e.getPrefix() + "reactrole select`, or `" + e.getPrefix() + "reactrole select <Message ID>` before you can add or remove a reaction role from it.");
            MessageHandler.sendMessage(e, embed.build());
            return;
        }

        Emote emote = (e.getMessage().getEmotes().size() > 0) ? e.getMessage().getEmotes().get(0) : null;
        Role role = (e.getMessage().getMentionedRoles().size() > 0) ? e.getMessage().getMentionedRoles().get(0) : null;

        // Emote null check.
        if(emote == null) {
            EmbedBuilder embed = new EmbedBuilder().setTitle("No Emote").setDescription("I couldn't detect any tagged emotes in the command... Doing nothing.");
            MessageHandler.sendMessage(e, embed.build());
            return;
        }

        // Role null and action check.
        if(role == null && !action.equals("rem")) {
            EmbedBuilder embed = new EmbedBuilder().setTitle("No Role").setDescription("I couldn't detect any tagged roles in the command... Doing nothing.");
            MessageHandler.sendMessage(e, embed.build());
            return;
        }

        // Checks to make sure the emote both exists and is available on the server.
        if(!e.getGuild().getEmoteCache().asList().contains(emote)) {
            EmbedBuilder embed = new EmbedBuilder().setTitle("Invalid Emote").setDescription("This emote is unavailable for use in a reaction role. Make sure that you are using emotes from this server.");
            MessageHandler.sendMessage(e, embed.build());
            return;
        }

        // Checks if the action is not `rem` before making role specific checks.
        if(!action.equals("rem")) {
            // Checks if the role exists, is available for use.
            if(!e.getGuild().getRoleCache().asList().contains(role)) {
                EmbedBuilder embed = new EmbedBuilder().setTitle("Invalid Role").setDescription("This role is unavailable for use in a reaction role. Make sure that you are using roles from this server.");
                MessageHandler.sendMessage(e, embed.build());
                return;
            }

            // Checks if role is lower in the hierarchy than Yuuko's highest role.
            if(role.getPositionRaw() >= highestSelfRole.getPositionRaw()) {
                EmbedBuilder embed = new EmbedBuilder().setTitle("Invalid Role").setDescription("I cannot assign roles that are higher than or equal to my highest role in the hierarchy.");
                MessageHandler.sendMessage(e, embed.build());
                return;
            }
        }

        if(action.equals("add")) {
            final Message finalMessage = selectedMessage;
            selectedMessage.addReaction(emote).queue(
                    s -> ReactionRoleFunctions.addReactionRole(e.getGuild(), finalMessage, emote, role),
                    f -> MessageHandler.sendMessage(e, new EmbedBuilder().setTitle("Error").setDescription("I was unable to add a reaction to the selected message. :(").build()));
        } else if(action.equals("rem")) {
            ReactionRoleFunctions.removeReactionRole(selectedMessage, emote);
        }
    }
}
