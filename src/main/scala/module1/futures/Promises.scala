package module1.futures

import java.util.{Timer, TimerTask}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

object Promises extends  App {
  /*
   Promise - контейнер для некоего одного значения типа Т, которое может появиться там в будущем:
   - не содержит значение типа Т
   - содержит значение типа Т
   - содержит ошибку

   В отличие от Future, в котором значение может просто появиться, в Promise мы можем значение положить сами.
   С точки зрения своей семаники Promise может быть также как и Future быть успешен с неким значением с типом Т, либо содержать ошибку,
   но при этом, в отличие от Future, он, в принципе,  может быть в состоянии, когда он ничего не содержит.
   Это можно воспринимать ка пустой контейнер, в котором есть "окошко", куда мы можем потом когда-нибудь что-то положить.
   Future - это про чтение. Во Future мы ничего потом реально положить не можем.    Мы его конструируем, и в нем что-то начинает вычисляться.
   Если мы его сами не конструируем, а получаем в качестве возвращаемого значения от какого-то API, то всё, что мы можем сделать,
   это прочитать в нем какое-то значение, когда оно появится, и применить к нему какие-то преобразования.
   A Promise - это про то, что мы сами можем в него что-то записать, когда нибудь потом.

   Тесная связь между ними заключается в том, что для любого Promise мы можем сразу вернуть проассоциированную с ним Future.
   Т.е. у нас будет связка Promise-Future, в которой мы можем в Promise что-то положить, и во Future это сразу появится.

   Если асинхронные вычисления представлять как pipeline,
   то со стороны Future мы может только читать, а со стороны Promise - только писать.
   */

  import scala.concurrent.ExecutionContext.Implicits.global

  val p1: Promise[Int] = Promise[Int]
  val f1: Future[Int] = p1.future // создаем ассоциированную Future

  println(p1.isCompleted) // false
  println(f1.isCompleted) // false

  f1.onComplete {
    case Success(value) => println(value)
    case Failure(ex) => println(ex.getMessage)
  }

  // Только после того, как мы добавим значение в Promise, будут выполняться isCompleted=true и onComplete=Success
  p1.complete(Try(10))

  println(p1.isCompleted) // true
  println(f1.isCompleted) // true
}
  object FutureSyntax {
    def map[T, B](future: Future[T])(f: T => B)(implicit ec: ExecutionContext): Future[B] = {
      flatMmap(future)(v => make(f(v)))
    }
    def flatMmap[T, B](future: Future[T])(f: T => Future[B])(implicit ec: ExecutionContext): Future[B] = {
      val p = Promise[B] // Promise от возвращаемого типа
      future.onComplete{ // ожидаем тип Т
        case Failure(exception) => p.failure(exception)
        case Success(value) => f(value).onComplete { // ожидаем тип В
          case Failure(exception) => p.failure(exception)
          case Success(value) => p.complete(Try(value))
        }
      }
      p.future
    }


    def make[T](v: => T)(implicit ec: ExecutionContext): Future[T] = {
      val p = Promise[T]
      val rr = new Runnable {
        override def run(): Unit = p.complete(Try(v))
      }
      ec.execute(rr) // запускаем новый поток
      p.future
    }

    def make[T](v: => T, timeout: Long): Future[T] = {
      val p = Promise[T]
      val timer = new Timer(true)
      val task = new TimerTask {
        override def run(): Unit = ???
      }
      timer.schedule(task, timeout)
      ???
    }



  }





