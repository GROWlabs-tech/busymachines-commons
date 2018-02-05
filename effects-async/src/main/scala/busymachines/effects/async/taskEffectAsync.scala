package busymachines.effects.async

import busymachines.core._
import busymachines.duration, duration.FiniteDuration
import busymachines.effects.sync._

import scala.collection.generic.CanBuildFrom
import scala.util.control.NonFatal

/**
  *
  * @author Lorand Szakacs, lsz@lorandszakacs.com, lorand.szakacs@busymachines.com
  * @since 26 Jan 2018
  *
  */
trait TaskTypeDefinitions {
  import monix.{execution => mex}
  import monix.{eval      => mev}

  type CancellableFuture[T] = mex.CancelableFuture[T]

  /**
    * N.B.
    * that Scheduler is also a [[scala.concurrent.ExecutionContext]],
    * which makes this type the only implicit in context necessary to do
    * interop between [[Task]] and [[scala.concurrent.Future]]
    */
  type Scheduler = mex.Scheduler
  type Task[T]   = mev.Task[T]

  val Scheduler: mex.Scheduler.type = mex.Scheduler
  val Task:      mev.Task.type      = mev.Task

}

object TaskSyntax {

  /**
    *
    */
  trait Implicits {
    implicit def bmcTaskCompanionObjectOps(obj: Task.type): CompanionObjectOps =
      new CompanionObjectOps(obj)

    implicit def bmcTaskReferenceOps[T](value: Task[T]): ReferenceOps[T] =
      new ReferenceOps(value)

    implicit def bmcTaskNestedOptionOps[T](nopt: Task[Option[T]]): NestedOptionOps[T] =
      new NestedOptionOps(nopt)

    implicit def bmcTaskNestedResultOps[T](result: Task[Result[T]]): NestedResultOps[T] =
      new NestedResultOps(result)

    implicit def bmcTaskBooleanOps(test: Boolean): BooleanOps =
      new BooleanOps(test)

    implicit def bmcTaskNestedBooleanOps(test: Task[Boolean]): NestedBooleanOps =
      new NestedBooleanOps(test)
  }

  /**
    *
    */
  final class CompanionObjectOps(val obj: Task.type) extends AnyVal {

    // —— def pure[T](value: T): Task[T] —— already defined on companion object

    /**
      * Failed effect but with an [[Anomaly]]
      */
    @scala.inline
    def fail[T](bad: Anomaly): Task[T] =
      TaskOps.fail(bad)

    /**
      * Failed effect but with a [[Throwable]]
      */
    @scala.inline
    def failThr[T](bad: Throwable): Task[T] =
      TaskOps.failThr(bad)

    // —— def unit: Task[Unit] —— already defined on Task object

    /**
      * Lift this [[Option]] and transform it into a failed effect if it is [[None]]
      */
    def fromOption[T](opt: Option[T], ifNone: => Anomaly): Task[T] =
      TaskOps.fromOption(opt, ifNone)

    /**
      *
      * Suspend any side-effects that might happen during the creation of this [[Option]].
      * If the option is [[None]] then we get back a failed effect with the given [[Anomaly]]
      *
      * N.B. this is useless if the [[Option]] was previously assigned to a "val".
      * You might as well use [[TaskOps.fromOption]]
      */
    def suspendOption[T](opt: => Option[T], ifNone: => Anomaly): Task[T] =
      TaskOps.suspendOption(opt, ifNone)

    /**
      * Lift this [[Option]] and transform it into a failed effect if it is [[None]]
      */
    def fromOptionThr[T](opt: Option[T], ifNone: => Throwable): Task[T] =
      TaskOps.fromOptionThr(opt, ifNone)

    /**
      *
      * Suspend any side-effects that might happen during the creation of this [[Option]].
      * If the option is [[None]] then we get back a failed effect with the given [[Throwable]]
      *
      * N.B. this is useless if the [[Option]] was previously assigned to a "val".
      * You might as well use [[TaskOps.fromOption]]
      */
    def suspendOptionThr[T](opt: => Option[T], ifNone: => Throwable): Task[T] =
      TaskOps.suspendOptionThr(opt, ifNone)

    // def fromTry[T](tr: Try[T]): Task[T] —— already defined on Task object

    /**
      *
      * Suspend any side-effects that might happen during the creation of this [[Try]].
      * Failed Try yields a failed effect
      * Successful Try yields a pure effect
      *
      * N.B. this is useless if the [[Try]] was previously assigned to a "val".
      * You might as well use [[Task.fromTry]]
      */
    def suspendTry[T](tr: => Try[T]): Task[T] =
      TaskOps.suspendTry(tr)

    /**
      * Lift this [[Either]] and transform its left-hand side into a [[Anomaly]] and sequence it within
      * this effect, yielding a failed effect.
      */
    def fromEither[L, R](either: Either[L, R], transformLeft: L => Anomaly): Task[R] =
      TaskOps.fromEither(either, transformLeft)

    /**
      *
      * Suspend any side-effects that might happen during the creation of this [[Either]].
      * And transform its left-hand side into a [[Anomaly]] and sequence it within
      * this effect, yielding a failed effect.
      *
      * N.B. this is useless if the [[Either]] was previously assigned to a "val".
      * You might as well use [[TaskOps.fromEither]]
      */
    def suspendEither[L, R](either: => Either[L, R], transformLeft: L => Anomaly): Task[R] =
      TaskOps.suspendEither(either, transformLeft)

    /**
      * Lift this [[Either]] and  sequence its left-hand-side [[Throwable]] within this effect
      * if it is a [[Throwable]].
      */
    def fromEitherThr[L, R](either: Either[L, R])(implicit ev: L <:< Throwable): Task[R] =
      TaskOps.fromEitherThr(either)(ev)

    /**
      *
      * Suspend any side-effects that might happen during the creation of this [[Either]].
      * And sequence its left-hand-side [[Throwable]] within this effect if it is a [[Throwable]]
      *
      * N.B. this is useless if the [[Either]] was previously assigned to a "val".
      * You might as well use [[TaskOps.fromEither]]
      */
    def suspendEitherThr[L, R](either: => Either[L, R])(implicit ev: L <:< Throwable): Task[R] =
      TaskOps.suspendEitherThr(either)(ev)

    /**
      * Lift this [[Either]] and transform its left-hand side into a [[Throwable]] and sequence it within
      * this effect, yielding a failed effect.
      */
    def fromEitherThr[L, R](either: Either[L, R], transformLeft: L => Throwable): Task[R] =
      TaskOps.fromEitherThr(either, transformLeft)

    /**
      * Suspend any side-effects that might happen during the creation of this [[Either]].
      * And transform its left-hand side into a [[Throwable]] and sequence it within
      * this effect, yielding a failed effect.
      *
      * N.B. this is useless if the [[Either]] was previously assigned to a "val".
      * You might as well use [[TaskOps.fromEither]]
      */
    def suspendEitherThr[L, R](either: => Either[L, R], transformLeft: L => Throwable): Task[R] =
      TaskOps.suspendEitherThr(either, transformLeft)

    /**
      *
      * Lift the [[Result]] in this effect
      * [[Incorrect]] becomes a failed effect
      * [[Correct]] becomes a pure effect
      *
      */
    def fromResult[T](result: Result[T]): Task[T] =
      TaskOps.fromResult(result)

