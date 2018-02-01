package busymachines.effects.async_test

import busymachines.core._
import busymachines.effects.async._
import busymachines.effects.sync._
import org.scalatest._

/**
  *
  * @author Lorand Szakacs, lsz@lorandszakacs.com, lorand.szakacs@busymachines.com
  * @since 28 Jan 2018
  *
  */
final class TaskEffectsAsyncTest extends FunSpec {
  implicit val ec: Scheduler = Scheduler.global
  //prevents atrocious English
  private def test: ItWord = it

  private implicit class TestSyntax[T](value: Task[T]) {
    //short for "run"
    def r: T = value.unsafeSyncGet()
  }

  private def sleep(l: Long = 10L): Unit = Thread.sleep(l)

  //--------------------------------------------------------------------------

  private val thr: RuntimeException         = new RuntimeException("runtime_exception")
  private val iae: IllegalArgumentException = new IllegalArgumentException("illegal_argument_exception")

  private val ano: InvalidInputAnomaly = InvalidInputFailure("invalid_input_failure")

  private val none: Option[Int] = Option.empty
  private val some: Option[Int] = Option(42)

  private val success: Try[Int] = Try.pure(42)
  private val failure: Try[Int] = Try.fail(ano)

  private val left:  Either[Throwable, Int] = Left(thr)
  private val right: Either[Throwable, Int] = Right(42)

  private val correct:   Result[Int] = Result(42)
  private val incorrect: Result[Int] = Result.fail(ano)

  private val failedF:  Future[Int] = Future.fail(ano)
  private val successF: Future[Int] = Future.pure(42)

  private val int2str: Int => String = i => i.toString
  private val res2str: Result[Int] => String = {
    case Correct(i)   => i.toString
    case Incorrect(t) => t.message
  }

  private val thr2str: Throwable => String    = thr => thr.getMessage
  private val thr2ano: Throwable => Anomaly   = thr => ForbiddenFailure
  private val thr2thr: Throwable => Throwable = thr => iae
  private val res2res: Result[Int] => Result[String] = {
    case Correct(i)   => Correct(i.toString)
    case Incorrect(_) => Incorrect(ForbiddenFailure)
  }

  private val failV: Task[Int] = Task.fail(ano)
  private val pureV: Task[Int] = Task.pure(42)

  private val btrue:  Task[Boolean] = Task.pure(true)
  private val bfalse: Task[Boolean] = Task.pure(false)
  private val bfail:  Task[Boolean] = Task.failWeak(iae)

