package busymachines.effects.sync

import busymachines.core._

import scala.util._
import scala.util.control.NonFatal

/**
  *
  * @author Lorand Szakacs, lsz@lorandszakacs.com, lorand.szakacs@busymachines.com
  * @since 28 Jan 2018
  *
  */
trait ResultTypeDefinitions {
  type Result[T]    = Either[Anomaly, T]
  type Correct[T]   = Right[Anomaly,  T]
  type Incorrect[T] = Left[Anomaly,   T]
}

object ResultSyntax {

  /**
    *
    */
  trait Implicits {

    implicit def bmcResultReferenceOps[T](value: Result[T]): ReferenceOps[T] =
      new ReferenceOps(value)

    implicit def bmcResultNestedOptionOps[T](nopt: Result[Option[T]]): NestedOptionOps[T] =
      new NestedOptionOps(nopt)

    implicit def bmcResultBooleanOps(test: Boolean): BooleanOps =
      new BooleanOps(test)

    implicit def bmcResultNestedBooleanOps(test: Result[Boolean]): NestedBooleanOps =
      new NestedBooleanOps(test)
  }

  // —— final class CompationObjectOps(val obj: Result.type) —— is not necessary, because we reference the Result
  // object directly in this case. A clever slight of hand

  /**
    *
    */
  final class ReferenceOps[T](private[this] val value: Result[T]) {

    def asOptionUnsafe(): Option[T] =
      Result.asOptionUnsafe(value)

    def asListUnsafe(): List[T] =
      Result.asListUnsafe(value)

    def asTry: Try[T] =
      Result.asTry(value)

    def unsafeGet(): T =
      Result.unsafeGet(value)

    def bimap[R](good: T => R, bad: Anomaly => Anomaly): Result[R] =
      Result.bimap(value, good, bad)

    def morph[R](good: T => R, bad: Anomaly => R): Result[R] =
      Result.morph(value, good, bad)

    def recover[R >: T](pf: PartialFunction[Anomaly, R]): Result[R] =
      Result.recover(value, pf)

    def recoverWith[R >: T](pf: PartialFunction[Anomaly, Result[R]]): Result[R] =
      Result.recoverWith(value, pf)

    def discardContent: Result[Unit] =
      Result.discardContent(value)
  }

  /**
    *
    *
    */
  final class NestedOptionOps[T](private[this] val nopt: Result[Option[T]]) {
    def flattenOption(ifNone: => Anomaly): Result[T] = Result.flattenOption(nopt, ifNone)
  }

  /**
    *
    *
    */
  final class BooleanOps(private[this] val test: Boolean) {

    def condResult[T](good: => T, bad: => Anomaly): Result[T] =
      Result.cond(test, good, bad)

    def condWithResult[T](good: => Result[T], bad: => Anomaly): Result[T] =
      Result.condWith(test, good, bad)

    def failOnTrueResult(bad: => Anomaly): Result[Unit] =
      Result.failOnTrue(test, bad)

    def failOnFalseResult(bad: => Anomaly): Result[Unit] =
      Result.failOnFalse(test, bad)

  }

  /**
    *
    *
    */
  final class NestedBooleanOps(private[this] val test: Result[Boolean]) {

    def cond[T](good: => T, bad: => Anomaly): Result[T] =
      Result.flatCond(test, good, bad)

    def condWith[T](good: => Result[T], bad: => Anomaly): Result[T] =
      Result.flatCondWith(test, good, bad)

    def failOnTrue(bad: => Anomaly): Result[Unit] =
      Result.flatFailOnTrue(test, bad)

    def failOnFalse(bad: => Anomaly): Result[Unit] =
      Result.flatFailOnFalse(test, bad)

  }
}

//=============================================================================
//=============================================================================
//=============================================================================
//=============================================================================

/**
  *
  * And alternative to this would be to alias the Either.type in [[ResultTypeDefinitions]]:
  * {{{
  *   val Result: Either.type = Either
  * }}}
  *
  * The problem with this is that it introduces conflcits when importing:
  * {{{
  *   import cats._, cats.implicits._
  * }}}
  *
  * Because of ambiguities in the same method names, and signatures which
  * only differ in that they use [[Anomaly]] instead of [[Throwable]],
  * and that is too ambiguous to the implicit search, for some reason...
  *
  * Moral of the story:
  * Overloading and implicit resolution DO NOT play well together.
  */
object Result {

  //===========================================================================
  //========================== Primary constructors ===========================
  //===========================================================================

  def pure[T](t:   T):       Result[T] = Correct(t)
  def fail[T](bad: Anomaly): Result[T] = Incorrect(bad)

  def correct[T](t:     T):       Result[T] = Correct(t)
  def incorrect[T](bad: Anomaly): Result[T] = Incorrect(bad)

  val unit: Result[Unit] = Correct(())

  def apply[T](thunk: => T): Result[T] = {
    try {
      Result.pure(thunk)
    } catch {
      case a: Anomaly                  => Result.incorrect(a)
      case t: Throwable if NonFatal(t) => Result.incorrect(CatastrophicError(t))
    }
  }

  //===========================================================================
  //==================== Result from various other effects =======================
  //===========================================================================

  def fromOption[T](opt: Option[T], ifNone: => Anomaly): Result[T] = opt match {
    case None    => Result.incorrect(ifNone)
    case Some(v) => Result.pure(v)
  }