    /**
      * Suspend any side-effects that might happen during the creation of this [[Result]].
      * Other than that it has the semantics of [[TaskOps.fromResult]]
      *
      * N.B. this is useless if the [[Result]] was previously assigned to a "val".
      * You might as well use [[TaskOps.fromResult]]
      */
    def suspendResult[T](result: => Result[T]): Task[T] =
      TaskOps.suspendResult(result)

    /**
      * !!! USE WITH CARE !!!
      *
      * In 99% of the cases you actually want to use [[TaskOps.suspendFuture]]
      *
      * If you are certain that this [[Future]] is pure, then you can use
      * this method to lift it into [[Task]].
      */
    def fromFuturePure[T](value: Future[T]): Task[T] =
      Task.fromFuture(value)

    /**
      *
      * Suspend the side-effects of this [[Future]] into a [[Task]]. This is the
      * most important operation when it comes to inter-op between the two effects.
      *
      * Usage. N.B. that this only makes sense if the creation of the Future itself
      * is also suspended in the [[Task]].
      * {{{
      *   def writeToDB(v: Int, s: String): Future[Long] = ???
      *   //...
      *   val task = Task.suspendFuture(writeToDB(42, "string"))
      *   //no database writes happened yet, since the future did
      *   //not do its annoying running of side-effects immediately!
      *
      *   //when we want side-effects:
      *   task.unsafeGetSync()
      * }}}
      *
      * This is almost useless unless you are certain that ??? is a pure computation
      * might as well use Task.fromFuturePure(???)
      * {{{
      *   val f: Future[Int] = Future.apply(???)
      *   Task.suspendFuture(f)
      * }}}
      *
      */
    def suspendFuture[T](result: => Future[T]): Task[T] =
      TaskOps.suspendFuture(result)

    /**
      * @return
      *   pure effect from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Anomaly]] if boolean is false
      */
    def cond[T](test: Boolean, good: => T, bad: => Anomaly): Task[T] =
      TaskOps.cond(test, good, bad)

    /**
      * @return
      *   pure effect from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Throwable]] if boolean is false
      */
    def condThr[T](test: Boolean, good: => T, bad: => Throwable): Task[T] =
      TaskOps.condThr(test, good, bad)

    /**
      * @return
      *   effect from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Anomaly]] if boolean is false
      */
    def condWith[T](test: Boolean, good: => Task[T], bad: => Anomaly): Task[T] =
      TaskOps.condWith(test, good, bad)

    /**
      * @return
      *   effect from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Throwable]] if boolean is false
      */
    def condWithThr[T](test: Boolean, good: => Task[T], bad: => Throwable): Task[T] =
      TaskOps.condWithThr(test, good, bad)

    /**
      * @return
      *   pure effect from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Anomaly]] if boolean is false
      *   failed effect if the effect wrapping the boolean is already failed
      */
    def flatCond[T](test: Task[Boolean], good: => T, bad: => Anomaly): Task[T] =
      TaskOps.flatCond(test, good, bad)

    /**
      * @return
      *   pure effect from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Throwable]] if boolean is false
      *   failed effect if the effect wrapping the boolean is already failed
      */
    def flatCondThr[T](test: Task[Boolean], good: => T, bad: => Throwable): Task[T] =
      TaskOps.flatCondThr(test, good, bad)

    /**
      * @return
      *   effect resulted from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Anomaly]] if boolean is false
      *   failed effect if the effect wrapping the boolean is already failed
      */
    def flatCondWith[T](test: Task[Boolean], good: => Task[T], bad: => Anomaly): Task[T] =
      TaskOps.flatCondWith(test, good, bad)

    /**
      * @return
      *   effect resulted from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Throwable]] if boolean is false
      *   failed effect if the effect wrapping the boolean is already failed
      */
    def flatCondWithThr[T](test: Task[Boolean], good: => Task[T], bad: => Throwable): Task[T] =
      TaskOps.flatCondWithThr(test, good, bad)

    /**
      * @return
      *   Failed effect, if the boolean is true
      */
    def failOnTrue(test: Boolean, bad: => Anomaly): Task[Unit] =
      TaskOps.failOnTrue(test, bad)

    /**
      * @return
      *   Failed effect, if the boolean is true
      */
    def failOnTrueThr(test: Boolean, bad: => Throwable): Task[Unit] =
      TaskOps.failOnTrueThr(test, bad)

    /**
      * @return
      *   Failed effect, if the boolean is false
      */
    def failOnFalse(test: Boolean, bad: => Anomaly): Task[Unit] =
      TaskOps.failOnFalse(test, bad)

    /**
      * @return
      *   Failed effect, if the boolean is false
      */
    def failOnFalseThr(test: Boolean, bad: => Throwable): Task[Unit] =
      TaskOps.failOnFalseThr(test, bad)

    /**
      * @return
      *   Failed effect, if the boxed boolean is true, or if the original effect is failed
      */
    def flatFailOnTrue(test: Task[Boolean], bad: => Anomaly): Task[Unit] =
      TaskOps.flatFailOnTrue(test, bad)

    /**
      * @return
      *   Failed effect, if the boxed boolean is true, or if the original effect is failed
      */
    def flatFailOnTrueThr(test: Task[Boolean], bad: => Throwable): Task[Unit] =
      TaskOps.flatFailOnTrueThr(test, bad)

    /**
      * @return
      *   Failed effect, if the boxed boolean is false, or if the original effect is failed
      */
    def flatFailOnFalse(test: Task[Boolean], bad: => Anomaly): Task[Unit] =
      TaskOps.flatFailOnFalse(test, bad)

    /**
      * @return
      *   Failed effect, if the boxed boolean is false, or if the original effect is failed
      */
    def flatFailOnFalseThr(test: Task[Boolean], bad: => Throwable): Task[Unit] =
      TaskOps.flatFailOnFalseThr(test, bad)

    /**
      * Sequences the given [[Anomaly]] if Option is [[None]] into this effect
      *
      * The failure of this effect takes precedence over the given failure
      */
    def unpackOption[T](nopt: Task[Option[T]], ifNone: => Anomaly): Task[T] =
      TaskOps.unpackOption(nopt, ifNone)

    /**
      * Sequences the given [[Throwable]] if Option is [[None]] into this effect
      *
      * The failure of this effect takes precedence over the given failure
      */
    def unpackOptionThr[T](nopt: Task[Option[T]], ifNone: => Throwable): Task[T] =
      TaskOps.unpackOptionThr(nopt, ifNone)

    /**
      * Sequences the failure of the [[Incorrect]] [[Result]] into this effect.
      *
      * The failure of this effect takes precedence over the failure of the [[Incorrect]] value.
      */
    def unpackResult[T](value: Task[Result[T]]): Task[T] =
      TaskOps.unpackResult(value)

    /**
      * Makes the failure, and non-failure part of this effect explicit in a [[Result]] type.
      *
      * This transforms any failed effect, into a pure one with and [[Incorrect]] value.
      */
    def attemptResult[T](value: Task[T]): Task[Result[T]] =
      TaskOps.attemptResult(value)

    /**
      * !!! USE WITH CARE !!!
      *
      * The moment you call this, the side-effects suspended in this [[IO]] start being
      * executed.
      */
    def asFutureUnsafe[T](value: Task[T])(implicit sc: Scheduler): Future[T] =
      TaskOps.asFutureUnsafe(value)

    /**
      * No gotchas. Pure functional programming = <3
      */
    def asIO[T](value: Task[T])(implicit sc: Scheduler): IO[T] =
      TaskOps.asIO(value)

