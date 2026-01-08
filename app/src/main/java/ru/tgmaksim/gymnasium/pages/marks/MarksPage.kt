package ru.tgmaksim.gymnasium.pages.marks

import android.os.Bundle
import android.view.View
import android.content.Intent
import android.view.ViewGroup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import java.util.concurrent.CancellationException
import androidx.recyclerview.widget.LinearLayoutManager

import ru.tgmaksim.gymnasium.R
import ru.tgmaksim.gymnasium.api.Dnevnik
import ru.tgmaksim.gymnasium.api.Request
import ru.tgmaksim.gymnasium.api.MarkLast
import ru.tgmaksim.gymnasium.api.MarksOther
import ru.tgmaksim.gymnasium.ui.LoginActivity
import ru.tgmaksim.gymnasium.utilities.Utilities
import ru.tgmaksim.gymnasium.api.MarksSubjectPeriod
import ru.tgmaksim.gymnasium.utilities.CacheManager
import ru.tgmaksim.gymnasium.databinding.MarksPageBinding

/**
 * Страница с оценками пользователя
 * @author Максим Дрючин (tgmaksim)
 * @see ru.tgmaksim.gymnasium.ui.MainActivity
 * */
class MarksPage : Fragment() {
    private lateinit var ui: MarksPageBinding
    private var isDarkTheme: Boolean = false
    companion object {
        private var lastMarks: List<MarkLast> = Dnevnik.getCacheLastMarks()
        private var periodMarks: List<MarksSubjectPeriod> = Dnevnik.getCachePeriodMarks()
        private var classRating: List<MarksOther> = Dnevnik.getCacheClassRating()
        private var needUpdate: Boolean = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (::ui.isInitialized && CacheManager.isDarkTheme == isDarkTheme)
            return ui.root

        ui = MarksPageBinding.inflate(inflater, container, false)
        isDarkTheme = CacheManager.isDarkTheme

        // Установка цвета в соответствии с темой
        ui.swipeRefresh.setColorSchemeResources(R.color.bg_gradient_center)
        ui.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_gradient_start)

        ui.swipeRefresh.setOnChildScrollUpCallback { _, _ -> true }

        ui.buttonRating.setOnClickListener {
            val note = "Рейтинг в классе по общему среднему баллу за текущий период"
            if (classRating.isNotEmpty())
                RatingDialogFragment(classRating, showNumber = true, note = note).show(
                    parentFragmentManager,
                    "rating"
                )
        }

        ui.lastMarks.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        ui.lastMarks.adapter = LastMarksAdapter()

        ui.periodMarks.layoutManager = LinearLayoutManager(requireContext())
        ui.periodMarks.adapter = MarksSubjectPeriodAdapter()

        viewLifecycleOwner.lifecycleScope.launch {
            delay(400)

            (ui.lastMarks.adapter as LastMarksAdapter).submitList(lastMarks)

            if (lastMarks.isEmpty())
                ui.lastMarks.visibility = View.GONE
            else
                ui.lastMarks.visibility = View.VISIBLE

            (ui.periodMarks.adapter as MarksSubjectPeriodAdapter).submitList(periodMarks)
        }

        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (needUpdate) {
            needUpdate = false
            lifecycleScope.launch {
                ui.swipeRefresh.isRefreshing = true
                loadCloudMarks()
                ui.swipeRefresh.isRefreshing = false
            }
        }

        Utilities.log("MarksPage загружена", tag="load") {
            param("place", "MarksPage")
        }
    }

    fun onBackPressed(): Boolean {
        val position = (ui.lastMarks.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (position != 0)
            ui.lastMarks.smoothScrollToPosition(0)

        return position != 0 && lastMarks.isNotEmpty()
    }

    private suspend fun loadCloudMarks() {
        val cacheLastMarks = lastMarks.hashCode()
        val cachePeriodMarks = periodMarks.hashCode()

        try {
            val response = Dnevnik.getMarks()

            // Если сессия не авторизована, то открывается Login
            // Если произошла ошибка, выводится ошибка
            if (!response.status || response.answer == null) {
                response.error?.type?.let { Utilities.log(it) }
                response.error?.errorMessage?.let { Utilities.showText(requireContext(), it) }

                when (response.error?.type) {
                    "UnauthorizedError" -> {
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        startActivity(intent)
                    }
                    in listOf("ValidationError", "ApiMethodNotFoundError") -> {
                        Utilities.showText(requireContext(), R.string.error_incorrect_data)
                    }
                    else -> {
                        if (response.error?.errorMessage == null)
                            Utilities.showText(requireContext(), R.string.error_api)
                    }
                }
            } else {
                lastMarks = response.answer.lastMarks
                periodMarks = response.answer.periodMarks
                classRating = response.answer.classRating
            }
        } catch (_: CancellationException) {
            ui.swipeRefresh.isRefreshing = false
            return
        } catch (e: Exception) {
            Utilities.log(e)
            if (!Request.checkInternet())
                Utilities.showText(requireContext(), R.string.error_internet)
            else
                Utilities.showText(requireContext(), R.string.error_load_marks)
            return
        }

        Utilities.log("Успешная загрузка оценок", tag="load") {
            param("place", "marks")
        }

        // Есть изменения
        if (cacheLastMarks != lastMarks.hashCode())
            (ui.lastMarks.adapter as LastMarksAdapter).submitList(lastMarks)
        if (cachePeriodMarks != periodMarks.hashCode())
            (ui.periodMarks.adapter as MarksSubjectPeriodAdapter).submitList(periodMarks)

        if (lastMarks.isEmpty())
            ui.lastMarks.visibility = View.GONE
        else
            ui.lastMarks.visibility = View.VISIBLE
    }
}