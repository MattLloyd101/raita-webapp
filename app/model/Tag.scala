package model

import anorm.Pk

case class Tag(id: Pk[Long], project:Long, name:String)

object Tag {

}