    /**
      * !!! USE WITH CARE !!!
      *
      * Mostly here for testing. There is almost no reason whatsover for you to explicitely
      * call this in your code. You have libraries that do this for you "at the end of the world"
      * parts of your program: e.g. akka-http when waiting for the response value to a request.
      */
    def unsafeSyncGet[T](value: Task[T], atMost: FiniteDuration = TaskOps.defaultDuration)(implicit sc: Scheduler): T =
      TaskOps.unsafeSyncGet(value, atMost)

    //=========================================================================
    //================= Run side-effects in varrying scenarios ================
    //=========================================================================

    /**
      *
      * @param value
      *   Runs the given effect when the value of this [[Boolean]] is ``true``
      *   Does not run the side-effect if the value is also a failed effect.
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def effectOnTrue[_](test: Boolean, effect: => Task[_]): Task[Unit] =
      TaskOps.effectOnTrue(test, effect)

    /**
      *
      * @param value
      *   Runs the given effect when the value of this [[Boolean]] is ``true``
      *   Does not run the side-effect if the value is also a failed effect.
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def flatEffectOnTrue[_](test: Task[Boolean], effect: => Task[_]): Task[Unit] =
      TaskOps.flatEffectOnTrue(test, effect)

    /**
      *
      * @param value
      *   Runs the given effect when the value of this [[Boolean]] is ``false``
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def effectOnFalse[_](test: Boolean, effect: => Task[_]): Task[Unit] =
      TaskOps.effectOnFalse(test, effect)

    /**
      *
      * @param value
      *   Runs the given effect when the value of this [[Boolean]] is ``false``
      *   Does not run the side-effect if the value is also a failed effect.
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def flatEffectOnFalse[_](test: Task[Boolean], effect: => Task[_]): Task[Unit] =
      TaskOps.flatEffectOnFalse(test, effect)

    /**
      *
      * @param value
      *   Runs the given effect when the value of this [[Option]] is [[None]]
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def effectOnFail[T, _](value: Option[T], effect: => Task[_]): Task[Unit] =
      TaskOps.effectOnFail(value, effect)

    /**
      *
      * @param value
      *   Runs the given effect when the value of this [[Option]] is [[None]]
      *   Does not run the side-effect if the value is also a failed effect.
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def flatEffectOnNone[T, _](value: Task[Option[T]], effect: => Task[_]): Task[Unit] =
      TaskOps.flatEffectOnNone(value, effect)

    /**
      *
      * @param value
      *   Runs the given effect when the value of this [[Option]] is [[Some]]
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def effectOnPure[T, _](value: Option[T], effect: T => Task[_]): Task[Unit] =
      TaskOps.effectOnPure(value, effect)

    /**
      *
      * @param value
      *   Runs the given effect when the value of this [[Option]] is [[Some]]
      *   Does not run the side-effect if the value is also a failed effect.
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def flatEffectOnSome[T, _](value: Task[Option[T]], effect: T => Task[_]): Task[Unit] =
      TaskOps.flatEffectOnSome(value, effect)

    /**
      *
      * @param value
      *   Runs the given effect when the value of this [[Result]] is [[Incorrect]]
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def effectOnFail[T, _](value: Result[T], effect: Anomaly => Task[_]): Task[Unit] =
      TaskOps.effectOnFail(value, effect)

    /**
      *
      * @param value
      *   Runs the given effect when the boxed value of this [[Result]] is [[Incorrect]]
      *   Does not run the side-effect if the value is also a failed effect.
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def flatEffectOnIncorrect[T, _](value: Task[Result[T]], effect: Anomaly => Task[_]): Task[Unit] =
      TaskOps.flatEffectOnIncorrect(value, effect)

    /**
      *
      * @param value
      *   Runs the given effect when the boxed value of this [[Result]] is [[Correct]]
      *   Does not run the side-effect if the value is also a failed effect.
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def flatEffectOnCorrect[T, _](value: Task[Result[T]], effect: T => Task[_]): Task[Unit] =
      TaskOps.flatEffectOnCorrect(value, effect)

    /**
      *
      * @param value
      *   Runs the given effect when the value of this [[Result]] is [[Correct]]
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def effectOnPure[T, _](value: Result[T], effect: T => Task[_]): Task[Unit] =
      TaskOps.effectOnPure(value, effect)

    //=========================================================================
    //============================== Transformers =============================
    //=========================================================================

    /**
      * Used to transform both the "pure" part of the effect, and the "fail" part. Hence the name
      * "bi" map, because it also allows you to change both branches of the effect, not just the
      * happy path.
      */
    def bimap[T, R](value: Task[T], good: T => R, bad: Throwable => Anomaly): Task[R] =
      TaskOps.bimap(value, good, bad)

    /**
      * Similar to the overload, but the [[Correct]] branch of the result is used to change the "pure" branch of this
      * effect, and [[Incorrect]] branch is used to change the "fail" branch of the effect.
      */
    def bimap[T, R](value: Task[T], result: Result[T] => Result[R]): Task[R] =
      TaskOps.bimap(value, result)

    /**
      * Similar to the overload, but the [[Correct]] branch of the result is used to change the "pure" branch of this
      * effect, and [[Incorrect]] branch is used to change the "fail" branch of the effect.
      *
      * The overload that uses [[Throwable]] instead of [[Anomaly]]
      */
    def bimapThr[T, R](value: Task[T], good: T => R, bad: Throwable => Throwable): Task[R] =
      TaskOps.bimapThr(value, good, bad)

    /**
      *
      * Given the basic two-pronged nature of this effect.
      * the ``good`` function transforms the underlying "pure" (correct, successful, etc) if that's the case.
      * the ``bad`` function transforms the underlying "failure" part of the effect into a "pure" part.
      *
      * Therefore, by using ``morph`` you are defining the rules by which to make the effect into a successful one
      * that does not short-circuit monadic flatMap chains.
      *
      * e.g:
      * {{{
      *   val f: Future[Int] = Future.fail(InvalidInputFailure)
      *   Future.morph(f, (i: Int) => i *2, (t: Throwable) => 42)
      * }}}
      *
      * Undefined behavior if you throw exceptions in the method. DO NOT do that!
      */
    def morph[T, R](value: Task[T], good: T => R, bad: Throwable => R): Task[R] =
      TaskOps.morph(value, good, bad)

    /**
      * Semantically equivalent to the overload ``morph`` that accepts two functions, but those encoded
      * as the corresponding branches of a Result type.
      *
      * Undefined behavior if you throw exceptions in the method. DO NOT do that!
      */
    def morph[T, R](value: Task[T], result: Result[T] => R): Task[R] =
      TaskOps.morph(value, result)

    /**
      *
      * Explicitely discard the contents of this effect, and return [[Unit]] instead.
      *
      * N.B. computation, and side-effects captured within this effect are still executed,
      * it's just the final value that is discarded
      *
      */
    def discardContent[_](value: Task[_]): Task[Unit] =
      TaskOps.discardContent(value)

    //=========================================================================
    //=============================== Traversals ==============================
    //=========================================================================

