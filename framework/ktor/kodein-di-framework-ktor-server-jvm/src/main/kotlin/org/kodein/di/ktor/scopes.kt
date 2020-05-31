package org.kodein.di.ktor

import io.ktor.application.*
import io.ktor.sessions.*
import org.kodein.di.*
import org.kodein.di.bindings.*

//region Session scope
/**
 * Interface that will help leverage the use of DI in the Ktor [Sessions] context
 */
interface KodeinDISession {
    fun getSessionId(): Any
}

/**
 * DI scope that will provide singletons according to a specific [KodeinDISession]
 */
object SessionScope : Scope<KodeinDISession> {

    private val mapRegistry = HashMap<Any, ScopeRegistry>()

    /**
     * Reclaim the right [ScopeRegistry] regarding to the given [KodeinDISession]
     * This will help maintaining and retrieving singletons linked with the [KodeinDISession]
     */
    override fun getRegistry(context: KodeinDISession): ScopeRegistry {
        return synchronized(mapRegistry) {
            mapRegistry[context.getSessionId()] ?: run {
                val scopeRegistry = StandardScopeRegistry()
                mapRegistry[context.getSessionId()] = scopeRegistry
                scopeRegistry
            }
        }
    }

    /**
     * Remove amd close the [ScopeRegistry] linked to the [KodeinDISession]
     * The linked singletons won't be retrievable anymore
     *
     * This is usually called when closing / expiring the session
     */
    fun close(session: KodeinDISession) {
        synchronized(mapRegistry) {
            val scopeRegistry = mapRegistry[session.getSessionId()]
            if (scopeRegistry != null) {
                mapRegistry.remove(session.getSessionId())
                scopeRegistry.clear()
            }
        }
    }
}

/**
 * Clear session instance with type [T] and clear the corresponding [ScopeRegistry]
 * @throws IllegalStateException if no session provider registered for type [T]
 */
inline fun <reified T> CurrentSession.clearSessionScope() {
    val session = get<T>()

    if(session != null && session is KodeinDISession){
        SessionScope.close(session)
    }

    this.clear<T>()
}
//endregion
//region Request scope
object CallScope : WeakContextScope<ApplicationCall>()
//endregion