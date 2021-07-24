import org.junit.Test
import org.slf4j.LoggerFactory
import ru.salauyou.fsm.Consumer
import ru.salauyou.fsm.Filter
import ru.salauyou.fsm.FsmBuilderImpl
import ru.salauyou.fsm.TransitionResult

/**
 * <p>Created on 2021-07-22
 * @author Aliaksandr Salauyou
 */
class TestFsmConfiguration {

    companion object {
        val logger = LoggerFactory.getLogger("test")
    }

    val valueLoggingConsumer = Consumer<Context, MyEvent> { _, e ->
        logger.info("Value is ${e.count}")
    }
    val successFilter = Filter<Context, Event> { _, _, -> TransitionResult.SUCCESS }
    val failingFilter = Filter<Context, Event> { _, _, -> TransitionResult.failed("D-oh!") }
    val throwingConsumer = Consumer<Context, Event> { _, _, ->
        throw IllegalArgumentException("Ugh!")
    }

    val valueExtractor: (MyEvent) -> Int = { it.count }

    @Test
    fun `test config`() {
        val builder = FsmBuilderImpl<Context, Event, State>()

        builder.transition(State.DRAFT, MyEvent::class.java, State.WAITING) {
            consumer(valueLoggingConsumer)
            filter(successFilter)
            router(valueExtractor) {
                whenever(1) {
                    consumer(LoggingConsumer("Draft"))
                    setState(State.DRAFT)
                }
                whenever(2) {
                    consumer(LoggingConsumer("Pending"))
                    setState(State.PENDING)
                }
                whenever(3) {
                    filter(failingFilter)
                    consumer(LoggingConsumer("This should not be printed"))
                }
                whenever(4) {
                    consumer(throwingConsumer)
                    consumer(LoggingConsumer("This shoud not be printed"))
                }
                otherwise {
                    filter(CountingFilter(5))
                    router(valueExtractor) {
                        whenever(8, 9, 10) {
                            consumer(LoggingConsumer("Finished"))
                            setState(State.FINISHED)
                        }
                        otherwise {
                            consumer(LoggingConsumer("Waiting"))
                        }
                    }
                }
            }
            consumer(LoggingConsumer("Router passed"))
        }.ifFailed {
            consumer(throwingConsumer)
        }.ifFailed {
            consumer(LoggingConsumer("Failed"))
        }

        for (i in 1..10) {
            logger.info("===========================")
            builder.build().apply {
                init(Context(), State.DRAFT)
            }.run {
                invoke(MyCountedEvent(i)).let {
                    logger.info("Result: [$it], state: [${getState()}]")
                }
            }
        }
    }


    class LoggingConsumer(val msg: String): Consumer<Context, Event> {
        override fun consume(context: Context, event: Event) {
            logger.info(msg)
        }
    }

    class CountingFilter(private val c: Int) : Filter<Context, MyEvent> {
        override fun filter(context: Context, event: MyEvent): TransitionResult =
            when(event.count > c) {
                true -> TransitionResult.SUCCESS
                false -> TransitionResult.failed("Value ${event.count} must be > $c")
            }
    }

    interface Event

    class Context

    open class MyEvent(open val count: Int): Event

    class MyCountedEvent(override val count: Int): MyEvent(count)

    enum class State {
        DRAFT,
        PENDING,
        WAITING,
        FINISHED,
    }
}