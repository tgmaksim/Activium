package ru.tgmaksim.gymnasium.pages.schedule

import java.util.Locale
import java.time.OffsetDateTime
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.concurrent.CancellationException

import android.os.Bundle
import android.view.View
import android.content.Intent
import android.view.ViewGroup
import android.content.Context
import android.widget.FrameLayout
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.LinearLayoutManager

import ru.tgmaksim.gymnasium.R
import ru.tgmaksim.gymnasium.api.Dnevnik
import ru.tgmaksim.gymnasium.api.Request
import ru.tgmaksim.gymnasium.api.ScheduleDay
import ru.tgmaksim.gymnasium.ui.LoginActivity
import ru.tgmaksim.gymnasium.utilities.Utilities
import ru.tgmaksim.gymnasium.utilities.CacheManager
import ru.tgmaksim.gymnasium.utilities.AlarmReceiver
import ru.tgmaksim.gymnasium.databinding.SchedulePageBinding
import ru.tgmaksim.gymnasium.databinding.ScheduleCalendarDayBinding

/**
 * Страница с расписанием уроков пользователя
 * @author Максим Дрючин (tgmaksim)
 * @see ru.tgmaksim.gymnasium.ui.MainActivity
 * */
class SchedulePage : Fragment() {
    private lateinit var ui: SchedulePageBinding
    private var isDarkTheme: Boolean = false
    private var scheduleLength: Int = CacheManager.scheduleBefore + 1 + CacheManager.scheduleAfter
    /** Текущий выбранный день в виде [ScheduleCalendarDayBinding.root] */
    private lateinit var lastSelected: FrameLayout

