package connect

import org.slf4j.LoggerFactory

trait Logger {
  val logger = LoggerFactory.getLogger(getClass)
}
