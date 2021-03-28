package com.yuuko.modules.audio.commands;

import com.yuuko.MessageDispatcher;
import com.yuuko.events.entity.MessageEvent;
import com.yuuko.modules.Command;
import com.yuuko.modules.audio.handlers.AudioManager;
import com.yuuko.modules.audio.handlers.GuildAudioManager;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.Arrays;

public class ShuffleCommand extends Command {

    public ShuffleCommand() {
        super("shuffle", 0, -1L, Arrays.asList("-shuffle"), false, null);
    }

    @Override
    public void onCommand(MessageEvent e) throws Exception {
        GuildAudioManager manager = AudioManager.getGuildAudioManager(e.getGuild());
        if(manager.getScheduler().queue.size() < 1) {
            EmbedBuilder embed = new EmbedBuilder().setTitle("There aren't any tracks to shuffle.");
            MessageDispatcher.reply(e, embed.build());
            return;
        }

        EmbedBuilder embed = new EmbedBuilder().setTitle("Shuffling").setDescription("The queue has been shuffled.");
        MessageDispatcher.reply(e, embed.build());
        manager.getScheduler().shuffle();
    }

}