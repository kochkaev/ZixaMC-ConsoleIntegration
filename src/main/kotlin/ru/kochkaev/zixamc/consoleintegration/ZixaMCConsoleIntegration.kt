package ru.kochkaev.zixamc.consoleintegration

import net.fabricmc.api.ModInitializer
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import ru.kochkaev.zixamc.api.Initializer
import ru.kochkaev.zixamc.api.config.ConfigManager
import ru.kochkaev.zixamc.api.sql.feature.FeatureTypes
import ru.kochkaev.zixamc.api.telegram.ServerBot

class ZixaMCConsoleIntegration: ModInitializer {

    override fun onInitialize() {
        ConfigManager.registerConfig(Config)
        FeatureTypes.registerType(ConsoleFeatureType)
        ServerBot.bot.registerMessageHandler(ConsoleFeature::executeCommand)
        startConsoleSync()
        Initializer.registerBeforeSQLStopEvent {
            ConsoleFeature.stopBroadcast()
            ConsoleFeature.job.join()
        }
    }
    fun startConsoleSync() {
        val context = LogManager.getContext(false) as LoggerContext
        val rootLogger = context.configuration.rootLogger
        val customAppender = ConsoleLogAppender.createAppender("ZIXA_consoleFeature", null, null)
        customAppender.start()
        rootLogger.addAppender(customAppender, Level.INFO, null)
        context.updateLoggers()
        ConsoleFeature.startPeriodicBroadcast()
    }

}