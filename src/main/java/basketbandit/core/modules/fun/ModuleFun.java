package basketbandit.core.modules.fun;

import basketbandit.core.modules.C;
import basketbandit.core.modules.Module;
import basketbandit.core.modules.fun.commands.CommandInsult;
import basketbandit.core.modules.fun.commands.CommandOverreact;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class ModuleFun extends Module {

    public ModuleFun() {
        super("ModuleFun", "modFun");
    }

    public ModuleFun(MessageReceivedEvent e) {
        super("ModuleFun", "modFun");

        if(!checkModuleSettings(e)) {
            return;
        }

        if(!executeCommand(e)) {
            e.getTextChannel().sendMessage("Sorry " + e.getAuthor().getAsMention() + ", that command was unable to execute correctly.").queue();
        }

    }

    protected boolean executeCommand(MessageReceivedEvent e) {
        String[] commandArray = e.getMessage().getContentRaw().toLowerCase().split("\\s+", 2);
        String command = commandArray[0];

        if(command.equals(C.OVERREACT.getEffectiveName())) {
            new CommandOverreact(e);
            return true;
        }

        if(command.equals(C.INSULT.getEffectiveName())) {
            new CommandInsult(e);
            return true;
        }

        return false;
    }
}