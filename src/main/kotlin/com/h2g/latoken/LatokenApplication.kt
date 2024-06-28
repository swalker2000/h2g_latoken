package com.h2g.latoken

import com.h2g.common.logger.LogAction
import com.h2g.latoken.telebot.Telebot
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LatokenApplication

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("Main")
    com.h2g.common.logger.LoggerFactory.debugLogAction = LogAction { logger.debug(it) }
    com.h2g.common.logger.LoggerFactory.infoLogAction = LogAction { logger.info(it) }
    com.h2g.common.logger.LoggerFactory.warnLogAction = LogAction { logger.warn(it) }
    com.h2g.common.logger.LoggerFactory.errorLogAction = LogAction { logger.error(it) }
    runApplication<LatokenApplication>(*args)
}
