package ru.tgmaksim.activium.pages

import android.annotation.SuppressLint
import android.transition.AutoTransition
import android.transition.TransitionManager

import android.os.Bundle
import android.view.View
import android.content.Intent
import android.os.Build
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.CancellationException

import ru.tgmaksim.activium.BuildConfig
import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.Child
import ru.tgmaksim.activium.api.MyReviewResult
import ru.tgmaksim.activium.api.Request
import ru.tgmaksim.activium.api.Review
import ru.tgmaksim.activium.api.Reviews
import ru.tgmaksim.activium.api.Settings
import ru.tgmaksim.activium.databinding.ChildItemBinding
import ru.tgmaksim.activium.databinding.DialogReviewEditorBinding
import ru.tgmaksim.activium.ui.LoginActivity
import ru.tgmaksim.activium.ui.ParentActivity
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.databinding.SettingsPageBinding
import ru.tgmaksim.activium.utilities.NotificationManager
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

/**
 * Fragment-страница с настройками приложения
 * @author Максим Дрючин (tgmaksim)
 * @see ru.tgmaksim.activium.ui.MainActivity
 * */
class SettingsPage : Fragment() {
    private lateinit var ui: SettingsPageBinding
    private var reload = false
    private var darkTheme = false
    private var isChildrenExpanded = false
    private var before = 3
    private var after = 3

    companion object { // Для новых переменных в setupButtons обнулять их значение
        private var children: List<Child> = emptyList()
        private var activeChildId = -1L
        private var dnevnikNotifications = false
        private var myReview: MyReviewResult? = null

        private const val REVIEW_TEXT_LIMIT = 512
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val theme = runBlocking {
            SettingsManager.getDarkTheme()
        }

        if (::ui.isInitialized && theme == darkTheme && !reload)
            return ui.root

        if (!::ui.isInitialized || theme != darkTheme)
            ui = SettingsPageBinding.inflate(inflater, container, false)
        darkTheme = theme

        lifecycleScope.launch {
            initSettingsValues()  // Установка настроек в нужное положение
        }
        lifecycleScope.launch {
            loadChildren()
        }
        lifecycleScope.launch {
            refreshReview()
        }
        lifecycleScope.launch {
            before = SettingsManager.getBeforeSchedule()
            after = SettingsManager.getAfterSchedule()
        }
        setupSettingsListener()  // Настройка обработчиков
        setupButtons()  // Настройка кнопок после настроек

        // Показ блока с обновлением приложения
        MemoryDataManager.getVersionStatus()?.let {
            ui.updateApplication.visibility = View.VISIBLE
            ui.updateDescription.text = StringBuilder(it.latestVersionString).apply {
                append(' ')
                append("(${it.latestVersionNumber})")
                append('\n')
                append(it.updateLogs)
            }.toString()
        }

        return ui.root
    }

