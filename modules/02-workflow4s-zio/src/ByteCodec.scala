package workflows4s.anorm

import scala.util.Try

trait ByteCodec[Event] {

  def read(bytes:  IArray[Byte]): Try[Event]
  def write(event: Event): IArray[Byte]

}
