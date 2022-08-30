package org.unitouhou.data

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.contact.Group

object MainConfig : AutoSavePluginConfig("UTMainConfig") {
    val commandPrefix by value("$")
    var adminList by value<MutableList<Long>>()
    var adminGroup by value<Long>()
    var whitelistGroup by value<MutableList<Long>>(mutableListOf())
    var whitelist by value<MutableList<Long>>()
}