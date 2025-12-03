package uic.cs554

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._

object Deadlocker {

  case class Iter(i: Int, last: Long)

  def apply(name: String, dest: ActorRef[Printer.PrintMe]): Behavior[Iter] = {
    /* actor HAS state
     * namely, an exclusive lock captured by this closure
     * the lock is Reentrant, meaning it can be acquired multiple times
     * However, lock itself stores state on a THREAD level
     * So if an actor moves between threads, it invalidates the assumptions of the lock!
     * We avoid deadlock using tryLock instead of lock
     * because the locks are exclusive, the moment we fail a tryLock, we know the system is about
     * to deadlock!
     * */
    val lock = new java.util.concurrent.locks.ReentrantLock()
    Behaviors.receive { 
      case (ctx, Iter(i, lastId)) =>
        val thisId = Thread.currentThread().getId()
        if (!lock.tryLock()) {
          dest ! Printer.PrintMe(String.format("%s failed to acquire its exclusive lock after %d iterations.", name, i))
          Behaviors.stopped
        } else { 
          ctx.self ! Iter(i + 1, thisId)
          Behaviors.same
        }
    }
  }
}

object DeadlockHarness {

  def apply(): Behavior[NotUsed] = Behaviors.setup { context =>
    val printer = context.spawn(Printer(), "printer")

    // Create 5 awesome hakkers and assign them their left and right chopstick
    val yellers = for (i <- 1 to 25) yield {
      val name = String.format("Acotr%d", i)
      context.spawn(Deadlocker(name, printer), name)
    }

    // Signal all hakkers that they should start thinking, and watch the show
    yellers.foreach(_ ! Deadlocker.Iter(0, 0))

    Behaviors.empty
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem(DeadlockHarness(), "sample")
  }
}