    companion object {
        private var schedule: List<ScheduleDay?>? = null
        private var updateToken: String? = null

        /**
         * Создание напоминания о внеурочном занятии в виде уведомления за несколько минут до начала
         * @param context Android-context
         * @author Максим Дрючин (tgmaksim)
         * */
        fun createRemindEA(context: Context) {
            // Следующее по времени внеурочное занятие (сегодня или в другой день)
            val scheduleDay = schedule?.find {
                val startTimeEA = it?.hoursEA?.startTime ?: return@find false
                it.ea.any() &&
                        (it.date > Utilities.localDate() ||
                                (it.date == Utilities.localDate() && startTimeEA > Utilities.localTime()))
            } ?: return  // Внеурочных занятий не найдено

            AlarmReceiver.createRemindEA(
                context,
                scheduleDay.ea,
                scheduleDay.date.with(scheduleDay.hoursEA!!.startTime)
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // При смене страниц ui сохраняется, если не изменилась тема и настройки
        val newScheduleLength = CacheManager.scheduleBefore + 1 + CacheManager.scheduleAfter
        if (::ui.isInitialized && CacheManager.isDarkTheme == isDarkTheme && scheduleLength == newScheduleLength) {
            return ui.root
        }

        ui = SchedulePageBinding.inflate(inflater, container, false)
        isDarkTheme = CacheManager.isDarkTheme
        scheduleLength = newScheduleLength

        // Синхронизация только при первой отрисовке или после входа по ссылке
        val intentData = requireActivity().intent.data
        var needUpdate = false
        if (schedule == null || intentData?.getQueryParameter("updateScheduleToken") != updateToken) {
            updateToken = intentData?.getQueryParameter("updateScheduleToken")
            needUpdate = true
        }

        showScheduleCalendar()  // Отображение даты на несколько дней
        showCacheSchedule()  // Показ расписания из кеша

        if (needUpdate) {
            lifecycleScope.launch {
                ui.swipeRefresh.isRefreshing = true
                loadCloudSchedule()  // Получение актуальных данных
                ui.swipeRefresh.isRefreshing = false
            }
        }

        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utilities.log("SchedulePage загружена", tag="load") {
            param("place", "SchedulePage")
        }
    }

    override fun onStop() {
        super.onStop()
        ui.swipeRefresh.isRefreshing = false
    }

    /**
     * Инициализация адаптера расписания после ее загрузки
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun initDayPagerAdapter() {
        val layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        ui.dayPager.layoutManager = layoutManager

        // Создание адаптера с возможностью перелистывания
        ui.dayPager.adapter = ScheduleAdapter().apply {
            submitList(schedule!!)
        }
        val snapHelper = PagerSnapHelper().apply {
            attachToRecyclerView(ui.dayPager)
        }

        // Выбор активного дня: до 15:00 - текущий, иначе - следующий
        val todayPosition = schedule!!.indexOfFirst { it?.date == Utilities.localDate() }
        if (Utilities.localTime().hour >= 15)
            ui.dayPager.scrollToPosition(todayPosition + 1)  // Прокрутка без анимации
        else
            ui.dayPager.scrollToPosition(todayPosition)  // Возвращение в начальное положение

        // Инициализация обработчика для смены активного дня в календаре
        ui.dayPager.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                // Выбор активного дня в мини-календаре в процессе и после перелистывания
                val snappedView = snapHelper.findSnapView(layoutManager)
                val position = snappedView?.let {
                    layoutManager.getPosition(it)
                } ?: return

                selectItemCalendar(ui.calendar.getChildAt(position) as FrameLayout)
            }
        })

        // Установка цвета в соответствии с темой
        ui.swipeRefresh.setColorSchemeResources(R.color.bg_gradient_center)
        ui.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_gradient_start)

        // Обновление жестом
        ui.swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                loadCloudSchedule()
                ui.swipeRefresh.isRefreshing = false
            }
        }
    }

    /**
     * Получение из кеша и показ сохраненного расписания на нужный день
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun showCacheSchedule() {
        schedule = Dnevnik.getCacheSchedule().filter {
            // Выбор только дней, входящих в период
            val offset = Utilities.localDate().until(it.date, ChronoUnit.DAYS)
            offset in -CacheManager.scheduleBefore..CacheManager.scheduleAfter
        }.sortedBy { it.date }.let { schedule ->
            // Сортировка по дате и добавление пустых дней при необходимости
            val offset = schedule.indexOfFirst { it.date == Utilities.localDate() }

            (List(CacheManager.scheduleBefore - offset) { null } + schedule).let {
                it + List(CacheManager.scheduleBefore + 1 + CacheManager.scheduleAfter - it.size) { null }
            }
        }

        drawWeekends()  // Раскраска выходных дней
        initDayPagerAdapter()  // Инициализация адаптера и показ расписания
    }

    /**
     * Обработка нажатия на системную кнопку назад (или жестом) для перелистывания дней расписания
     * @return true, если действие выполнено, иначе false
     * @author Максим Дрючин (tgmaksim)
     * */
    fun onBackPressed(): Boolean {
        val defaultDate = getDefaultDate()
        if ((lastSelected.tag as OffsetDateTime) != defaultDate) {
            openDay(ui.calendar.findViewWithTag(defaultDate))
            return true
        }

        return false
    }

    /**
     * Загрузка актуального расписания API-запросом на сервер. Инициализация [schedule]
     * @author Максим Дрючин (tgmaksim)
     * */
    private suspend fun loadCloudSchedule() {
        val cacheSchedule = schedule.hashCode()

        try {
            val response = Dnevnik.getSchedule()

            // Если сессия не авторизована, то открывается Login
            // Если произошла ошибка, выводится ошибка
            if (!response.status || response.answer == null) {
                response.error?.type?.let { Utilities.log(it) }
                response.error?.errorMessage?.let { Utilities.showText(requireContext(), it) }

                when (response.error?.type) {
                    "UnauthorizedError" -> {
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        startActivity(intent)
                        // Без закрытия MainActivity по нажатию системной кнопки назад (или жестом)
                        // можно открыть локальное расписания
//                        mainActivity.finish()
                    }
                    in listOf("ValidationError", "ApiMethodNotFoundError") -> {
                        Utilities.showText(requireContext(), R.string.error_incorrect_data)
                    }
                    else -> {
                        Utilities.showText(requireContext(), R.string.error_api)
                    }
                }
            } else {
                schedule = response.answer.schedule  // Сохранение расписания
            }
        } catch (_: CancellationException) {
            ui.swipeRefresh.isRefreshing = false
            return
        } catch (e: Exception) {
            Utilities.log(e)
            if (!Request.checkInternet())
                Utilities.showText(requireContext(), R.string.error_internet)
            else
                Utilities.showText(requireContext(), R.string.error_load_schedule)
            return
        }

        Utilities.log("Успешная загрузка расписания", tag="load") {
            param("place", "schedule")
        }

        // Есть изменения
        if (cacheSchedule != schedule.hashCode()) {
            schedule?.let {
                (ui.dayPager.adapter as ScheduleAdapter).submitList(it)
            }
            drawWeekends()  // Снова раскраска выходных дней
        }
        createRemindEA(requireContext())  // Напоминание о новых внеурочных занятиях
    }

    /**
     * Показ горизонтального мини-календаря с прокруткой для просмотра расписания
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun showScheduleCalendar() {
        val today = Utilities.localDate()
        val firstDay = today.minusDays(CacheManager.scheduleBefore.toLong())
        val time = Utilities.localTime()

        // Заполнение дней
        repeat(scheduleLength) { i ->
            val item = ScheduleCalendarDayBinding.inflate(layoutInflater, ui.calendar, false)
            item.root.setBackgroundResource(R.drawable.bg_button_day_selected)

            // Число и день недели
            val date = firstDay.plusDays(i.toLong())
            item.dayNumber.text = date.dayOfMonth.toString()
            item.root.tag = date // Идентификация по дате

            @Suppress("DEPRECATION")  // У Local нет России
            val dayOfWeek = date.format(DateTimeFormatter.ofPattern("EE", Locale("ru")))
            item.weekday.text = dayOfWeek.uppercase()

            // Выбор активного дня: до 15:00 - текущий, иначе - следующий
            if (date == today && time.hour < 15 || date == today.plusDays(1) && time.hour >= 15) {
                lastSelected = item.root
                selectItemCalendar(item.root)
            }

            if (date == today) {
                item.root.isActivated = true  // Сегодня
            }

            // Определение действия при нажатии
            item.root.setOnClickListener {
                openDay(item.root)
            }

            ui.calendar.addView(item.root)
        }
    }

    /**
     * Открытие определенного дня расписания по нажатии на кнопку, без анимации перехода
     * @param item Объект дня в мини-календаре
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun openDay(item: FrameLayout) {
        selectItemCalendar(item)

        val index = ui.calendar.indexOfChild(item)
        ui.dayPager.scrollToPosition(index)  // Без анимации
    }

    /**
     * Выбор активного дня в мини-календаре
     * @param item Объект дня в мини-календаре
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun selectItemCalendar(item: FrameLayout) {
        // Обновление выделения
        lastSelected.isSelected = false
        item.isSelected = true  // Выбран
        lastSelected = item

        // Центрирование кнопки текущего дня
        ui.calendarScroll.post {
            val scrollTo = item.left - (ui.calendarScroll.width - item.width) / 2
            ui.calendarScroll.smoothScrollTo(scrollTo, 0)
        }
    }

    /**
     * Закрашивание обводки дня в мини-календаре мягким красным цветом, обозначающим выходной день
     * @author Максим Дрючин (tgmaksim)
     * */
    fun drawWeekends() {
        val schedule = schedule ?: return
        for (i in 0..<scheduleLength) {
            if (schedule.getOrNull(i)?.lessons?.isEmpty() == true)
                ui.calendar.getChildAt(i).isHovered = true  // Выходной
        }
    }

    /**
     * День расписания по умолчанию (сегодня или завтра с 15:00)
     * @return день в виде [OffsetDateTime]
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun getDefaultDate(): OffsetDateTime =
        if (Utilities.localDate().hour > 15) {
            Utilities.localDate().plusDays(1)
        } else {
            Utilities.localDate()
        }
}