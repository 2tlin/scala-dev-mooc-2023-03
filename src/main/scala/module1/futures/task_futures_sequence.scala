package module1.futures

import HomeworksUtils.TaskSyntax

import scala.::
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

object task_futures_sequence {

  /**
   * В данном задании Вам предлагается реализовать функцию fullSequence,
   * похожую на Future.sequence, но в отличии от нее,
   * возвращающую все успешные и не успешные результаты.
   * Возвращаемое тип функции - кортеж из двух списков,
   * в левом хранятся результаты успешных выполнений,
   * в правово результаты неуспешных выполнений.
   * Не допускается использование методов объекта Await и мутабельных переменных var
   */
  /**
   * @param futures список асинхронных задач
   * @return асинхронную задачу с кортежом из двух списков
   */
  def flatMmap[T, B](future: Future[T])(f: T => Future[B])(implicit ec: ExecutionContext): Future[B] = {
    val p = Promise[B] // Promise от возвращаемого типа
    future.onComplete { // ожидаем тип Т
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
  def fullSequence[A](futures: List[Future[A]])(implicit ex: ExecutionContext): Future[(List[A], List[Throwable])] = {
    val p = Promise[Future[(List[A], List[Throwable])]]
    futures
      .foldLeft((List.empty[A], List.empty[Throwable])) { (acc, fut) =>
        fut.onComplete {
          case Failure(exception) => p.failure(exception) :: acc._2
          case Success(value) => p.complete(Try(value :: acc._1, acc._2))
        }
        acc
      }
    p.future
    }

    task"Реализуйте метод `fullSequence`"()

    /** Simple version of `Future.traverse`. Asynchronously and non-blockingly transforms, in essence, a `IterableOnce[Future[A]]`
     *  into a `Future[IterableOnce[A]]`. Useful for reducing many `Future`s into a single `Future`.
     *
     * @tparam A        the type of the value inside the Futures
     * @tparam CC       the type of the `IterableOnce` of Futures
     * @tparam To       the type of the resulting collection
     * @param in        the `IterableOnce` of Futures which will be sequenced
     * @return          the `Future` of the resulting collection
     */
    final def sequence[A, CC[X] <: IterableOnce[X], To](in: CC[Future[A]])(implicit bf: BuildFrom[CC[Future[A]], A, To], executor: ExecutionContext): Future[To] =
      in.iterator.foldLeft(successful(bf.newBuilder(in))) {
        (fr, fa) => fr.zipWith(fa)(Future.addToBuilderFun)
      }.map(_.result())(if (executor.isInstanceOf[BatchingExecutor]) executor else parasitic)


  }
}
