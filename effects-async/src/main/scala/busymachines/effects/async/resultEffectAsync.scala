package busymachines.effects.async

import busymachines.core.Anomaly
import busymachines.effects.sync._

/**
  *
  * @author Lorand Szakacs, lsz@lorandszakacs.com, lorand.szakacs@busymachines.com
  * @since 29 Jan 2018
  *
  */
object ResultSyntaxAsync {

  /**
    *
    */
  trait Implcits {
    implicit final def bmcResultAsyncCompanionObjectOps(obj: Result.type): CompanionObjectOps =
      new CompanionObjectOps(obj)

    implicit final def bmcResultAsyncReferenceOps[T](value: Result[T]): ReferenceOps[T] =
      new ReferenceOps(value)

    implicit final def bmcResultAsyncSafeReferenceOps[T](value: => Result[T]): SafeReferenceOps[T] =
      new SafeReferenceOps(value)
  }

  /**
    *
    */
  final class CompanionObjectOps(val obj: Result.type) extends AnyVal {

    @inline def asFuture[T](value: Result[T]): Future[T] =
      FutureOps.fromResult(value)

    @inline def asIO[T](value: Result[T]): IO[T] =
      IOOps.fromResult(value)

    @inline def asTask[T](value: Result[T]): Task[T] =
      TaskOps.fromResult(value)

    @inline def suspendInFuture[T](value: => Result[T])(implicit ec: ExecutionContext): Future[T] =
      FutureOps.suspendResult(value)

    @inline def suspendInIO[T](value: => Result[T]): IO[T] =
      IOOps.suspendResult(value)

    @inline def suspendInTask[T](value: => Result[T]): Task[T] =
      TaskOps.suspendResult(value)
  }

  /**
    *
    */
  final class ReferenceOps[T](val value: Result[T]) extends AnyVal {

    /**
      *
      * Lift the [[Result]] in this effect
      * [[Incorrect]] becomes a failed effect
      * [[Correct]] becomes a pure effect
      *
      */
    @inline def asFuture: Future[T] =
      FutureOps.fromResult(value)

    /**
      *
      * Lift the [[Result]] in this effect
      * [[Incorrect]] becomes a failed effect
      * [[Correct]] becomes a pure effect
      *
      */
    @inline def asIO: IO[T] =
      IOOps.fromResult(value)

    /**
      *
      * Lift the [[Result]] in this effect
      * [[Incorrect]] becomes a failed effect
      * [[Correct]] becomes a pure effect
      *
      */
    @inline def asTask: Task[T] =
      TaskOps.fromResult(value)

    //=========================================================================
    //==================== Run side-effects on Option state ===================
    //=========================================================================

    /**
      *
      * Runs the given effect when the value of this [[Result]] is [[Incorrect]]
      *
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    @inline def effectOnFailFuture(effect: Anomaly => Future[_])(implicit ec: ExecutionContext): Future[Unit] =
      FutureOps.effectOnFail(value, effect)

    /**
      *
      * Runs the given effect when the value of this [[Result]] is [[Correct]]
      *
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    @inline def effectOnPureFuture(effect: T => Future[_])(implicit ec: ExecutionContext): Future[Unit] =
      FutureOps.effectOnPure(value, effect)

    /**
      *
      * Runs the given effect when the value of this [[Result]] is [[Incorrect]]
      *
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    @inline def effectOnFailIO(effect: Anomaly => IO[_]): IO[Unit] =
      IOOps.effectOnFail(value, effect)

    /**
      *
      * Runs the given effect when the value of this [[Result]] is [[Correct]]
      *
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    @inline def effectOnPureIO(effect: T => IO[_]): IO[Unit] =
      IOOps.effectOnPure(value, effect)

    /**
      *
      * Runs the given effect when the value of this [[Result]] is [[Incorrect]]
      *
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    @inline def effectOnFailTask(effect: Anomaly => Task[_]): Task[Unit] =
      TaskOps.effectOnFail(value, effect)

    /**
      *
      * Runs the given effect when the value of this [[Result]] is [[Correct]]
      *
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    @inline def effectOnPureTask(effect: T => Task[_]): Task[Unit] =
      TaskOps.effectOnPure(value, effect)

  }

  /**
    *
    */
  final class SafeReferenceOps[T](value: => Result[T]) {

    /**
      * N.B.
      * For Future in particular, this is useless, since you suspend a side-effect which
      * gets immediately applied due to the nature of the Future. This is useful only that
      * any exceptions thrown (bad code) is captured "within" the Future.
      *
      * Suspend any side-effects that might happen during the creation of this [[Result]].
      *
      * N.B. this is useless if the [[Result]] was previously assigned to a "val".
      * You might as well use [[FutureOps.fromResult]]
      */
    @inline def suspendInFuture(implicit ec: ExecutionContext): Future[T] =
      FutureOps.suspendResult(value)

    /**
      * Suspend any side-effects that might happen during the creation of this [[Result]].
      * Other than that it has the semantics of [[IOOps.fromResult]]
      *
      * N.B. this is useless if the [[Result]] was previously assigned to a "val".
      * You might as well use [[IOOps.fromResult]]
      */
    @inline def suspendInIO: IO[T] =
      IOOps.suspendResult(value)

    /**
      * Suspend any side-effects that might happen during the creation of this [[Result]].
      * Other than that it has the semantics of [[TaskOps.fromResult]]
      *
      * N.B. this is useless if the [[Result]] was previously assigned to a "val".
      * You might as well use [[TaskOps.fromResult]]
      */
    @inline def suspendInTask: Task[T] =
      TaskOps.suspendResult(value)
  }
}
