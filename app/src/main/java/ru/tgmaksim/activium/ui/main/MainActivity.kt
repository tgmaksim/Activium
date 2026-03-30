package ru.tgmaksim.activium.ui.main

import android.os.Bundle
import android.graphics.Rect
import android.graphics.Color
import android.widget.TextView
import android.view.MotionEvent

import kotlinx.coroutines.launch

import androidx.lifecycle.Lifecycle
import androidx.activity.viewModels
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.BuildConfig
import ru.tgmaksim.activium.ui.ParentActivity
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.api.VersionsResult
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.pages.marks.MarksPage
import ru.tgmaksim.activium.ui.pages.school.SchoolPage
import ru.tgmaksim.activium.utilities.NotificationManager
import ru.tgmaksim.activium.ui.pages.schedule.SchedulePage
import ru.tgmaksim.activium.ui.pages.settings.SettingsPage
import ru.tgmaksim.activium.databinding.ActivityMainBinding

/**
 * Главная Activity приложения
 * @author Максим Дрючин (tgmaksim)
 * */
class MainActivity : ParentActivity() {
    private lateinit var ui: ActivityMainBinding
    val activityViewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Устанавливается сохраненная тема
        setupActivityTheme()
        super.onCreate(savedInstanceState)

        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Настройка системных полей сверху и снизу
        setupSystemBars(ui.rootLayout)

        // После перерисовки текущий fragment сам отрисуется
        if (savedInstanceState == null)
            openPage(R.id.it_schedule)

        setupMenuListener()  // Настройка нажатий на пункты меню
        setupBackListener()  // Настройка нажатий на системную кнопку назад (или жестом)

        setupVersionStateListener()

        if (savedInstanceState == null) {
            NotificationManager.setupPostNotifications(this)

            // Проверка текущей версии приложения
            activityViewModel.checkVersion(BuildConfig.VERSION_CODE)
        }
    }

    /**
     * Настройка нажатий на кнопки меню
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun setupMenuListener() {
        ui.bottomMenu.setOnItemSelectedListener { item ->
            openPage(item.itemId)
            true
        }
    }

    /**
     * Смена страницы
     * @param itemId Идентификатор нужной страницы
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun openPage(itemId: Int) {
        val transaction = supportFragmentManager.beginTransaction()

        val target = getOrCreateFragment(itemId)

        supportFragmentManager.fragments.forEach {
            transaction.hide(it)
        }

        if (target.isAdded) {
            transaction.show(target)
        } else {
            transaction.add(R.id.contentContainer, target, itemId.toString())
        }

        transaction.commit()
    }

    /**
     * Открытие страницы или создание нового экземпляра
     * @param itemId Идентификатор страницы
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun getOrCreateFragment(itemId: Int): Fragment {
        return supportFragmentManager.findFragmentByTag(itemId.toString()) ?: when (itemId) {
            R.id.it_schedule -> SchedulePage()
            R.id.it_marks -> MarksPage()
            R.id.it_school -> SchoolPage()
            R.id.it_settings -> SettingsPage()
            else -> throw IllegalArgumentException()
        }
    }

    /**
     * Настройка нажатий на системную кнопку назад (или жестом)
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun setupBackListener() {
        onBackPressedDispatcher.addCallback(this) {
            moveTaskToBack(true)
//            when (ui.bottomMenu.selectedItemId) {
//                R.id.it_schedule ->
//                    if ((pages[R.id.it_schedule] as? SchedulePage)?.onBackPressed() != true)
//                        moveTaskToBack(true)
//                R.id.it_marks ->
//                    if ((pages[R.id.it_marks] as? MarksPage)?.onBackPressed() != true)
//                        ui.bottomMenu.selectedItemId = R.id.it_schedule

//                else -> ui.bottomMenu.selectedItemId = R.id.it_schedule
//            }
        }
    }

    private fun setupVersionStateListener() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                activityViewModel.versionState.collect { state ->
                    if (state is LoadState.Success) {
                        if (state.data.latestVersionNumber > BuildConfig.VERSION_CODE)
                            showNewVersionInfo(state)
                    } else if (state is LoadState.Error) {
                        Utilities.showUiMessage(this@MainActivity, state.message)
                        activityViewModel.reset(MainActivityViewModel.StateType.Version)
                    }
                }
            }
        }
    }

    private fun showNewVersionInfo(state: LoadState.Success<VersionsResult>) {
        // Показ красной точки возле иконки настроек
        ui.bottomMenu.getOrCreateBadge(R.id.it_settings).apply {
            isVisible = true
            backgroundColor = Color.RED
            clearNumber()
        }

        if (state.data.versionStatus == "Требуется обновить") {
            Utilities.showAlertDialog(
                this,
                state.data.versionStatus,
                getString(R.string.message_dialog_important_version),
                getString(R.string.button_dialog_new_version)
            ) { _, _ ->
                ui.bottomMenu.selectedItemId = R.id.it_settings
            }
        } else if (state.data.versionStatus == "Новая функция") {
            Utilities.showAlertDialog(
                this,
                state.data.versionStatus,
                getString(R.string.message_dialog_version_with_new_function),
                getString(R.string.button_dialog_new_version)
            ) { _, _ ->
                ui.bottomMenu.selectedItemId = R.id.it_settings
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN)
            return super.dispatchTouchEvent(event)

        // При нажатии на любую область вне TextView с возможным выделением текста,
        // фокус сбрасывается
        val el = currentFocus
        if (el is TextView) {
            val outRect = Rect()
            el.getGlobalVisibleRect(outRect)

            if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt()))
                el.clearFocus()
        }

        return super.dispatchTouchEvent(event)
    }
}