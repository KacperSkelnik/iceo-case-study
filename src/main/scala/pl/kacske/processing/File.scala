package pl.kacske.processing

import cats.effect.std.Queue
import cats.effect.{Async, Ref, Concurrent}
import cats.implicits._
import fs2.data.csv.decodeWithoutHeaders
import fs2.io.file.{Files, Path}
import fs2.{Pipe, Stream}
import pl.kacske.processing.CSVRow.implicits._

sealed trait File[F[_]] {
  def get(filePath: Path, moduloDivider: Int): Stream[F, Stream[F, Result]]
}

object File {

  final class groupBeResult[F[_], K, A](
      key: K,
      queue: Queue[F, Option[A]],
      stream: Stream[F, A]
  ) {
    def getKey: K = key
    def getQueue: Queue[F, Option[A]] = queue
    def getStream: Stream[F, A] = stream
  }

  def make[F[_]: Async: Concurrent]: File[F] = new File[F] {
    /*
     * inspired by https://gist.github.com/kiambogo/8247a7bbf79f00414d1489b7e6fc90d0
     *  */
    private[processing] def groupByStream[K, A](selector: A => F[K]): Pipe[F, A, groupBeResult[F, K, A]] = in =>
      fs2.Stream
        .eval(Ref.of[F, Map[K, groupBeResult[F, K, A]]](Map.empty))
        .flatMap { ref =>
          val cleanup: F[Unit] =
            ref.get.flatMap(_.toList.traverse(_._2.getQueue.offer(None))).map(_ => ())

          (in ++ fs2.Stream.eval(cleanup).drain).evalMap { el =>
            (selector(el), ref.get).flatMapN { (key, queues) =>
              queues.get(key) match {
                case Some(r) => r.getQueue.offer(el.some).as(Option.empty[groupBeResult[F, K, A]])
                case None =>
                  for {
                    newQ <- Queue.unbounded[F, Option[A]]
                    newStream <- Stream.fromQueueNoneTerminated(newQ).pure[F]
                    newResult <- new groupBeResult(key, newQ, newStream).pure[F]
                    _ <- newQ.offer(el.some)
                    _ <- ref.modify(x => (x + (key -> newResult), x))
                  } yield newResult.some
              }
            }
          }.unNone
        }

    private[processing] def sum(moduloResult: Int): Pipe[F, CSVRow, Result] = stream =>
      fs2.Stream
        .eval(Ref[F].of(Result(0, 0)))
        .flatMap { sum =>
          stream
            .evalMap(row => sum.updateAndGet(result => Result(result.sum + row.value, moduloResult)))
        }
        .takeRight(1)

    def get(filePath: Path, moduloDivider: Int): Stream[F, Stream[F, Result]] =
      Files[F]
        .readUtf8(filePath)
        .filter(s => s.trim.nonEmpty)
        .through(decodeWithoutHeaders[CSVRow]())
        .through(groupByStream[Int, CSVRow](csvRow => Math.floorMod(csvRow.value, moduloDivider).pure[F]))
        .map(result => result.getStream.through(sum(result.getKey)))
  }
}
