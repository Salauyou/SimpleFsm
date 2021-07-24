package ru.salauyou.fsm

/**
 * <p>Created on 2021-07-22
 * @author Aliaksandr Salauyou
 */

/**
 * Decides to continue or not execution by returning [TransitionResult].
 * Execution will not continue if [TransitionResult.isSuccess] = [false]
 */
fun interface Filter<C:Any, E:Any> {
    fun filter(context: C, event: E): TransitionResult
}

/**
 * Executes some action
 */
fun interface Consumer<C:Any, E:Any> {
    fun consume(context: C, event: E)
    fun consume(context: C, event: E, result: TransitionResult) = consume(context, event)
}

/**
 * Transforms an event. Returned event will be used for further execution
 */
fun interface Transformer<C:Any, E:Any, R:E> {
    fun transform(context: C, event: E): R
}

/**
 * SM configuration builder
 *
 * @param C context type
 * @param E SM event type
 * @param S state type
 */
interface FsmBuilder<C:Any, E:Any, S:Any> {

    /**
     * Defines a transition which should be done when SM being in
     * [sourceState] receives [event] of type [T]. Upon successful
     * execution SM switches into [targetState] unless it is [null]
     */
    fun <T:E> transition(
        sourceState: S,
        event: Class<T>,
        targetState: S? = null,
        execution: Execution<C, T, S>.() -> Unit)
    : Transition<C, T, S>

    fun build(): FsmInstance<C, E, S>
}

/**
 * SM transition
 *
 * @param <C> context type
 * @param <E> event type
 * @param <S> state type
 */
interface Transition<C:Any, E:Any, S:Any> {

    /**
     * Adds execution block to run if prior execution failed.
     * In this block [TransitionResult] is not modified and SM does not move
     * into target state
     */
    fun ifFailed(execution: Execution<C, E, S>.() -> Unit): Transition<C, E, S>
}

/**
 * SM instance built on SM configuration
 */
interface FsmInstance<C:Any, E:Any, S:Any> {
    fun init(context: C, state: S)
    fun invoke(event: E): TransitionResult
    fun getState(): S
    fun getContext(): C
}