    /**
      *
      * Syntactically inspired from [[Future.traverse]].
      *
      * See [[FutureOps.serialize]] for semantics.
      *
      * Usage:
      * {{{
      *   import busymachines.effects.async._
      *   val patches: Seq[Patch] = //...
      *
      *   //this ensures that no two changes will be applied in parallel.
      *   val allPatches: Task[Seq[Patch]] = Task.serialize(patches){ patch: Patch =>
      *     Task {
      *       //apply patch
      *     }
      *   }
      *   //... and so on, and so on!
      * }}}
      *
      *
      */
    def serialize[A, B, C[X] <: TraversableOnce[X]](col: C[A])(fn: A => Task[B])(
      implicit
      cbf: CanBuildFrom[C[A], B, C[B]]
    ): Task[C[B]] = TaskOps.serialize(col)(fn)
  }

  /**
    *
    */
  final class ReferenceOps[T](val value: Task[T]) extends AnyVal {

    /**
      * Makes the failure, and non-failure part of this effect explicit in a [[Result]] type.
      *
      * This transforms any failed effect, into a pure one with and [[Incorrect]] value.
      */
    def attempResult: Task[Result[T]] =
      TaskOps.attemptResult(value)

    /**
      * !!! USE WITH CARE !!!
      *
      * The moment you call this, the side-effects suspended in this [[IO]] start being
      * executed.
      */
    def asFutureUnsafe()(implicit sc: Scheduler): Future[T] =
      TaskOps.asFutureUnsafe(value)

    /**
      * No gotchas. Pure functional programming = <3
      */
    def asIO(implicit sc: Scheduler): IO[T] =
      TaskOps.asIO(value)

    /**
      * !!! USE WITH CARE !!!
      *
      * Mostly here for testing. There is almost no reason whatsover for you to explicitely
      * call this in your code. You have libraries that do this for you "at the end of the world"
      * parts of your program: e.g. akka-http when waiting for the response value to a request.
      */
    def unsafeSyncGet(atMost: FiniteDuration = TaskOps.defaultDuration)(implicit sc: Scheduler): T =
      TaskOps.unsafeSyncGet(value, atMost)

    /**
      * Used to transform both the "pure" part of the effect, and the "fail" part. Hence the name
      * "bi" map, because it also allows you to change both branches of the effect, not just the
      * happy path.
      */
    def bimap[R](good: T => R, bad: Throwable => Anomaly): Task[R] =
      TaskOps.bimap(value, good, bad)

    /**
      * Similar to the overload, but the [[Correct]] branch of the result is used to change the "pure" branch of this
      * effect, and [[Incorrect]] branch is used to change the "fail" branch of the effect.
      */
    def bimap[R](result: Result[T] => Result[R]): Task[R] =
      TaskOps.bimap(value, result)

    /**
      * Similar to the overload, but the [[Correct]] branch of the result is used to change the "pure" branch of this
      * effect, and [[Incorrect]] branch is used to change the "fail" branch of the effect.
      *
      * The overload that uses [[Throwable]] instead of [[Anomaly]]
      */
    def bimapThr[R](good: T => R, bad: Throwable => Throwable): Task[R] =
      TaskOps.bimapThr(value, good, bad)

    /**
      *
      * Given the basic two-pronged nature of this effect.
      * the ``good`` function transforms the underlying "pure" (correct, successful, etc) if that's the case.
      * the ``bad`` function transforms the underlying "failure" part of the effect into a "pure" part.
      *
      * Therefore, by using ``morph`` you are defining the rules by which to make the effect into a successful one
      * that does not short-circuit monadic flatMap chains.
      *
      * e.g:
      * {{{
      *   val f: Future[Int] = Future.fail(InvalidInputFailure)
      *   Future.morph(f, (i: Int) => i *2, (t: Throwable) => 42)
      * }}}
      *
      * Undefined behavior if you throw exceptions in the method. DO NOT do that!
      */
    def morph[R](good: T => R, bad: Throwable => R): Task[R] =
      TaskOps.morph(value, good, bad)

    /**
      * Semantically equivalent to the overload ``morph`` that accepts two functions, but those encoded
      * as the corresponding branches of a Result type.
      *
      * Undefined behavior if you throw exceptions in the method. DO NOT do that!
      */
    def morph[R](result: Result[T] => R): Task[R] =
      TaskOps.morph(value, result)

    /**
      *
      * Explicitely discard the contents of this effect, and return [[Unit]] instead.
      *
      * N.B. computation, and side-effects captured within this effect are still executed,
      * it's just the final value that is discarded
      *
      */
    def discardContent: Task[Unit] =
      TaskOps.discardContent(value)
  }

  /**
    *
    *
    */
  final class NestedOptionOps[T](val nopt: Task[Option[T]]) extends AnyVal {

    /**
      * Sequences the given [[Anomaly]] if Option is [[None]] into this effect
      *
      * The failure of this effect takes precedence over the given failure
      */
    def unpack(ifNone: => Anomaly): Task[T] =
      TaskOps.unpackOption(nopt, ifNone)

    /**
      * Sequences the given [[Throwable]] if Option is [[None]] into this effect
      *
      * The failure of this effect takes precedence over the given failure
      */
    def unpackThr(ifNone: => Throwable): Task[T] =
      TaskOps.unpackOptionThr(nopt, ifNone)

    /**
      *
      * Runs the given effect when the value of this [[Option]] is [[None]]
      * Does not run the side-effect if the value is also a failed effect.
      *
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def effectOnFail[_](effect: => Task[_]): Task[Unit] =
      TaskOps.flatEffectOnNone(nopt, effect)

    /**
      *
      * Runs the given effect when the value of this [[Option]] is [[Some]]
      * Does not run the side-effect if the value is also a failed effect.
      *
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def effectOnPure[_](effect: T => Task[_]): Task[Unit] =
      TaskOps.flatEffectOnSome(nopt, effect)

  }

  /**
    *
    */
  final class NestedResultOps[T](val result: Task[Result[T]]) extends AnyVal {

    /**
      * Sequences the failure of the [[Incorrect]] [[Result]] into this effect.
      *
      * The failure of this effect takes precedence over the failure of the [[Incorrect]] value.
      */
    def unpack: Task[T] =
      TaskOps.unpackResult(result)

    /**
      *
      * Runs the given effect when the boxed value of this [[Result]] is [[Incorrect]]
      * Does not run the side-effect if the value is also a failed effect.
      *
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def effectOnFail[_](effect: Anomaly => Task[_]): Task[Unit] =
      TaskOps.flatEffectOnIncorrect(result, effect)

    /**
      *
      * Runs the given effect when the boxed value of this [[Result]] is [[Correct]]
      * Does not run the side-effect if the value is also a failed effect.
      *
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def effectOnPure[_](effect: T => Task[_]): Task[Unit] =
      TaskOps.flatEffectOnCorrect(result, effect)
  }

  /**
    *
    *
    */
  final class BooleanOps(val test: Boolean) extends AnyVal {

    /**
      * @return
      *   pure effect from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Anomaly]] if boolean is false
      */
    def condTask[T](good: => T, bad: => Anomaly): Task[T] =
      TaskOps.cond(test, good, bad)

    /**
      * @return
      *   pure effect from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Throwable]] if boolean is false
      */
    def condTaskThr[T](good: => T, bad: => Throwable): Task[T] =
      TaskOps.condThr(test, good, bad)

    /**
      * @return
      *   effect from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Anomaly]] if boolean is false
      */
    def condWithTask[T](good: => Task[T], bad: => Anomaly): Task[T] =
      TaskOps.condWith(test, good, bad)

    /**
      * @return
      *   effect from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Throwable]] if boolean is false
      */
    def condWithTaskThr[T](good: => Task[T], bad: => Throwable): Task[T] =
      TaskOps.condWithThr(test, good, bad)

