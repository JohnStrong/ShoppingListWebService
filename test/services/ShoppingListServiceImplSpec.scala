package services

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import models.ShoppingListItem

class ShoppingListServiceImplSpec extends AnyWordSpec with Matchers {

  private def freshService() = new ShoppingListServiceImpl()

  "getShoppingListItems" should {

    "return Left with error when no list exists for email" in {
      val service = freshService()
      val result = service.getShoppingListItems("unknown@example.com")

      result shouldBe a[Left[_, _]]
      result.left.toOption.get should include("No shopping list found")
    }
  }
}
