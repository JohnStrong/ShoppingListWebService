package controllers

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.mockito.Mockito.*
import play.api.test.*
import play.api.test.Helpers.*
import play.api.libs.json.*
import services.ShoppingListService
import models.ShoppingListItem

class ShoppingListControllerSpec extends AnyWordSpec with Matchers {

  private def createFixture() = {
    val mockService = mock(classOf[ShoppingListService])
    val stubComponents = Helpers.stubControllerComponents()
    val controller = new ShoppingListController(stubComponents, mockService)
    (controller, mockService)
  }

  private val testItems = List(
    ShoppingListItem("Milk", 2),
    ShoppingListItem("Bread", 1)
  )

  "getShoppingList" should {

    "return 200 with shopping list JSON when found" in {
      val (controller, mockService) = createFixture()
      when(mockService.getAllShoppingLists("user@example.com")).thenReturn(Right(testItems))

      val result = controller.getShoppingList("user@example.com").apply(FakeRequest())

      status(result) shouldBe OK
      val json = contentAsJson(result).as[List[JsObject]]
      json.length shouldBe 2
      (json.head \ "name").as[String] shouldBe "Milk"
      (json.head \ "quantity").as[Int] shouldBe 2
    }

    "return 404 when no shopping list exists" in {
      val (controller, mockService) = createFixture()
      when(mockService.getAllShoppingLists("nobody@example.com"))
        .thenReturn(Left("No shopping list found for email nobody@example.com."))

      val result = controller.getShoppingList("nobody@example.com").apply(FakeRequest())

      status(result) shouldBe NOT_FOUND
      (contentAsJson(result) \ "error").as[String] should include("No shopping list found")
    }
  }
}
