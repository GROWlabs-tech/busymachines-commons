package busymachines.effects.async

import busymachines.core._
import busymachines.effects.sync._

import scala.{concurrent => sc}

/**
  *
  * @author Lorand Szakacs, lsz@lorandszakacs.com, lorand.szakacs@busymachines.com
  * @since 29 Jan 2018
  *
  */
trait FutureTypeDefinitions {
  type Future[T] = sc.Future[T]
  val Future: sc.Future.type = sc.Future

  type ExecutionContext = sc.ExecutionContext
  val ExecutionContext: sc.ExecutionContext.type = sc.ExecutionContext

  val Await: sc.Await.type = sc.Await

  def blocking[T](body: => T): T = sc.blocking(body)

}

object FutureSyntax {

  /**
    *
    */
  trait Implicits {
    implicit def bmcFutureCompanionObjectOps(obj: Future.type): CompanionObjectOps =
      new CompanionObjectOps(obj)

    implicit def bmcFutureReferenceOps[T](value: Future[T]): ReferenceOps[T] =
      new ReferenceOps(value)

    implicit def bmcFutureNestedOptionOps[T](nopt: Future[Option[T]]): NestedOptionOps[T] =
      new NestedOptionOps(nopt)

    implicit def bmcFutureNestedResultOps[T](result: Future[Result[T]]): NestedResultOps[T] =
      new NestedResultOps(result)

    implicit def bmcFutureBooleanOps(test: Boolean): BooleanOps =
      new BooleanOps(test)

    implicit def bmcFutureNestedBooleanOps(test: Future[Boolean]): NestedBooleanOps =
      new NestedBooleanOps(test)
  }

  /**
    *
    */
  final class CompanionObjectOps(val obj: Future.type) {

    def pure[T](value: T): Future[T] =
      ???

    def successful[T](value: T): Future[T] =
      ???

    def fail[T](bad: Anomaly): Future[T] =
      ???

    def failWeak[T](bad: Throwable): Future[T] =
      ???

    def failure[T](bad: Anomaly): Future[T] =
      ???

    def failureWeak[T](bad: Throwable): Future[T] =
      ???

    // —— def unit: Future[Unit] —— already defined on Future object

    def fromOption[T](opt: Option[T], ifNone: => Anomaly): Future[T] =
      ???

    def fromOptionWeak[T](opt: Option[T], ifNone: => Throwable): Future[T] =
      ???

    def fromEither[L, R](either: Either[L, R], transformLeft: L => Anomaly): Future[R] =
      ???

    def fromEitherWeak[L, R](either: Either[L, R])(implicit ev: L <:< Throwable): Future[R] =
      ???

    def fromEitherWeak[L, R](either: Either[L, R], transformLeft: L => Throwable): Future[R] =
      ???

    def fromResult[T](result: Result[T]) =
      ???

    def cond[T](test: Boolean, good: => T, bad: => Anomaly): Future[T] =
      ???

    def condWeak[T](test: Boolean, good: => T, bad: => Throwable): Future[T] =
      ???

    def condWith[T](test: Boolean, good: => Future[T], bad: => Anomaly): Future[T] =
      ???

    def condWithWeak[T](test: Boolean, good: => Future[T], bad: => Throwable): Future[T] =
      ???

    def flatCond[T](test: Future[Boolean], good: => T, bad: => Anomaly): Future[T] =
      ???

    def flatCondWeak[T](test: Future[Boolean], good: => T, bad: => Throwable): Future[T] =
      ???

    def flatCondWith[T](test: Future[Boolean], good: => Future[T], bad: => Anomaly): Future[T] =
      ???

    def flatCondWithWeak[T](test: Future[Boolean], good: => Future[T], bad: => Throwable): Future[T] =
      ???

    def failOnTrue(test: Boolean, bad: => Anomaly): Future[Unit] =
      ???

    def failOnTrueWeak(test: Boolean, bad: => Throwable): Future[Unit] =
      ???

    def failOnFalse(test: Boolean, bad: => Anomaly): Future[Unit] =
      ???

    def failOnFalseWeak(test: Boolean, bad: => Throwable): Future[Unit] =
      ???

    def flatFailOnTrue(test: Future[Boolean], bad: => Anomaly): Future[Unit] =
      ???

    def flatFailOnTrueWeak(test: Future[Boolean], bad: => Throwable): Future[Unit] =
      ???

    def flatFailOnFalse(test: Future[Boolean], bad: => Anomaly): Future[Unit] =
      ???

    def flatFailOnFalseWeak(test: Future[Boolean], bad: => Throwable): Future[Unit] =
      ???

    def flattenOption[T](nopt: Future[Option[T]], ifNone: => Anomaly): Future[T] =
      ???

    def flattenOptionWeak[T](nopt: Future[Option[T]], ifNone: => Throwable): Future[T] =
      ???

    def flattenResult[T](value: Future[Result[T]]): Future[T] =
      ???

