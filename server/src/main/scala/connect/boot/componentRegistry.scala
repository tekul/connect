package connect.boot

import javax.servlet._
import tools.nsc.Settings
import tools.nsc.interpreter.IMain
import javax.servlet.ServletContext
import scala.io.Source
import javax.script.ScriptException


trait ComponentRegistry

abstract class ComponentRegistryFilter[T <: ComponentRegistry] extends javax.servlet.Filter {
//  type C <: ComponentRegistry
  private var filter: Filter = null

  protected def delegate(cr: ComponentRegistry): Filter

  def init(fc: FilterConfig) {
    val cr = ComponentRegistry[T](fc.getServletContext())
    filter = delegate(cr)
  }

  def destroy() {}

  final def doFilter (request: ServletRequest, response: ServletResponse , chain: FilterChain) {
    filter.doFilter(request, response, chain)
  }
}


object ComponentRegistry {
  val Key = "_REGISTRY_"

  class ScriptResult(var value: Any)

  def apply[T <: ComponentRegistry](script: String, log: String => Unit): T = {
    require(script != null && script.length > 0)
    val settings = new Settings(log)
    settings.embeddedDefaults(Thread.currentThread.getContextClassLoader)
    settings.classpath.value = classpath
    log("Interpreter classpath is: " + settings.classpath.value)

    //settings.usejavacp.value = true
    val imain = new IMain(settings)
    log("Checking classloader")
    log("Classloader is: " + imain.classLoader)

    try {
      import scala.tools.nsc.interpreter.Results._

      val result = new ScriptResult(null)
      imain.addImports("connect._", "connect.boot._", "connect.boot.ComponentRegistry.ScriptResult")

      imain.bind("__result__", result)
      imain.interpret("__result__.value = {" + script + "}") match {
        case Success =>
          val cr = result.value
          require(cr != null && cr.isInstanceOf[T], "Script must return a non-null ComponentRegistry")
          cr.asInstanceOf[T]
        case Error => throw new ScriptException("Error executing script")
        case Incomplete => throw new ScriptException("Incomplete input while executing script")
      }
    } finally {
      log("Closing IMain...")
      imain.reset()
      imain.close()
    }
  }

  def apply[T <: ComponentRegistry] (ctx: ServletContext): T = {
    val cr = ctx.getAttribute(Key)

    require(cr != null && cr.isInstanceOf[T])


    cr.asInstanceOf[T]
  }

  private def classpath: String = {
    val cpUrls = Thread.currentThread.getContextClassLoader match {
      case cl: java.net.URLClassLoader => cl.getURLs.toList
      case _ => sys.error("classloader is not a URLClassLoader")
    }
    val classpath = cpUrls map {_.toString}
    classpath.distinct.mkString(java.io.File.pathSeparator)
  }
}


class ComponentRegistryListener extends ServletContextListener {

  def contextInitialized(sce: ServletContextEvent) {
    val ctx = sce.getServletContext

    val componentsClass = ctx.getInitParameter("component-registry-class")

    val componentRegistry: ComponentRegistry = if (componentsClass != null) {
      ctx.log("Loading ComponentRegistry " + componentsClass)
      Thread.currentThread().getContextClassLoader().loadClass(componentsClass).newInstance().asInstanceOf[ComponentRegistry]
    } else {
      val bootScript = ctx.getInitParameter("component-registry-script")

      require(bootScript != null, "'component-registry-script' or 'component-registry-class' must be supplied")
      ctx.log("Loading component registry from boot script " + bootScript)

      ComponentRegistry(Source.fromInputStream(ctx.getResourceAsStream(bootScript), "UTF-8").mkString, ctx.log(_))
    }

    ctx.setAttribute(ComponentRegistry.Key, componentRegistry)
  }

  def contextDestroyed(sce: ServletContextEvent) { }
}
