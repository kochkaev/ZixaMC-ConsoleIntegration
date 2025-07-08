package ru.kochkaev.zixamc.consoleintegration

import net.fabricmc.loader.api.FabricLoader
import ru.kochkaev.zixamc.api.config.ConfigFile
import java.io.File

data class Config(
    val display: String = "Консоль 💻",
    val description: String = "<b>Вы можете связать эту группу с консолью Minecraft сервера! 💻</b>\nВы сможете видеть всё сообщения из консоли в этой группе, а также сможете выполнять команды сервера.\nВы можете изменить параметры функции в настройках » /settings",
    val doneTopic: String = "<b>Готово!</b> 🎉\nТеперь этот топик синхронизирован с консолью сервера!\nВы можете изменить параметры функции в настройках » /settings",
    val doneNoTopic: String = "<b>Готово!</b> 🎉\nТеперь эта группа синхронизирована с консолью сервера!",
    val options: String = "» ID топика -> <code>{topicId}</code>",
    val newSession: String = "<b>Стартовала новая сессия!</b> ✅",
    val stopSession: String = "<b>Сессия завершена</b> ❌",
) {
    companion object: ConfigFile<Config>(
        file = File(FabricLoader.getInstance().configDir.toFile(), "ZixaMC-ConsoleIntegration.json"),
        model = Config::class.java,
        supplier = ::Config
    )
}
