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
        info("""??????Project??????????????????Bot""")
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

                startsWith("${commandPrefix}help"){subject.sendMessage("????????????Bot")}
                startsWith("${commandPrefix}updateWL") {
                    logger.info { "???????????????" }
                    subject.sendMessage("???????????????")
                    updateWhitelist(bot)
                    logger.info { "???????????????" }
                    subject.sendMessage("???????????????")
                }
                startsWith("${commandPrefix}????????????"){
                    subject.sendMessage((airingQueue.peek()?:"???????????????").deserializeMiraiCode())
                }
                startsWith("${commandPrefix}????????????"){
                    airingQueue.iterator().forEach { m -> subject.sendMessage(m.deserializeMiraiCode()) }
                }
                startsWith("${commandPrefix}????????????"){
                    bot.groups[adminGroup]?.settings!!.isMuteAll = true

                    subject.sendMessage("??????????????????????????????????????????")
                    val tmp = selectMessages{
                        has<PlainText>{ return@has message }
                        default { return@default message }
                        timeout(30_000){ subject.sendMessage(PlainText("??????30?????????????????????")); PlainText( "").toMessageChain()}
                    }

                    if (!(tmp.content.isEmpty()||tmp.content.isBlank())){
                        airingQueue.add(tmp.serializeToMiraiCode())
                        subject.sendMessage("????????????")
                    }else{
                        subject.sendMessage("????????????????????????")
                    }

                    bot.groups[adminGroup]?.settings!!.isMuteAll = false

                }
                startsWith("${commandPrefix}????????????"){
                    airingQueue.clear()
                    subject.sendMessage("????????????")
                }
                startsWith("${commandPrefix}??????"){
                    if (airingQueue.peek() != null) {
                        var groupsText = messageChainOf()
                        bot.groups.forEach { if (it.id != adminGroup){groupsText+=PlainText("${it.name}\n")} }
                        subject.sendMessage(PlainText("[!!????????????!!]\n")
                                + PlainText("==={????????????}===")
                                + (airingQueue.peek()?:"???????????????").deserializeMiraiCode()
                                + "={??????????????????}="
                                + groupsText
                        ).quoteReply("????????????").let {  }
                        val selectedMessage = (airingQueue.poll()?:"???????????????").deserializeMiraiCode()
                        airing(this,selectedMessage)
                    }else{
                        subject.sendMessage("???????????????")
                        return@startsWith
                    }
                }

        }

       /* messageEvent.subscribeGroupMessages(priority = EventPriority.HIGH){
            startsWith("${commandPrefix}??????"){
                if (it.isEmpty()||it.isBlank()){
                    subject.sendMessage("??????????????????????????????????????????")
                }else{
                    bot.groups[adminGroup]?.sendMessage(this.message)
                }
            }
        }
*/

    }
}