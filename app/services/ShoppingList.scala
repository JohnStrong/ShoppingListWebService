package services

import models.ShoppingListItem
import scala.collection.mutable

trait ShoppingListService {
  def getAllShoppingLists(email: String): Either[String, List[ShoppingListItem]]
}

class ShoppingListServiceImpl extends ShoppingListService {

  private val shoppingLists = mutable.HashMap[String, List[ShoppingListItem]]()

  override def getAllShoppingLists(email: String): Either[String, List[ShoppingListItem]] = {
    shoppingLists.get(email)
      .toRight(s"No shopping list found for email $email.")
  }
}
