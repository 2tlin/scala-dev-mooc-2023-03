package module1.futures

import HomeworksUtils.TaskSyntax

import scala.concurrent.{ExecutionContext, Future}

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
  def fullSequence[A](futures: List[Future[A]])
                     (implicit ex: ExecutionContext): Future[(List[A], List[Throwable])] = {
    task"Реализуйте метод `fullSequence`"()
    /*
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
     */

  }
}
