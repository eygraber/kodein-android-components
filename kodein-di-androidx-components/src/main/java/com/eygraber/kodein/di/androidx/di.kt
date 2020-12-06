package com.eygraber.kodein.di.androidx

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import org.kodein.di.DI
import org.kodein.di.bindings.BindingDI
import org.kodein.di.bindings.NoArgBindingDI
import org.kodein.di.bindings.Scope
import org.kodein.di.bindings.ScopeRegistry
import org.kodein.di.bindings.StandardScopeRegistry
import org.kodein.di.internal.synchronizedIfNull
import org.kodein.di.multiton
import org.kodein.di.scoped
import org.kodein.di.singleton

internal val ALLOWED_BUT_NOT_REQUIRED = null

@PublishedApi
internal object AndroidScope : Scope<Any> {
    private val newRegistry = ::StandardScopeRegistry
    private val map = HashMap<LifecycleOwner, ScopeRegistry>()

    override fun getRegistry(context: Any): ScopeRegistry {
        (context as? LifecycleOwner)
            ?: throw IllegalStateException("The context of an AndroidScope must be a LifecycleOwner")

        return synchronizedIfNull(
            lock = map,
            predicate = { map[context] },
            ifNotNull = { it },
            ifNull = {
                val registry =
                    newRegistry()
                map[context] = registry
                context.lifecycle.addObserver(
                    object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            context.lifecycle.removeObserver(this)
                            registry.clear()
                            map.remove(context)
                        }
                    }
                )
                registry
            }
        )
    }
}

@PublishedApi
internal object FragmentViewScope : Scope<Fragment> {
    private val newRegistry = ::StandardScopeRegistry
    private val map = HashMap<LifecycleOwner, ScopeRegistry>()

    override fun getRegistry(context: Fragment): ScopeRegistry {
        return synchronizedIfNull(
            lock = map,
            predicate = { map[context] },
            ifNotNull = { it },
            ifNull = {
                val registry =
                    newRegistry()
                map[context] = registry
                context.viewLifecycleOwner.lifecycle.addObserver(
                    object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            context.lifecycle.removeObserver(this)
                            registry.clear()
                            map.remove(context)
                        }
                    }
                )
                registry
            }
        )
    }
}

inline fun <reified T : Any> DI.Builder.activitySingleton(
    noinline creator: NoArgBindingDI<Activity>.() -> T
) = scoped<Activity>(AndroidScope).singleton(creator = creator)

inline fun <reified T : Any> DI.Builder.fragmentSingleton(
    noinline creator: NoArgBindingDI<Fragment>.() -> T
) = scoped<Fragment>(AndroidScope).singleton(creator = creator)

inline fun <reified A : Any, reified T : Any> DI.Builder.activityMultiton(
    noinline creator: BindingDI<Activity>.(A) -> T
) = scoped<Activity>(AndroidScope).multiton(creator = creator)

inline fun <reified A : Any, reified T : Any> DI.Builder.fragmentMultiton(
    noinline creator: BindingDI<Fragment>.(A) -> T
) = scoped<Fragment>(AndroidScope).multiton(creator = creator)

inline fun <reified T : Any> DI.Builder.fragmentViewSingleton(
    noinline creator: NoArgBindingDI<Fragment>.() -> T
) = scoped(FragmentViewScope).singleton(creator = creator)

inline fun <reified T : Any> DI.Builder.lifecycleServiceSingleton(
    noinline creator: NoArgBindingDI<LifecycleService>.() -> T
) = scoped<LifecycleService>(AndroidScope).singleton(creator = creator)
