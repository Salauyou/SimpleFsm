package ru.salauyou.fsm

/**
 * <p>Created on 2021-07-24
 * @author Aliaksandr Salauyou
 */
class TransitionResult private constructor(
        private val status: TransitionStatus,
        private val message: String?) {

    companion object {
        val SUCCESS = success()
        fun success(msg: String? = null) = TransitionResult(TransitionStatus.SUCCESS, msg)
        fun failed(msg: String? = null) = TransitionResult(TransitionStatus.TRANSITION_FAILED, msg)
        fun error(msg: String? = null) = TransitionResult(TransitionStatus.TRANSITION_ERROR, msg)
    }

    fun isSuccess() = status.isSuccess()
    fun getStatus() = status
    fun getMessage() = message

    override fun toString() = "$status${message?.let {": $it"} ?: "" }"
}


enum class TransitionStatus(private val success: Boolean) {

    /** Successful transition */
    SUCCESS(true),

    /** Failed due to filter fail */
    TRANSITION_FAILED(false),

    /** Failed due to uncaught exception */
    TRANSITION_ERROR(false);

    fun isSuccess() = success
}