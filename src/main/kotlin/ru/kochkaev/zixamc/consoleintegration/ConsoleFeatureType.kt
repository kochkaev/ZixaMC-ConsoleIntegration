package ru.kochkaev.zixamc.consoleintegration

import ru.kochkaev.zixamc.api.formatLang
import ru.kochkaev.zixamc.api.sql.data.AccountType
import ru.kochkaev.zixamc.api.telegram.ServerBot.bot
import ru.kochkaev.zixamc.consoleintegration.Config.Companion.config
import ru.kochkaev.zixamc.api.sql.feature.TopicFeatureType
import ru.kochkaev.zixamc.api.sql.feature.data.TopicFeatureData
import ru.kochkaev.zixamc.api.telegram.ServerBot

object ConsoleFeatureType: TopicFeatureType<TopicFeatureData>(
    model = TopicFeatureData::class.java,
    serializedName = "CONSOLE",
    tgDisplayName = { config.display },
    tgDescription = { config.description },
    tgOnDone = {
        if (bot.getChat(it.id).isForum)
            config.doneTopic
        else config.doneNoTopic
    },
    checkAvailable = { it.hasProtectedLevel(AccountType.ADMIN) },
    getDefault = { TopicFeatureData() },
    optionsResolver = {
        config.options.formatLang(
            "topicId" to (it.topicId?.toString() ?: ServerBot.config.group.settings.nullTopicPlaceholder),
        )
    },
)