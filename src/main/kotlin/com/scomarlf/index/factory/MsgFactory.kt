package com.scomarlf.index.factory

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup
import com.pengrad.telegrambot.request.SendMessage
import com.scomarlf.index.MismatchException
import com.scomarlf.index.datasource.Elasticsearch
import com.scomarlf.index.datasource.Reply
import com.scomarlf.index.datasource.Telegram
import com.scomarlf.index.datasource.Type
import org.springframework.stereotype.Component

@Component
class MsgFactory(
    private val reply: Reply,
    private val type: Type,
    private val elasticsearch: Elasticsearch
) {

    fun makeEnrollMsg(chatId: Long, telegramMod: Telegram.TelegramMod, enrollId: String): SendMessage {
        val content = makeRecordDetail(telegramMod)
        val keyboard = makeEnrollKeyboardMarkup(enrollId)
        return SendMessage(chatId, content).disableWebPagePreview(true).parseMode(ParseMode.HTML).replyMarkup(keyboard)
    }

    fun makeEnrollMsg(chatId: Long, enrollId: String): SendMessage {
        val enroll = elasticsearch.getEnroll(enrollId)!!
        val detail = makeRecordDetail(enroll)
        val keyboard = makeEnrollKeyboardMarkup(enrollId)
        return SendMessage(chatId, detail)
            .replyMarkup(keyboard)
            .parseMode(ParseMode.HTML)
            .disableWebPagePreview(true)
            .replyMarkup(keyboard)
    }

    fun makeEnrollChangeClassificationMsg(chatId: Long, enrollId: String): SendMessage {
        val enroll = elasticsearch.getEnroll(enrollId)!!
        val detail = makeRecordDetail(enroll)
        val keyboard = makeInlineKeyboardMarkup(enrollId)
        val msg = SendMessage(chatId, detail)
        return msg.parseMode(ParseMode.HTML).disableWebPagePreview(true).replyMarkup(keyboard)
    }

    fun makeReplyMsg(chatId: Long, replyType: String): SendMessage {
        return SendMessage(chatId, reply.message[replyType])
    }

    fun makeListReplyMsg(chatId: Long): SendMessage {
        val keyboard = makeReplyKeyboardMarkup()
        return SendMessage(chatId, "list").replyMarkup(keyboard)
    }

    fun makeExceptionMsg(chatId: Long, e: Exception): SendMessage {
        return when (e) {
            is MismatchException -> SendMessage(chatId, e.message)
            else -> SendMessage(chatId, "未知错误")
        }
    }

    private fun makeRecordDetail(mod: Telegram.TelegramMod): String {
        val link = "https://t.me/${mod.id}"
        val detailSB = StringBuffer()
        detailSB.append("<b>标题</b>： <a href=\"$link\">${mod.title}</a>\n")
        detailSB.append("<b>标签</b>： 暂无\n")
        detailSB.append("<b>分类</b>： 暂无\n")
        detailSB.append("<b>简介</b>：\n")
        detailSB.append(mod.about.replace("<", "&lt;").replace(">", "&gt;"))
        return detailSB.toString()
    }

    private fun makeRecordDetail(enroll: Elasticsearch.Enroll): String {
        val link = "https://t.me/${enroll.code}"
        val detailSB = StringBuffer()
        detailSB.append("<b>标题</b>： <a href=\"$link\">${enroll.title}</a>\n")
        detailSB.append("<b>标签</b>： ${if (enroll.tags == null) "暂无" else enroll.tags.joinToString(" ")}\n")
        detailSB.append("<b>分类</b>： ${enroll.classification ?: "暂无"}\n")
        detailSB.append("<b>简介</b>：\n")
        detailSB.append(enroll.about.replace("<", "&lt;").replace(">", "&gt;"))
        return detailSB.toString()
    }

    private fun makeReplyKeyboardMarkup(): ReplyKeyboardMarkup {
        // 每行countInRow数量个按钮
        val countInRow = 3
        // 将多个类型按照countInRow拆分为多行
        var counter = 0
        val rows = mutableListOf<Array<String>>()
        while (counter < type.types.size) {
            var endOfIndex = counter + countInRow
            endOfIndex = if (endOfIndex <= type.types.size) endOfIndex else type.types.size
            val row = type.types.copyOfRange(counter, endOfIndex)
            counter += countInRow
            rows.add(row)
        }
        // 制作键盘
        val keyboard = ReplyKeyboardMarkup(*rows.toTypedArray())
        keyboard.oneTimeKeyboard(false)
        keyboard.resizeKeyboard(true)
        keyboard.selective(true)
        return keyboard
    }

    private fun makeInlineKeyboardMarkup(id: String): InlineKeyboardMarkup {
        // 每行countInRow数量个按钮
        val countInRow = 3
        // 将多个类型按照countInRow拆分为多行
        var counter = 0
        val buttonLines = mutableListOf<Array<InlineKeyboardButton>>()
        while (counter < type.types.size) {
            var endOfIndex = counter + countInRow
            endOfIndex = if (endOfIndex <= type.types.size) endOfIndex else type.types.size
            val row = type.types.copyOfRange(counter, endOfIndex)
            val buttons = row.map {
                InlineKeyboardButton(it).callbackData("classification:$it&$id")
            }.toTypedArray()
            buttonLines.add(buttons)
            counter += countInRow
        }
        return InlineKeyboardMarkup(*buttonLines.toTypedArray())
    }

    private fun makeEnrollKeyboardMarkup(id: String): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            arrayOf(
                InlineKeyboardButton("✍编辑标题").callbackData("enroll:title&$id"),
                InlineKeyboardButton("✍编辑简介").callbackData("enroll:about&$id"),
            ),
            arrayOf(
                InlineKeyboardButton("✍编辑标签").callbackData("enroll:tags&$id"),
                InlineKeyboardButton("✍编辑分类").callbackData("enroll:classification&$id"),
            ),
            arrayOf(
                InlineKeyboardButton("✅提交").callbackData("enroll:submit&$id"),
                InlineKeyboardButton("❎取消").callbackData("enroll:cancel&$id"),
            )
        )
    }

    private fun makeApproveKeyboardMarkup(id: String): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            arrayOf(
                InlineKeyboardButton("✅通过").callbackData("approve:pass&$id"),
                InlineKeyboardButton("❎不通过").callbackData("approve:fail&$id"),
            )
        )
    }
}