import services.{CustomerService, CustomerServiceImpl, ShoppingListService, ShoppingListServiceImpl}
import com.google.inject.AbstractModule

class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[CustomerService]).to(classOf[CustomerServiceImpl])
    bind(classOf[ShoppingListService]).to(classOf[ShoppingListServiceImpl])
  }
}
