package com.h2g.latoken.telebot

import com.h2g.common.repository.H2gRepository
import com.h2g.common.repository.data.newanswers.NewAnswersMessage
import com.h2g.common.repository.data.newanswers.PersonalDataMessage
import com.h2g.latoken.telebot.interviewer.Interviewer
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.extensions.utils.asCommonUser
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.updates.hasNoCommands
import dev.inmo.tgbotapi.types.chat.PreviewChat
import dev.inmo.tgbotapi.types.message.HTML
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Telebot {
    private val questionnaireName = System.getenv("questionnaireName")
    private var questionnaireInfo = H2gRepository.get.questionnaire(questionnaireName, true)
    private val bot = telegramBot(questionnaireInfo.tgToken)
    private val logger = LoggerFactory.getLogger(Telebot::class.java)
    private val tgIdToInterviewer = mutableMapOf<Long, Interviewer>()


    init
    {
        runBlocking { run() }
    }

    private suspend fun run()
    {
        bot.buildBehaviourWithLongPolling {
            //ON COMMAND
            onCommand("start"){
                sendMessageToUser(it.chat, questionnaireInfo.startMessage)
            }
            /*
            onCommand("run")
            {commonMessage->
                newSenseAction(commonMessage)
            }
             */
            //ON MESSAGE
            onText{commonMessage->
                if(commonMessage.hasNoCommands())
                    newMessage(commonMessage)
            }
        }.join()
    }

    /**
     * Start new sense action.
     *  trigger on /run command
     */
    private suspend fun newSenseAction(commonMessage: CommonMessage<TextContent>)
    {
        val interviewer = Interviewer(questionnaireInfo)
        tgIdToInterviewer[commonMessage.chat.id.chatId] = interviewer
    }


    private suspend fun sendMessageToUser(chat: PreviewChat, message : String, warnLog : Boolean = false, html : Boolean = false)
    {
        if(warnLog)
            logger.warn("TD[${chat.id.chatId}] : $message")
        else
            logger.info("TD[${chat.id.chatId}] : $message")
        if(html)
            bot.send(chat, message, parseMode = HTML)
        else
            bot.send(chat,message)
    }


    private suspend fun CoroutineScope.newMessage(commonMessage: CommonMessage<TextContent>)
    {
        if(!tgIdToInterviewer.containsKey(commonMessage.chat.id.chatId)) {
            newSenseAction(commonMessage)
        }
            val interviewer = tgIdToInterviewer[commonMessage.chat.id.chatId]!!
            val questionsIsOver : Boolean = interviewer.setAnswerOnQuestion(commonMessage.content.text)
            if(interviewer.interviewInProcess)
            {
                sendMessageToUser(commonMessage.chat, questionnaireInfo.stopMessage)
            }
            else if(!questionsIsOver) {
                val questionText = interviewer.getQuestionText()
                sendMessageToUser(commonMessage.chat, questionText)
            }
            else{
                sendMessageToUser(commonMessage.chat, questionnaireInfo.stopMessage)
                launch {
                    val answer = interviewer.start()
                    sendMessageToUser(commonMessage.chat, answer, false, true)
                    if(questionnaireInfo.advertising.length>1)
                        sendMessageToUser(commonMessage.chat, questionnaireInfo.advertising, false, false)
                    launch {
                        sendResults(commonMessage, answer, interviewer.getUserResponseList())
                    }
                    tgIdToInterviewer.remove(commonMessage.chat.id.chatId)
                }
        }

    }


    /**
     * Send test results to the server.
     */
    private suspend fun sendResults(commonMessage: CommonMessage<TextContent>, aiAnswer : String, userResponseList: List<String>)
    {
        val chat  = commonMessage.chat
        val user = commonMessage.from
        val userId : Long = if(user!=null) user.id.chatId else chat.id.chatId
        val personalDataMessage = PersonalDataMessage(userId.toString())
        fun addIfNotEmpty(paramName : String, paramValue : String?)
        {
            if(!paramValue.isNullOrEmpty())
                personalDataMessage.data[paramName]=paramValue
        }

        if(user!=null) {
            addIfNotEmpty("firstName", user.firstName)
            addIfNotEmpty("lastName", user.lastName)
            if(user.username!=null)
                addIfNotEmpty("username", user.username!!.withoutAt)
            val commonUser = user.asCommonUser()
            if(commonUser!=null)
            {
                addIfNotEmpty("isPremium", commonUser.isPremium.toString())
                addIfNotEmpty("languageCode", commonUser.languageCode)
            }

        }
        val newAnswersMessage = NewAnswersMessage(personalDataMessage, questionnaireName)
        newAnswersMessage.aiAnswer = aiAnswer
        newAnswersMessage.answerList = userResponseList.mapIndexed { index, s -> Pair(index, s) }.toMap()
        H2gRepository.set.answers(newAnswersMessage)
    }

}