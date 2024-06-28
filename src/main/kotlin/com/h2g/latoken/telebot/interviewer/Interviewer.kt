package com.h2g.latoken.telebot.interviewer

import com.h2g.common.aiconnector.AiConnector
import com.h2g.common.repository.data.questionnaire.QuestionnaireMessage

class Interviewer(private val questionnaireInfo  :QuestionnaireMessage) {
    private val aiConnector = AiConnector(System.getenv("chatGptUrl"))
    private val interviewerRequestData = InterviewerRequestData()

    @Volatile
    var interviewInProcess = false
        private set

    /********OTHER*******/

    /**
     * Get user response list on questions.
     */
    fun getUserResponseList() : List<String>
    {
        return interviewerRequestData.userResponseList
    }


    /******START*******/
    fun start() : String
    {
        interviewInProcess = true
        val messageForAi = getMessageForAi()
        try {
            return aiConnector.request(messageForAi)
        }
        catch (ex : Exception) {
            return ex.stackTraceToString()
        }
        finally {
            interviewInProcess = false
        }
    }

    private fun getMessageForAi() :String
    {
        var messageForAi = questionnaireInfo.messageBeforeQuestions + "\n"
        for(i in 0..<questionnaireInfo.questionMap.size)
        {
            messageForAi = messageForAi+questionnaireInfo.questionMap[i]+ " \n" + interviewerRequestData.userResponseList[i] + "\n\n"
        }
        return messageForAi
    }


    /*****QUESTION*****/

    /**
     * After calling this method, the getQuestionText() function will return the next question
     * (if the current question is not the last one).
     */
    fun getQuestionText(): String
    {
        return questionnaireInfo.questionMap[currentQuestionNumber()]!!
    }

    /**
     * Provide an answer to the current question.
     *
     * @return Are we done collecting the list of questions?
     */
    fun setAnswerOnQuestion(answer  :String) : Boolean
    {
        interviewerRequestData.userResponseList.add(answer)
        return isTestOver()
    }

    /**
     * @return Are we done collecting the list of questions?
     */
    private fun isTestOver(): Boolean
    {
        return currentQuestionNumber()>=questionnaireInfo.questionMap.size
    }
    private fun currentQuestionNumber() = interviewerRequestData.userResponseList.size

}