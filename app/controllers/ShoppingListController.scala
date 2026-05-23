package controllers

import javax.inject.*
import play.api.mvc.*
import play.api.libs.json.*
import play.api.mvc.BaseController
import models.ShoppingListItem
import services.ShoppingListService

class ShoppingListController @Inject()(
  val controllerComponents: ControllerComponents,
  val service: ShoppingListService

) extends BaseController {

  def getShoppingList(email: String): Action[AnyContent] = Action {
    service.getAllShoppingLists(email) match {
      case Left(errorMessage) => NotFound(Json.obj("error" -> errorMessage))
      case Right(shoppingList) => Ok(Json.toJson(shoppingList))
    }
  }
}
