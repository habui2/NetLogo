// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.parse

import org.scalatest.FunSuite
import collection.immutable.ListMap
import org.nlogo.api.{ Breed, Program, Token, TokenType }
import BreedIdentifierHandler.Spec

class BreedIdentifierHandlerTests extends FunSuite {

  def tester(handler: BreedIdentifierHandler.Helper, code: String, tokenString: String): Token = {
    val program =
      Program.empty.copy(
        breeds = ListMap("FROGS" -> Breed("FROGS", "FROG")),
        linkBreeds = ListMap(
          "AS" -> Breed("AS", "A", isDirected = true),
          "BS" -> Breed("BS", "B", isDirected = false)))
    handler.process(
      Parser.tokenizer.tokenize(code).find(_.name.equalsIgnoreCase(tokenString)).orNull,
      program)
      .get
  }

  test("turtleBreedIdentifier") {
    val token = tester(BreedIdentifierHandler.turtle(Spec("CREATE-*", TokenType.Command, false,
      "_createturtles")),
      "breed[frogs frog] to foo create-frogs 1 end", "CREATE-FROGS")
    expectResult("_createturtles:FROGS,+0")(token.value.toString)
  }

  test("directedLinkBreedIdentifier1") {
    val token = tester(BreedIdentifierHandler.directedLink(Spec
      ("CREATE-*-TO", TokenType.Command, true,
        "_createlinkto")),
      "directed-link-breed[as a] to foo ask turtle 0 [ create-a-to turtle 1 ] end",
      "CREATE-A-TO")
    expectResult("_createlinkto:AS,+0")(token.value.toString)
  }

  test("directedLinkBreedIdentifier2") {
    val token = tester(BreedIdentifierHandler.directedLink(Spec
      ("OUT-*-NEIGHBOR?", TokenType.Reporter, true,
        "_outlinkneighbor")),
      "directed-link-breed[as a] to foo ask turtle 0 [ print out-a-neighbor? turtle 1 ] end",
      "OUT-A-NEIGHBOR?")
    expectResult("_outlinkneighbor:AS")(token.value.toString)
  }

  test("undirectedLinkBreedIdentifier") {
    val token = tester(BreedIdentifierHandler.undirectedLink(Spec
      ("CREATE-*-WITH", TokenType.Command, true,
        "_createlinkwith")),
      "undirected-link-breed[bs b] to foo ask turtle 0 [ create-b-with turtle 1 ] end",
      "CREATE-B-WITH")
    expectResult("_createlinkwith:BS,+0")(token.value.toString)
  }

}
