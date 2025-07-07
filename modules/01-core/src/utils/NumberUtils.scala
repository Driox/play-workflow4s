package utils

object NumberUtils {

  def random(bound: Long = Long.MaxValue): Long = {
    val random = new java.util.Random()
    random.nextLong(bound)
  }
}
