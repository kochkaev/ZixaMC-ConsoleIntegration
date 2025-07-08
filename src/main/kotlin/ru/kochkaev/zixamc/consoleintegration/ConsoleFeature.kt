package ru.kochkaev.zixamc.consoleintegration

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.kochkaev.zixamc.api.telegram.ServerBot.bot
import ru.kochkaev.zixamc.consoleintegration.Config.Companion.config
import ru.kochkaev.zixamc.api.ZixaMC
import ru.kochkaev.zixamc.api.telegram.model.TgMessage
import ru.kochkaev.zixamc.api.sql.SQLChat
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.api.sql.data.AccountType
import ru.kochkaev.zixamc.api.sql.data.NewProtectedData
import ru.kochkaev.zixamc.api.telegram.model.TgReplyParameters
import ru.kochkaev.zixamc.api.Initializer.coroutineScope
import ru.kochkaev.zixamc.api.escapeHTML

object ConsoleFeature {
    private val groups: HashMap<Long, Int?>
        get() = SQLGroup.getAllWithFeature(ConsoleFeatureType).fold(hashMapOf()) { acc, sql ->
            sql.features.getCasted(ConsoleFeatureType)?.run {
                acc[sql.id] = this.topicId
            }
            acc
        }
    private val accumulatedMessages = StringBuilder()
    private var lastMessage: String? = null
    private val lastedMessage: HashMap<Long, Int?> = hashMapOf()
    private val broadcastLock = Mutex()
    lateinit var job: Job
    private suspend fun broadcast(message: String) {
        groups.forEach { (chatId, topicId) ->
            if (listOf(true, null).contains(lastedMessage[chatId]?.let { lastMessage!=null && lastMessage!!.length + message.length > 4096 })) {
                try {
                    lastedMessage[chatId] = bot.sendMessage(
                        chatId = chatId,
                        text = message,
                        messageThreadId = topicId,
                        protectContent = true,
                    ).also { SQLChat.get(chatId)?.setProtectedInfoMessage(
                        message = it,
                        protectLevel = AccountType.ADMIN,
                        protectedType = NewProtectedData.ProtectedType.TEXT,
                        senderBotId = bot.me.id,
                    ) } .messageId
                    lastMessage = message
                } catch (_: Exception) {}
            } else {
                try {
                    lastedMessage[chatId] = lastedMessage[chatId]!!.let {
                        lastMessage += message
                        bot.editMessageText(
                            chatId = chatId,
                            messageId = it,
                            text = lastMessage!!,
                        ).messageId
                    }
                } catch (_: Exception) {}
            }
        }
    }
    fun addToBroadcast(message: String) {
        accumulatedMessages.append("\n" + message)
    }
    fun startPeriodicBroadcast() {
        job = coroutineScope.launch {
            broadcast(config.newSession)
            while (isActive) {
                delay(10_000L) // ожидание 10 секунд
                val messagesToSend = broadcastLock.withLock {
                    if (accumulatedMessages.isNotEmpty()) {
                        val content = accumulatedMessages.toString()
                        accumulatedMessages.clear()
                        content
                    } else null
                }
                messagesToSend?.let { broadcast(it) }
            }
        }
    }
    fun stopBroadcast() {
        coroutineScope.launch {
            val messagesToSend = broadcastLock.withLock {
                if (accumulatedMessages.isNotEmpty()) {
                    val content = accumulatedMessages.toString()
                    accumulatedMessages.clear()
                    content
                } else null
            } + "\n" + config.stopSession
            broadcast(messagesToSend)
            job.cancel()
        }
    }
    fun executeCommand(msg: TgMessage) = withScopeAndLock {
        val group = SQLGroup.get(msg.chat.id) ?: return@withScopeAndLock
        val data = group.features.getCasted(ConsoleFeatureType) ?: return@withScopeAndLock
        if (msg.messageThreadId != data.topicId) return@withScopeAndLock
        val message = msg.effectiveText ?: return@withScopeAndLock
        try {
            lastedMessage[group.id] = null
            lastMessage = null
            ZixaMC.runConsoleCommand(message)
        } catch (e: Exception) {
            e.message?.also {
                bot.sendMessage(
                    chatId = group.id,
                    text = it.escapeHTML(),
                    messageThreadId = data.topicId,
                    replyParameters = TgReplyParameters(msg.messageId),
                    protectContent = true,
                ).also { msg ->
                    group.setProtectedInfoMessage(
                        message = msg,
                        protectLevel = AccountType.ADMIN,
                        protectedType = NewProtectedData.ProtectedType.TEXT,
                        senderBotId = bot.me.id
                    )
                }
            }
        }
    }

    fun withScopeAndLock(fn: suspend () -> Unit) {
        coroutineScope.launch {
            broadcastLock.withLock {
                fn()
            }
        }
    }
}