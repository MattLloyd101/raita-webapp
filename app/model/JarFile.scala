package model

import anorm.Pk

case class JarFile(id: Pk[Long], project: Long, filename: String)

object JarFile {

}
