package lang

object using {
  def apply[A <: {def close(): Unit}, B](obj: A)(f : A => B): B = {
    try {
      f(obj)
    } finally {
      obj.close()
    }
  }
}
