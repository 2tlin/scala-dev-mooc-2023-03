package module1.futures

import module1.utils.NameableThreads
import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

object ExecutionContexts extends App {
  // Execution context
  // How to create Execution Context - wrapper над java Executor

  val pool1: ExecutorService = Executors.newFixedThreadPool(2, NameableThreads("fixed-pool-1"))
  val pool2: ExecutorService = Executors.newCachedThreadPool(NameableThreads("cached-pool-2"))
  val pool3: ExecutorService = Executors.newWorkStealingPool(4)
  val pool4: ExecutorService = Executors.newSingleThreadExecutor(NameableThreads("singleThread-pool-4"))

  lazy val ec  = ExecutionContext.fromExecutor(pool1)
  lazy val ec2 = ExecutionContext.fromExecutor(pool2)
  lazy val ec3 = ExecutionContext.fromExecutor(pool3)
  lazy val ec4 = ExecutionContext.fromExecutor(pool4)

  def action(v: Int): Int = {
    Thread.sleep(1000)
    println(s"Action $v in ${Thread.currentThread().getName}")
    v
  }

  // Transfer different execution contexts based on different pools to different futures:

  val f5 = Future(action(10))(ec)
  val f6 = Future(action(20))(ec2)

  val f7 = f5.flatMap {v1 =>
    action(50)
    f6.map{v2 => action(v1 + v2)
    }(ec4)
  }(ec3)

  Await.result(f7, 6 seconds)
  /*
  Action 10 in fixed-pool-1-thread-0
  Action 20 in cached-pool-2-thread-0
  Action 50 in ForkJoinPool-1-worker-3
  Action 30 in singleThread-pool-4-thread-0
   */

  // все Executors нужно явным образом останавливать. Для этого есть метод shutdown
  // Чтобы остановить основной поток main, нужно остановить все другие потоки кроме WorkStealingPool, который запускает демоны
  // т.к. ExecutionContext.global - это как раз WorkStealingPool поток, то его закрывать напрямую не нужно

  pool1.shutdownNow()
  pool2.shutdownNow()
  pool3.shutdownNow() // необязательно
  pool4.shutdownNow()
}

