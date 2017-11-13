package busymachines.semver

import busymachines.semver.SemanticVersionParsers.SemVerParsingResult

/**
  *
  * @author Lorand Szakacs, lsz@lorandszakacs.com, lorand.szakacs@busymachines.com
  * @since 13 Nov 2017
  *
  */
object syntax {

  implicit class SemanticVersionCompanionOps(doNotCare: SemanticVersion.type) {
    def fromString(semVer: String): SemVerParsingResult[SemanticVersion] =
      SemanticVersionParsers.parseSemanticVersion(semVer)

    def unsafeFromString(semVer: String): SemanticVersion =
      SemanticVersionParsers.unsafeParseSemanticVersion(semVer)
  }

  implicit class SemanticVersionLabelCompanionOps(doNotCare: Labels.type) {
    def fromString(semVer: String): SemVerParsingResult[Label] =
      SemanticVersionParsers.parseLabel(semVer)

    def unsafeFromString(label: String): Label =
      SemanticVersionParsers.unsafeParseLabel(label)
  }

}
