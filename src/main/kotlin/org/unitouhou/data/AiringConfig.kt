package org.unitouhou.data

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import org.unitouhou.util.MutableQueue
import org.unitouhou.util.mutableQueueOf
import java.util.*

object AiringConfig : AutoSavePluginConfig("UTAiringConfig") {
    var selectedGroup by value<MutableList<Long>>()
    var airingQueue by value<MutableQueue<String>>(mutableQueueOf())
    val airingSuffix by value("")
}