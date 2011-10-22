package connect
import org.scalatest.junit.JUnitSuite
import org.junit.Test
import connect.boot.ComponentRegistry

class ComponentRegistryTests extends JUnitSuite {

  class MyCR extends ComponentRegistry

  @Test def componentRegistryCanBeExtractedFromScript() {
    val script = """new ConnectComponentRegistry"""

    val cr = ComponentRegistry[ConnectComponentRegistry](script, (x: String) => println(x))
  }
}