    /**
      * @return
      *   Failed effect, if the boolean is true
      */
    def failOnTrueTask(bad: => Anomaly): Task[Unit] =
      TaskOps.failOnTrue(test, bad)

    /**
      * @return
      *   Failed effect, if the boolean is true
      */
    def failOnTrueTaskThr(bad: => Throwable): Task[Unit] =
      TaskOps.failOnTrueThr(test, bad)

    /**
      * @return
      *   Failed effect, if the boolean is false
      */
    def failOnFalseTask(bad: => Anomaly): Task[Unit] =
      TaskOps.failOnFalse(test, bad)

    /**
      * @return
      *   Failed effect, if the boolean is false
      */
    def failOnFalseTaskThr(bad: => Throwable): Task[Unit] =
      TaskOps.failOnFalseThr(test, bad)

    /**
      *
      * Runs the given effect when the value of this [[Boolean]] is ``false``
      *
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def effectOnFalseTask[_](effect: => Task[_]): Task[_] =
      TaskOps.effectOnFalse(test, effect)

    /**
      *
      * Runs the given effect when the value of this [[Boolean]] is ``true``
      * Does not run the side-effect if the value is also a failed effect.
      *
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def effectOnTrueTask[_](effect: => Task[_]): Task[Unit] =
      TaskOps.effectOnTrue(test, effect)

  }

  /**
    *
    *
    */
  final class NestedBooleanOps(val test: Task[Boolean]) extends AnyVal {

    /**
      * @return
      *   pure effect from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Anomaly]] if boolean is false
      *   failed effect if the effect wrapping the boolean is already failed
      */
    def cond[T](good: => T, bad: => Anomaly): Task[T] =
      TaskOps.flatCond(test, good, bad)

    /**
      * @return
      *   pure effect from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Throwable]] if boolean is false
      *   failed effect if the effect wrapping the boolean is already failed
      */
    def condThr[T](good: => T, bad: => Throwable): Task[T] =
      TaskOps.flatCondThr(test, good, bad)

    /**
      * @return
      *   effect resulted from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Anomaly]] if boolean is false
      *   failed effect if the effect wrapping the boolean is already failed
      */
    def condWith[T](good: => Task[T], bad: => Anomaly): Task[T] =
      TaskOps.flatCondWith(test, good, bad)

    /**
      * @return
      *   effect resulted from ``good`` if the boolean is true
      *   failed effect with ``bad`` [[Throwable]] if boolean is false
      *   failed effect if the effect wrapping the boolean is already failed
      */
    def condWithThr[T](good: => Task[T], bad: => Throwable): Task[T] =
      TaskOps.flatCondWithThr(test, good, bad)

    /**
      * @return
      *   Failed effect, if the boxed boolean is true, or if the original effect is failed
      */
    def failOnTrue(bad: => Anomaly): Task[Unit] =
      TaskOps.flatFailOnTrue(test, bad)

    /**
      * @return
      *   Failed effect, if the boxed boolean is true, or if the original effect is failed
      */
    def failOnTrueThr(bad: => Throwable): Task[Unit] =
      TaskOps.flatFailOnTrueThr(test, bad)

    /**
      * @return
      *   Failed effect, if the boxed boolean is false, or if the original effect is failed
      */
    def failOnFalse(bad: => Anomaly): Task[Unit] =
      TaskOps.flatFailOnFalse(test, bad)

    /**
      * @return
      *   Failed effect, if the boxed boolean is false, or if the original effect is failed
      */
    def failOnFalseThr(bad: => Throwable): Task[Unit] =
      TaskOps.flatFailOnFalseThr(test, bad)

    /**
      *
      * Runs the given effect when the value of this [[Boolean]] is ``false``
      * Does not run the side-effect if the value is also a failed effect.
      *
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def effectOnFalse[_](effect: => Task[_]): Task[_] =
      TaskOps.flatEffectOnFalse(test, effect)

    /**
      *
      * Runs the given effect when the value of this [[Boolean]] is ``true``
      * Does not run the side-effect if the value is also a failed effect.
      *
      * @param effect
      *   The effect to run
      * @return
      *   Does not return anything, this method is inherently imperative, and relies on
      *   side-effects to achieve something.
      */
    def effectOnTrue[_](effect: => Task[_]): Task[_] =
      TaskOps.flatEffectOnTrue(test, effect)

  }
}

/**
  *
  */
object TaskOps {
  import cats.syntax.applicativeError._
  import cats.syntax.monadError._

  /**
    * N.B. pass only pure values. If you have side effects, then
    * use [[Task.apply]] to suspend them inside this future.
    */
  @scala.inline
  def pure[T](value: T): Task[T] =
    Task.pure(value)

  /**
    * Failed effect but with an [[Anomaly]]
    */
  @scala.inline
  def fail[T](bad: Anomaly): Task[T] =
    Task.raiseError(bad.asThrowable)

  /**
    * Failed effect but with a [[Throwable]]
    */
  @scala.inline
  def failThr[T](bad: Throwable): Task[T] =
    Task.raiseError(bad)

  // —— def unit: Task[Unit] —— already defined on Task object

  /**
    * Lift this [[Option]] and transform it into a failed effect if it is [[None]]
    */
  def fromOption[T](opt: Option[T], ifNone: => Anomaly): Task[T] = opt match {
    case None        => TaskOps.fail(ifNone)
    case Some(value) => TaskOps.pure(value)
  }

  /**
    *
    * Suspend any side-effects that might happen during the creation of this [[Option]].
    * If the option is [[None]] then we get back a failed effect with the given [[Anomaly]]
    *
    * N.B. this is useless if the [[Option]] was previously assigned to a "val".
    * You might as well use [[TaskOps.fromOption]]
    */
  def suspendOption[T](opt: => Option[T], ifNone: => Anomaly): Task[T] =
    Task.suspend(TaskOps.fromOption(opt, ifNone))

  /**
    * Lift this [[Option]] and transform it into a failed effect if it is [[None]]
    */
  def fromOptionThr[T](opt: Option[T], ifNone: => Throwable): Task[T] = opt match {
    case None        => TaskOps.failThr(ifNone)
    case Some(value) => TaskOps.pure(value)
  }

  /**
    *
    * Suspend any side-effects that might happen during the creation of this [[Option]].
    * If the option is [[None]] then we get back a failed effect with the given [[Throwable]]
    *
    * N.B. this is useless if the [[Option]] was previously assigned to a "val".
    * You might as well use [[TaskOps.fromOption]]
    */
  def suspendOptionThr[T](opt: => Option[T], ifNone: => Throwable): Task[T] =
    Task.suspend(TaskOps.fromOptionThr(opt, ifNone))

  // def fromTry[T](tr: Try[T]): Task[T] —— already defined on Task object

  /**
    *
    * Suspend any side-effects that might happen during the creation of this [[Try]].
    * Failed Try yields a failed effect
    * Successful Try yields a pure effect
    *
    * N.B. this is useless if the [[Try]] was previously assigned to a "val".
    * You might as well use [[Task.fromTry]]
    */
  def suspendTry[T](tr: => Try[T]): Task[T] =
    Task.suspend(Task.fromTry(tr))

  /**
    * Lift this [[Either]] and transform its left-hand side into a [[Anomaly]] and sequence it within
    * this effect, yielding a failed effect.
    */
  def fromEither[L, R](either: Either[L, R], transformLeft: L => Anomaly): Task[R] = either match {
    case Left(value)  => TaskOps.fail(transformLeft(value))
    case Right(value) => TaskOps.pure(value)
  }

