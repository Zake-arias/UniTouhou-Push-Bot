package org.unitouhou.handlers

import kotlinx.coroutines.delay
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.BotIsBeingMutedException
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.SendMessageFailedException
import net.mamoe.mirai.contact.isAdministrator
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.AtAll
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageSource
import org.unitouhou.UniTouhou
import org.unitouhou.airingEmergencyStop
import org.unitouhou.data.AiringConfig.airingSuffix
import org.unitouhou.data.MainConfig

fun updateWhitelist(bot: Bot){
    for (group in (MainConfig.whitelistGroup + MainConfig.adminGroup)){
        MainConfig.whitelist += bot.groups[group]?.members?.map { it.id } ?: mutableListOf()
        MainConfig.whitelist = MainConfig.whitelist.distinct().toMutableList()
    }
    MainConfig.whitelist = MainConfig.whitelist.distinct().toMutableList()
}
suspend fun airing(event:MessageEvent, selectedMessage: MessageChain){
    event.run {
        var succeed = 0
        var inMuted = 0
        var failed = 0

        var messageSources = mutableListOf<MessageReceipt<Group>>()

        bot.groups.forEach {
            if (!airingEmergencyStop) {
                if (it.id != MainConfig.adminGroup) {
                    try {
                        if (it.botPermission.isAdministrator()){
                            //messageSources += it.sendMessage(AtAll +"\n"+selectedMessage+airingSuffix)
                            succeed += 1
                        }else{
                            //messageSources += it.sendMessage(selectedMessage+airingSuffix)
                            succeed += 1
                        }
                    } catch (e: BotIsBeingMutedException) {
                        bot.logger.warning("在群${it.name}(${it.id})中被禁言，跳过发送")
                        inMuted += 1
                    }catch (e:SendMessageFailedException){
                        bot.logger.warning("在群${it.name}(${it.id})中${e.message}")
                        //messageSources += it.sendMessage(selectedMessage+airingSuffix)
                        failed += 1
                    }
                    val offset = (50..200).random().toLong()
                    bot.logger.warning("在群${it.name}(${it.id})发送完毕，(${offset}ms)")
                    delay(offset)
                }
            } else {
                messageSources.forEach { it.recall() }
                subject.sendMessage(
                    """
=========
=紧急停止=
========="""
                            +"\n消息已撤回\n"
                            +"发送完毕\n成功发送$succeed 次\n被禁言$inMuted 次\n发送异常$failed 次"
                )
                bot.logger.warning(
                    """
===========================
=         紧急停止         =
==========================="""
                )

                return
            }
        }
        subject.sendMessage("发送完毕\n成功发送$succeed 次\n被禁言$inMuted 次\n发送异常$failed 次")
    }
}