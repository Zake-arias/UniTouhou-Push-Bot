package org.unitouhou.handlers

import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.message.data.AtAll
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import org.unitouhou.data.MainConfig

suspend fun BotInvitedJoinGroupRequestEvent.onBotInvitedJoinGroup(target: BotInvitedJoinGroupRequestEvent){
    val replay = buildMessageChain {
        +AtAll
        +PlainText("\n[Bot被邀请]\n")
        +PlainText("群名称:\n${target.groupName}\n群号:\n${target.groupId}\n邀请人:${target.invitorNick}\n邀请人QQID:${target.invitorId}")
    }
    if (invitorId in MainConfig.whitelist) {
        replay + PlainText("\n邀请人在白名单内，已自动同意")
        target.accept()
    }
    bot.groups[MainConfig.adminGroup]?.sendMessage(replay)
}