    @SuppressLint("SetTextI18n")
    @OptIn(FormatStringsInDatetimeFormats::class)
    private fun renderReview(myReview: MyReviewResult?) {
        ui.buttonWriteReview.visibility = if (myReview == null) View.GONE else View.VISIBLE

        if (myReview == null || myReview.review == null) {
            ui.reviewMeta.visibility = View.GONE
            ui.reviewStars.visibility = View.GONE
            ui.reviewText.visibility = View.GONE
            ui.reviewLikes.visibility = View.GONE

            ui.buttonEditReview.visibility = View.GONE
            ui.buttonDeleteReview.visibility = View.GONE
        } else {
            val review = myReview.review

            val editedText = if (review.isUpdated) " (${getString(R.string.review_edit_marker)})" else "" +
                    if (myReview.onModeration) " (${getString(R.string.review_moderation_marker)})" else ""
            val createdAt = review.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).format(LocalDateTime.Format { byUnicodePattern("dd.MM.yyyy") })
            ui.reviewMeta.text = "${review.name} • ${createdAt}$editedText"
            ui.reviewMeta.visibility = View.VISIBLE

            updateStars(ui.reviewStars.children.toList().map { it as ImageView }, review.stars)
            ui.reviewStars.visibility = View.VISIBLE

            ui.reviewText.text = review.text
            ui.reviewText.visibility = View.VISIBLE

            ui.reviewLikesIcon.setImageResource(if (review.likes == 0) R.drawable.like_outline else R.drawable.like)
            ui.reviewLikesCounter.text = review.likes.toString()
            ui.reviewLikes.visibility = View.VISIBLE

            ui.buttonWriteReview.visibility = View.GONE
            ui.buttonEditReview.visibility = View.VISIBLE
            ui.buttonDeleteReview.visibility = View.VISIBLE
        }
    }

    private fun refreshReview(forceUpdate: Boolean = false) {
        if (myReview != null && !forceUpdate) {
            ui.buttonRefreshReview.visibility = View.VISIBLE
            renderReview(myReview)
            return
        }

        lifecycleScope.launch {
            ui.buttonRefreshReview.visibility = View.GONE
            ui.reviewLoading.visibility = View.VISIBLE

            try {
                val response = Reviews.getMyReview()

                if (!response.status || response.answer == null) {
                    if (response.error != null)
                        Utilities.log("API error(${response.error.type}) at myReview: ${response.error.errorMessage}")

                    if (response.error?.errorMessage != null)
                        Utilities.showText(requireContext(), response.error.errorMessage)
                    else
                        Utilities.showText(requireContext(), R.string.error_api)
                }

                myReview = response.answer
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                Utilities.log(e)
                if (!Request.checkInternet())
                    Utilities.showText(requireContext(), R.string.error_internet)
                else
                    Utilities.showText(requireContext(), R.string.error_review)
            } finally {
                renderReview(myReview)
                ui.reviewLoading.visibility = View.GONE
                ui.buttonRefreshReview.visibility = View.VISIBLE
            }
        }
    }

    private fun updateStars(stars: List<ImageView>, selectedStars: Int) {
        stars.forEachIndexed { index, imageView ->
            val filled = index < selectedStars
            imageView.setImageResource(
                if (filled) R.drawable.star_filled else R.drawable.star_outline
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun openReviewEditor(existing: Review? = null) {
        val view = DialogReviewEditorBinding.inflate(layoutInflater, ui.root, false)

        val stars = listOf(view.star1, view.star2, view.star3, view.star4, view.star5)

        var selectedStars = existing?.stars ?: 0

        view.title.text = if (existing == null) getString(R.string.review_editor_new_title) else getString(R.string.review_editor_edit_title)

        view.text.setText(existing?.text.orEmpty())
        view.textCounter.text = "${view.text.text?.length ?: 0}/$REVIEW_TEXT_LIMIT"

        updateStars(stars, selectedStars)

        stars.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                selectedStars = index + 1
                updateStars(stars, selectedStars)
            }
        }

        view.text.addTextChangedListener {
            view.textCounter.text = "${it?.length ?: 0}/$REVIEW_TEXT_LIMIT"
            if ((it?.length ?: 0) > REVIEW_TEXT_LIMIT)
                view.textCounter.setTextColor(resources.getColor(android.R.color.holo_red_dark, requireContext().theme))
        }

        val dialog = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.AppDialogTheme
        ).setView(view.root).create()

        view.buttonSendReview.setOnClickListener {
            lifecycleScope.launch {
                val text = view.text.text?.toString().orEmpty().trim()

                if (selectedStars == 0) {
                    Utilities.showAlertDialog(
                        requireContext(),
                        "Оценка",
                        "Поставьте от 1 до 5 звезд",
                        "А, точно!"
                    )
                    return@launch
                }

                dialog.dismiss()

                ui.buttonRefreshReview.visibility = View.GONE
                ui.reviewLoading.visibility = View.VISIBLE

                try {
                    val response = Reviews.createReview(selectedStars, text)

                    if (!response.status || response.answer == null) {
                        if (response.error != null)
                            Utilities.log("API error(${response.error.type}) at createReview: ${response.error.errorMessage}")

                        if (response.error?.errorMessage != null)
                            Utilities.showText(requireContext(), response.error.errorMessage)
                        else
                            Utilities.showText(requireContext(), R.string.error_api)
                    } else {
                        myReview = response.answer
                        Utilities.showAlertDialog(
                            requireContext(),
                            getString(R.string.review_saved_title),
                            getString(R.string.review_send_moderation),
                            "Ок"
                        )
                        refreshReview()
                    }
                } catch (_: CancellationException) {
                } catch (e: Exception) {
                    Utilities.log(e)
                    if (!Request.checkInternet())
                        Utilities.showText(requireContext(), R.string.error_internet)
                    else
                        Utilities.showText(requireContext(), R.string.error_create_review)
                } finally {
                    ui.reviewLoading.visibility = View.GONE
                    ui.buttonRefreshReview.visibility = View.VISIBLE
                }
            }
        }

        dialog.show()
    }

    private suspend fun loadChildren() {
        if (activeChildId != -1L) {
            renderChildren()
            renderSwitchDnevnikNotifications()
            return
        }

        switchChildrenLoading(true)
        ui.dnevnikNotificationsLoading.visibility = View.VISIBLE

        try {
            val response = Settings.getChildren()

            if (!response.status || response.answer == null) {
                if (response.error != null)
                    Utilities.log("API error(${response.error.type}) at children: ${response.error.errorMessage}")

                if (response.error?.errorMessage != null)
                    Utilities.showText(requireContext(), response.error.errorMessage)
                else
                    Utilities.showText(requireContext(), R.string.error_api)
            } else {
                children = response.answer.children
                activeChildId = response.answer.activeChildId

                switchChildrenLoading(false)

                renderChildren()

                val responseDN = Settings.getStatusDnevnikNotifications(activeChildId)

                if (!responseDN.status || responseDN.answer == null) {
                    if (responseDN.error != null)
                        Utilities.log("API error(${responseDN.error.type}) at dnevnikNotifications: ${responseDN.error.errorMessage}")

                    if (responseDN.error?.errorMessage != null)
                        Utilities.showText(requireContext(), responseDN.error.errorMessage)
                    else
                        Utilities.showText(requireContext(), R.string.error_api)
                } else {
                    dnevnikNotifications = responseDN.answer.status

                    ui.dnevnikNotificationsLoading.visibility = View.GONE
                    renderSwitchDnevnikNotifications()
                }
            }
        } catch (_: CancellationException) {
        } catch (e: Exception) {
            Utilities.log(e)
            if (!Request.checkInternet())
                Utilities.showText(requireContext(), R.string.error_internet)
            else
                Utilities.showText(requireContext(), R.string.error_children)
        } finally {
            switchChildrenLoading(false)
            ui.dnevnikNotificationsLoading.visibility = View.GONE
        }
    }

    private fun switchChildrenLoading(isLoading: Boolean) {
        ui.childrenLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        ui.childrenArrow.visibility = if (isLoading) View.GONE else View.VISIBLE

        ui.childrenHeader.isEnabled = !isLoading
    }

    private fun renderSwitchDnevnikNotifications() {
        ui.settingsDnevnikNotifications.setOnCheckedChangeListener(null)

        ui.settingsDnevnikNotifications.visibility = View.VISIBLE
        ui.settingsDnevnikNotifications.isChecked = dnevnikNotifications

        ui.settingsDnevnikNotifications.setOnCheckedChangeListener { _, isChecked ->
            dnevnikNotificationsListener(isChecked)
        }
    }

    private fun renderChildren() {
        val active = children.find { it.childId == activeChildId }

        ui.activeChildText.text = active?.name ?: getString(R.string.no_child)

        ui.childrenList.removeAllViews()

        children.forEach { child ->
            val item = ChildItemBinding.inflate(layoutInflater, ui.childrenList, false)

            item.childName.text = child.name
            item.childActive.visibility = if (child.childId == activeChildId) View.VISIBLE else View.GONE

            item.root.setOnClickListener {
                lifecycleScope.launch {
                    selectChild(child.childId)
                }
            }

            ui.childrenList.addView(item.root)
        }
    }

    private suspend fun selectChild(childId: Long) {
        switchChildrenLoading(true)
        ui.settingsDnevnikNotifications.visibility = View.GONE
        ui.dnevnikNotificationsLoading.visibility = View.VISIBLE

        try {
            val response = Settings.setActiveChild(childId)

            if (!response.status) {
                if (response.error != null)
                    Utilities.log("API error(${response.error.type}) at selectChildren: ${response.error.errorMessage}")

                if (response.error?.errorMessage != null)
                    Utilities.showText(requireContext(), response.error.errorMessage)
                else
                    Utilities.showText(requireContext(), R.string.error_api)
            } else {
                activeChildId = childId
                SettingsManager.setActiveChildId(childId)

                switchChildrenLoading(false)
                renderChildren()
                collapse()

                val responseDN = Settings.getStatusDnevnikNotifications(childId)

                if (!responseDN.status || responseDN.answer == null) {
                    if (responseDN.error != null)
                        Utilities.log("API error(${responseDN.error.type}) at dnevnikNotifications: ${responseDN.error.errorMessage}")

                    if (responseDN.error?.errorMessage != null)
                        Utilities.showText(requireContext(), responseDN.error.errorMessage)
                    else
                        Utilities.showText(requireContext(), R.string.error_api)
                } else {
                    dnevnikNotifications = responseDN.answer.status

                    ui.dnevnikNotificationsLoading.visibility = View.GONE
                    renderSwitchDnevnikNotifications()
                }
            }
        } catch (_: CancellationException) {
        } catch (e: Exception) {
            Utilities.log(e)
            if (!Request.checkInternet())
                Utilities.showText(requireContext(), R.string.error_internet)
            else
                Utilities.showText(requireContext(), R.string.error_children)
        } finally {
            switchChildrenLoading(false)
            ui.dnevnikNotificationsLoading.visibility = View.GONE
        }
    }

    private fun collapse() {
        isChildrenExpanded = false

        ui.childrenList.visibility = View.GONE

        ui.childrenArrow.animate()
            .rotation(0f)
            .setDuration(600)
            .start()
    }

    @SuppressLint("SetTextI18n")
    private suspend fun initSettingsValues() {
        val settings = SettingsManager.snapshot()

        // Установка Switch в нужное положение
        ui.settingsDocumentView.isChecked = settings.openWebView
        ui.settingsEANotifications.isChecked = settings.eaNotifications
        ui.settingsTheme.isChecked = settings.darkTheme

        // Установка нужного диапазона
        ui.settingsScheduleRange.values = listOf(
            -settings.beforeSchedule.toFloat(),
            settings.afterSchedule.toFloat()
        )

        // Определение формата
        ui.settingsScheduleRange.setLabelFormatter { value: Float ->
            when (value.toInt()) {
                in -14..-1 -> "${-value.toInt()} до"
                0 -> "сегодня"
                1 -> "завтра"
                else -> "${value.toInt()} после"
            }
        }

        ui.version.text = "Версия: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        ui.android.text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    private fun setupSettingsListener() {
        // Смена темы приложения
        ui.settingsTheme.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                SettingsManager.setDarkTheme(isChecked)
                (requireActivity() as ParentActivity).setupActivityTheme()
            }
        }

        // Смена настройки для уведомлений с напоминанием о внеурочном занятии
