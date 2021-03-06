package com.github.terrakok.modo

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE

interface StackAction
data class Pop(val count: Int) : StackAction
data class Push(val screens: List<Screen>) : StackAction

/**
 * Navigation state render based on FragmentManager
 */
open class ModoRender(
    protected val fragmentManager: FragmentManager,
    protected val containerId: Int,
    protected val exitAction: () -> Unit
) : NavigationRender {
    protected var currentState = run {
        val names = mutableListOf<String>()
        for (i in 0 until fragmentManager.backStackEntryCount) {
            fragmentManager.getBackStackEntryAt(i).name?.let { names.add(it) }
        }
        restoreStateFromScreenStrings(names)
    }

    constructor(
        activity: FragmentActivity,
        containerId: Int
    ) : this(activity.supportFragmentManager, containerId, { activity.finish() })

    override fun invoke(state: NavigationState) {
        val diff = diff(currentState, state)
        if (diff.isNotEmpty()) {
            currentState = state
            if (currentState.chain.isEmpty()) {
                exitAction()
            } else {
                diff.forEach { action ->
                    when (action) {
                        is Pop -> pop(action.count)
                        is Push -> push(action.screens)
                    }
                }
            }
        }
    }

    protected fun pop(count: Int) {
        val entryIndex = fragmentManager.backStackEntryCount - count
        val entryName =
            if (entryIndex in 0 until fragmentManager.backStackEntryCount) {
                fragmentManager.getBackStackEntryAt(entryIndex).name
            } else {
                null
            }
        fragmentManager.popBackStack(entryName, POP_BACK_STACK_INCLUSIVE)
    }

    protected fun push(screens: List<Screen>) {
        screens.map { screen ->
            screen as? AppScreen ?: error("ModoRender works with AppScreens only!")
            fragmentManager.beginTransaction().apply {
                setReorderingAllowed(true)
                if (screen.replacePreviousScreen) {
                    replace(containerId, screen.create(), screen.id)
                } else {
                    add(containerId, screen.create(), screen.id)
                }
                addToBackStack(screen.stringify())
            }.commit()
        }
    }

    internal companion object {

        fun diff(prev: NavigationState, next: NavigationState): List<StackAction> = when {
            prev.chain.isEmpty() && next.chain.isEmpty() -> emptyList()
            prev.chain.isEmpty() -> listOf(Push(next.chain))
            next.chain.isEmpty() -> listOf(Pop(prev.chain.size))
            else -> {
                var result: List<StackAction>? = null
                for (i in 0 until maxOf(prev.chain.size, next.chain.size)) {
                    val p = prev.chain.getOrNull(i)?.id
                    val n = next.chain.getOrNull(i)?.id
                    if (p == n) continue
                    result = when {
                        p == null -> listOf(
                            Push(next.chain.subList(i, next.chain.size))
                        )
                        n == null -> listOf(
                            Pop(prev.chain.size - i)
                        )
                        else -> listOf(
                            Pop(prev.chain.size - i),
                            Push(next.chain.subList(i, next.chain.size))
                        )
                    }
                    break
                }
                result ?: emptyList()
            }
        }
    }
}