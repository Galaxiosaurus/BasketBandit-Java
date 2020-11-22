package com.yuuko.core;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;
import com.yuuko.core.api.ApiManager;
import com.yuuko.core.commands.Command;
import com.yuuko.core.commands.Module;
import com.yuuko.core.commands.audio.handlers.AudioManagerController;
import com.yuuko.core.commands.audio.handlers.lavalink.LavalinkManager;
import com.yuuko.core.database.connection.DatabaseConnection;
import com.yuuko.core.database.function.DatabaseFunctions;
import com.yuuko.core.database.function.GuildFunctions;
import com.yuuko.core.events.GenericEventManager;
import com.yuuko.core.metrics.MetricsManager;
import com.yuuko.core.scheduler.ScheduleHandler;
import com.yuuko.core.scheduler.jobs.OneHourlyJob;
import com.yuuko.core.scheduler.jobs.TenSecondlyJob;
import com.yuuko.core.scheduler.jobs.ThirtySecondlyJob;
import com.yuuko.core.scheduler.jobs.TwelveHourlyJob;
import com.yuuko.core.utilities.Utilities;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.discordbots.api.client.DiscordBotListAPI;
import org.reflections8.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class Config {
    private static final Logger log = LoggerFactory.getLogger(Config.class);

    public static final String VERSION = "202011r5";
    public static String AUTHOR;
    public static String AUTHOR_WEBSITE;
    public static String SUPPORT_GUILD;
    public static String BOT_ID;
    private static String BOT_TOKEN;
    public static int SHARDS_TOTAL = 0;
    public static int SHARDS_INSTANCE = 0;
    public static List<Integer> SHARD_IDS = new ArrayList<>();
    public static String GLOBAL_PREFIX;
    public static ApiManager API_MANAGER;
    public static LavalinkManager LAVALINK;
    public static ShardManager SHARD_MANAGER;
    public static SelfUser BOT;
    public static Map<String, Command> COMMANDS;
    public static Map<String, Module> MODULES;
    public static final List<String> LOCKED_MODULES = Arrays.asList("core", "setting", "developer");
    public static DiscordBotListAPI BOT_LIST;
    public static final List<String> STANDARD_STRINGS = Arrays.asList(VERSION,
            VERSION + " • Requested by ",
            VERSION + " • Asked by ",
            "Requested by "
    );

    public static boolean LOG_METRICS = true;

    /**
     * Loads all of the bots configurations. (Order DOES matter)
     */
    void setup() {
        try {
            long loadStart = System.nanoTime();
            setupFiles();
            setupDatabase();
            loadConfiguration();
            registerShards();
            setupApi();
            setupCommands();
            setupAudio();
            buildShardManager();
            verifyDatabase();
            setupMetrics();
            setupScheduler();
            setupBotLists();

            log.info("Loading complete... time taken: {} seconds.", (BigDecimal.valueOf((System.nanoTime() - loadStart) / 1000000000.0).setScale(2, RoundingMode.HALF_UP)));

        } catch(Exception ex) {
            log.error("An error occurred while running the {} class, message: {}", Config.class.getSimpleName(), ex.getMessage(), ex);
        }
    }

    /**
     * Creates and initialises configuration files if they are missing.
     */
    private void setupFiles() {
        try {
            boolean needToEdit = new File("./config").mkdirs();
            new File("./config/api").mkdirs();
            new File("./config/hikari").mkdirs();
            new File("./data").mkdirs();

            if(new File("./config/config.yaml").createNewFile()) {
                log.warn("Created configuration file: ./config/config.yaml");
                try(FileWriter w = new FileWriter(new File("./config/config.yaml"))) {
                    w.write(
                            "author: \"\"" + System.lineSeparator() +
                                    "website: \"\"" + System.lineSeparator() +
                                    "support: \"\"" + System.lineSeparator() +
                                    "bot_id: \"\"" + System.lineSeparator() +
                                    "bot_token: \"\"" + System.lineSeparator() +
                                    "shards: \"1\""
                    );
                }
            }

            if(new File("./config/hikari/externaldb.properties").createNewFile()) {
                log.warn("Created configuration file: ./config/hikari/externaldb.properties");
                try(FileWriter w = new FileWriter(new File("./config/hikari/externaldb.properties"))) {
                    w.write(
                            "driverClassName=com.mysql.cj.jdbc.Driver" + System.lineSeparator() +
                                    "jdbcUrl=jdbc:mysql://localhost/dbyuuko?useSSL=true&serverTimezone=UTC" + System.lineSeparator() +
                                    "dataSource.user=" + System.lineSeparator() +
                                    "dataSource.password=" + System.lineSeparator() +
                                    "dataSource.cachePrepStmts=true" + System.lineSeparator() +
                                    "dataSource.prepStmtCacheSize=250" + System.lineSeparator() +
                                    "dataSource.prepStmtCacheSqlLimit=2048" + System.lineSeparator() +
                                    "dataSource.useServerPrepStmts=true" + System.lineSeparator() +
                                    "dataSource.useLocalSessionState=true" + System.lineSeparator() +
                                    "dataSource.rewriteBatchedStatements=true" + System.lineSeparator() +
                                    "dataSource.cacheResultSetMetadata=true" + System.lineSeparator() +
                                    "dataSource.cacheServerConfiguration=true" + System.lineSeparator() +
                                    "dataSource.elideSetAutoCommits=true" + System.lineSeparator() +
                                    "dataSource.maintainTimeStats=false"
                    );
                }
            }

            if(new File("./config/lavalink.yaml").createNewFile()) {
                log.warn("Created configuration file: ./config/lavalink.yaml");
                try(FileWriter w = new FileWriter(new File("./config/lavalink.yaml"))) {
                    w.write(
                            "# add nodes below as necessary, using format:" + System.lineSeparator() +
                                    "#" + System.lineSeparator() +
                                    "# ---" + System.lineSeparator() +
                                    "# address: \"ws://ip:port\"" + System.lineSeparator() +
                                    "# password: \"password\""
                    );
                }
            }

            List<String> apiList = Arrays.asList("discordbots", "genius", "github", "google", "newsapi", "openweathermap", "osu", "tesco", "transportforlondon");
            apiList.forEach(api -> {
                try {
                    if(new File("./config/api/" + api + ".yaml").createNewFile()) {
                        log.warn("Created configuration file: ./config/api/{}.yaml", api);
                        try(FileWriter w = new FileWriter(new File("./config/api/" + api + ".yaml"))) {
                            w.write(
                                    "!!com.yuuko.core.api.entity.Api" + System.lineSeparator() +
                                            "name: \"" + api + "\"" + System.lineSeparator() +
                                            "applicationId: \"\"" + System.lineSeparator() +
                                            "apiKey: \"\""
                            );
                        }
                    }
                } catch(IOException e) {
                    log.error("An error occurred while running the {} class, message: {}", Config.class.getSimpleName(), e.getMessage(), e);
                }
            });

            if(needToEdit) {
                System.exit(0);
            }
        } catch(Exception e) {
            log.error("An error occurred while running the {} class, message: {}", Config.class.getSimpleName(), e.getMessage(), e);
        }
    }

    /**
     * Sets up database from properties file. Also handles backup and internal/external usage.
     */
    private void setupDatabase() {
        DatabaseConnection.setupDatabase();
    }

    /**
     * Loads the main configuration file which includes information regarding author, author's website, support guild, the bot's ID and token.
     * Must be done before buildShardManager().
     */
    private void loadConfiguration() {
        try(InputStream inputStream = new FileInputStream(new File("./config/config.yaml"))) {
            Map<String, String> config = new Yaml().load(inputStream);
            AUTHOR = config.get("author");
            AUTHOR_WEBSITE = config.get("website");
            SUPPORT_GUILD = config.get("support");
            BOT_ID = config.get("bot_id");
            BOT_TOKEN = config.get("bot_token");
            SHARDS_INSTANCE = Integer.parseInt(config.get("shards_instance"));
            SHARDS_TOTAL = Integer.parseInt(config.get("shards_total"));
            GLOBAL_PREFIX = "<@!" + BOT_ID + "> ";
        } catch(IOException ex) {
            log.error("An error occurred while running the {} class, message: {}", Config.class.getSimpleName(), ex.getMessage(), ex);
        }
    }

    /**
     * Prunes any expired shards, sets both total shard count and shard ID.
     * Must be done before initialiseAudio().
     */
    private void registerShards() {
        DatabaseFunctions.pruneExpiredShards();
        for(int i = 0; i < SHARDS_INSTANCE; i++) {
            SHARD_IDS.add(DatabaseFunctions.provideShardId());
        }
        log.info("Shard ID: {}, Total Shards: {}.", SHARD_IDS, SHARDS_TOTAL);
    }

    /**
     * Loads api keys from the api key folder.
     */
    private void setupApi() {
        try {
            API_MANAGER = new ApiManager();
            log.info("Loaded {} API keys - {}", API_MANAGER.size(), API_MANAGER.getNames().toString());
        } catch(Exception ex) {
            log.error("An error occurred while running the {} class, message: {}", Config.class.getSimpleName(), ex.getMessage(), ex);
        }
    }

    /**
     * Initialises both the commands and the modules encapsulating those commands.
     */
    private void setupCommands() {
        try {
            Reflections reflections = new Reflections("com.yuuko.core.commands");

            Set<Class<? extends Module>> modules = reflections.getSubTypesOf(Module.class);
            Set<Class<? extends Command>> commands = reflections.getSubTypesOf(Command.class);

            MODULES = new HashMap<>(modules.size());
            COMMANDS = new HashMap<>(commands.size());

            for(Class<? extends Module> module : modules) {
                if(!Modifier.isAbstract(module.getModifiers())) {
                    Module obj = module.getConstructor().newInstance();
                    MODULES.put(obj.getName(), obj);
                }
            }

            MODULES.values().forEach(m -> {
                m.getCommands().forEach(c -> {
                    try {
                        Command command = c.getConstructor().newInstance();
                        COMMANDS.put(command.getName(), command);
                    } catch(Exception e) {
                        log.error("There was an error loading commands, message: {}", e.getMessage(), e);
                    }
                });
            });

            log.info("Loaded {} modules, containing {} commands.", MODULES.size(), COMMANDS.size());

        } catch(Exception ex) {
            log.error("An error occurred while running the {} class, message: {}", Config.class.getSimpleName(), ex.getMessage(), ex);
        }
    }

    /**
     * Initialises Lavalink.
     * MUST be done before buildShardManager(), MUST be done AFTER loadMainConfiguration().
     */
    private void setupAudio() {
        AudioManagerController.registerSourceManagers();
        LAVALINK = new LavalinkManager();
        log.info("Initialised Lavalink.");
    }

    /**
     * Builds the shard manager and initialises the BOT object.
     * Must be done before synchronizeDatabase().
     */
    private void buildShardManager() {
        try {
            SHARD_MANAGER = DefaultShardManagerBuilder.create(
                    GatewayIntent.GUILD_BANS,
                    GatewayIntent.GUILD_EMOJIS,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS,
                    GatewayIntent.GUILD_VOICE_STATES
            )
                    .setToken(BOT_TOKEN)
                    .disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS)
                    .addEventListeners(new GenericEventManager(), LAVALINK.getLavalink())
                    .setAudioSendFactory(new NativeAudioSendFactory())
                    .setVoiceDispatchInterceptor(LAVALINK.getLavalink().getVoiceInterceptor())
                    .setActivity(Activity.of(Activity.ActivityType.LISTENING, "@Yuuko help"))
                    .setShardsTotal(SHARDS_TOTAL)
                    .setShards(SHARD_IDS)
                    .build();

            while(!isConstructed()) {
                Thread.sleep(100);
            }

            BOT = Utilities.getSelfUser();
        } catch(Exception ex) {
            log.error("An error occurred while running the {} class, message: {}", Config.class.getSimpleName(), ex.getMessage(), ex);
        }
    }

    /**
     * Synchronizes the database, adding any guilds that added the bot while it was offline.
     */
    private void verifyDatabase() {
        SHARD_MANAGER.getShards().forEach(shard -> shard.getGuildCache().forEach(GuildFunctions::verifyIntegrity));
        log.info("Database integrity verified with JDA.");
    }

    /**
     * Initialises metrics right away instead of waiting for the scheduler.
     */
    private void setupMetrics() {
        new MetricsManager(SHARD_IDS.size());

        SHARD_IDS.forEach(shardId -> {
            MetricsManager.truncateMetrics(shardId);
            MetricsManager.getDiscordMetrics(shardId).update();
        });
        log.info("Initialised metrics.");
    }

    /**
     * Initialises bot-list objects and then updates them to match the database.
     */
    private void setupBotLists() {
        if(API_MANAGER.containsKey("discordbots")) {
            BOT_LIST = new DiscordBotListAPI.Builder().botId(BOT.getId()).token(API_MANAGER.getApi("discordbots").getKey()).build();
        }
        log.info("Initialised bot lists.");
    }

    /**
     * Initialises scheduler which runs tasks at set intervals.
     */
    private void setupScheduler() {
        ScheduleHandler.registerJob(new TenSecondlyJob());
        ScheduleHandler.registerJob(new ThirtySecondlyJob());
        ScheduleHandler.registerJob(new OneHourlyJob());
        ScheduleHandler.registerJob(new TwelveHourlyJob());

        log.info("Initialised scheduler.");
    }

    /**
     * Checks to see if all shards are connected.
     *
     * @return boolean
     */
    private boolean isConstructed() {
        if(SHARD_MANAGER == null) {
            return false;
        }

        for(JDA shard : SHARD_MANAGER.getShards()) {
            if(shard.getStatus() != JDA.Status.CONNECTED) {
                return false;
            }
        }
        return true;
    }

}
