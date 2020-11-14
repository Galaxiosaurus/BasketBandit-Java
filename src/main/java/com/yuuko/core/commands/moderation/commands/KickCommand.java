package com.yuuko.core.commands.moderation.commands;

import com.yuuko.core.Config;
import com.yuuko.core.commands.Command;
import com.yuuko.core.commands.setting.commands.ModerationLogSetting;
import com.yuuko.core.events.entity.MessageEvent;
import com.yuuko.core.utilities.MessageUtilities;
import com.yuuko.core.utilities.Sanitiser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.util.Arrays;

public class KickCommand extends Command {

    public KickCommand() {
        super("kick", Config.MODULES.get("moderation"),1, -1L, Arrays.asList("-kick @user", "-kick @user <reason>"), false, Arrays.asList(Permission.KICK_MEMBERS));
    }

    @Override
    public void onCommand(MessageEvent e) {
        String[] commandParameters = e.getParameters().split("\\s+", 2);
        Member target = MessageUtilities.getMentionedMember(e, true);

        if(target == null) {
            return;
        }

        if(!Sanitiser.canInteract(e, target, "kick", true)) {
            return;
        }

        if(commandParameters.length < 2) {
            e.getGuild().kick(target).queue(s -> {
                e.getMessage().addReaction("✅").queue();
                ModerationLogSetting.execute(e, "Kick", target.getUser(), "None");
            }, f -> e.getMessage().addReaction("❌").queue());
        } else {
            e.getGuild().kick(target, commandParameters[1]).queue(s -> {
                e.getMessage().addReaction("✅").queue();
                ModerationLogSetting.execute(e, "Kick", target.getUser(), commandParameters[1]);
            }, f -> e.getMessage().addReaction("❌").queue());
        }
    }

}
