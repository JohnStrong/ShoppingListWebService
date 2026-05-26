package repositories.shoppinglist

import models.{ShoppingList, ShoppingListItem}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ShoppingListRepositorySpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with BeforeAndAfterEach
  with EitherValues {

  private val shoppingList = ShoppingList(
    email = "example@test.com",
    name = "Test",
    items = List(
      ShoppingListItem(name = "Milk", quantity = 2),
      ShoppingListItem(name = "Bread", quantity = 1)
    )
  )

  private var shoppingListRepository: ShoppingListRepository = _

  override def beforeEach(): Unit = {
    shoppingListRepository = ShoppingListRepository()
  }

  "create" should {
    "persist into the repository and return the created shopping list" in {
      val result = shoppingListRepository.create(shoppingList).futureValue
      result.value shouldBe shoppingList

      val actualStored = shoppingListRepository.findByIdentifier(shoppingList.email).futureValue
      actualStored.value shouldBe shoppingList
    }

    "return an error message if a shopping list already exists" in {
      shoppingListRepository.create(shoppingList).futureValue

      val result = shoppingListRepository.create(shoppingList).futureValue

      result.left.value should include("already exists")
    }
  }

  "findByIdentifier" should {
    "return the shopping list with items if there exists one" in {
      shoppingListRepository.create(shoppingList).futureValue

      val result = shoppingListRepository.findByIdentifier(shoppingList.email).futureValue

      result.value shouldBe shoppingList
    }

    "return a message if there is no shopping list" in {
      val result = shoppingListRepository.findByIdentifier("nonexistent@example.com").futureValue

      result.left.value should include("No shopping list found")
    }
  }
}
