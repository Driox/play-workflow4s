package effect

import _root_.zio.ZIO

package object sorus {

  type Sorus[R, A] = ZIO[R, Fail, A]

}
