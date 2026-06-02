import models.{Customer, ShoppingList}
import services.{CustomerService, CustomerServiceImpl, ShoppingListService, ShoppingListServiceImpl}
import com.google.inject.{AbstractModule, TypeLiteral}
import repositories.DataRepository
import repositories.customer.{CustomerRepository, SlickCustomerRepository}
import repositories.shoppinglist.{ShoppingListRepository, SlickShoppingListRepository}

class Module extends AbstractModule {
  override def configure(): Unit = {

    /**
     * Customer
     */
    bind(new TypeLiteral[DataRepository[String, Customer]]() {}).to(classOf[SlickCustomerRepository])
    bind(classOf[CustomerService]).to(classOf[CustomerServiceImpl])

    /**
     * Shopping List
     */
    bind(new TypeLiteral[DataRepository[String, ShoppingList]]() {}).to(classOf[ShoppingListRepository])
    bind(classOf[ShoppingListService]).to(classOf[ShoppingListServiceImpl])
  }
}
