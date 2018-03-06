package busymachines

import scalafix._
import scala.meta._

final case class InspectAnomalies(index: SemanticdbIndex) extends SemanticRule(index, "InspectAnomalies") {

  override def fix(ctx: RuleCtx): Patch = {
    ctx.debugIndex()
    ctx.tree.children.foreach { child =>
      println(s"----------- child: L${child.pos.startLine} ----------- ")
      println(child.syntax)
      println("")
      println("")
    }

    //println(s"Tree.syntax: " + ctx.tree.syntax)
    //println(s"Tree.structure: " + ctx.tree.structure)
    Patch.empty
  }

}
