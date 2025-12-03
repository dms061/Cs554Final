package uic.cs554

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._

/* From the Pekko docs on interaction styles
 * This actor is the interface to "stdout"
 * Any actor can use the context log (I believe)
 * However, to demonstrate how to correctly guard
 * a resource in an Actor framework, we've created
 * made a single actor who is responsible for logging */
object Printer {

  case class PrintMe(message: String)

  def apply(): Behavior[PrintMe] =
    Behaviors.receive {
      case (context, PrintMe(message)) =>
        context.log.info(message)
        Behaviors.same
    }
}

object Yeller {

  case class Iter(i: Int, last: Long)

  def apply(name: String, dest: ActorRef[Printer.PrintMe]): Behavior[Iter] =
    /* This actor continues to execute until it observes itself transition between threads
     * Transitions are observed by comparing the id of the current thread with the last observed thread id
     * If the actor detects it is on a new thread, it sends a message to the Printer and terminates
     *
     * The actor is referentially transparent as the actor itself is stateless (e.g., no mutable fields)
     * Instead, it uses the message to hold the last "state" (e.g., last observed thread id)
     * To update the state, it sends itself a message, and processes it
     * Functional Actor programming!
     * */
    Behaviors.receive { 
      case (ctx, Iter(i, lastId)) =>
        val thisId = Thread.currentThread().getId()
        if (lastId > 0 && thisId != lastId) {
          dest ! Printer.PrintMe(String.format("%s moved from %d to %d after %d iterations", name, lastId, thisId, i))
          Behaviors.stopped
        } else { 
          ctx.self ! Iter(i + 1, thisId)
          Behaviors.same
        }
    }
}

object Harness {

  def apply(): Behavior[NotUsed] = Behaviors.setup { context =>
    val printer = context.spawn(Printer(), "printer")

    // Create 5 awesome hakkers and assign them their left and right chopstick
    val yellers = for (i <- 1 to 25) yield {
      val name = String.format("Acotr%d", i)
      context.spawn(Yeller(name, printer), name)
    }

    // Signal all hakkers that they should start thinking, and watch the show
    yellers.foreach(_ ! Yeller.Iter(0, 0))

    Behaviors.empty
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem(Harness(), "sample")
  }
}
