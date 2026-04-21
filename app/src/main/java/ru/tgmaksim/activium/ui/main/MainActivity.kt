package ru.tgmaksim.activium.ui.main

import android.os.Bundle
import android.graphics.Rect
import android.content.Intent
import android.graphics.Color
import android.widget.TextView
import android.view.MotionEvent
import java.util.concurrent.TimeUnit
import androidx.core.content.ContextCompat

import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.activity.viewModels
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Rotation
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import nl.dionsegijn.konfetti.xml.image.DrawableImage

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.BuildConfig
import ru.tgmaksim.activium.ui.ParentActivity
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.api.VersionsResult
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.pages.MainFragment
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
        setupSystemBars(ui.root)
        setupPaddingBottomMenu(ui.bottomMenu)

        // После перерисовки текущий fragment сам отрисуется
        if (savedInstanceState == null) {
            if (!handleIntent(intent))
                openPage(R.id.it_schedule)
        } else {
            waitFragments { handleIntent(intent) }
        }

        setupMenuListener()  // Настройка нажатий на пункты меню
        setupBackListener()  // Настройка нажатий на системную кнопку назад (или жестом)

        setupCollectors()

        if (savedInstanceState == null) {
            NotificationManager.setupPostNotifications(this)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?): Boolean {
        if (intent == null) return false

        val fromNotification = intent.getStringExtra("from_notification")
        fromNotification?.let {
            var processing = false
            when (it) {
                "new_mark" -> {
                    processing = true
                    ui.bottomMenu.setOnItemSelectedListener {
                        openPage(R.id.it_marks, "update")
                        true
                    }
                    ui.bottomMenu.selectedItemId = R.id.it_marks
                    setupMenuListener()

                    val goodMark = intent.getStringExtra("good_mark") == "true"
                    if (goodMark)
                        startKonfettiAnimation(ui.konfettiView)
                }
                "ea", "remind_note" -> {
                    processing = true
                    ui.bottomMenu.setOnItemSelectedListener {
                        openPage(R.id.it_schedule, "today")
                        true
                    }
                    ui.bottomMenu.selectedItemId = R.id.it_schedule
                    setupMenuListener()
                }
                "praise" -> {
                    processing = true
                    startKonfettiAnimation(ui.konfettiView)
                }
                "publish_review" -> {
                    processing = true
                    ui.bottomMenu.setOnItemSelectedListener {
                        openPage(R.id.it_settings, "update_review")
                        true
                    }
                    ui.bottomMenu.selectedItemId = R.id.it_settings
                    setupMenuListener()
                }
            }
            intent.removeExtra("from_notification")

            val buttonId = intent.getIntExtra("notificationId", -1)
            if (buttonId != -1) {
                val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.cancel(buttonId)
            }
            intent.removeExtra("notificationId")

            return processing
        }

        return false
    }

    private fun waitFragments(callback: (Fragment) -> Unit) {
        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                super.onFragmentResumed(fm, f)
                callback(f)
            }
        }, false)
    }

    fun startKonfettiAnimation(view: KonfettiView? = null, location: FloatArray? = null) {
        val x = location?.get(0)
        val y = location?.get(1)

        val shapes = listOf(
            R.drawable.ic_praise_thumb_up,
            R.drawable.ic_praise_heart,
            R.drawable.ic_praise_spark
        ).mapNotNull { resId ->
            ContextCompat.getDrawable(this, resId)?.let { drawable ->
                Shape.DrawableShape(DrawableImage(
                    drawable = drawable,
                    width = drawable.intrinsicWidth,
                    height = drawable.intrinsicHeight
                ))
            }
        }

        val party = Party(
            speed = 15f,
            maxSpeed = 25f,
            rotation = Rotation(enabled = false),
            damping = 0.92f,
            spread = 360,
            timeToLive = 3000L,
            fadeOutEnabled = true,
            colors = listOf(
                ContextCompat.getColor(this, R.color.praise_particle_primary),
                ContextCompat.getColor(this, R.color.praise_particle_secondary),
                ContextCompat.getColor(this, R.color.praise_particle_accent)
            ),
            shapes = shapes,
            size = listOf(Size(20, 8f), Size(30, 10f), Size(40, 12f)),
            position = if (x != null && y != null) Position.Absolute(x, y) else Position.Relative(0.5, 0.5),
            emitter = Emitter(120, TimeUnit.MILLISECONDS).max(50)
        )

        val konfetti = view ?: ui.konfettiView
        konfetti.post {
            konfetti.start(party)
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
    private fun openPage(itemId: Int, param: String? = null) {
        val target = getOrCreateFragment(itemId, param)
        if (target.isVisible) {
            param?.let { target.newIntent(it) }
            return
        }

        val transaction = supportFragmentManager.beginTransaction()

        supportFragmentManager.fragments.forEach {
            transaction.hide(it)
        }

        if (target.isAdded) {
            transaction.show(target)
            param?.let { target.newIntent(it) }
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
    private fun getOrCreateFragment(itemId: Int, param: String? = null): MainFragment {
        return supportFragmentManager.findFragmentByTag(itemId.toString()) as? MainFragment ?: when (itemId) {
            R.id.it_schedule -> SchedulePage(param)
            R.id.it_marks -> MarksPage(param)
            R.id.it_school -> SchoolPage(param)
            R.id.it_settings -> SettingsPage(param)
            else -> throw IllegalArgumentException()
        }
    }

    /**
     * Настройка нажатий на системную кнопку назад (или жестом)
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun setupBackListener() {
        onBackPressedDispatcher.addCallback(this) {
            when (ui.bottomMenu.selectedItemId) {
                R.id.it_schedule ->
                    if (!(getOrCreateFragment(R.id.it_schedule) as SchedulePage).onBackPressed())
                        moveTaskToBack(true)
                R.id.it_marks ->
                    if (!(getOrCreateFragment(R.id.it_marks) as MarksPage).onBackPressed())
                        ui.bottomMenu.selectedItemId = R.id.it_schedule
                R.id.it_settings ->
                    if (!(getOrCreateFragment(R.id.it_settings) as SettingsPage).onBackPressed())
                        ui.bottomMenu.selectedItemId = R.id.it_schedule

                else -> ui.bottomMenu.selectedItemId = R.id.it_schedule
            }
        }
    }

    private fun setupCollectors() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    activityViewModel.versionState.collect { state ->
                        when (state) {
                            LoadState.Empty -> {
                                activityViewModel.checkVersion(BuildConfig.VERSION_CODE)
                            }
                            is LoadState.Success -> {
                                if (state.data.latestVersionNumber > BuildConfig.VERSION_CODE)
                                    showNewVersionInfo(state)
                            }
                            else -> {}
                        }
                    }
                }
                launch {
                    activityViewModel.infoStatus.collect { state ->
                        when (state) {
                            LoadState.Empty -> {
                                activityViewModel.checkInfoNotifications()
                            }
                            is LoadState.Success -> {
                                for (message in state.data.messages) {
                                    Utilities.showAlertDialog(
                                        this@MainActivity,
                                        message.title,
                                        message.text,
                                        getString(R.string.ok)
                                    )
                                }
                                activityViewModel.reset(MainActivityViewModel.StateType.Info)
                            }
                            else -> {}
                        }
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

        state.data.info?.let { info ->
            Utilities.showAlertDialog(
                this,
                state.data.versionStatus,
                info,
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