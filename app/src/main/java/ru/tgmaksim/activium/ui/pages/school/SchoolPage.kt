package ru.tgmaksim.activium.ui.pages.school

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater

import ru.tgmaksim.activium.ui.pages.MainFragment
import ru.tgmaksim.activium.databinding.SchoolPageBinding

/**
 * Страница с мероприятиями и другими событиями и объявлениями школы
 * @author Максим Дрючин (tgmaksim)
 * @see ru.tgmaksim.activium.ui.main.MainActivity
 * */
class SchoolPage(param: String? = null) : MainFragment(param) {
    private lateinit var ui: SchoolPageBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = SchoolPageBinding.inflate(inflater, container, false)

        return ui.root
    }
}