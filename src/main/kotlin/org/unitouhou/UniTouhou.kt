package org.unitouhou

import io.ktor.http.*
import kotlinx.coroutines.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.ConsoleCommandSender.bot
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.disable
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.isAdministrator
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.info
import okhttp3.internal.toHexString
import org.unitouhou.data.AiringConfig
import org.unitouhou.data.AiringConfig.airingQueue
import org.unitouhou.data.AiringConfig.airingSuffix
import org.unitouhou.data.MainConfig
import org.unitouhou.data.MainConfig.adminGroup
import org.unitouhou.data.MainConfig.commandPrefix
import org.unitouhou.data.MainConfig.whitelist
import org.unitouhou.data.MainConfig.whitelistGroup
import org.unitouhou.handlers.airing
import org.unitouhou.handlers.onBotInvitedJoinGroup
import org.unitouhou.handlers.updateWhitelist
import org.unitouhou.util.mutableQueueOf
import java.io.InputStream
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.properties.Delegates
import kotlin.reflect.KMutableProperty

var airingEmergencyStop:Boolean by Delegates.observable(false){ prop,old,new ->
    prop as KMutableProperty<*>
    UniTouhou.logger.info("AESEventCall-[old:$old,new:$new]")
    if (!old&&new){
        UniTouhou.logger.info("AESEventStart-[old:$old,new:$new]")
        UniTouhou.launch(Dispatchers.IO+CoroutineName("AESEvent@${(0..0xFFFFFFFF).random().toHexString()}")){
            delay(5000)
            UniTouhou.logger.info("AESEventReset-[old:$old,new:$new]")
            airingEmergencyStop = false
        }
    }else{
        UniTouhou.logger.info("AESEventFailed-[old:$old,new:$new]")
    }
}

object UniTouhou : KotlinPlugin(
    JvmPluginDescription(
        id = "org.unitouhou.UniTouhou",
        name = "UniTouhou",
        version = "1.0.0",
    ) {
        author("BoundaryBreakerStudio")
        info("""东方Project咨询统一推送Bot""")
    }
) {

    private val AuthCoroutineScope = CoroutineScope(EmptyCoroutineContext)

    private val authEvent = GlobalEventChannel
        .filter { it is BotInvitedJoinGroupRequestEvent || it is NewFriendRequestEvent }
        .parentScope(AuthCoroutineScope)
    private val messageEvent = GlobalEventChannel
        //.filter { it is MessageEvent }
        .parentScope(this)

    override fun onEnable() {
        MainConfig.reload()
        AiringConfig.reload()
        logger.info { "Plugin loaded" }

        authEvent.subscribeAlways(handler = BotInvitedJoinGroupRequestEvent::onBotInvitedJoinGroup)
        messageEvent.subscribeGroupMessages{

        }
        messageEvent
            .filter { it is GroupMessageEvent && it.subject.id == adminGroup }
            .subscribeMessages(priority = EventPriority.HIGHEST){
                startsWith("${commandPrefix}AES"){ airingEmergencyStop = true}
            }
        messageEvent
            .filter { it is GroupMessageEvent && it.subject.id == adminGroup }
            .subscribeMessages {

                startsWith("${commandPrefix}help"){subject.sendMessage("东方推送Bot")}
                startsWith("${commandPrefix}updateWL") {
                    logger.info { "更新白名单" }
                    subject.sendMessage("更新白名单")
                    updateWhitelist(bot)
                    logger.info { "更新完成！" }
                    subject.sendMessage("更新完成！")
                }
                startsWith("${commandPrefix}查看队首"){
                    subject.sendMessage((airingQueue.peek()?:"队列为空！").deserializeMiraiCode())
                }
                startsWith("${commandPrefix}查看队列"){
                    airingQueue.iterator().forEach { m -> subject.sendMessage(m.deserializeMiraiCode()) }
                }
                startsWith("${commandPrefix}加入队列"){
                    bot.groups[adminGroup]?.settings!!.isMuteAll = true

                    subject.sendMessage("请在命令后跟随要发送的文案！")
                    val tmp = selectMessages{
                        has<PlainText>{ return@has message }
                        default { return@default message }
                        timeout(30_000){ subject.sendMessage(PlainText("请在30秒内输入文案！")); PlainText( "").toMessageChain()}
                    }

                    if (!(tmp.content.isEmpty()||tmp.content.isBlank())){
                        airingQueue.add(tmp.serializeToMiraiCode())
                        subject.sendMessage("存贮完毕")
                    }else{
                        subject.sendMessage("文案不能为空白！")
                    }

                    bot.groups[adminGroup]?.settings!!.isMuteAll = false

                }
                startsWith("${commandPrefix}清空队列"){
                    airingQueue.clear()
                    subject.sendMessage("清空完毕")
                }
                startsWith("${commandPrefix}广播"){
                    if (airingQueue.peek() != null) {
                        var groupsText = messageChainOf()
                        bot.groups.forEach { if (it.id != adminGroup){groupsText+=PlainText("${it.name}\n")} }
                        subject.sendMessage(PlainText("[!!准备广播!!]\n")
                                + PlainText("==={消息预览}===")
                                + (airingQueue.peek()?:"队列为空！").deserializeMiraiCode()
                                + "={消息将发送至}="
                                + groupsText
                        ).quoteReply("开始发送").let {  }
                        val selectedMessage = (airingQueue.poll()?:"队列为空！").deserializeMiraiCode()
                        airing(this,selectedMessage)
                    }else{
                        subject.sendMessage("队列为空！")
                        return@startsWith
                    }
                }

        }

       /* messageEvent.subscribeGroupMessages(priority = EventPriority.HIGH){
            startsWith("${commandPrefix}投稿"){
                if (it.isEmpty()||it.isBlank()){
                    subject.sendMessage("请在命令后跟随要发送的文案！")
                }else{
                    bot.groups[adminGroup]?.sendMessage(this.message)
                }
            }
        }
*/

    }
}