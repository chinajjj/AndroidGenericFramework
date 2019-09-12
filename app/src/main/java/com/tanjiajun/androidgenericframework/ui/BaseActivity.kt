package com.tanjiajun.androidgenericframework.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

/**
 * Created by TanJiaJun on 2019-07-28.
 */
abstract class BaseActivity : AppCompatActivity() {

    private lateinit var manager: FragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = supportFragmentManager
    }

    open fun getContainId(): Int = -1

    fun getCurrentFragment(): BaseFragment =
            manager.findFragmentById(getContainId()) as BaseFragment

    fun getFragmentList(): List<Fragment?> =
            mutableListOf<Fragment?>().apply {
                val count = manager.backStackEntryCount

                for (i in 0 until count) {
                    add(manager.findFragmentByTag(manager.getBackStackEntryAt(i).name))
                }
            }

    fun getPreviousFragment(): BaseFragment? =
            manager.backStackEntryCount
                    .takeIf { it >= 2 }
                    ?.let {
                        manager.findFragmentByTag(manager.getBackStackEntryAt(it.minus(2)).name) as BaseFragment
                    }

    fun popBackStackImmediate(): Boolean =
            manager.run {
                if (backStackEntryCount < 1) {
                    return@run false
                }

                try {
                    manager.popBackStackImmediate()
                } catch (exception: IllegalStateException) {
                    return@run false
                }
            }

    fun popBackStackImmediate(name: String, flags: Int): Boolean =
            manager.run {
                if (backStackEntryCount < 1) {
                    return@run false
                }

                try {
                    manager.popBackStackImmediate(name, flags)
                } catch (exception: IllegalStateException) {
                    return@run false
                }
            }

    fun addFragment(fragment: BaseFragment) =
            operateFragmentTransaction(fragment, ACTION_ADD, true, true)

    fun addFragmentAndHideOthers(fragment: BaseFragment) =
            operateFragmentTransaction(fragment, ACTION_ADD_AND_HIDE_OTHERS, true, true)

    fun addFragmentAndHideOthersNotExecutePending(fragment: BaseFragment) =
            operateFragmentTransaction(fragment, ACTION_ADD_AND_HIDE_OTHERS, true, false)

    fun replaceFragment(fragment: BaseFragment) =
            replaceFragment(fragment, true)

    fun replaceFragment(fragment: BaseFragment, addToBackStack: Boolean) =
            operateFragmentTransaction(fragment, ACTION_REPLACE, addToBackStack, true)

    protected fun addFragmentNotAddToBackStack(fragment: BaseFragment) =
            operateFragmentTransaction(fragment, ACTION_ADD, false, false)

    fun addFragmentNotExecutePending(fragment: BaseFragment) =
            operateFragmentTransaction(fragment, ACTION_ADD, true, false)

    private fun operateFragmentTransaction(
            fragment: BaseFragment,
            action: Int,
            isAddToBackStack: Boolean,
            isExecutePending: Boolean
    ) =
            with(manager) {
                val containId = getContainId()

                if (containId > 0) {
                    beginTransaction().let {
                        if (fragment.enableAnimation()) {
                            it.setCustomAnimations(
                                    fragment.getEnterAnimation(),
                                    fragment.getExitAnimation(),
                                    fragment.getPopEnterAnimation(),
                                    fragment.getPopExitAnimation()
                            )
                        }

                        operateFragment(action, containId, fragment, manager, it, isAddToBackStack)
                        it.commitAllowingStateLoss()

                        if (isExecutePending) {
                            try {
                                executePendingTransactions()
                            } catch (exception: NullPointerException) {
                                // ignore exception
                            }
                        }
                    }
                }
            }

    private fun operateFragment(action: Int,
                                containId: Int,
                                fragment: BaseFragment,
                                manager: FragmentManager,
                                transaction: FragmentTransaction,
                                isAddToBackStack: Boolean) =
            with(manager) {
                when (action) {
                    ACTION_ADD -> transaction.add(containId, fragment, fragment.getTransactionTag())
                    ACTION_REPLACE -> transaction.replace(containId, fragment, fragment.getTransactionTag())
                    ACTION_ADD_AND_HIDE_OTHERS -> {
                        fragments
                                .filter { it.isVisible }
                                .forEach { transaction.hide(it) }

                        transaction.add(containId, fragment, fragment.getTransactionTag())
                    }
                    else -> Unit
                }

                if (isAddToBackStack) {
                    transaction.addToBackStack(fragment.getTransactionTag())
                }
            }

    override fun onBackPressed() =
            with(manager) {
                if (backStackEntryCount > getFragmentCountToFinish()) {
                    (findFragmentById(getContainId()) as BaseFragment).let {
                        popBackStackImmediate()
                        it.onHandleGoBack()
                    }
                } else {
                    onFinishActivity()
                }
            }

    protected fun getFragmentCountToFinish() = 1

    protected fun onFinishActivity() {
        finish()
        overridePendingTransition(0, 0)
    }

}

const val ACTION_ADD = 0
const val ACTION_REPLACE = 1
const val ACTION_ADD_AND_HIDE_OTHERS = 2