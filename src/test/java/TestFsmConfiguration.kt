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

    val fakeConsumer = Consumer<Context, Event> { _, _, -> }
    val successFilter = Filter<Context, Event> { _, _, -> TransitionResult.SUCCESS }
    val failingFilter = Filter<Context, Event> { _, _, -> TransitionResult.failed("D-oh!") }
    val throwingConsumer = Consumer<Context, Event> { _, _, -> throw IllegalArgumentException() }
    val valueExtractor: (MyEvent) -> Int = { it.count }

    @Test
    fun `test config`() {
        val builder = FsmBuilderImpl<Context, Event, State>()

        builder.transition(State.DRAFT, MyEvent::class.java, State.WAITING) {
            consume(fakeConsumer)
            filter(successFilter)
            route(valueExtractor) {
                whenever(1) {
                    consume(LoggingConsumer("Value is 1"))
                    consume(LoggingConsumer("Draft"))
                    setState(State.DRAFT)
                }
                whenever(2) {
                    consume(LoggingConsumer("Value is 2"))
                    consume(LoggingConsumer("Pending"))
                    setState(State.PENDING)
                }
                whenever(3) {
                    consume(LoggingConsumer("Value is 3"))
                    filter(failingFilter)
                    consume(LoggingConsumer("This should not be printed"))
                }
                otherwise {
                    filter(CountingFilter(5))
                    route(valueExtractor) {
                        whenever(8, 9, 10) {
                            consume(LoggingConsumer("Booked"))
                            setState(State.FINISHED)
                        }
                        otherwise {
                            consume(LoggingConsumer("Placed"))
                        }
                    }
                }
            }
            consume(LoggingConsumer("Router passed"))
        }.ifFailed {
            consume(throwingConsumer)
        }.ifFailed {
            consume(LoggingConsumer("Failed"))
        }

        for (i in 1..10) {
            logger.info("===========================")
            builder.build().apply {
                init(Context(), State.DRAFT)
            }.run {
                invoke(MyCountedEvent(i)).let {
                    logger.info("Message: " + it.msg)
                }
                logger.info("State: " + getState())
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