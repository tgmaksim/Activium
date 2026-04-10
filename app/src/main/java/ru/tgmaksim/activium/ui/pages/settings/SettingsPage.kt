package ru.tgmaksim.activium.ui.pages.settings

import android.os.Build
import android.os.Bundle
import android.view.View
import android.graphics.Color
import android.view.ViewGroup
import android.widget.ImageView
import android.view.LayoutInflater
import android.transition.AutoTransition
import android.transition.TransitionManager

import androidx.core.view.children
import androidx.lifecycle.Lifecycle
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.fragment.app.activityViewModels
import androidx.core.widget.addTextChangedListener

import com.google.android.material.slider.RangeSlider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider

import kotlinx.datetime.format
import kotlinx.datetime.TimeZone
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.format.FormatStringsInDatetimeFormats

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.Child
import ru.tgmaksim.activium.api.Review
import ru.tgmaksim.activium.BuildConfig
import ru.tgmaksim.activium.ui.LoginActivity
import ru.tgmaksim.activium.ui.ParentActivity
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.api.ChildrenResult
import ru.tgmaksim.activium.api.MyReviewResult
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.main.MainActivity
import ru.tgmaksim.activium.databinding.ChildItemBinding
import ru.tgmaksim.activium.utilities.NotificationManager
import ru.tgmaksim.activium.databinding.SettingsPageBinding
import ru.tgmaksim.activium.api.StatusEANotificationsResult
import ru.tgmaksim.activium.api.StatusMarksNotificationsResult
import ru.tgmaksim.activium.utilities.datastore.SettingsManager
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager
import ru.tgmaksim.activium.databinding.DialogReviewEditorBinding

/**
 * Fragment-страница с настройками приложения
 * @author Максим Дрючин (tgmaksim)
 * @see MainActivity
 * */
class SettingsPage : Fragment() {
    private lateinit var ui: SettingsPageBinding
    private val settingsViewModel: SettingsViewModel by activityViewModels()

    private var isChildrenExpanded = false
    private var before = 3
    private var after = 3

    companion object {
        private const val REVIEW_TEXT_LIMIT = 512
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = SettingsPageBinding.inflate(inflater, container, false)

        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            initSettingsValues()  // Установка настроек в нужное положение
        }

        showUpdateInfo()  // Показ информации об обновлении приложения, если требуется

        setupSettingsListener()  // Настройка обработчиков настроек
        setupButtonsListener()  // Настройка кнопок после настроек

