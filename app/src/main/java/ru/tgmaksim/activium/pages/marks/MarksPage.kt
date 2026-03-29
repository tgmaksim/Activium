package ru.tgmaksim.activium.pages.marks

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.fragment.app.Fragment

import ru.tgmaksim.activium.databinding.MarksPageBinding

/**
 * Страница с мероприятиями и другими событиями и объявлениями школы
 * @author Максим Дрючин (tgmaksim)
 * @see ru.tgmaksim.activium.ui.main.MainActivity
 * */
class MarksPage : Fragment() {
    private lateinit var ui: MarksPageBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = MarksPageBinding.inflate(inflater, container, false)

        return ui.root
    }
}