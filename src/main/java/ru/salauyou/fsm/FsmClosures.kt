package ru.salauyou.fsm

/**
 * <p>Created on 2021-07-24
 * @author Aliaksandr Salauyou
 */

/**
 * SM execution closure
 *
 * @param C context type
 * @param E event type
 * @param S state type
 */
interface Execution<C:Any, E:Any, S:Any> {
    fun consumer(consumer: Consumer<in C, in E>)
    fun filter(filter: Filter<in C, in E>)
    fun <R:E> transform(transformer: Transformer<in C, in E, out R>)
    fun <K> router(keyExtractor: (E) -> K, execution: RouteExecution<C, E, S, K>.() -> Unit)

    /** Manually updates SM state. If invoked, target
     * transition state is not applied */
    fun setState(state: S)
}

/**
 * SM routing closure
 *
 * @param C context type
 * @param E event type
 * @param S state type
 * @param K routing key type
 */
interface RouteExecution<C:Any, E:Any, S:Any, K> {
    fun whenever(vararg keys: K, execution: Execution<C, E, S>.() -> Unit)
    fun otherwise(execution: Execution<C, E, S>.() -> Unit)
}
