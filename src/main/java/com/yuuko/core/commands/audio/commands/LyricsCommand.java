package com.yuuko.core.commands.audio.commands;

import com.google.gson.JsonObject;
import com.yuuko.core.Configuration;
import com.yuuko.core.MessageHandler;
import com.yuuko.core.commands.Command;
import com.yuuko.core.commands.audio.AudioModule;
import com.yuuko.core.events.extensions.MessageEvent;
import com.yuuko.core.utilities.Utilities;
import com.yuuko.core.utilities.json.JsonBuffer;
import com.yuuko.core.utilities.json.RequestProperty;
import net.dv8tion.jda.core.EmbedBuilder;
import org.jsoup.Jsoup;

public class LyricsCommand extends Command {

    public LyricsCommand() {
        super("lyrics", AudioModule.class, 1, new String[]{"-lyrics <song|artist>"}, false, null);
    }

    @Override
    public void onCommand(MessageEvent e) {
        try {
            JsonObject json = new JsonBuffer("https://api.genius.com/search?q=" + e.getCommand()[1].replace(" ", "%20"), "default", "default", new RequestProperty("Authorization", "Bearer " + Utilities.getApiKey("genius"))).getAsJsonObject();
            int response = json.get("meta").getAsJsonObject().get("status").getAsInt();

            if(response != 200) {
                EmbedBuilder embed = new EmbedBuilder().setTitle("Invalid Parameter").setDescription("There were no matches found for **" + e.getCommand()[1] + "**.");
                MessageHandler.sendMessage(e, embed.build());
            }

            JsonObject data = json.get("response").getAsJsonObject().get("hits").getAsJsonArray().get(0).getAsJsonObject().get("result").getAsJsonObject();
            String[] lyricsArray = null;
            String lyrics = Jsoup.connect(data.get("url").getAsString()).get().getElementsByClass("lyrics").get(0).text().replace("[", "\n\n[").replace("]", "]\n");
            int noInfo = lyrics.indexOf("\n \n\n");

            if(noInfo > -1) {
                lyrics = lyrics.substring(lyrics.indexOf("\n \n\n"));
            }

            if(lyrics.length() > 2048) {
                lyricsArray = new String[]{lyrics.substring(0, 2047), lyrics.substring(2047)};
            }

            if(lyricsArray == null) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setAuthor("Lyrics")
                        .setTitle(data.get("full_title").getAsString())
                        .setThumbnail(data.get("header_image_url").getAsString())
                        .setDescription(lyrics)
                        .setFooter(Configuration.STANDARD_STRINGS[1] + e.getMember().getEffectiveName(), e.getAuthor().getEffectiveAvatarUrl());
                MessageHandler.sendMessage(e, embed.build());
            } else {
                for(int i = 0; i < lyricsArray.length; i++) {
                    EmbedBuilder embed = new EmbedBuilder()
                            .setAuthor("Lyrics (" + (i+1) + "/" + lyricsArray.length + ")")
                            .setTitle(data.get("full_title").getAsString())
                            .setThumbnail(data.get("header_image_url").getAsString())
                            .setDescription(lyricsArray[0]);
                    MessageHandler.sendMessage(e, embed.build());
                }
            }

        } catch(Exception ex) {
            log.error("An error occurred while running the {} class, message: {}", this, ex.getMessage(), ex);
        }

    }

}
