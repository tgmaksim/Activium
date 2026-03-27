package ru.tgmaksim.activium.pages

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import kotlinx.coroutines.runBlocking

import ru.tgmaksim.activium.databinding.SchoolPageBinding
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

/**
 * Страница с мероприятиями и другими событиями и объявлениями школы
 * @author Максим Дрючин (tgmaksim)
 * @see ru.tgmaksim.activium.ui.MainActivity
 * */
class SchoolPage : Fragment() {
    private lateinit var ui: SchoolPageBinding
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

        ui = SchoolPageBinding.inflate(inflater, container, false)
        darkTheme = theme

        return ui.root
    }
}