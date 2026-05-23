package api

import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.*
import play.api.test.*
import play.api.test.Helpers.*

class ShoppingListSpecFunctionalTest extends PlaySpec with GuiceOneAppPerSuite {

  "ShoppingListController" should {
    "A valid customer can create a shopping list, then retrieve it" in {
      // Create customer
      val createCustomer = FakeRequest(POST, "/api/v1/customer")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.obj("email" -> "functional@test.com"))

      val customerResult = route(app, createCustomer).get
      status(customerResult) mustBe CREATED

      // Create Shopping List
      val createShoppingList = FakeRequest(POST, "/api/v1/shopping-list")
        .withHeaders("Content-Type" -> "application/json")
        .withBody(Json.obj(
          "email" -> "functional@test.com",
          "name" -> "Weekly Groceries",
          "items" -> Json.arr(
            Json.obj("name" -> "Milk", "quantity" -> 2),
            Json.obj("name" -> "Bread", "quantity" -> 1)
          )
        ))
      val createShoppingListResult = route(app, createShoppingList).get
      status(createShoppingListResult) mustBe CREATED

      // Can retrieve it

      val shoppingList = FakeRequest(GET, "/api/v1/shopping-list/functional@test.com")
        .withHeaders("Content-Type" -> "application/json")
      val shoppingListResult = route(app, shoppingList).get
      status(shoppingListResult) mustBe OK

      val json = contentAsJson(shoppingListResult)
      (json \ "name").as[String] mustBe "Weekly Groceries"
      (json \ "items").as[List[JsObject]] must contain theSameElementsAs List(
        Json.obj("name" -> "Milk", "quantity" -> 2),
        Json.obj("name" -> "Bread", "quantity" -> 1)
      )
    }
  }
}
