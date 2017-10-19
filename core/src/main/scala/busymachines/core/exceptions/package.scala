package busymachines.core

import busymachines.core.exceptions.FailureMessage.StringOrSeqString

import scala.collection.immutable
import scala.language.implicitConversions

/**
  *
  * @author Lorand Szakacs, lsz@lorandszakacs.com, lorand.szakacs@busymachines.com
  * @since 31 Jul 2017
  *
  */
package object exceptions {

  implicit def stringToStringOrSeqStringWrapper(s: String): StringOrSeqString =
    FailureMessage.StringWrapper(s)

  implicit def seqOfStringToStringOrSeqStringWrapper(ses: immutable.Seq[String]): StringOrSeqString =
    FailureMessage.SeqStringWrapper(ses)
}