  /**
    *
    * Suspend any side-effects that might happen during the creation of this [[Either]].
    * And transform its left-hand side into a [[Anomaly]] and sequence it within
    * this effect, yielding a failed effect.
    *
    * N.B. this is useless if the [[Either]] was previously assigned to a "val".
    * You might as well use [[TaskOps.fromEither]]
    */
  def suspendEither[L, R](either: => Either[L, R], transformLeft: L => Anomaly): Task[R] =
    Task.suspend(TaskOps.fromEither(either, transformLeft))

  /**
    * Lift this [[Either]] and  sequence its left-hand-side [[Throwable]] within this effect
    * if it is a [[Throwable]].
    */
  def fromEitherThr[L, R](either: Either[L, R])(implicit ev: L <:< Throwable): Task[R] = either match {
    case Left(value)  => TaskOps.failThr(ev(value))
    case Right(value) => TaskOps.pure(value)
  }

  /**
    *
    * Suspend any side-effects that might happen during the creation of this [[Either]].
    * And sequence its left-hand-side [[Throwable]] within this effect if it is a [[Throwable]]
    *
    * N.B. this is useless if the [[Either]] was previously assigned to a "val".
    * You might as well use [[TaskOps.fromEither]]
    */
  def suspendEitherThr[L, R](either: => Either[L, R])(implicit ev: L <:< Throwable): Task[R] =
    Task.suspend(TaskOps.fromEitherThr(either)(ev))

  /**
    * Lift this [[Either]] and transform its left-hand side into a [[Throwable]] and sequence it within
    * this effect, yielding a failed effect.
    */
  def fromEitherThr[L, R](either: Either[L, R], transformLeft: L => Throwable): Task[R] = either match {
    case Left(value)  => TaskOps.failThr(transformLeft(value))
    case Right(value) => TaskOps.pure(value)
  }

  /**
    * Suspend any side-effects that might happen during the creation of this [[Either]].
    * And transform its left-hand side into a [[Throwable]] and sequence it within
    * this effect, yielding a failed effect.
    *
    * N.B. this is useless if the [[Either]] was previously assigned to a "val".
    * You might as well use [[TaskOps.fromEither]]
    */
  def suspendEitherThr[L, R](either: => Either[L, R], transformLeft: L => Throwable): Task[R] =
    Task.suspend(TaskOps.fromEitherThr(either, transformLeft))

  /**
    *
    * Lift the [[Result]] in this effect
    * [[Incorrect]] becomes a failed effect
    * [[Correct]] becomes a pure effect
    *
    */
  def fromResult[T](result: Result[T]): Task[T] = result match {
    case Left(value)  => TaskOps.fail(value)
    case Right(value) => TaskOps.pure(value)
  }

  /**
    * Suspend any side-effects that might happen during the creation of this [[Result]].
    * Other than that it has the semantics of [[TaskOps.fromResult]]
    *
    * N.B. this is useless if the [[Result]] was previously assigned to a "val".
    * You might as well use [[TaskOps.fromResult]]
    */
  def suspendResult[T](result: => Result[T]): Task[T] =
    Task.suspend(TaskOps.fromResult(result))

  /**
    * !!! USE WITH CARE !!!
    *
    * In 99% of the cases you actually want to use [[suspendFuture]]
    *
    * If you are certain that this [[Future]] is pure, then you can use
    * this method to lift it into [[Task]].
    */
  def fromFuturePure[T](value: Future[T]): Task[T] =
    Task.fromFuture(value)

  /**
    *
    * Suspend the side-effects of this [[Future]] into a [[Task]]. This is the
    * most important operation when it comes to inter-op between the two effects.
    *
    * Usage. N.B. that this only makes sense if the creation of the Future itself
    * is also suspended in the [[Task]].
    * {{{
    *   def writeToDB(v: Int, s: String): Future[Long] = ???
    *   //...
    *   val task = Task.suspendFuture(writeToDB(42, "string"))
    *   //no database writes happened yet, since the future did
    *   //not do its annoying running of side-effects immediately!
    *
    *   //when we want side-effects:
    *   task.unsafeGetSync()
    * }}}
    *
    * This is almost useless unless you are certain that ??? is a pure computation
    * might as well use Task.fromFuturePure(???)
    * {{{
    *   val f: Future[Int] = Future.apply(???)
    *   Task.suspendFuture(f)
    * }}}
    *
    */
  def suspendFuture[T](value: => Future[T]): Task[T] =
    Task.deferFuture(value)

  /**
    *
    * Transform an [[IO]] into a [[Task]]. No gotchas because pure
    * functional programming is awesome.
    */
  def fromIO[T](value: IO[T]): Task[T] =
    Task.fromIO(value)

  /**
    * @return
    *   pure effect from ``good`` if the boolean is true
    *   failed effect with ``bad`` [[Anomaly]] if boolean is false
    */
  def cond[T](test: Boolean, good: => T, bad: => Anomaly): Task[T] =
    if (test) TaskOps.pure(good) else TaskOps.fail(bad)

  /**
    * @return
    *   pure effect from ``good`` if the boolean is true
    *   failed effect with ``bad`` [[Throwable]] if boolean is false
    */
  def condThr[T](test: Boolean, good: => T, bad: => Throwable): Task[T] =
    if (test) TaskOps.pure(good) else TaskOps.failThr(bad)

  /**
    * @return
    *   effect from ``good`` if the boolean is true
    *   failed effect with ``bad`` [[Anomaly]] if boolean is false
    */
  def condWith[T](test: Boolean, good: => Task[T], bad: => Anomaly): Task[T] =
    if (test) good else TaskOps.fail(bad)

  /**
    * @return
    *   effect from ``good`` if the boolean is true
    *   failed effect with ``bad`` [[Throwable]] if boolean is false
    */
  def condWithThr[T](test: Boolean, good: => Task[T], bad: => Throwable): Task[T] =
    if (test) good else TaskOps.failThr(bad)

  /**
    * @return
    *   pure effect from ``good`` if the boolean is true
    *   failed effect with ``bad`` [[Anomaly]] if boolean is false
    *   failed effect if the effect wrapping the boolean is already failed
    */
  def flatCond[T](test: Task[Boolean], good: => T, bad: => Anomaly): Task[T] =
    test.flatMap(t => TaskOps.cond(t, good, bad))

  /**
    * @return
    *   pure effect from ``good`` if the boolean is true
    *   failed effect with ``bad`` [[Throwable]] if boolean is false
    *   failed effect if the effect wrapping the boolean is already failed
    */
  def flatCondThr[T](test: Task[Boolean], good: => T, bad: => Throwable): Task[T] =
    test.flatMap(t => TaskOps.condThr(t, good, bad))

  /**
    * @return
    *   effect resulted from ``good`` if the boolean is true
    *   failed effect with ``bad`` [[Anomaly]] if boolean is false
    *   failed effect if the effect wrapping the boolean is already failed
    */
  def flatCondWith[T](test: Task[Boolean], good: => Task[T], bad: => Anomaly): Task[T] =
    test.flatMap(t => TaskOps.condWith(t, good, bad))

