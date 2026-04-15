package ru.tgmaksim.activium.ui.pages

import androidx.fragment.app.Fragment

open class MainFragment(var param: String? = null) : Fragment() {
    open fun newIntent(param: String) {
        this.param = param
    }
}