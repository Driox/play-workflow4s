package utils

import java.security.SecureRandom
import scala.util.Random

object StringUtils {

  // Random Generator
  private val random = new SecureRandom()

  // Generate a random alphabnumeric string of length n
  def randomAlphanumericString(n: Int): String = Random(random).alphanumeric.take(n).mkString
}