  /**
    * @return
    *   effect resulted from ``good`` if the boolean is true
    *   failed effect with ``bad`` [[Throwable]] if boolean is false
    *   failed effect if the effect wrapping the boolean is already failed
    */
  def flatCondWithThr[T](test: Task[Boolean], good: => Task[T], bad: => Throwable): Task[T] =
    test.flatMap(t => TaskOps.condWithThr(t, good, bad))

  /**
    * @return
    *   Failed effect, if the boolean is true
    */
  def failOnTrue(test: Boolean, bad: => Anomaly): Task[Unit] =
    if (test) TaskOps.fail(bad) else Task.unit

  /**
    * @return
    *   Failed effect, if the boolean is true
    */
  def failOnTrueThr(test: Boolean, bad: => Throwable): Task[Unit] =
    if (test) TaskOps.failThr(bad) else Task.unit

  /**
    * @return
    *   Failed effect, if the boolean is false
    */
  def failOnFalse(test: Boolean, bad: => Anomaly): Task[Unit] =
    if (!test) TaskOps.fail(bad) else Task.unit

  /**
    * @return
    *   Failed effect, if the boolean is false
    */
  def failOnFalseThr(test: Boolean, bad: => Throwable): Task[Unit] =
    if (!test) TaskOps.failThr(bad) else Task.unit

  /**
    * @return
    *   Failed effect, if the boxed boolean is true, or if the original effect is failed
    */
  def flatFailOnTrue(test: Task[Boolean], bad: => Anomaly): Task[Unit] =
    test.flatMap(t => TaskOps.failOnTrue(t, bad))

  /**
    * @return
    *   Failed effect, if the boxed boolean is true, or if the original effect is failed
    */
  def flatFailOnTrueThr(test: Task[Boolean], bad: => Throwable): Task[Unit] =
    test.flatMap(t => TaskOps.failOnTrueThr(t, bad))

  /**
    * @return
    *   Failed effect, if the boxed boolean is false, or if the original effect is failed
    */
  def flatFailOnFalse(test: Task[Boolean], bad: => Anomaly): Task[Unit] =
    test.flatMap(t => TaskOps.failOnFalse(t, bad))

  /**
    * @return
    *   Failed effect, if the boxed boolean is false, or if the original effect is failed
    */
  def flatFailOnFalseThr(test: Task[Boolean], bad: => Throwable): Task[Unit] =
    test.flatMap(t => TaskOps.failOnFalseThr(t, bad))

  /**
    * Sequences the given [[Anomaly]] if Option is [[None]] into this effect
    *
    * The failure of this effect takes precedence over the given failure
    */
  def unpackOption[T](nopt: Task[Option[T]], ifNone: => Anomaly): Task[T] =
    nopt.flatMap {
      case None    => TaskOps.fail(ifNone)
      case Some(v) => TaskOps.pure(v)
    }

  /**
    * Sequences the given [[Throwable]] if Option is [[None]] into this effect
    *
    * The failure of this effect takes precedence over the given failure
    */
  def unpackOptionThr[T](nopt: Task[Option[T]], ifNone: => Throwable): Task[T] =
    nopt.flatMap {
      case None    => TaskOps.failThr(ifNone)
      case Some(v) => TaskOps.pure(v)
    }

  /**
    * Sequences the failure of the [[Incorrect]] [[Result]] into this effect.
    *
    * The failure of this effect takes precedence over the failure of the [[Incorrect]] value.
    */
  def unpackResult[T](value: Task[Result[T]]): Task[T] = value.flatMap {
    case Left(a)  => TaskOps.fail(a)
    case Right(a) => TaskOps.pure(a)
  }

  /**
    * Makes the failure, and non-failure part of this effect explicit in a [[Result]] type.
    *
    * This transforms any failed effect, into a pure one with and [[Incorrect]] value.
    */
  def attemptResult[T](value: Task[T]): Task[Result[T]] =
    value.attempt.map((e: Either[Throwable, T]) => Result.fromEitherThr(e))

  /**
    * !!! USE WITH CARE !!!
    *
    * The moment you call this, the side-effects suspended in this [[IO]] start being
    * executed.
    */
  def asFutureUnsafe[T](value: Task[T])(implicit sc: Scheduler): CancellableFuture[T] =
    value.runAsync

  /**
    * No gotchas. Pure functional programming = <3
    */
  def asIO[T](value: Task[T])(implicit sc: Scheduler): IO[T] =
    value.toIO

  /**
    * !!! USE WITH CARE !!!
    *
    * Mostly here for testing. There is almost no reason whatsover for you to explicitely
    * call this in your code. You have libraries that do this for you "at the end of the world"
    * parts of your program: e.g. akka-http when waiting for the response value to a request.
    */
  def unsafeSyncGet[T](
    value:  Task[T],
    atMost: FiniteDuration = defaultDuration
  )(
    implicit sc: Scheduler
  ): T = value.runAsync.unsafeSyncGet(atMost)

  //=========================================================================
  //================= Run side-effects in varrying scenarios ================
  //=========================================================================

  /**
    *
    * @param value
    *   Runs the given effect when the value of this [[Boolean]] is ``true``
    *   Does not run the side-effect if the value is also a failed effect.
    * @param effect
    *   The effect to run
    * @return
    *   Does not return anything, this method is inherently imperative, and relies on
    *   side-effects to achieve something.
    */
  def effectOnTrue[_](test: Boolean, effect: => Task[_]): Task[Unit] =
    if (test) TaskOps.discardContent(effect) else Task.unit

  /**
    *
    * @param value
    *   Runs the given effect when the value of this [[Boolean]] is ``true``
    *   Does not run the side-effect if the value is also a failed effect.
    * @param effect
    *   The effect to run
    * @return
    *   Does not return anything, this method is inherently imperative, and relies on
    *   side-effects to achieve something.
    */
  def flatEffectOnTrue[_](test: Task[Boolean], effect: => Task[_]): Task[Unit] =
    test.flatMap(t => TaskOps.effectOnTrue(t, effect))

  /**
    *
    * @param value
    *   Runs the given effect when the value of this [[Boolean]] is ``false``
    * @param effect
    *   The effect to run
    * @return
    *   Does not return anything, this method is inherently imperative, and relies on
    *   side-effects to achieve something.
    */
  def effectOnFalse[_](test: Boolean, effect: => Task[_]): Task[Unit] =
    if (!test) TaskOps.discardContent(effect) else Task.unit

  /**
    *
    * @param value
    *   Runs the given effect when the value of this [[Boolean]] is ``false``
    *   Does not run the side-effect if the value is also a failed effect.
    * @param effect
    *   The effect to run
    * @return
    *   Does not return anything, this method is inherently imperative, and relies on
    *   side-effects to achieve something.
    */
  def flatEffectOnFalse[_](test: Task[Boolean], effect: => Task[_]): Task[Unit] =
    test.flatMap(t => TaskOps.effectOnFalse(t, effect))

  /**
    *
    * @param value
    *   Runs the given effect when the value of this [[Option]] is [[None]]
    * @param effect
    *   The effect to run
    * @return
    *   Does not return anything, this method is inherently imperative, and relies on
    *   side-effects to achieve something.
    */
  def effectOnFail[T, _](value: Option[T], effect: => Task[_]): Task[Unit] =
    if (value.isEmpty) TaskOps.discardContent(effect) else Task.unit

