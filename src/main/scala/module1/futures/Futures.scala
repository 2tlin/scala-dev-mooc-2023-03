package module1.futures

import scala.collection.mutable
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object Futures extends App {
  def longRunningComputation = ???

  // Constructors
  lazy val f1: Future[Int] = Future.successful(10) // no concurrency
  lazy val f2: Future[Nothing] = Future.failed(new NoSuchElementException()) // no concurrency
  lazy val f3: Future[Int] = Future.fromTry(Success(10)) // no concurrency
  lazy val f4: Future[Nothing] = Future.fromTry(Failure(new NoSuchElementException())) // no concurrency
  lazy val f5: Future[Int] = Future(10)(scala.concurrent.ExecutionContext.global) // concurrency
  println(f1) // Future(Success(10))
  println(f2) // Future(Failure(java.util.NoSuchElementException))
  println(f3) // Future(Success(10))
  println(f4) // Future(Failure(java.util.NoSuchElementException))
  println(f5) // Future(<not completed>)

  // longRunningComputation будет выполняться в отдельном потоке, который предоставит ExecutionContext
  lazy val f6 = Future(longRunningComputation)(scala.concurrent.ExecutionContext.global)

  //combinators

  import scala.concurrent.ExecutionContext.Implicits.global // Это ForkJoin, где пулы это потоки-демоны

  def getRatesLocation1: Future[Int] = Future {
    Thread.sleep(1000)
    println("Get getRatesLocation1 " + Thread.currentThread().getName)
    10
  }

  def getRatesLocation2: Future[Int] = Future {
    Thread.sleep(2000)
    println("Get getRatesLocation2 " + Thread.currentThread().getName)
    20
  }

  def printRunningTime2(v: => Unit): Unit = {
    val start = System.currentTimeMillis()
    v
    val end = System.currentTimeMillis()
    println(s"Execution time ${end - start}")
  }

  // Вариант с Future
  // т.к. Future имеет map/flatMap, то используем for-compehension
  def printRunningTime[T](v: => Future[T]): Future[T] =
    for {
      start <- Future.successful(System.currentTimeMillis())
      res <- v
      end <- Future.successful(System.currentTimeMillis())
      _ <- Future.successful(println(s"Execution time ${end - start}"))
    } yield res

  def getRates: Future[Int] = for { // как бы два вложенных цикла
    r1 <- getRatesLocation1 // вложенный цикл через flatMap
    r2 <- getRatesLocation2 // внешний цикл через маp
  } yield r1 + r2

  // , т.к. в случае с потоками-демонами программа не ждет окнчания работы таких потоков
  // и они завершают работу в фоновом режиме.

//  printRunningTime(getRates).foreach(println) // Ничего не выводит

  // Чтобы увидеть результат, можно создать ExecutionContext НЕ из ForkJoin пулов
  // или заставить основной поток подождать результатов

//  Thread.sleep(3000)

  /*
  Get getRatesLocation1 scala-execution-context-global-14
  Get getRatesLocation2 scala-execution-context-global-14
  Execution time 3004
  30
   */

  // Произошло последовательное выполнение на потоке scala-execution-context-global-14,
  // т.к. flatMap - это последовательный комбинатор
  // Представим for-comprehention в getRates через последовательные вызовы

  // def map[S](f: T => S): Future[S]  = ???
  // def flatMap[S](f: T => Future[S]) = ???

  val c: Future[Int] = getRatesLocation1.flatMap(r1 =>
    getRatesLocation2.map(r2 => r1 + r2)
  )

  // Т.е. чтобы выполнялась r2 => r1 + r2, сначала нужно, чтобы завершилась функция getRatesLocation1.
  // Только тогда мы получим r1, а getRatesLocation2 будет вызвана только потом после завершения getRatesLocation1
  // Потом, когда завершится getRatesLocation2, будет происходить сложение r1 + r2

  // Что сделать, чб функции getRatesLocation выполнялись параллельно ?

  def getRates2: Future[Int] = {
    val v1 = getRatesLocation1
    val v2 = getRatesLocation2
    for { // как бы два вложенных цикла
      r1 <- v1 //  вложенный цикл через flatMap
      r2 <- v2 //  внешний цикл через маp
    } yield r1 + r2
  }

  printRunningTime(getRates2).foreach(println) // видим выполнение в разных потоках
  Thread.sleep(3000)
  /*
  Get getRatesLocation1 scala-execution-context-global-14
  Get getRatesLocation2 scala-execution-context-global-15
  Execution time 2002
  30
   */

  /*
  Создание Future происходит во время вызова функции getRatesLocation. Поэтому в случае getRates,
  когда мы ее вызывали внутри цепочки flatMap/map, вторая функция вызывалась только тогда,
  когда завершалась первая функция (последовтаельное выполнение).
  В случае с getRates2 мы инициализировали Future ВНЕ for-comprehentionи получили параллельное выполнение.
   */

  // Future methods
  def getRates3: Future[(Int, Int)] = getRatesLocation1 zip getRatesLocation2

  printRunningTime(getRates3).foreach(println)
  Thread.sleep(3000)
  /*
  Get getRatesLocation1 scala-execution-context-global-14
  Get getRatesLocation2 scala-execution-context-global-15
  Execution time 2002
  (10,20)
   */

  // методы API Future такие как map, flatMap, zip, foreach, recover etc, которые производят модификации над элементом Future,
  // который появится когда-нибудь потом, не блокируют текущий поток выполнения

  // recover - функция восстановления состояния при исключении

  val default: Future[Int] = getRatesLocation1.recover {
    case e => 0 // e - Exception
  }

  // Await.result() ринимает Future и блокирует поток до его выполнения

  val result: (Int, Int) = Await.result(getRates3, Duration(5, SECONDS)) // блокирующая операция
  println(result._2) // 20

  val resultF: Future[(Int, Int)] = Await.ready(getRates3, Duration(5, SECONDS)) // блокирующая операция
  resultF.foreach(println)// (10, 20)

  val a: Future[Int] = resultF.map(p => (p._1 + 1, p._2 + 1)).map {
    case a => a._2
  }

  Thread.sleep(3000)
  println(a)


//  val intF: Future[Int] = Future.successful(10)

  val b1 = Await.ready(Future(10), Duration(3, SECONDS)).onComplete {
    case Success(value) => value
    case Failure(exception) => new Exception(exception)
  }

  // Как получить результат Future.onComplete
  val res: mutable.ArrayBuffer[Int] = mutable.ArrayBuffer.empty

  Future(10).onComplete {
    case Success(value) =>  res.addOne(value)
    case Failure(exception) => new Exception(exception)
  }

  Thread.sleep(2000)
  println(res) // ArrayBuffer(10)

}





