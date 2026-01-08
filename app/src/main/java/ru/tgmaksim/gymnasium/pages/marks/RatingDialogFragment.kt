package ru.tgmaksim.gymnasium.pages.marks

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import ru.tgmaksim.gymnasium.R
import ru.tgmaksim.gymnasium.api.MarksOther
import ru.tgmaksim.gymnasium.databinding.MarksRatingBinding

class RatingDialogFragment(
    private val rating: List<MarksOther>,
    private val showNumber: Boolean = false,
    private val note: String? = null
) : BottomSheetDialogFragment() {
    private lateinit var ui: MarksRatingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = MarksRatingBinding.inflate(inflater, container, false)

        if (note.isNullOrEmpty())
            ui.note.visibility = View.GONE
        ui.note.text = note ?: ""

        ui.ratingList.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        ui.ratingList.adapter = MarksRatingAdapter(showNumber).apply {
            submitList(rating.sortedByDescending { person ->
                val sum = person.marks.sumOf {
                    // Учет плюса и минуса в оценках как 0.25 балла для учета при сортировке
                    it.value
                        .replace(Regex("[+-]"), "")
                        .replace(",", ".")  // Средний балл
                        .toDouble() +
                            if (it.value.last() in listOf('+', '-'))
                                "${it.value.last()}0.25".toDouble()
                            else 0.0
                }

                // Сортировка по среднему баллу оценок
                return@sortedByDescending sum / person.marks.size
            })
        }

        return ui.root
    }
}