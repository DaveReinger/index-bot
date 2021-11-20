package com.tgse.index.area.msgFactory

import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.EditMessageText
import com.pengrad.telegrambot.request.SendMessage
import com.tgse.index.infrastructure.provider.BotProvider
import com.tgse.index.domain.service.ClassificationService
import com.tgse.index.domain.service.RecordService
import com.tgse.index.domain.service.ReplyService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class ListMsgFactory(
    override val botProvider: BotProvider,
    override val replyService: ReplyService,
    private val classificationService: ClassificationService,
    private val recordService: RecordService,
    @Value("\${secretary.list.size}")
    private val perPageSize: Int,
    @Value("\${secretary.memory.size}")
    private val memorySize: Int,
    @Value("\${secretary.memory.cycle}")
    private val memoryCycle: Int
) : BaseMsgFactory(replyService, botProvider) {

    private val searchListSaved = mutableMapOf<String, MutableMap<Int, Pair<MutableList<RecordService.Record>, Long>>>()
    private val searchListSavedTimers = mutableMapOf<String, MutableMap<Int, Timer>>()
    private var searchListSavedCount = 0

    fun makeListFirstPageMsg(chatId: Long, keywords: String, pageIndex: Int): SendMessage? {
        val range = IntRange(((pageIndex - 1) * perPageSize), pageIndex * perPageSize)
        val (records, totalCount) = searchList(keywords,range.first)
        if (totalCount == 0L) return null
        val sb = StringBuffer()
        records.forEach {
            val item = generateListItem(it)
            sb.append(item)
        }
        val keyboard = makeListPageKeyboardMarkup(keywords, totalCount, pageIndex, perPageSize, range)
        return SendMessage(chatId, sb.toString())
            .parseMode(ParseMode.HTML).disableWebPagePreview(true).replyMarkup(keyboard)
    }

    fun makeListNextPageMsg(chatId: Long, messageId: Int, keywords: String, pageIndex: Int): EditMessageText {
        val range = IntRange(((pageIndex - 1) * perPageSize), pageIndex * perPageSize)
        val (records, totalCount) = searchList(keywords, range.first)
        val sb = StringBuffer()
        records.forEach {
            val item = generateListItem(it)
            sb.append(item)
        }
        val keyboard = makeListPageKeyboardMarkup(keywords, totalCount, pageIndex, perPageSize, range)
        return EditMessageText(chatId, messageId, sb.toString())
            .parseMode(ParseMode.HTML).disableWebPagePreview(true).replyMarkup(keyboard)
    }

    private fun searchList(keywords: String, from: Int): Pair<MutableList<RecordService.Record>, Long> {
        // 如若已暂存直接返回
        val saved = get(keywords, from)
        if (saved != null) return saved
        // 如若未暂存，去elasticsearch中查询
        val isShouldConsiderKeywords = classificationService.contains(keywords)
        val searched =
            if (isShouldConsiderKeywords) recordService.searchRecordsByClassification(keywords, from, perPageSize)
            else recordService.searchRecordsByKeyword(keywords, from, perPageSize)
        save(keywords,from,searched)
        return searched
    }

    private fun makeListPageKeyboardMarkup(
        keywords: String,
        totalCount: Long,
        pageIndex: Int,
        perPageSize: Int,
        range: IntRange
    ): InlineKeyboardMarkup {
        return when {
            totalCount > perPageSize && range.first == 0 ->
                InlineKeyboardMarkup(
                    arrayOf(
                        InlineKeyboardButton("下一页").callbackData("page:$keywords&${pageIndex + 1}"),
                    )
                )
            totalCount > perPageSize && range.first != 0 && range.last < totalCount ->
                InlineKeyboardMarkup(
                    arrayOf(
                        InlineKeyboardButton("上一页").callbackData("page:$keywords&${pageIndex - 1}"),
                        InlineKeyboardButton("下一页").callbackData("page:$keywords&${pageIndex + 1}"),
                    )
                )
            totalCount > perPageSize && range.last >= totalCount ->
                InlineKeyboardMarkup(
                    arrayOf(
                        InlineKeyboardButton("上一页").callbackData("page:$keywords&${pageIndex - 1}"),
                    )
                )
            else -> InlineKeyboardMarkup(
                arrayOf(
                    InlineKeyboardButton("高性价比机场魔法云").url("https://t.me/magic_cfw"),
                )
            )
        }
    }

    @Synchronized
    private fun get(keywords: String, from: Int): Pair<MutableList<RecordService.Record>, Long>? {
        return if (searchListSaved[keywords] != null && searchListSaved[keywords]!![from] != null)
            searchListSaved[keywords]!![from]!!
        else
            null
    }

    @Synchronized
    private fun save(keywords: String, from: Int, searchList: Pair<MutableList<RecordService.Record>, Long>) {
        if (searchListSavedCount >= memorySize) return
        if (searchListSaved[keywords] == null) searchListSaved[keywords] = mutableMapOf()
        searchListSaved[keywords]!![from] = searchList
        searchListSavedCount += 1

        val timer = Timer("saved-list-cancel", true)
        val timerTask = object : TimerTask() {
            override fun run() {
                try {
                    remove(keywords, from)
                } catch (e: Throwable) {
                    // ignore
                }
            }
        }
        timer.schedule(timerTask, memoryCycle * 1000L)
        if (searchListSavedTimers[keywords] == null) searchListSavedTimers[keywords] = mutableMapOf()
        if (searchListSavedTimers[keywords]!![from] != null) searchListSavedTimers[keywords]!![from]!!.cancel()
        searchListSavedTimers[keywords]!![from] = timer
    }

    @Synchronized
    private fun remove(keywords: String, from: Int) {
        if (searchListSaved[keywords]!!.size == 1) searchListSaved.remove(keywords)
        else searchListSaved[keywords]!!.remove(from)
        searchListSavedCount -= 1
    }

}
