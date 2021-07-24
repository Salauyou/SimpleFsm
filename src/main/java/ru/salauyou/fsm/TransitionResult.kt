package ru.salauyou.fsm

/**
 * <p>Created on 2021-07-24
 * @author Aliaksandr Salauyou
 */
class TransitionResult private constructor(val status: TransitionStatus, val msg: String?) {

    companion object {
        val SUCCESS = success()
        fun success(msg: String? = null) = TransitionResult(TransitionStatus.SUCCESS, msg)
        fun failed(msg: String? = null) = TransitionResult(TransitionStatus.TRANSITION_FAILED, msg)
        fun error(msg: String? = null) = TransitionResult(TransitionStatus.TRANSITION_ERROR, msg)
    }

    fun success() = status.success()

    fun status() = status
}


enum class TransitionStatus(private val success: Boolean) {

    /** Successful transition */
    SUCCESS(true),

    /** Failed due to filter fail */
    TRANSITION_FAILED(false),

    /** Failed due to uncaught exception */
    TRANSITION_ERROR(false);

    fun success() = success
}