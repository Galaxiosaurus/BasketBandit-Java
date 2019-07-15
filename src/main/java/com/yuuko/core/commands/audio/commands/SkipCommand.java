package com.yuuko.core.commands.audio.commands;

import com.yuuko.core.Configuration;
import com.yuuko.core.MessageHandler;
import com.yuuko.core.commands.Command;
import com.yuuko.core.commands.audio.handlers.AudioManagerController;
import com.yuuko.core.commands.audio.handlers.GuildAudioManager;
import com.yuuko.core.events.entity.MessageEvent;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.Arrays;

public class SkipCommand extends Command {

    public SkipCommand() {
        super("skip", Configuration.MODULES.get("audio"), 0, Arrays.asList("-skip"), false, null);
    }

    @Override
    public void onCommand(MessageEvent e) {
        try {
            GuildAudioManager manager = AudioManagerController.getGuildAudioManager(e.getGuild());

            if(manager.getPlayer().getPlayingTrack() != null) {
                EmbedBuilder embed = new EmbedBuilder().setTitle("Skipping").setDescription(manager.getPlayer().getPlayingTrack().getInfo().title);
                MessageHandler.sendMessage(e, embed.build());

                manager.getPlayer().stopTrack();
                manager.getScheduler().nextTrack();
            } else {
                EmbedBuilder embed = new EmbedBuilder().setTitle("There is no current track to skip.");
                MessageHandler.sendMessage(e, embed.build());
            }

        } catch(Exception ex) {
            log.error("An error occurred while running the {} class, message: {}", this, ex.getMessage(), ex);
        }
    }

}
