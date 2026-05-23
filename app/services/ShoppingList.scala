package services

import models.ShoppingListItem
import scala.collection.mutable

trait ShoppingListService {
  def getShoppingListItems(email: String): Either[String, List[ShoppingListItem]]
}

class ShoppingListServiceImpl extends ShoppingListService {

  // TODO: move to a db - h2 initially for development
  private val shoppingLists = mutable.HashMap[String, List[ShoppingListItem]]()

  override def getShoppingListItems(email: String): Either[String, List[ShoppingListItem]] = {
    shoppingLists.get(email)
      .toRight(s"No shopping list found for email $email.")
  }
}