  def fromTry[T](t: Try[T]): Result[T] = t match {
    case Failure(a: Anomaly) => Result.incorrect(a)
    case Failure(NonFatal(r)) => Result.incorrect(CatastrophicError(r))
    case Success(value)       => Result.pure(value)
  }

  def fromEither[L, R](elr: Either[L, R])(implicit ev: L <:< Throwable): Result[R] = elr match {
    case Left(left) =>
      ev(left) match {
        case a: Anomaly => Result.incorrect(a)
        case NonFatal(t) => Result.incorrect(CatastrophicError(t))
      }
    case Right(value) => Result.pure(value)
  }

  def fromEither[L, R](elr: Either[L, R], transformLeft: L => Anomaly): Result[R] = elr match {
    case Left(left)   => Result.incorrect(transformLeft(left))
    case Right(value) => Result.pure(value)
  }

  //===========================================================================
  //======================== Result from special cased Result =======================
  //===========================================================================

  def cond[T](test: Boolean, good: => T, bad: => Anomaly): Result[T] =
    if (test) Result.pure(good) else Result.fail(bad)

  def condWith[T](test: Boolean, good: => Result[T], bad: => Anomaly): Result[T] =
    if (test) good else Result.fail(bad)

  def failOnTrue(test: Boolean, bad: => Anomaly): Result[Unit] =
    if (test) Result.fail(bad) else Result.unit

  def failOnFalse(test: Boolean, bad: => Anomaly): Result[Unit] =
    if (!test) Result.fail(bad) else Result.unit

  def flatCond[T](test: Result[Boolean], good: => T, bad: => Anomaly): Result[T] =
    test.flatMap(b => Result.cond(b, good, bad))

  def flatCondWith[T](test: Result[Boolean], good: => Result[T], bad: => Anomaly): Result[T] =
    test.flatMap(b => Result.condWith(b, good, bad))

  def flatFailOnTrue(test: Result[Boolean], bad: => Anomaly): Result[Unit] =
    test.flatMap(b => if (b) Result.fail(bad) else Result.unit)

  def flatFailOnFalse(test: Result[Boolean], bad: => Anomaly): Result[Unit] =
    test.flatMap(b => if (!b) Result.fail(bad) else Result.unit)

  def flattenOption[T](nopt: Result[Option[T]], ifNone: => Anomaly): Result[T] =
    nopt.flatMap(opt => Result.fromOption(opt, ifNone))

  //===========================================================================
  //======================= Result to various effects =========================
  //===========================================================================

  def asOptionUnsafe[T](value: Result[T]): Option[T] = value match {
    case Left(value)  => throw value.asThrowable
    case Right(value) => Option(value)
  }

  def asListUnsafe[T](value: Result[T]): List[T] = value match {
    case Left(value)  => throw value.asThrowable
    case Right(value) => List(value)
  }

  def asTry[T](value: Result[T]): Try[T] = value match {
    case Left(value)  => scala.util.Failure(value.asThrowable)
    case Right(value) => scala.util.Success(value)
  }

  def unsafeGet[T](value: Result[T]): T = value match {
    case Left(value)  => throw value.asThrowable
    case Right(value) => value
  }

  //===========================================================================
  //============================== Transformers ===============================
  //===========================================================================

  def bimap[T, R](value: Result[T], good: T => R, bad: Anomaly => Anomaly): Result[R] =
    value.right.map(good).left.map(bad)

  def morph[T, R](value: Result[T], good: T => R, bad: Anomaly => R): Result[R] = value match {
    case Left(value)  => Result.pure(bad(value))
    case Right(value) => Result.pure(good(value))
  }

  def recover[T, R >: T](value: Result[T], pf: PartialFunction[Anomaly, R]): Result[R] = value match {
    case Left(a: Anomaly) if pf.isDefinedAt(a) => Result.pure(pf(a))
    case _ => value
  }

  def recoverWith[T, R >: T](value: Result[T], pf: PartialFunction[Anomaly, Result[R]]): Result[R] = value match {
    case Left(a: Anomaly) if pf.isDefinedAt(a) => pf(a)
    case _ => value
  }

  private val UnitFunction: Any => Unit = _ => ()

  def discardContent[T](value: Result[T]): Result[Unit] =
    value.map(UnitFunction)
}

/**
  * Convenience methods to provide more semantically meaningful pattern matches.
  * If you want to preserve the semantically richer meaning of Result, you'd
  * have to explicitely match on the Left with Anomaly, like such:
  * {{{
  *   result match {
  *      Right(v)         => v //...
  *      Left(a: Anomaly) => throw a.asThrowable
  *   }
  * }}}
  *
  * But with these convenience unapplies, the above becomes:
  *
  * {{{
  *   result match {
  *      Correct(v)   => v //...
  *      Incorrect(a) =>  throw a.asThrowable
  *   }
  * }}}
  *
  */
object Correct {

  def apply[T](value: T): Result[T] =
    Right[Anomaly, T](value)

  def unapply[A <: Anomaly, C](arg: Right[A, C]): Option[C] =
    Right.unapply(arg)
}

object Incorrect {

  def apply[T](bad: Anomaly): Result[T] =
    Left[Anomaly, T](bad)

  def unapply[A <: Anomaly, C](arg: Left[A, C]): Option[A] =
    Left.unapply(arg)
}