    def asResult[T](value: Future[T]): Future[Result[T]] =
      ???

    def suspendInIO[T](value: => Future[T]): IO[T] =
      ???

    def suspendInTask[T](value: => Future[T]): Task[T] =
      ???

    def unsafeSyncGet[T](value: Future[T]): T =
      ???

    //=========================================================================
    //================= Run side-effects in varrying scenarios ================
    //=========================================================================

    def effectOnTrue[_](test: Boolean, effect: => Future[_]): Future[Unit] =
      ???

    def flatEffectOnTrue[_](test: Future[Boolean], effect: => Future[_]): Future[Unit] =
      ???

    def effectOnFalse[_](test: Boolean, effect: => Future[_]): Future[Unit] =
      ???

    def flatEffectOnFalse[_](test: Future[Boolean], effect: => Future[_]): Future[Unit] =
      ???

    def effectOnEmpty[T, _](value: Option[T], effect: => Future[_]): Future[Unit] =
      ???

    def flatEffectOnEmpty[T, _](value: Future[Option[T]], effect: => Future[_]): Future[Unit] =
      ???

    def effectOnSome[T, _](value: Option[T], effect: T => Future[_]): Future[Unit] =
      ???

    def flatEffectOnSome[T, _](value: Future[Option[T]], effect: T => Future[_]): Future[Unit] =
      ???

    //=========================================================================
    //============================== Transformers =============================
    //=========================================================================

    def bimap[T, R](value: Future[T], good: T => R, bad: Throwable => Anomaly): Future[R] =
      ???

    def bimapWeak[T, R](value: Future[T], good: T => R, bad: Throwable => Throwable): Future[R] =
      ???

    def morph[T, R](value: Future[T], good: T => R, bad: Throwable => R): Future[R] =
      ???

    def discardContent[T](value: Future[T]): Future[Unit] =
      ???
  }

  /**
    *
    */
  final class ReferenceOps[T](private[this] val value: Future[T]) {

    def asResult: Future[Result[T]] =
      ???

    def unsafeSyncGet(): T =
      ???

    def bimap[R](good: T => R, bad: Throwable => Anomaly): Future[R] =
      ???

    def bimapWeak[R](good: T => R, bad: Throwable => Throwable): Future[R] =
      ???

    def morph[R](good: T => R, bad: Throwable => R): Future[R] =
      ???

    def discardContent: Future[Unit] =
      ???
  }

  final class SafeReferenceOps[T](value: => Future[T]) {

    def suspendInIO: IO[T] =
      ???

    def suspendInTask: Task[T] =
      ???

  }

  /**
    *
    *
    */
  final class NestedOptionOps[T](private[this] val nopt: Future[Option[T]]) {

    def flattenOption(ifNone: => Anomaly): Future[T] =
      ???

    def flattenOptionWeak(ifNone: => Throwable): Future[T] =
      ???

    def effectOnEmpty[R](effect: => Future[R]): Future[Unit] =
      ??? //Future.flatEffectOnEmpty

    def effectOnSome[R](effect: T => Future[R]): Future[Unit] =
      ??? //Future.flatEffectOnSome

  }

  /**
    *
    */
  final class NestedResultOps[T](private[this] val result: Future[Result[T]]) {

    def flattenResult: Future[T] =
      ???
  }

  /**
    *
    *
    */
  final class BooleanOps(private[this] val test: Boolean) {

    def condFuture[T](good: => T, bad: => Anomaly): Future[T] =
      ???

    def condFutureWeak[T](good: => T, bad: => Throwable): Future[T] =
      ???

    def condWithFuture[T](good: => Future[T], bad: => Anomaly): Future[T] =
      ???

    def condWithFutureWeak[T](good: => Future[T], bad: => Throwable): Future[T] =
      ???

    def failOnTrueFuture(bad: => Anomaly): Future[Unit] =
      ???

    def failOnTrueFutureWeak(bad: => Throwable): Future[Unit] =
      ???

    def failOnFalseFuture(bad: => Anomaly): Future[Unit] =
      ???

    def failOnFalseFutureWeak(bad: => Throwable): Future[Unit] =
      ???

  }

  /**
    *
    *
    */
  final class NestedBooleanOps(private[this] val test: Future[Boolean]) {

    def cond[T](good: => T, bad: => Anomaly): Future[T] =
      ???

    def condWeak[T](good: => T, bad: => Throwable): Future[T] =
      ???

    def condWith[T](good: => Future[T], bad: => Anomaly): Future[T] =
      ???

    def condWithWeak[T](good: => Future[T], bad: => Throwable): Future[T] =
      ???

    def failOnTrue(bad: => Anomaly): Future[Unit] =
      ???

    def failOnTrueWeak(bad: => Throwable): Future[Unit] =
      ???

    def failOnFalse(bad: => Anomaly): Future[Unit] =
      ???

    def failOnFalseWeak(bad: => Throwable): Future[Unit] =
      ???

  }
}
