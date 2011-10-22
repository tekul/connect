package connect.util

import scala.tools.nsc.interpreter.IMain
import scala.tools.nsc.Settings
import unfiltered.request._
import unfiltered.response._
import java.io._
import connect.Logger
import connect.boot.ComponentRegistry

class ReplPlan (components: ComponentRegistry) extends unfiltered.filter.Plan with Logger {
  logger.info("Creating ReplPlan")

  class ReplStream(val baos: ByteArrayOutputStream) extends PrintStream(baos) {
    def text = baos.toString()
    def clear() = { val content=text; baos.reset(); content }
  }

  val out = new ReplStream(new ByteArrayOutputStream)

  lazy val interpreter = {
    logger.info("Initializing interpreter")
    val urls = java.lang.Thread.currentThread.getContextClassLoader match {
      case cl: java.net.URLClassLoader => cl.getURLs.toList
      case _ => sys.error("classloader is not a URLClassLoader")
    }
    val classpath = urls map { _.toString }
    logger.info("Classpath: " + classpath)

    val s = new Settings(out.println(_))
    s.classpath.value = classpath.distinct.mkString(java.io.File.pathSeparator)

    val cl = Thread.currentThread.getContextClassLoader()
    System.setProperty("scala.repl.debug", "true")
    val i = new IMain(s, new PrintWriter(out)) {
      override def parentClassLoader = cl
    }
    logger.info("Binding component registry" + components)
    i.addImports("connect._")
    i.addImports("connect.openid._")
    i.addImports("connect.tokens._")
    val res = i.bind("cr", components)
    logger.info("Result: " + res)
    i.initialize()
    i
  }

  def intent = {
    case Params(p) =>
      logger.info("Received request with parameters:" + p)
      p("interpret") match {
        case Seq(s) =>
          logger.info("Received command:" + s)
          interpreter.interpret(s)
          ResponseString(out.clear())
        case _ => BadRequest ~> ResponseString("Only 'interpret' commands are accepted")
      }
    case r => BadRequest ~> ResponseString(r.method + " " + r.uri)
  }
}
