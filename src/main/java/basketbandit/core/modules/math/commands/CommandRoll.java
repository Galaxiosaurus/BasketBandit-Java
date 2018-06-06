package basketbandit.core.modules.math.commands;

import basketbandit.core.modules.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.Random;

public class CommandRoll extends Command {

    public CommandRoll() {
        super("roll", "basketbandit.core.modules.math.ModuleMath", null);
    }

    public CommandRoll(MessageReceivedEvent e, String[] command) {
        super("roll", "basketbandit.core.modules.math.ModuleMath", null);
        executeCommand(e, command);
    }

    /**
     * Executes command using MessageReceivedEvent e.
     * @param e; MessageReceivedEvent.
     * @return boolean; if the command executed correctly.
     * @throws IllegalArgumentException;
     */
    protected void executeCommand(MessageReceivedEvent e, String[] command) throws IllegalArgumentException {
        int num;
        int rollNum = 0;

        // I assume someone will try to roll something that isn't a number.
        if(command[1].matches("[0-9]+")) {
            rollNum = Integer.parseInt(command[1]);
        } else if(command[1].contains("-")) {
            e.getTextChannel().sendMessage("Sorry " + e.getAuthor().getAsMention() +", you can't roll on a negative number or anything that isn't a number").queue();
            throw new IllegalArgumentException();
        }

        if(command[1].equals("00")) {
            num = (new Random().nextInt(10) + 1) * 10;
        } else {
            num = new Random().nextInt(rollNum) + 1;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.RED)
                .setAuthor(e.getGuild().getMemberById(e.getAuthor().getIdLong()).getEffectiveName() + " rolled a " + num + ".", null, e.getAuthor().getAvatarUrl());

        if(num != 0) {
            e.getTextChannel().sendMessage(embed.build()).queue();
        } else {
            e.getTextChannel().sendMessage("Sorry " + e.getAuthor().getAsMention() +", something has gone wrong...").queue();
        }

    }

}
