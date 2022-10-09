package ckcsc.asadfgglie.main;

import ckcsc.asadfgglie.main.exam.Exam;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class Main {
    public static JDA BUILDER;
    public static MessageChannel mainChannel;

    public static void main (String[] args) throws Exception {
        String token = args[0];
        BUILDER = JDABuilder.create(token, GatewayIntent.GUILD_EMOJIS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS)
                .setActivity(Activity.of(Activity.ActivityType.PLAYING, "無情的監考官"))
                .enableCache(CacheFlag.EMOTE)
                .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS)
                .build();
        BUILDER.awaitReady();

        mainChannel = BUILDER.getGuildById(947729574924529664L).getTextChannelById(947729577214627892L);
        BUILDER.addEventListener(new Exam());
    }
}