  /**
    *
    * @param value
    *   Runs the given effect when the value of this [[Option]] is [[None]]
    *   Does not run the side-effect if the value is also a failed effect.
    * @param effect
    *   The effect to run
    * @return
    *   Does not return anything, this method is inherently imperative, and relies on
    *   side-effects to achieve something.
    */
  def flatEffectOnNone[T, _](value: Task[Option[T]], effect: => Task[_]): Task[Unit] =
    value.flatMap(opt => TaskOps.effectOnFail(opt, effect))

  /**
    *
    * @param value
    *   Runs the given effect when the value of this [[Option]] is [[Some]]
    * @param effect
    *   The effect to run
    * @return
    *   Does not return anything, this method is inherently imperative, and relies on
    *   side-effects to achieve something.
    */
  def effectOnPure[T, _](value: Option[T], effect: T => Task[_]): Task[Unit] =
    value match {
      case None    => Task.unit
      case Some(v) => TaskOps.discardContent(effect(v))

    }

  /**
    *
    * @param value
    *   Runs the given effect when the value of this [[Option]] is [[Some]]
    *   Does not run the side-effect if the value is also a failed effect.
    * @param effect
    *   The effect to run
    * @return
    *   Does not return anything, this method is inherently imperative, and relies on
    *   side-effects to achieve something.
    */
  def flatEffectOnSome[T, _](value: Task[Option[T]], effect: T => Task[_]): Task[Unit] =
    value.flatMap(opt => TaskOps.effectOnPure(opt, effect))

  /**
    *
    * @param value
    *   Runs the given effect when the value of this [[Result]] is [[Incorrect]]
    * @param effect
    *   The effect to run
    * @return
    *   Does not return anything, this method is inherently imperative, and relies on
    *   side-effects to achieve something.
    */
  def effectOnFail[T, _](value: Result[T], effect: Anomaly => Task[_]): Task[Unit] = value match {
    case Correct(_)         => Task.unit
    case Incorrect(anomaly) => TaskOps.discardContent(effect(anomaly))
  }

  /**
    *
    * @param value
    *   Runs the given effect when the boxed value of this [[Result]] is [[Incorrect]]
    *   Does not run the side-effect if the value is also a failed effect.
    * @param effect
    *   The effect to run
    * @return
    *   Does not return anything, this method is inherently imperative, and relies on
    *   side-effects to achieve something.
    */
  def flatEffectOnIncorrect[T, _](value: Task[Result[T]], effect: Anomaly => Task[_]): Task[Unit] =
    value.flatMap(result => TaskOps.effectOnFail(result, effect))

  /**
    *
    * @param value
    *   Runs the given effect when the value of this [[Result]] is [[Correct]]
    * @param effect
    *   The effect to run
    * @return
    *   Does not return anything, this method is inherently imperative, and relies on
    *   side-effects to achieve something.
    */
  def effectOnPure[T, _](value: Result[T], effect: T => Task[_]): Task[Unit] =
    value match {
      case Incorrect(_) => Task.unit
      case Correct(v)   => TaskOps.discardContent(effect(v))
    }

  /**
    *
    * @param value
    *   Runs the given effect when the boxed value of this [[Result]] is [[Correct]]
    *   Does not run the side-effect if the value is also a failed effect.
    * @param effect
    *   The effect to run
    * @return
    *   Does not return anything, this method is inherently imperative, and relies on
    *   side-effects to achieve something.
    */
  def flatEffectOnCorrect[T, _](value: Task[Result[T]], effect: T => Task[_]): Task[Unit] =
    value.flatMap(result => TaskOps.effectOnPure(result, effect))

  //=========================================================================
  //============================== Transformers =============================
  //=========================================================================

  /**
    * Used to transform both the "pure" part of the effect, and the "fail" part. Hence the name
    * "bi" map, because it also allows you to change both branches of the effect, not just the
    * happy path.
    */
  def bimap[T, R](value: Task[T], good: T => R, bad: Throwable => Anomaly): Task[R] =
    value.map(good).adaptError {
      case NonFatal(t) => bad(t).asThrowable
    }

  /**
    * Similar to the overload, but the [[Correct]] branch of the result is used to change the "pure" branch of this
    * effect, and [[Incorrect]] branch is used to change the "fail" branch of the effect.
    */
  def bimap[T, R](value: Task[T], result: Result[T] => Result[R]): Task[R] =
    TaskOps.attemptResult(value).map(result).flatMap {
      case Correct(v)   => TaskOps.pure(v)
      case Incorrect(v) => TaskOps.fail(v)
    }

  /**
    * Similar to the overload, but the [[Correct]] branch of the result is used to change the "pure" branch of this
    * effect, and [[Incorrect]] branch is used to change the "fail" branch of the effect.
    *
    * The overload that uses [[Throwable]] instead of [[Anomaly]]
    */
  def bimapThr[T, R](value: Task[T], good: T => R, bad: Throwable => Throwable): Task[R] =
    value.map(good).adaptError {
      case NonFatal(t) => bad(t)
    }

  /**
    *
    * Given the basic two-pronged nature of this effect.
    * the ``good`` function transforms the underlying "pure" (correct, successful, etc) if that's the case.
    * the ``bad`` function transforms the underlying "failure" part of the effect into a "pure" part.
    *
    * Therefore, by using ``morph`` you are defining the rules by which to make the effect into a successful one
    * that does not short-circuit monadic flatMap chains.
    *
    * e.g:
    * {{{
    *   val f: Future[Int] = Future.fail(InvalidInputFailure)
    *   Future.morph(f, (i: Int) => i *2, (t: Throwable) => 42)
    * }}}
    *
    * Undefined behavior if you throw exceptions in the method. DO NOT do that!
    */
  def morph[T, R](value: Task[T], good: T => R, bad: Throwable => R): Task[R] =
    value.map(good).recover {
      case NonFatal(t) => bad(t)
    }

  /**
    * Semantically equivalent to the overload ``morph`` that accepts two functions, but those encoded
    * as the corresponding branches of a Result type.
    *
    * Undefined behavior if you throw exceptions in the method. DO NOT do that!
    */
  def morph[T, R](value: Task[T], result: Result[T] => R): Task[R] =
    TaskOps.attemptResult(value).map(result)

  /**
    *
    * Explicitely discard the contents of this effect, and return [[Unit]] instead.
    *
    * N.B. computation, and side-effects captured within this effect are still executed,
    * it's just the final value that is discarded
    *
    */
  def discardContent[_](value: Task[_]): Task[Unit] =
    value.map(UnitFunction)

  //=========================================================================
  //=============================== Traversals ==============================
  //=========================================================================

  /**
    *
    * Syntactically inspired from [[Future.traverse]].
    *
    * See [[FutureOps.serialize]] for semantics.
    *
    * Usage:
    * {{{
    *   import busymachines.effects.async._
    *   val patches: Seq[Patch] = //...
    *
    *   //this ensures that no two changes will be applied in parallel.
    *   val allPatches: Task[Seq[Patch]] = Task.serialize(patches){ patch: Patch =>
    *     Task {
    *       //apply patch
    *     }
    *   }
    *   //... and so on, and so on!
    * }}}
    *
    *
    */
  def serialize[A, B, C[X] <: TraversableOnce[X]](col: C[A])(fn: A => Task[B])(
    implicit
    cbf: CanBuildFrom[C[A], B, C[B]]
  ): Task[C[B]] = Task.traverse(col)(fn)(cbf)
  //=========================================================================
  //=============================== Constants ===============================
  //=========================================================================

  private val UnitFunction: Any => Unit = _ => ()

  private[async] val defaultDuration: FiniteDuration = duration.minutes(1)
}
