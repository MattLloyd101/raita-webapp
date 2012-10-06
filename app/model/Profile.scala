package model

import anorm.Pk

case class Profile(id: Pk[Long], project: Long, name:String)

object Profile {

}
