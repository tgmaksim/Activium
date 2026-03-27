package ru.tgmaksim.activium.pages.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.runBlocking
import ru.tgmaksim.activium.databinding.SchedulePageBinding
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

/**
 * Страница с мероприятиями и другими событиями и объявлениями школы
 * @author Максим Дрючин (tgmaksim)
 * @see ru.tgmaksim.activium.ui.MainActivity
 * */
class SchedulePage : Fragment() {
    private lateinit var ui: SchedulePageBinding
    private var darkTheme = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val theme = runBlocking {
            SettingsManager.getDarkTheme()
        }

        if (::ui.isInitialized && theme == darkTheme)
            return ui.root

        ui = SchedulePageBinding.inflate(inflater, container, false)
        darkTheme = theme

        return ui.root
    }
}