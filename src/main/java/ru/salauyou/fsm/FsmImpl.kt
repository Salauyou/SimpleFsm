package ru.salauyou.fsm

import java.util.concurrent.atomic.AtomicReference

/**
 * <p>Created on 2021-07-22
 * @author Aliaksandr Salauyou
 */
class FsmBuilderImpl<C:Any, E:Any, S:Any> : FsmBuilder<C, E, S> {

    private val transitions = mutableMapOf<Class<out E>, AtomicReference<TransitionImpl<C, E, S>>>()
    private lateinit var currentBuild: Pair<S, Class<out E>>

    override fun <T:E> transition(
            sourceState: S,
            event: Class<T>,
            targetState: S?,
            transition: Execution<C, T, S>.() -> Unit)
        : Transition<C, T, S> {

        currentBuild = sourceState to event
        val ref = transitions.computeIfAbsent(currentBuild.second) {
            AtomicReference<TransitionImpl<C, E, S>>()
        } as AtomicReference<TransitionImpl<C, T, S>>
        TransitionImpl(ref, sourceState, targetState, transition).let {
            ref.set(it)
            return it
        }
    }

    override fun build(): FsmInstance<C, E, S> = transitions
        .map { it.key to it.value.get() }
        .toMap().let {
            FsmInstanceImpl(it)
        }
}


private class ExecutionImpl<C:Any, E:Any, S:Any>(
        private val context: C,
        internal var currentState: S,
        private var event: E)
    : Execution<C, E, S> {

    var result = TransitionResult.SUCCESS
    var manualState: S? = null

    override fun filter(filter: Filter<in C, in E>) {
        filter.filter(context, event).takeIf { !it.isSuccess() }?.let {
            throw FailedTransitionException(it)
        }
    }

    override fun consumer(consumer: Consumer<in C, in E>) {
        consumer.consume(context, event, result)
    }

    override fun <R:E> transform(transformer: Transformer<in C, in E, out R>) {
        event = transformer.transform(context, event)
    }

    override fun setState(state: S) {
        manualState = state
    }

    override fun <V> router(router: (E) -> V, executions: RouteExecution<C, E, S, V>.() -> Unit) {
        RouteExecutionImpl(this, router.invoke(event)).let {
            executions.invoke(it)
        }
    }
}

private class RouteExecutionImpl<C:Any, E:Any, S:Any, V>(
        private val ctx: ExecutionImpl<C, E, S>,
        private val evaluated: V)
    : RouteExecution<C, E, S, V> {

    private var valueMet = false

    override fun whenever(vararg values: V, execution: Execution<C, E, S>.() -> Unit) {
        if (evaluated in values) {
            execution.invoke(ctx)
            valueMet = true
        }
    }

    override fun otherwise(execution: Execution<C, E, S>.() -> Unit) {
        if (!valueMet) {
            execution.invoke(ctx)
            valueMet = true
        }
    }
}


private open class TransitionImpl<C:Any, E:Any, S:Any>(
        private val ref: AtomicReference<TransitionImpl<C, E, S>>,
        val sourceState: S,
        val targetState: S? = null,
        val execution: ExecutionImpl<C, E, S>.() -> Unit = {})
    : Transition<C, E, S> {

    open fun execute(exec: ExecutionImpl<C, E, S>): TransitionResult {
        return execution.run {
            try {
                invoke(exec)
                targetState?.let {
                    exec.currentState = it
                }
            } catch (e: FailedTransitionException) {
                exec.result = e.result
            } catch (e: Exception) {
                exec.result = TransitionResult.error("$e")
            }
            exec.result
        }
    }

    override fun ifFailed(execution: Execution<C, E, S>.() -> Unit): Transition<C, E, S> {
        val parent = ref.get()
        object: TransitionImpl<C, E, S>(ref, parent.sourceState) {
            override fun execute(exec: ExecutionImpl<C, E, S>): TransitionResult {
                return parent.execute(exec).takeIf { !it.isSuccess() }?.run {
                    try {
                        execution.invoke(exec)
                        TransitionResult.SUCCESS
                    } catch (e: Exception) {
                        TransitionResult.failed()
                    }
                } ?: exec.result
            }
        }.let {
            ref.set(it)  // replace in builder
            return it
        }
    }
}

private class FsmInstanceImpl<C:Any, E:Any, S:Any>(
        private val transitions: Map<Class<out E>, TransitionImpl<C, E, S>>)
    : FsmInstance<C, E, S> {

    private lateinit var context: C
    private lateinit var state: S

    override fun init(context: C, state: S) {
        this.state = state
        this.context = context
    }

    override fun invoke(event: E): TransitionResult {
        var transition: TransitionImpl<C, E, S>?
        var eventClass: Class<*>? = event.javaClass
        do {
            transition = transitions[eventClass]
            eventClass = eventClass?.run { superclass } ?: break
        } while (transition == null)
        transition?.run {
            try {
                ExecutionImpl(context, state, event).let {
                    execute(it)
                    state = it.manualState ?: it.currentState
                    return it.result
                }
            } catch (e: UninitializedPropertyAccessException) {
                return TransitionResult.error("Context not initialized")
            }
        } ?: return TransitionResult.error("Transition not found")
    }

    override fun getState(): S = state

    override fun getContext(): C = context
}

private class FailedTransitionException(val result: TransitionResult) : Exception()
