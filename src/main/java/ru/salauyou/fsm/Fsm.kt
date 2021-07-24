package ru.salauyou.fsm

/**
 * <p>Created on 2021-07-22
 * @author Aliaksandr Salauyou
 */
interface Transition<C:Any, E:Any, S:Any> {
    fun ifFailed(execution: Execution<C, E, S>.() -> Unit): Transition<C, E, S>
}

interface Execution<C:Any, E:Any, S:Any> {
    fun consume(consumer: Consumer<in C, in E>)
    fun filter(filter: Filter< in C, in E>)
    fun setState(state: S)
    fun <R:E> transform(transformer: Transformer<in C, in E, out R>)
    fun <V> route(router: (E) -> V, executions: RouteExecution<C, E, S, V>.() -> Unit)
}

interface RouteExecution<C:Any, E:Any, S:Any, V> {
    fun whenever(vararg values: V, execution: Execution<C, E, S>.() -> Unit)
    fun otherwise(execution: Execution<C, E, S>.() -> Unit)
}

fun interface Consumer<C:Any, E:Any> {
    fun consume(context: C, event: E)
    fun consume(context: C, event: E, result: TransitionResult) = consume(context, event)
}

fun interface Transformer<C:Any, E:Any, R:E> {
    fun transform(context: C, event: E): R
}

fun interface Filter<C:Any, E:Any> {
    fun filter(context: C, event: E): TransitionResult
}

interface FsmBuilder<C:Any, E:Any, S:Any> {
    fun <T:E> transition(
        sourceState: S,
        event: Class<T>,
        targetState: S? = null,
        transition: Execution<C, T, S>.() -> Unit)
    : Transition<C, T, S>

    fun build(): FsmInstance<C, E, S>
}

interface FsmInstance<C:Any, E:Any, S:Any> {
    fun init(context: C, state: S)
    fun invoke(event: E): TransitionResult
    fun getState(): S
    fun getContext(): C
}