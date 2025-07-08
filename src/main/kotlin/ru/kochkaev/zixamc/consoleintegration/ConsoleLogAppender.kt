package ru.kochkaev.zixamc.consoleintegration

import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.Layout
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginElement
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.apache.logging.log4j.core.layout.PatternLayout
import java.io.Serializable
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Plugin(name = "ZIXA_consoleFeature", category = "Core", elementType = "appender", printObject = true)
class ConsoleLogAppender(
    name: String,
    filter: Filter?,
    layout: Layout<out Serializable>?,
    ignoreExceptions: Boolean,
    properties: Array<Property>,
) : AbstractAppender(name, filter, layout, ignoreExceptions, properties) {

    override fun append(event: LogEvent) {
        val message = event.message.formattedMessage
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        ConsoleFeature.withScopeAndLock {
            ConsoleFeature.addToBroadcast("[$currentTime] $message")
        }
    }

    companion object {
        @JvmStatic
        @PluginFactory
        fun createAppender(
            @PluginAttribute("name") name: String?,
            @PluginElement("Filter") filter: Filter?,
            @PluginElement("Layout") layout: Layout<out Serializable>?
        ): ConsoleLogAppender {
            val appenderName = name ?: throw IllegalArgumentException("Не указано имя для ConsoleLogAppender")
            val appenderLayout = layout ?: PatternLayout.createDefaultLayout()
            return ConsoleLogAppender(
                appenderName,
                filter,
                appenderLayout,
                true,
                arrayOf()
            )
        }
    }
}