  //---------------------------------------------------------------------------
  describe("sync + pure") {

    describe("Task — companion object syntax") {

      describe("constructors") {
        test("pure") {
          assert(Task.pure(42).unsafeSyncGet() == 42)
        }

        test("fail") {
          assertThrows[InvalidInputFailure](Task.fail(ano).r)
          assertThrows[RuntimeException](Task.failWeak(thr).r)
        }

        test("unit") {
          assert(Task.unit == Task.unit)
        }

        describe("fromOption") {
          test("none") {
            assertThrows[InvalidInputFailure](Task.fromOption(none, ano).r)
          }

          test("some") {
            assert(Task.fromOption(some, ano).r == 42)
          }
        }

        describe("fromOptionWeak") {
          test("none") {
            assertThrows[RuntimeException](Task.fromOptionWeak(none, thr).r)
          }

          test("some") {
            assert(Task.fromOptionWeak(some, thr).r == 42)
          }
        }

        describe("fromTry") {

          test("failure") {
            assertThrows[InvalidInputFailure](Task.fromTry(failure).r)
          }

          test("success") {
            assert(Task.fromTry(success).r == 42)
          }
        }

        describe("fromEither") {
          test("left") {
            assertThrows[RuntimeException](Task.fromEitherWeak(left).r)
          }

          test("left — transform") {
            assertThrows[ForbiddenFailure](Task.fromEither(left, thr2ano).r)
          }

          test("right") {
            assert(Task.fromEitherWeak(right).r == 42)
          }

          test("right — transform") {
            assert(Task.fromEither(right, thr2ano).r == 42)
          }
        }

        describe("fromEitherWeak") {
          test("left — transform") {
            assertThrows[IllegalArgumentException](Task.fromEitherWeak(left, (t: Throwable) => iae).r)
          }

          test("right") {
            assert(Task.fromEitherWeak(right, (t: Throwable) => iae).r == 42)
          }
        }

        describe("fromResult") {
          test("incorrect") {
            assertThrows[InvalidInputFailure](Task.fromResult(incorrect).r)
          }

          test("correct") {
            assert(Task.fromResult(correct).r == 42)
          }
        }

        describe("fromFuture") {
          test("failed") {
            assertThrows[InvalidInputFailure](Task.fromFuture(failedF).r)
          }

          test("success") {
            assert(Task.fromFuture(successF).r == 42)
          }
        }

        describe("fromIO") {
          test("fail") {
            assertThrows[InvalidInputFailure](Task.fromIO(IO.fail(ano)).r)
          }

          test("pure") {
            assert(Task.fromIO(IO.pure(42)).r == 42)
          }
        }

      } //end constructors

      describe("boolean") {

        describe("cond") {
          test("false") {
            val value = Task.cond(
              false,
              42,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("true") {
            val value = Task.cond(
              true,
              42,
              ano
            )
            assert(value.r == 42)
          }
        }

        describe("condWeak") {
          test("false") {
            val value = Task.condWeak(
              false,
              42,
              thr
            )
            assertThrows[RuntimeException](value.r)
          }

          test("true") {
            val value = Task.condWeak(
              true,
              42,
              thr
            )
            assert(value.r == 42)
          }
        }

        describe("condWith") {
          test("false — pure") {
            val value = Task.condWith(
              false,
              pureV,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("true — pure") {
            val value = Task.condWith(
              true,
              pureV,
              ano
            )
            assert(value.r == 42)
          }

          test("false — fail") {
            val value = Task.condWith(
              false,
              failV,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("true — fail") {
            val value = Task.condWith(
              true,
              failV,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }
        }

        describe("condWithWeak") {
          test("false — pure") {
            val value = Task.condWithWeak(
              false,
              pureV,
              thr
            )
            assertThrows[RuntimeException](value.r)
          }

          test("true — pure") {
            val value = Task.condWithWeak(
              true,
              pureV,
              thr
            )
            assert(value.r == 42)
          }

          test("false — fail") {
            val value = Task.condWithWeak(
              false,
              failV,
              thr
            )
            assertThrows[RuntimeException](value.r)
          }

          test("true — fail") {
            val value = Task.condWithWeak(
              true,
              failV,
              thr
            )
            assertThrows[InvalidInputFailure](value.r)
          }
        }

        describe("flatCond") {
          test("false") {
            val value = Task.flatCond(
              bfalse,
              42,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("true") {
            val value = Task.flatCond(
              btrue,
              42,
              ano
            )
            assert(value.r == 42)
          }

          test("fail") {
            val value = Task.flatCond(
              bfail,
              42,
              ano
            )
            assertThrows[IllegalArgumentException](value.r)
          }
        }

        describe("flatCondWeak") {
          test("false") {
            val value = Task.flatCondWeak(
              bfalse,
              42,
              thr
            )
            assertThrows[RuntimeException](value.r)
          }

          test("true") {
            val value = Task.flatCondWeak(
              btrue,
              42,
              thr
            )
            assert(value.r == 42)
          }

          test("fail") {
            val value = Task.flatCondWeak(
              bfail,
              42,
              thr
            )
            assertThrows[IllegalArgumentException](value.r)
          }
        }

        describe("flatCondWith") {
          test("false — pure") {
            val value = Task.flatCondWith(
              bfalse,
              pureV,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("false — fail") {
            val value = Task.flatCondWith(
              bfalse,
              failV,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("true — pure") {
            val value = Task.flatCondWith(
              btrue,
              pureV,
              ano
            )
            assert(value.r == 42)
          }

          test("true — fail") {
            val value = Task.flatCondWith(
              btrue,
              failV,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("fail — pure") {
            val value = Task.flatCondWith(
              bfail,
              pureV,
              ano
            )
            assertThrows[IllegalArgumentException](value.r)
          }

          test("fail — fail") {
            val value = Task.flatCondWith(
              bfail,
              failV,
              ano
            )
            assertThrows[IllegalArgumentException](value.r)
          }
        }

        describe("flatCondWithWeak") {
          test("false — pure") {
            val value = Task.flatCondWithWeak(
              bfalse,
              pureV,
              thr
            )
            assertThrows[RuntimeException](value.r)
          }

          test("false — fail") {
            val value = Task.flatCondWithWeak(
              bfalse,
              failV,
              thr
            )
            assertThrows[RuntimeException](value.r)
          }

          test("true — pure") {
            val value = Task.flatCondWithWeak(
              btrue,
              pureV,
              thr
            )
            assert(value.r == 42)
          }

          test("true — fail") {
            val value = Task.flatCondWithWeak(
              btrue,
              failV,
              thr
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("fail — pure") {
            val value = Task.flatCondWithWeak(
              bfail,
              pureV,
              thr
            )
            assertThrows[IllegalArgumentException](value.r)
          }

          test("fail — fail") {
            val value = Task.flatCondWithWeak(
              bfail,
              failV,
              thr
            )
            assertThrows[IllegalArgumentException](value.r)
          }
        }

        describe("failOnTrue") {
          test("false") {
            val value = Task.failOnTrue(
              false,
              ano
            )
            value.r
          }

          test("true") {
            val value = Task.failOnTrue(
              true,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }
        }

        describe("failOnTrueWeak") {
          test("false") {
            val value = Task.failOnTrueWeak(
              false,
              thr
            )
            value.r
          }

          test("true") {
            val value = Task.failOnTrueWeak(
              true,
              thr
            )
            assertThrows[RuntimeException](value.r)
          }
        }

        describe("failOnFalse") {
          test("false") {
            val value = Task.failOnFalse(
              false,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("true") {
            val value = Task.failOnFalse(
              true,
              ano
            )
            value.r
          }
        }

        describe("failOnFalseWeak") {
          test("false") {
            val value = Task.failOnFalseWeak(
              false,
              thr
            )
            assertThrows[RuntimeException](value.r)
          }

          test("true") {
            val value = Task.failOnFalseWeak(
              true,
              thr
            )
            value.r
          }
        }

        describe("flatFailOnTrue") {
          test("false") {
            val value = Task.flatFailOnTrue(
              bfalse,
              ano
            )
            value.r
          }

          test("true") {
            val value = Task.flatFailOnTrue(
              btrue,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("fail") {
            val value = Task.flatFailOnTrue(
              bfail,
              ano
            )
            assertThrows[IllegalArgumentException](value.r)
          }

        }

        describe("flatFailOnTrueWeak") {
          test("false") {
            val value = Task.flatFailOnTrueWeak(
              bfalse,
              thr
            )
            value.r
          }

          test("true") {
            val value = Task.flatFailOnTrueWeak(
              btrue,
              thr
            )
            assertThrows[RuntimeException](value.r)
          }

          test("fail") {
            val value = Task.flatFailOnTrueWeak(
              bfail,
              thr
            )
            assertThrows[IllegalArgumentException](value.r)
          }

        }

        describe("flatFailOnFalse") {
          test("false") {
            val value = Task.flatFailOnFalse(
              bfalse,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("true") {
            val value = Task.flatFailOnFalse(
              btrue,
              ano
            )
            value.r
          }

          test("fail") {
            val value = Task.flatFailOnFalse(
              bfail,
              ano
            )
            assertThrows[IllegalArgumentException](value.r)
          }

        }

        describe("flatFailOnFalseWeak") {
          test("false") {
            val value = Task.flatFailOnFalseWeak(
              bfalse,
              thr
            )
            assertThrows[RuntimeException](value.r)
          }

          test("true") {
            val value = Task.flatFailOnFalseWeak(
              btrue,
              thr
            )
            value.r
          }

          test("fail") {
            val value = Task.flatFailOnFalseWeak(
              bfail,
              thr
            )
            assertThrows[IllegalArgumentException](value.r)
          }

        }

      } //end boolean

      describe("nested") {

        describe("flattenOption") {
          test("pure — none") {
            assertThrows[InvalidInputAnomaly] {
              Task.flattenOption(Task.pure(none), ano).r
            }
          }

          test("pure — some") {
            assert(Task.flattenOption(Task.pure(some), ano).r == 42)
          }

          test("fail") {
            assertThrows[RuntimeException] {
              Task.flattenOption(Task.failWeak[Option[Int]](thr), ano).r
            }
          }
        }

        describe("flattenOptionWeak") {

          test("pure — none") {
            assertThrows[RuntimeException] {
              Task.flattenOptionWeak(Task.pure(none), thr).r
            }
          }

          test("pure — some") {
            assert(Task.flattenOptionWeak(Task.pure(some), thr).r == 42)
          }

          test("fail") {
            assertThrows[InvalidInputFailure] {
              Task.flattenOptionWeak(Task.fail[Option[Int]](ano), thr).r
            }
          }
        }

        describe("flattenResult") {
          test("incorrect") {
            assertThrows[InvalidInputFailure](
              Task.flattenResult(Task.pure(incorrect)).r
            )
          }

          test("correct") {
            assert(Task.flattenResult(Task.pure(correct)).r == 42)
          }
        }

      } //end nested

      describe("as{Effect}") {

        describe("attemptResult") {
          test("fail") {
            assert(Task.attemptResult(failV).r == incorrect)
          }

          test("pure") {
            assert(Task.attemptResult(pureV).r == correct)
          }

        }

        describe("unsafeGet") {

          test("fail") {
            assertThrows[InvalidInputFailure](Task.unsafeSyncGet(failV))
          }

          test("pure") {
            assert(Task.unsafeSyncGet(pureV) == 42)
          }

        }

      } //end as{Effect}

      describe("as{Effect} — reverse") {

        describe("future as Task") {
          test("fail") {
            assertThrows[InvalidInputFailure](failedF.asTask.r)
          }

          test("pure") {
            assert(successF.asTask.r == 42)
          }
        }

        describe("io as Task") {
          test("fail") {
            assertThrows[InvalidInputFailure](IO.fail(ano).asTask.r)
          }

          test("pure") {
            assert(IO.pure(42).asTask.r == 42)
          }

        }

        describe("unsafeGet") {

          test("fail") {
            assertThrows[InvalidInputFailure](Task.unsafeSyncGet(failV))
          }

          test("pure") {
            assert(Task.unsafeSyncGet(pureV) == 42)
          }

        }

      } //end as{Effect}

      describe("transformers") {

        describe("bimap") {

          test("fail") {
            val value = Task.bimap(
              failV,
              int2str,
              thr2ano
            )

            assertThrows[ForbiddenFailure](value.r)
          }

          test("pure") {
            val value = Task.bimap(
              pureV,
              int2str,
              thr2ano
            )

            assert(value.r == "42")
          }

        }

        describe("bimap — result") {

          test("fail") {
            val value = Task.bimap(
              failV,
              res2res
            )

            assertThrows[ForbiddenFailure](value.r)
          }

          test("pure") {
            val value = Task.bimap(
              pureV,
              res2res
            )

            assert(value.r == "42")
          }

        }

        describe("bimapWeak") {

          test("fail") {
            val value = Task.bimapWeak(
              failV,
              int2str,
              thr2thr
            )

            assertThrows[IllegalArgumentException](value.r)
          }

          test("pure") {
            val value = Task.bimapWeak(
              pureV,
              int2str,
              thr2thr
            )

            assert(value.r == "42")
          }

        }

        describe("morph") {

          test("fail") {
            val value = Task.morph(
              failV,
              int2str,
              thr2str
            )
            assert(value.r == ano.message)
          }

          test("pure") {
            val value = Task.morph(
              pureV,
              int2str,
              thr2str
            )
            assert(value.r == "42")
          }
        }

        describe("morph — result") {

          test("fail") {
            val value = Task.morph(
              failV,
              res2str
            )
            assert(value.r == ano.message)
          }

          test("pure") {
            val value = Task.morph(
              pureV,
              res2str
            )
            assert(value.r == "42")
          }
        }

        describe("discardContent") {

          test("fail") {
            assertThrows[InvalidInputFailure](Task.discardContent(failV).r)
          }

          test("pure") {
            Task.discardContent(pureV).r
          }
        }

      } //end transformers

    } //end companion object syntax tests

    //===========================================================================
    //===========================================================================
    //===========================================================================

    describe("Task — reference syntax") {

      describe("boolean") {

        describe("cond") {
          test("false") {
            val value = false.condTask(
              42,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("true") {
            val value = true.condTask(
              42,
              ano
            )
            assert(value.r == 42)
          }
        }

        describe("condWeak") {
          test("false") {
            val value =
              false.condTaskWeak(
                42,
                thr
              )
            assertThrows[RuntimeException](value.r)
          }

          test("true") {
            val value = true.condTaskWeak(
              42,
              thr
            )
            assert(value.r == 42)
          }
        }

        describe("condWith") {
          test("false — pure") {
            val value = false.condWithTask(
              pureV,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("true — pure") {
            val value = true.condWithTask(
              pureV,
              ano
            )
            assert(value.r == 42)
          }

          test("false — fail") {
            val value = false.condWithTask(
              failV,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("true — fail") {
            val value = true.condWithTask(
              failV,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }
        }

        describe("condWithWeak") {
          test("false — pure") {
            val value = false.condWithTaskWeak(
              pureV,
              thr
            )
            assertThrows[RuntimeException](value.r)
          }

          test("true — pure") {
            val value = true.condWithTaskWeak(
              pureV,
              thr
            )
            assert(value.r == 42)
          }

          test("false — fail") {
            val value =
              false.condWithTaskWeak(
                failV,
                thr
              )
            assertThrows[RuntimeException](value.r)
          }

          test("true — fail") {
            val value = true.condWithTaskWeak(
              failV,
              thr
            )
            assertThrows[InvalidInputFailure](value.r)
          }
        }

        describe("flatCond") {
          test("false") {
            val value = bfalse.cond(
              42,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("true") {
            val value = btrue.cond(
              42,
              ano
            )
            assert(value.r == 42)
          }

          test("fail") {
            val value = bfail.cond(
              42,
              ano
            )
            assertThrows[IllegalArgumentException](value.r)
          }
        }

        describe("flatCondWeak") {
          test("false") {
            val value = bfalse.condWeak(
              42,
              thr
            )
            assertThrows[RuntimeException](value.r)
          }

          test("true") {
            val value = btrue.condWeak(
              42,
              thr
            )
            assert(value.r == 42)
          }

          test("fail") {
            val value = bfail.condWeak(
              42,
              thr
            )
            assertThrows[IllegalArgumentException](value.r)
          }
        }

        describe("flatCondWith") {
          test("false — pure") {
            val value = bfalse.condWith(
              pureV,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("false — fail") {
            val value = bfalse.condWith(
              failV,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("true — pure") {
            val value = btrue.condWith(
              pureV,
              ano
            )
            assert(value.r == 42)
          }

          test("true — fail") {
            val value = btrue.condWith(
              failV,
              ano
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("fail — pure") {
            val value = bfail.condWith(
              pureV,
              ano
            )
            assertThrows[IllegalArgumentException](value.r)
          }

          test("fail — fail") {
            val value = bfail.condWith(
              failV,
              ano
            )
            assertThrows[IllegalArgumentException](value.r)
          }
        }

        describe("flatCondWithWeak") {
          test("false — pure") {
            val value = bfalse.condWithWeak(
              pureV,
              thr
            )
            assertThrows[RuntimeException](value.r)
          }

          test("false — fail") {
            val value = bfalse.condWithWeak(
              failV,
              thr
            )
            assertThrows[RuntimeException](value.r)
          }

          test("true — pure") {
            val value = btrue.condWithWeak(
              pureV,
              thr
            )
            assert(value.r == 42)
          }

          test("true — fail") {
            val value = btrue.condWithWeak(
              failV,
              thr
            )
            assertThrows[InvalidInputFailure](value.r)
          }

          test("fail — pure") {
            val value = bfail.condWithWeak(
              pureV,
              thr
            )
            assertThrows[IllegalArgumentException](value.r)
          }

          test("fail — fail") {
            val value = bfail.condWithWeak(
              failV,
              thr
            )
            assertThrows[IllegalArgumentException](value.r)
          }
        }

        describe("failOnTrue") {
          test("false") {
            val value = false.failOnTrueTask(ano)
            value.r
          }

          test("true") {
            val value = true.failOnTrueTask(ano)
            assertThrows[InvalidInputFailure](value.r)
          }
        }

        describe("failOnTrueWeak") {
          test("false") {
            val value = false.failOnTrueTaskWeak(thr)
            value.r
          }

          test("true") {
            val value = true.failOnTrueTaskWeak(thr)
            assertThrows[RuntimeException](value.r)
          }
        }

        describe("failOnFalse") {
          test("false") {
            val value = false.failOnFalseTask(ano)
            assertThrows[InvalidInputFailure](value.r)
          }

          test("true") {
            val value = true.failOnFalseTask(ano)
            value.r
          }
        }

        describe("failOnFalseWeak") {
          test("false") {
            val value = false.failOnFalseTaskWeak(thr)
            assertThrows[RuntimeException](value.r)
          }

          test("true") {
            val value = true.failOnFalseTaskWeak(thr)
            value.r
          }
        }

        describe("flatFailOnTrue") {
          test("false") {
            val value = bfalse.failOnTrue(ano)
            value.r
          }

          test("true") {
            val value = btrue.failOnTrue(ano)
            assertThrows[InvalidInputFailure](value.r)
          }

          test("fail") {
            val value = bfail.failOnTrue(ano)
            assertThrows[IllegalArgumentException](value.r)
          }

        }

        describe("flatFailOnTrueWeak") {
          test("false") {
            val value = bfalse.failOnTrueWeak(thr)
            value.r
          }

          test("true") {
            val value = btrue.failOnTrueWeak(thr)
            assertThrows[RuntimeException](value.r)
          }

          test("fail") {
            val value = bfail.failOnTrueWeak(thr)
            assertThrows[IllegalArgumentException](value.r)
          }

        }

        describe("flatFailOnFalse") {
          test("false") {
            val value = bfalse.failOnFalse(ano)
            assertThrows[InvalidInputFailure](value.r)
          }

          test("true") {
            val value = btrue.failOnFalse(ano)
            value.r
          }

          test("fail") {
            val value = bfail.failOnFalse(ano)
            assertThrows[IllegalArgumentException](value.r)
          }

        }

        describe("flatFailOnFalseWeak") {
          test("false") {
            val value = bfalse.failOnFalseWeak(thr)
            assertThrows[RuntimeException](value.r)
          }

          test("true") {
            val value = btrue.failOnFalseWeak(thr)
            value.r
          }

          test("fail") {
            val value = bfail.failOnFalseWeak(thr)
            assertThrows[IllegalArgumentException](value.r)
          }

        }

      } //end boolean

      describe("nested") {

        describe("flattenOption") {
          test("pure — none") {
            assertThrows[InvalidInputAnomaly] {
              Task.pure(none).flattenOption(ano).r
            }
          }

          test("pure — some") {
            assert(Task.pure(some).flattenOption(ano).r == 42)
          }

          test("fail") {
            assertThrows[RuntimeException] {
              Task.failWeak[Option[Int]](thr).flattenOption(ano).r
            }
          }
        }

        describe("flattenOptionWeak") {

          test("pure — none") {
            assertThrows[RuntimeException] {
              Task.pure(none).flattenOptionWeak(thr).r
            }
          }

          test("pure — some") {
            assert(Task.pure(some).flattenOptionWeak(thr).r == 42)
          }

          test("fail") {
            assertThrows[InvalidInputFailure] {
              Task.fail[Option[Int]](ano).flattenOptionWeak(thr).r
            }
          }
        }

        describe("flattenResult") {
          test("incorrect") {
            assertThrows[InvalidInputFailure](
              Task.pure(incorrect).flattenResult.r
            )
          }

          test("correct") {
            assert(Task.pure(correct).flattenResult.r == 42)
          }
        }

      } //end nested

      describe("as{Effect}") {

        describe("attemptResult") {

          test("fail") {
            assert(failV.attempResult.r == incorrect)
          }

          test("pure") {
            assert(pureV.attempResult.r == correct)
          }

        }

        describe("unsafeGet") {

          test("fail") {
            assertThrows[InvalidInputFailure](failV.unsafeSyncGet())
          }

          test("pure") {
            assert(pureV.unsafeSyncGet() == 42)
          }

        }

      } //end as{Effect}

      describe("transformers") {

        describe("bimap") {

          test("fail") {
            val value = failV.bimap(
              int2str,
              thr2ano
            )

            assertThrows[ForbiddenFailure](value.r)
          }

          test("pure") {
            val value = pureV.bimap(
              int2str,
              thr2ano
            )

            assert(value.r == "42")
          }

        }

        describe("bimap — result") {

          test("fail") {
            val value = failV.bimap(
              res2res
            )

            assertThrows[ForbiddenFailure](value.r)
          }

          test("pure") {
            val value = pureV.bimap(
              res2res
            )

            assert(value.r == "42")
          }

        }

        describe("bimapWeak") {

          test("fail") {
            val value = failV.bimapWeak(
              int2str,
              thr2thr
            )

            assertThrows[IllegalArgumentException](value.r)
          }

          test("pure") {
            val value = pureV.bimapWeak(
              int2str,
              thr2thr
            )

            assert(value.r == "42")
          }

        }

        describe("morph") {

          test("fail") {
            val value = failV.morph(
              int2str,
              thr2str
            )
            assert(value.r == ano.message)
          }

          test("pure") {
            val value = pureV.morph(
              int2str,
              thr2str
            )
            assert(value.r == "42")
          }
        }

        describe("morph — result") {

          test("fail") {
            val value = failV.morph(
              res2str
            )
            assert(value.r == ano.message)
          }

          test("pure") {
            val value = pureV.morph(
              res2str
            )
            assert(value.r == "42")
          }
        }

        describe("discardContent") {

          test("fail") {
            assertThrows[InvalidInputFailure](failV.discardContent.r)
          }

          test("pure") {
            pureV.discardContent.r
          }
        }

      } //end transformers

    } //end reference syntax tests
  }

  //===========================================================================
  //===========================================================================
  //===========================================================================

  describe("async + impure") {

    describe("Task — companion object syntax") {

      describe("suspend") {

        test("suspendOption") {
          val f = Task.suspendOption(
            Option(throw thr),
            ano
          )
          assertThrows[RuntimeException](f.r)

        }

        test("suspendOptionWeak") {
          val f = Task.suspendOptionWeak(
            Option(throw thr),
            iae
          )
          assertThrows[RuntimeException](f.r)

        }

        test("suspendTry") {
          val f = Task.suspendTry(
            Try.pure(throw thr)
          )
          assertThrows[RuntimeException](f.r)
        }

        test("suspendEither") {
          val f = Task.suspendEither(
            Right[Throwable, String](throw thr),
            thr2ano
          )
          assertThrows[RuntimeException](f.r)
        }

        test("suspendEitherWeak") {
          val f = Task.suspendEitherWeak(
            Right[Throwable, String](throw thr)
          )
          assertThrows[RuntimeException](f.r)
        }

        test("suspendEitherWeak — transform") {
          val f = Task.suspendEitherWeak(
            Right[Throwable, String](throw thr),
            thr2thr
          )
          assertThrows[RuntimeException](f.r)
        }

        test("suspendResult") {
          val f = Task.suspendResult(
            Result.pure(throw thr)
          )
          assertThrows[RuntimeException](f.r)
        }

        test("suspendFuture") {
          var sideEffect: Int = 0
          val f = Task.suspendFuture(
            Future {
              sideEffect = 42
              sideEffect
            }
          )
          if (sideEffect == 42) fail("side effect should not have been applied yet")
          f.r
          assert(sideEffect == 42)
        }

      } //end suspend

      describe("effect on boolean") {

        describe("effectOnFalse") {

          test("false") {
            var sideEffect: Int = 0
            val f = Task.effectOnFalse(
              false,
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }

          test("true") {
            var sideEffect: Int = 0
            val f = Task.effectOnFalse(
              true,
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

        }

        describe("effectOnTrue") {

          test("false") {
            var sideEffect: Int = 0
            val f = Task.effectOnTrue(
              false,
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

          test("true") {
            var sideEffect: Int = 0
            val f = Task.effectOnTrue(
              true,
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }
        }

        describe("flatEffectOnFalse") {

          test("false") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnFalse(
              bfalse,
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }

          test("true") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnFalse(
              btrue,
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

          test("fail") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnFalse(
              bfail,
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            assertThrows[IllegalArgumentException](f.r)
            assert(sideEffect == 0, "side effect should not have applied on fail")

          }

        }

        describe("flatEffectOnTrue") {

          test("false") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnTrue(
              bfalse,
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

          test("true") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnTrue(
              btrue,
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }

          test("fail") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnTrue(
              bfail,
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            assertThrows[IllegalArgumentException](f.r)
            assert(sideEffect == 0, "side effect should not have applied on fail")

          }

        }
      }

      describe("effect on option") {

        describe("effectOnEmpty") {

          test("none") {
            var sideEffect: Int = 0
            val f = Task.effectOnEmpty(
              none,
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }

          test("some") {
            var sideEffect: Int = 0
            val f = Task.effectOnEmpty(
              some,
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }
        }

        describe("effectOnSome") {

          test("none") {
            var sideEffect: Int = 0
            val f = Task.effectOnSome(
              none,
              (x: Int) =>
                Task {
                  sideEffect = x
                  sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

          test("some") {
            var sideEffect: Int = 0
            val f = Task.effectOnSome(
              some,
              (x: Int) =>
                Task {
                  sideEffect = x
                  sideEffect
              }
            )
            f.r
            assert(sideEffect == 42)

          }
        }

        describe("flatEffectOnEmpty") {

          test("none") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnEmpty(
              Task.pure(none),
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)
          }

          test("some") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnEmpty(
              Task.pure(some),
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

          test("fail") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnEmpty(
              Task.fail[Option[Int]](ano),
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            assertThrows[InvalidInputFailure](f.r)
            assert(sideEffect == 0, "side effect should not have applied on fail")

          }

        }

        describe("flatEffectOnSome") {

          test("none") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnSome(
              Task.pure(none),
              (x: Int) =>
                Task {
                  sideEffect = x
                  sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")
          }

          test("some") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnSome(
              Task.pure(some),
              (x: Int) =>
                Task {
                  sideEffect = x
                  sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }

          test("fail") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnSome(
              Task.fail[Option[Int]](ano),
              (x: Int) =>
                Task {
                  sideEffect = x
                  sideEffect
              }
            )
            assertThrows[InvalidInputFailure](f.r)
            assert(sideEffect == 0, "side effect should not have applied on fail")

          }

        }

      }

      describe("effect on result") {

        describe("effectOnIncorrect") {

          test("incorrect") {
            var sideEffect: Int = 0
            val f = Task.effectOnIncorrect(
              incorrect,
              (a: Anomaly) =>
                Task {
                  sideEffect = 42
                  sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }

          test("correct") {
            var sideEffect: Int = 0
            val f = Task.effectOnIncorrect(
              correct,
              (a: Anomaly) =>
                Task {
                  sideEffect = 42
                  sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }
        }

        describe("effectOnCorrect") {

          test("incorrect") {
            var sideEffect: Int = 0
            val f = Task.effectOnCorrect(
              incorrect,
              (x: Int) =>
                Task {
                  sideEffect = x
                  sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

          test("correct") {
            var sideEffect: Int = 0
            val f = Task.effectOnCorrect(
              correct,
              (x: Int) =>
                Task {
                  sideEffect = x
                  sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }
        }

        describe("flatEffectOnIncorrect") {

          test("incorrect") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnIncorrect(
              Task.pure(incorrect),
              (a: Anomaly) =>
                Task {
                  sideEffect = 42
                  sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)
          }

          test("correct") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnIncorrect(
              Task.pure(correct),
              (a: Anomaly) =>
                Task {
                  sideEffect = 42
                  sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

          test("fail") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnIncorrect(
              Task.fail[Result[Int]](ano),
              (a: Anomaly) =>
                Task {
                  sideEffect = 42
                  sideEffect
              }
            )
            assertThrows[InvalidInputFailure](f.r)
            assert(sideEffect == 0, "side effect should not have applied on fail")

          }

        }

        describe("flatEffectOnCorrect") {

          test("incorrect") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnCorrect(
              Task.pure(incorrect),
              (x: Int) =>
                Task {
                  sideEffect = x
                  sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")
          }

          test("correct") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnCorrect(
              Task.pure(correct),
              (x: Int) =>
                Task {
                  sideEffect = x
                  sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }

          test("fail") {
            var sideEffect: Int = 0
            val f = Task.flatEffectOnCorrect(
              Task.fail[Result[Int]](ano),
              (x: Int) =>
                Task {
                  sideEffect = x
                  sideEffect
              }
            )
            assertThrows[InvalidInputFailure](f.r)
            assert(sideEffect == 0, "side effect should not have applied on fail")

          }

        }

      }

    }

    describe("Task — other effect reference syntax") {

      describe("suspendInTask") {

        test("suspendOption") {
          val f = Option(throw thr).suspendInTask(ano)
          assertThrows[RuntimeException](f.r)
        }

        test("suspendOptionWeak") {
          val f = Option(throw thr).suspendInTask(ano)
          assertThrows[RuntimeException](f.r)
        }

        test("suspendTry") {
          val f = Try.pure(throw thr).suspendInTask
          assertThrows[RuntimeException](f.r)
        }

        test("suspendEither") {
          val f = Right[Throwable, String](throw thr).suspendInTask(thr2ano)
          assertThrows[RuntimeException](f.r)
        }

        test("suspendEitherWeak") {
          val f = Right[Throwable, String](throw thr).suspendInTaskWeak
          assertThrows[RuntimeException](f.r)
        }

        test("suspendEitherWeak — transform") {
          val f = Right[Throwable, String](throw thr).suspendInTaskWeak(thr2thr)
          assertThrows[RuntimeException](f.r)
        }

        test("suspendResult") {
          val f = Result.pure(throw thr).suspendInTask
          assertThrows[RuntimeException](f.r)
        }

        test("suspendFuture") {
          var sideEffect: Int = 0
          val f = Future[Int] {
            sideEffect = 42
            sideEffect
          }.suspendInTask
          sleep()
          if (sideEffect == 42) fail("side effect should not have been applied yet")
          f.r
          assert(sideEffect == 42)
        }

      } //end suspend

      describe("effect on boolean") {

        describe("effectOnFalse") {

          test("false") {
            var sideEffect: Int = 0
            val f = false.effectOnFalseTask(
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }

          test("true") {
            var sideEffect: Int = 0
            val f = true.effectOnFalseTask(
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

        }

        describe("effectOnTrue") {

          test("false") {
            var sideEffect: Int = 0
            val f = false.effectOnTrueTask(
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

          test("true") {
            var sideEffect: Int = 0
            val f = true.effectOnTrueTask(
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }
        }

        describe("flatEffectOnFalse") {

          test("false") {
            var sideEffect: Int = 0
            val f = bfalse.effectOnFalse(
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }

          test("true") {
            var sideEffect: Int = 0
            val f = btrue.effectOnFalse(
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

          test("fail") {
            var sideEffect: Int = 0
            val f = bfail.effectOnFalse(
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            assertThrows[IllegalArgumentException](f.r)
            assert(sideEffect == 0, "side effect should not have applied on fail")

          }

        }

        describe("flatEffectOnTrue") {

          test("false") {
            var sideEffect: Int = 0
            val f = bfalse.effectOnTrue(
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

          test("true") {
            var sideEffect: Int = 0
            val f = btrue.effectOnTrue(
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }

          test("fail") {
            var sideEffect: Int = 0
            val f = bfail.effectOnTrue(
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            assertThrows[IllegalArgumentException](f.r)
            assert(sideEffect == 0, "side effect should not have applied on fail")

          }

        }
      }

      describe("effect on option") {

        describe("effectOnEmpty") {

          test("none") {
            var sideEffect: Int = 0
            val f = none.effectOnEmptyTask(
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }

          test("some") {
            var sideEffect: Int = 0
            val f = some.effectOnEmptyTask(
              Task {
                sideEffect = 42
                sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }
        }

        describe("effectOnSome") {

          test("none") {
            var sideEffect: Int = 0
            val f = none.effectOnSomeTask(
              (x: Int) =>
                Task {
                  sideEffect = x
                  sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

          test("some") {
            var sideEffect: Int = 0
            val f = some.effectOnSomeTask(
              (x: Int) =>
                Task {
                  sideEffect = x
                  sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }
        }

        describe("flatEffectOnEmpty") {

          test("none") {
            var sideEffect: Int = 0
            val f = Task
              .pure(none)
              .effectOnEmpty(
                Task {
                  sideEffect = 42
                  sideEffect
                }
              )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)
          }

          test("some") {
            var sideEffect: Int = 0
            val f =
              Task
                .pure(some)
                .effectOnEmpty(
                  Task {
                    sideEffect = 42
                    sideEffect
                  }
                )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

          test("fail") {
            var sideEffect: Int = 0
            val f = Task
              .fail[Option[Int]](ano)
              .effectOnEmpty(
                Task {
                  sideEffect = 42
                  sideEffect
                }
              )
            assertThrows[InvalidInputFailure](f.r)
            assert(sideEffect == 0, "side effect should not have applied on fail")

          }

        }

        describe("flatEffectOnSome") {

          test("none") {
            var sideEffect: Int = 0
            val f = Task
              .pure(none)
              .effectOnSome(
                (x: Int) =>
                  Task {
                    sideEffect = x
                    sideEffect
                }
              )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")
          }

          test("some") {
            var sideEffect: Int = 0
            val f = Task
              .pure(some)
              .effectOnSome(
                (x: Int) =>
                  Task {
                    sideEffect = x
                    sideEffect
                }
              )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }

          test("fail") {
            var sideEffect: Int = 0
            val f = Task
              .fail[Option[Int]](ano)
              .effectOnSome(
                (x: Int) =>
                  Task {
                    sideEffect = x
                    sideEffect
                }
              )
            assertThrows[InvalidInputFailure](f.r)
            assert(sideEffect == 0, "side effect should not have applied on fail")

          }

        }

      }

      describe("effect on result") {

        describe("effectOnIncorrect") {

          test("incorrect") {
            var sideEffect: Int = 0
            val f = incorrect.effectOnIncorrectTask(
              (a: Anomaly) =>
                Task {
                  sideEffect = 42
                  sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }

          test("correct") {
            var sideEffect: Int = 0
            val f = correct.effectOnIncorrectTask(
              (a: Anomaly) =>
                Task {
                  sideEffect = 42
                  sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }
        }

        describe("effectOnCorrect") {

          test("incorrect") {
            var sideEffect: Int = 0
            val f = incorrect.effectOnCorrectTask(
              (x: Int) =>
                Task {
                  sideEffect = x
                  sideEffect
              }
            )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

          test("correct") {
            var sideEffect: Int = 0
            val f = correct.effectOnCorrectTask(
              (x: Int) =>
                Task {
                  sideEffect = x
                  sideEffect
              }
            )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }
        }

        describe("flatEffectOnIncorrect") {

          test("incorrect") {
            var sideEffect: Int = 0
            val f = Task
              .pure(incorrect)
              .effectOnIncorrect(
                (a: Anomaly) =>
                  Task {
                    sideEffect = 42
                    sideEffect
                }
              )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)
          }

          test("correct") {
            var sideEffect: Int = 0
            val f = Task
              .pure(correct)
              .effectOnIncorrect(
                (a: Anomaly) =>
                  Task {
                    sideEffect = 42
                    sideEffect
                }
              )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")

          }

          test("fail") {
            var sideEffect: Int = 0
            val f = Task
              .fail[Result[Int]](ano)
              .effectOnIncorrect(
                (a: Anomaly) =>
                  Task {
                    sideEffect = 42
                    sideEffect
                }
              )
            assertThrows[InvalidInputFailure](f.r)
            assert(sideEffect == 0, "side effect should not have applied on fail")

          }

        }

        describe("flatEffectOnCorrect") {

          test("incorrect") {
            var sideEffect: Int = 0
            val f =
              Task
                .pure(incorrect)
                .effectOnCorrect(
                  (x: Int) =>
                    Task {
                      sideEffect = x
                      sideEffect
                  }
                )
            f.r
            if (sideEffect == 42) fail("side effect should not have executed on other branch")
          }

          test("correct") {
            var sideEffect: Int = 0
            val f = Task
              .pure(correct)
              .effectOnCorrect(
                (x: Int) =>
                  Task {
                    sideEffect = x
                    sideEffect
                }
              )
            if (sideEffect == 42) fail("side effect should not have been applied yet")
            f.r
            assert(sideEffect == 42)

          }

          test("fail") {
            var sideEffect: Int = 0
            val f =
              Task
                .fail[Result[Int]](ano)
                .effectOnCorrect(
                  (x: Int) =>
                    Task {
                      sideEffect = x
                      sideEffect
                  }
                )
            assertThrows[InvalidInputFailure](f.r)
            assert(sideEffect == 0, "side effect should not have applied on fail")

          }

        }

      }
    }

    describe("Task.serialize") {

      test("empty list") {
        val input:    Seq[Int] = List()
        val expected: Seq[Int] = List()

        var sideEffect: Int = 0

        val eventualResult = Task.serialize(input) { i =>
          Task {
            sideEffect = 42
          }
        }

        assert(eventualResult.r == expected)
        assert(sideEffect == 0, "nothing should have happened")
      }

      test("no two tasks should run in parallel") {
        val input: Seq[Int] = (1 to 100).toList
        val expected = input.map(_.toString)

        var previouslyProcessed: Option[Int] = None
        var startedFlag:         Option[Int] = None

        val eventualResult: Task[Seq[String]] = Task.serialize(input) { i =>
          Task {
            assert(
              startedFlag.isEmpty,
              s"started flag should have been empty at the start of each tasks but was: $startedFlag"
            )
            previouslyProcessed foreach { previous =>
              assertResult(expected = i - 1, "... the task were not executed in the correct order.")(
                actual = previous
              )
            }
            startedFlag         = Some(i)
            startedFlag         = None
            previouslyProcessed = Some(i)
            i.toString
          }
        }
        assert(expected == eventualResult.r)
      }

    }
  }

} //end test
