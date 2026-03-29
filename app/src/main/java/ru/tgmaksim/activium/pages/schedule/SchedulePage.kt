package ru.tgmaksim.activium.pages.schedule

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.fragment.app.Fragment

import ru.tgmaksim.activium.databinding.SchedulePageBinding

/**
 * Страница с мероприятиями и другими событиями и объявлениями школы
 * @author Максим Дрючин (tgmaksim)
 * @see ru.tgmaksim.activium.ui.main.MainActivity
 * */
class SchedulePage : Fragment() {
    private lateinit var ui: SchedulePageBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = SchedulePageBinding.inflate(inflater, container, false)

        return ui.root
    }
}