//        ui.settingsEANotifications.setOnCheckedChangeListener { switch, isChecked ->
//            if (!isChecked) {
//                lifecycleScope.launch {
//                    SettingsManager.setEaNotifications(false)
//                }
//                return@setOnCheckedChangeListener
//            }
//
//            val context = requireContext()
//            if (NotificationManager.checkPermission(context) && NotificationManager.canScheduleExactAlarms(context)) {
//                CacheManager.EANotifications = true
//                Utilities.log("EANotifications = true", tag="settings") {
//                    param("name", "ea_notifications")
//                    param("is_checked", "true")
//                }
//                SchedulePage.createRemindEA(context)
//            } else {
//                switch.isChecked = false
//                NotificationManager.setupPostNotifications(requireActivity())
//            }
//        }

        ui.settingsDnevnikNotifications.setOnCheckedChangeListener { _, isChecked ->
            dnevnikNotificationsListener(isChecked)
        }

        // Смена настройки для открытия документов в домашнем задании
        ui.settingsDocumentView.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                SettingsManager.setOpenWebView(isChecked)
            }
        }

        // Смена периода загружаемого расписания
        ui.settingsScheduleRange.addOnChangeListener { slider, _, _ ->
            val left = slider.values.first().toInt()
            val right = slider.values.last().toInt()

            if (left in -14..0 && right in 1..21 && right - left <= 31) {
                before = -left
                after = right
                lifecycleScope.launch {
                    SettingsManager.setBeforeSchedule(-left)
                    SettingsManager.setAfterSchedule(right)
                }
            } else {
                slider.values = listOf(
                    -before.toFloat(),
                    after.toFloat()
                )
            }
        }
    }

    private fun dnevnikNotificationsListener(isChecked: Boolean) {
        lifecycleScope.launch {
            val context = requireContext()
            if (!NotificationManager.checkPermission(context)) {
                Utilities.showAlertDialog(
                    context,
                    "Разрешение на уведомления",
                    "Предоставьте разрешения на уведомления. Если окно не откроется, посетите настройки",
                    "Предоставить"
                ) { _, _ ->
                    NotificationManager.setupPostNotifications(requireActivity())
                }
            }

            ui.settingsDnevnikNotifications.visibility = View.GONE
            ui.dnevnikNotificationsLoading.visibility = View.VISIBLE

            try {
                val response = Settings.switchDnevnikNotifications(activeChildId, isChecked)

                if (!response.status) {
                    if (response.error != null)
                        Utilities.log("API error(${response.error.type}) at switchDnevnikNotifications: ${response.error.errorMessage}")

                    if (response.error?.errorMessage != null)
                        Utilities.showText(requireContext(), response.error.errorMessage)
                    else
                        Utilities.showText(requireContext(), R.string.error_api)
                } else {
                    dnevnikNotifications = isChecked

                    ui.dnevnikNotificationsLoading.visibility = View.GONE
                    renderSwitchDnevnikNotifications()
                }
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                Utilities.log(e)
                if (!Request.checkInternet())
                    Utilities.showText(requireContext(), R.string.error_internet)
                else
                    Utilities.showText(requireContext(), R.string.error_children)
            } finally {
                switchChildrenLoading(false)
                ui.dnevnikNotificationsLoading.visibility = View.GONE
            }
        }
    }

    private fun setupButtons() {
        // Нажатие на кнопку обновления
        ui.buttonUpdate.setOnClickListener {
            val sessionId = MemoryDataManager.sessionId.value
            Utilities.openUrl(requireContext(), "${BuildConfig.DOMAIN}?sessionId=${sessionId}")
        }

        // Нажатие на кнопку открытия сайта
        ui.buttonOpenSite.setOnClickListener {
            val sessionId = MemoryDataManager.sessionId.value
            Utilities.openUrl(requireContext(), "${BuildConfig.DOMAIN}?sessionId=${sessionId}")
        }

        // Нажатие на кнопку выхода
        ui.buttonLogout.setOnClickListener {
            lifecycleScope.launch {
                SettingsManager.setSessionId(null)
                MemoryDataManager.sessionId.value = null

                children = emptyList()
                activeChildId = -1L
                dnevnikNotifications = false
                myReview = null
                reload = true

                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        }

        ui.childrenHeader.setOnClickListener {
            if (children.isEmpty()) return@setOnClickListener

            isChildrenExpanded = !isChildrenExpanded

            if (isChildrenExpanded) {
                ui.childrenList.visibility = View.VISIBLE

                TransitionManager.beginDelayedTransition(
                    ui.settingsChildren,
                    AutoTransition().apply {
                        duration = 600
                    }
                )
            } else
                ui.childrenList.visibility = View.GONE

            val active = children.find { it.childId == activeChildId }
            ui.activeChildText.text = if (isChildrenExpanded) getString(R.string.select_profile) else active?.name ?: getString(R.string.no_child)

            ui.childrenArrow.animate()
                .rotation(if (isChildrenExpanded) 180f else 0f)
                .setDuration(600)
                .start()
        }

        ui.buttonRefreshReview.setOnClickListener {
            refreshReview(forceUpdate = true)
        }

        ui.buttonWriteReview.setOnClickListener {
            openReviewEditor()
        }

        ui.buttonEditReview.setOnClickListener {
            openReviewEditor(myReview?.review)
        }

        ui.buttonDeleteReview.setOnClickListener {
            Utilities.showAlertDialog(
                requireContext(),
                getString(R.string.review),
                "Удалить Ваш отзыв? Это действие нельзя отменить!",
                "Да, удалить"
            ) { _, _ ->
                lifecycleScope.launch {
                    try {
                        val response = Reviews.deleteReview()

                        if (!response.status) {
                            if (response.error != null)
                                Utilities.log("API error(${response.error.type}) at deleteReview: ${response.error.errorMessage}")

                            if (response.error?.errorMessage != null)
                                Utilities.showText(requireContext(), response.error.errorMessage)
                            else
                                Utilities.showText(requireContext(), R.string.error_api)
                        } else {
                            myReview = MyReviewResult(review = null, onModeration = false)
                        }
                    } catch (_: CancellationException) {
                    } catch (e: Exception) {
                        Utilities.log(e)
                        if (!Request.checkInternet())
                            Utilities.showText(requireContext(), R.string.error_internet)
                        else
                            Utilities.showText(requireContext(), R.string.error_delete_review)
                    } finally {
                        refreshReview()
                    }
                }
            }
        }

        ui.buttonOpenAllReviews.setOnClickListener {
            val sessionId = MemoryDataManager.sessionId.value
            Utilities.openUrl(requireContext(), "${BuildConfig.DOMAIN}?sessionId=${sessionId}#reviews")
        }
    }
}