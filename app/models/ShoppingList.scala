package models

import play.api.libs.json.*

case class ShoppingListItem(name: String, quantity: Int)

object ShoppingListItem {
  implicit val format: Format[ShoppingListItem] = Json.format[ShoppingListItem]
}