        setupStatesListener()
    }

    /**
     * Установление положения переключателей настроек, заполнение данных о версии приложения и Android
     * и инициализация формата текста в Label у Range
     * @author Максим Дрючин (tgmaksim)
     * */
    private suspend fun initSettingsValues() {
        val settings = SettingsManager.snapshot()

        // Установка Switch в нужное положение
        ui.settingsTheme.isChecked = settings.darkTheme
        ui.settingsTheme.visibility = View.VISIBLE

        // Установка нужного диапазона
        before = settings.beforeSchedule
        after = settings.afterSchedule
        ui.settingsScheduleRange.values = listOf(-before.toFloat(), after.toFloat())

        // Определение формата
        ui.settingsScheduleRange.setLabelFormatter { value: Float ->
            when (value.toInt()) {
                in -14..-1 -> getString(R.string.schedule_range_label_from, -value.toInt())
                0 -> getString(R.string.schedule_range_label_today)
                1 -> getString(R.string.schedule_range_label_tomorrow)
                else -> getString(R.string.schedule_range_label_to, value.toInt())
            }
        }

        ui.settingsLastMarksPeriod.value = settings.lastMarksPeriod.toFloat()
        ui.settingsLastMarksPeriod.setLabelFormatter { value ->
            resources.getQuantityString(R.plurals.last_marks_label, value.toInt(), value.toInt())
        }

        ui.settingsShowNullSubjectMarks.isChecked = settings.showNullSubjectMarks
        ui.settingsShowNullSubjectMarks.visibility = View.VISIBLE

        ui.version.text = getString(R.string.version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        ui.android.text = getString(R.string.android, Build.VERSION.RELEASE,Build.VERSION.SDK_INT)
    }

    /**
     * Показ блока с обновлением приложения, если требуется
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun showUpdateInfo() {
        val version = MemoryDataManager.versionStatus.value
        if (version != null && version.latestVersionNumber > BuildConfig.VERSION_CODE)
            MemoryDataManager.versionStatus.value?.let {
                ui.updateDescription.text = getString(
                    R.string.update_description,
                    it.latestVersionString, it.latestVersionNumber, it.updateLogs
                )
                ui.updateApplication.visibility = View.VISIBLE
            }
    }

    /**
     * Настройка обработчиков переключателей настроек и слайдера
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun setupSettingsListener() {
        // Смена темы приложения
        ui.settingsTheme.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.updateTheme(isChecked)
            MemoryDataManager.darkTheme.value = isChecked
            (requireActivity() as ParentActivity).setupActivityTheme()
        }

        ui.settingsMarksNotifications.setOnCheckedChangeListener { _, isChecked ->
            marksNotificationsListener(isChecked)
        }

        ui.settingsEANotifications.setOnCheckedChangeListener { _, isChecked ->
            eaNotificationsListener(isChecked)
        }

        // Смена периода загружаемого расписания
        ui.settingsScheduleRange.addOnChangeListener { slider, _, _ ->
            settingsScheduleRangeListener(slider)
        }
        ui.settingsScheduleRange.addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: RangeSlider) {}

            override fun onStopTrackingTouch(slider: RangeSlider) {
                val left = slider.values[0].toInt()
                val right = slider.values[1].toInt()
                settingsViewModel.setRangeSchedule(-left, right)
            }
        })

        ui.settingsLastMarksPeriod.addOnChangeListener { slider, _, _ ->
            settingsLastMarksSliderListener(slider)
        }
        ui.settingsLastMarksPeriod.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}

            override fun onStopTrackingTouch(slider: Slider) {
                settingsViewModel.setLastMarksPeriod(slider.value.toInt())
            }
        })

        ui.settingsShowNullSubjectMarks.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.updateShowNullSubjectMarks(isChecked)
        }
    }

    /**
     * Обработчик переключателя уведомлений о новых оценках
     * @param isChecked Новый статус настройки
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun marksNotificationsListener(isChecked: Boolean) {
        if (!NotificationManager.checkPermission(requireContext())) {
            Utilities.showAlertDialog(
                requireContext(),
                getString(R.string.title_dialog_permission_dnevnik_notifications),
                getString(R.string.message_dialog_permission_dnevnik_notifications),
                getString(R.string.button_dialog_permission_dnevnik_notifications)
            ) { _, _ ->
                NotificationManager.setupPostNotifications(requireActivity())
            }
        }

        settingsViewModel.switchMarksNotifications(isChecked)
    }

    /**
     * Обработчик переключателя уведомлений о внеурочных занятиях
     * @param isChecked Новый статус настройки
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun eaNotificationsListener(isChecked: Boolean) {
        if (!NotificationManager.checkPermission(requireContext())) {
            Utilities.showAlertDialog(
                requireContext(),
                getString(R.string.title_dialog_permission_dnevnik_notifications),
                getString(R.string.message_dialog_permission_dnevnik_notifications),
                getString(R.string.button_dialog_permission_dnevnik_notifications)
            ) { _, _ ->
                NotificationManager.setupPostNotifications(requireActivity())
            }
        }

        settingsViewModel.switchEANotifications(isChecked)
    }

    /**
     * Обработчик слайдера диапазона расписания
     * @param slider Объект, переданный после установления нового положения
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun settingsScheduleRangeListener(slider: RangeSlider) {
        val left = slider.values.first().toInt()
        val right = slider.values.last().toInt()

        if (left in -14..0 && right in 1..21 && right - left <= 31) {
            before = -left
            after = right
        } else {
            slider.values = listOf(-before.toFloat(), after.toFloat())
        }

        ui.settingsScheduleStatus.visibility = if (-left > 7 || right > 14) View.VISIBLE else View.GONE
    }

    /**
     * Обработчик слайдера периода последних оценок
     * @param slider Объект, переданный после установления нового положения
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun settingsLastMarksSliderListener(slider: Slider) {
        val value = slider.value.toInt()

        if (value == 0) {
            slider.value = 1f
        }

        ui.settingsLastMarksStatus.visibility = if (value > 7) View.VISIBLE else View.GONE
    }

    /**
     * Настройка обработчиков кнопок
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun setupButtonsListener() {
        // Нажатие на кнопку ошибки для обновления профилей и статуса настройки учебных уведомлений
        ui.marksNotificationsError.setOnClickListener {
            settingsViewModel.loadMarksNotifications()
        }
        ui.eaNotificationsError.setOnClickListener {
            settingsViewModel.loadEANotifications()
        }
        ui.childrenError.setOnClickListener {
            settingsViewModel.loadChildren()
        }

        // Нажатие на кнопку обновления
        ui.buttonUpdate.setOnClickListener {
            openSite()
        }

        // Нажатие на кнопку раскрытия списка профилей
        ui.childrenHeader.setOnClickListener {
            childrenListener()
        }

        // Нажатие на кнопку обновления отзыва
        ui.buttonRefreshReview.setOnClickListener {
            settingsViewModel.loadReview()
        }
        ui.reviewError.setOnClickListener {
            settingsViewModel.loadReview()
        }

        ui.buttonWriteReview.setOnClickListener {
            openReviewEditor()
        }

        ui.buttonEditReview.setOnClickListener {
            val stateValue = (settingsViewModel.reviewState.value as? LoadState.Success)
            openReviewEditor(stateValue?.data?.review)
        }

        // Нажатие на кнопку удаления отзыва
        ui.buttonDeleteReview.setOnClickListener {
            deleteReviewListener()
        }

        // Нажатие на кнопку открытия сайта
        ui.buttonOpenSite.setOnClickListener {
            openSite()
        }

        // Нажатие на кнопку выхода
        ui.buttonLogout.setOnClickListener {
            logout()
        }

        ui.buttonOpenAllReviews.setOnClickListener {
            openSite("reviews")
        }
    }

    /**
     * Открыть сайт и передать сессию в параметрах
     * @param hash Необязательный параметр, передача хеша страницы
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun openSite(hash: String? = null) {
        val sessionId = MemoryDataManager.sessionId.value
        Utilities.openUrl(
            requireContext(),
            "${BuildConfig.DOMAIN}?sessionId=${sessionId}${if (hash != null) "#$hash" else ""}"
        )
    }

    /**
     * Настройка обработчиков состояний
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun setupStatesListener() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    settingsViewModel.childrenState.collect { state ->
                        if (state is LoadState.Empty) {
                            settingsViewModel.loadChildren()
                        } else {
                            switchChildrenState(state)

                            if (state is LoadState.Success)
                                renderChildren(state.data.children, state.data.activeChildId)
                            else if (state is LoadState.Error) {
                                Utilities.showUiMessage(requireContext(), state.message)
                                settingsViewModel.resetError(SettingsViewModel.StateType.Children)
                                if (state.unauthorized)
                                    logout()
                            }
                        }
                    }
                }
                launch {
                    settingsViewModel.statusMarksNotificationsState.collect { state ->
                        if (state is LoadState.Empty) {
                            settingsViewModel.loadMarksNotifications()
                        } else {
                            if (state is LoadState.Success)
                                renderSwitchMarksNotifications(state.data.status)
                            else if (state is LoadState.Error) {
                                Utilities.showUiMessage(requireContext(), state.message)
                                settingsViewModel.resetError(SettingsViewModel.StateType.StatusMarksNotifications)
                                if (state.unauthorized)
                                    logout()
                            }

                            switchMarksNotificationsState(state)
                        }
                    }
                }
                launch {
                    settingsViewModel.statusEANotificationsState.collect { state ->
                        if (state is LoadState.Empty) {
                            settingsViewModel.loadEANotifications()
                        } else {
                            if (state is LoadState.Success)
                                renderSwitchEANotifications(state.data.status)
                            else if (state is LoadState.Error) {
                                Utilities.showUiMessage(requireContext(), state.message)
                                settingsViewModel.resetError(SettingsViewModel.StateType.StatusEANotifications)
                                if (state.unauthorized)
                                    logout()
                            }

                            switchEANotificationsState(state)
                        }
                    }
                }
                launch {
                    settingsViewModel.reviewState.collect { state ->
                        if (state is LoadState.Empty) {
                            settingsViewModel.loadReview()
                        } else {
                            if (state is LoadState.Success && state.data.review != null)
                                renderReview(state.data.review, state.data.onModeration)
                            else if (state is LoadState.Error) {
                                Utilities.showUiMessage(requireContext(), state.message)
                                settingsViewModel.resetError(SettingsViewModel.StateType.Review)
                                if (state.unauthorized)
                                    logout()
                            }

                            switchReviewState(state)
                        }
                    }
                }
                launch {
                    (requireActivity() as MainActivity).activityViewModel.versionState.collect {
                        showUpdateInfo()
                    }
                }
            }
        }
    }

    /**
     * Обработчик заголовка профилей
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun childrenListener() {
        val state = settingsViewModel.childrenState.value
        if (state !is LoadState.Success) return

        isChildrenExpanded = !isChildrenExpanded

        if (isChildrenExpanded) {
            ui.childrenList.visibility = View.VISIBLE

            TransitionManager.beginDelayedTransition(
                ui.settingsChildren,
                AutoTransition().apply {
                    duration = 600
                }
            )
        } else {
            ui.childrenList.visibility = View.GONE
        }

        val active = state.data.children.find { it.childId == state.data.activeChildId }
        ui.activeChildText.text = if (isChildrenExpanded) getString(R.string.select_profile) else active?.name ?: getString(
            R.string.no_child)

        ui.childrenArrow.animate()
            .rotation(if (isChildrenExpanded) 180f else 0f)
            .setDuration(600)
            .start()
    }

    /**
     * Выход из аккаунта и открытие LoginActivity
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun logout() {
        settingsViewModel.logout()

        LoginActivity.openLoginActivity(requireActivity())
    }

    /**
     * Добавление в раскрывающийся список профилей
     * @param children Список профилей
     * @param activeChildId Идентификатор активного профиля
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun renderChildren(children: List<Child>, activeChildId: Long) {
        val active = children.find { it.childId == activeChildId }

        ui.activeChildText.text = active?.name ?: getString(R.string.no_child)

        ui.childrenList.removeAllViews()

        children.forEach { child ->
            val item = ChildItemBinding.inflate(layoutInflater, ui.childrenList, false)

            item.childName.text = child.name
            item.childActive.visibility = if (child.childId == activeChildId) View.VISIBLE else View.GONE

            item.root.setOnClickListener {
                selectChild(child.childId)
            }

            ui.childrenList.addView(item.root)
        }
    }

    /**
     * Выбор активного профиля
     * @param childId Идентификатор активного профиля
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun selectChild(childId: Long) {
        collapse()

        val state = settingsViewModel.childrenState.value as? LoadState.Success
        val active = state?.data?.children?.find { it.childId == childId }
        ui.activeChildText.text = active?.name ?: getString(
            R.string.no_child)

        settingsViewModel.selectActiveChild(childId)
    }

    /**
     * Сворачивание раскрывающегося списка профилей
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun collapse() {
        isChildrenExpanded = false

        ui.childrenList.visibility = View.GONE

        ui.childrenArrow.animate()
            .rotation(0f)
            .setDuration(600)
            .start()
    }

    /**
     * Установление переключателя настройки уведомлений о новых оценках в нужное положение
     * @param isChecked Новый статус настройки
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun renderSwitchMarksNotifications(isChecked: Boolean) {
        ui.settingsMarksNotifications.setOnCheckedChangeListener(null)

        ui.settingsMarksNotifications.isChecked = isChecked

        ui.settingsMarksNotifications.setOnCheckedChangeListener { _, isChecked ->
            marksNotificationsListener(isChecked)
        }
    }

    /**
     * Установление переключателя настройки уведомлений о внеурочных занятиях в нужное положение
     * @param isChecked Новый статус настройки
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun renderSwitchEANotifications(isChecked: Boolean) {
        ui.settingsEANotifications.setOnCheckedChangeListener(null)

        ui.settingsEANotifications.isChecked = isChecked

        ui.settingsEANotifications.setOnCheckedChangeListener { _, isChecked ->
            eaNotificationsListener(isChecked)
        }
    }

    /**
     * Обработчик кнопки удаления отзыва
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun deleteReviewListener() {
        Utilities.showAlertDialog(
            requireContext(),
            getString(R.string.review),
            getString(R.string.message_dialog_delete_review),
            getString(R.string.button_dialog_delete_review)
        ) { _, _ ->
            settingsViewModel.deleteReview()
        }
    }

    /**
     * Показ данных отзыва
     * @param review Отзыв
     * @param onModeration Статус отзыва на модерации
     * @author Максим Дрючин (tgmaksim)
     * */
    @OptIn(FormatStringsInDatetimeFormats::class)
    private fun renderReview(review: Review, onModeration: Boolean) {
        val editedText = if (review.isUpdated) " (${getString(R.string.review_edit_marker)})" else ""
        val moderationText = if (onModeration) " (${getString(R.string.review_moderation_marker)})" else ""
        val metaText = editedText + moderationText
        val createdAt = review.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
            .format(LocalDateTime.Format { byUnicodePattern("dd.MM.yyyy") })
        ui.reviewMeta.text = getString(R.string.review_meta, review.name, createdAt, metaText)

        updateStars(ui.reviewStars.children.toList().map { it as ImageView }, review.stars)

        ui.reviewText.text = review.text

        ui.reviewLikesIcon.setImageResource(if (review.likes == 0) R.drawable.like_outline else R.drawable.like)
        ui.reviewLikesCounter.text = review.likes.toString()
    }

    private fun updateStars(stars: List<ImageView>, selectedStars: Int) {
        stars.forEachIndexed { index, imageView ->
            val filled = index < selectedStars
            imageView.setImageResource(
                if (filled) R.drawable.star_filled else R.drawable.star_outline
            )
        }
    }

    private fun openReviewEditor(existing: Review? = null) {
        val view = DialogReviewEditorBinding.inflate(layoutInflater, ui.root, false)

        val stars = listOf(view.star1, view.star2, view.star3, view.star4, view.star5)

        var selectedStars = existing?.stars ?: 0

        view.title.text = if (existing == null) getString(R.string.review_editor_new_title) else getString(R.string.review_editor_edit_title)

        view.text.setText(existing?.text.orEmpty())
        view.textCounter.text = getString(R.string.review_text_counter, view.text.text?.length ?: 0, REVIEW_TEXT_LIMIT)

        updateStars(stars, selectedStars)

        stars.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                selectedStars = index + 1
                updateStars(stars, selectedStars)
            }
        }

        view.text.addTextChangedListener {
            view.textCounter.text = getString(R.string.review_text_counter, view.text.text?.length ?: 0, REVIEW_TEXT_LIMIT)
            if ((it?.length ?: 0) > REVIEW_TEXT_LIMIT)
                view.textCounter.setTextColor(Color.RED)
            else if ((it?.length ?: 0) == REVIEW_TEXT_LIMIT)
                view.textCounter.setTextColor(requireContext().getColor(R.color.text_secondary))
        }

        val dialog = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.AppDialogTheme
        ).setView(view.root).create()

        view.buttonSendReview.setOnClickListener {
            val text = view.text.text?.toString()?.trim()?.ifEmpty { null }

            if (selectedStars == 0) {
                Utilities.showAlertDialog(
                    requireContext(),
                    getString(R.string.title_dialog_review_no_stars),
                    getString(R.string.message_dialog_review_no_stars),
                    getString(R.string.button_dialog_review_no_stars)
                )
                return@setOnClickListener
            }

            dialog.dismiss()

            Utilities.showAlertDialog(
                requireContext(),
                getString(R.string.title_dialog_review_sent),
                getString(R.string.message_dialog_review_sent),
                getString(R.string.button_dialog_review_sent)
            )

            settingsViewModel.sendReview(selectedStars, text)
        }

        dialog.show()
    }

    private fun switchChildrenState(state: LoadState<ChildrenResult>) {
        ui.childrenLoading.visibility = if (state is LoadState.Loading) View.VISIBLE else View.GONE
        ui.childrenArrow.visibility = if (state is LoadState.Success) View.VISIBLE else View.GONE
        ui.childrenError.visibility = if (state.isError()) View.VISIBLE else View.GONE

        ui.childrenHeader.isEnabled = state is LoadState.Success
    }

    private fun switchMarksNotificationsState(state: LoadState<StatusMarksNotificationsResult>) {
        ui.marksNotificationsLoading.visibility = if (state is LoadState.Loading) View.VISIBLE else View.GONE
        ui.settingsMarksNotifications.visibility = if (state is LoadState.Success) View.VISIBLE else View.GONE
        ui.marksNotificationsError.visibility = if (state.isError()) View.VISIBLE else View.GONE
    }

    private fun switchEANotificationsState(state: LoadState<StatusEANotificationsResult>) {
        ui.eaNotificationsLoading.visibility = if (state is LoadState.Loading) View.VISIBLE else View.GONE
        ui.settingsEANotifications.visibility = if (state is LoadState.Success) View.VISIBLE else View.GONE
        ui.eaNotificationsError.visibility = if (state.isError()) View.VISIBLE else View.GONE
    }

    private fun switchReviewState(state: LoadState<MyReviewResult>) {
        ui.buttonRefreshReview.visibility = if (state is LoadState.Success) View.VISIBLE else View.GONE
        ui.reviewLoading.visibility = if (state is LoadState.Loading) View.VISIBLE else View.GONE
        ui.reviewError.visibility = if (state.isError()) View.VISIBLE else View.GONE

        if (state is LoadState.Success && state.data.review != null) {
            ui.reviewMeta.visibility = View.VISIBLE
            ui.reviewStars.visibility = View.VISIBLE
            ui.reviewText.visibility = View.VISIBLE
            ui.reviewLikes.visibility = View.VISIBLE

            ui.buttonWriteReview.visibility = View.GONE
            ui.buttonEditReview.visibility = View.VISIBLE
            ui.buttonDeleteReview.visibility = View.VISIBLE
        } else {
            ui.reviewMeta.visibility = View.GONE
            ui.reviewStars.visibility = View.GONE
            ui.reviewText.visibility = View.GONE
            ui.reviewLikes.visibility = View.GONE

            ui.buttonEditReview.visibility = View.GONE
            ui.buttonDeleteReview.visibility = View.GONE
            ui.buttonWriteReview.visibility = if (state is LoadState.Success && state.data.review == null) View.VISIBLE else View.GONE
        }
    }
}