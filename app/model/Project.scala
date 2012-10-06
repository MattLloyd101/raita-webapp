package model

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current


case class Project(id: Pk[Long], name: String)

object Project {

  val projectParse = {
    get[Pk[Long]]("project.id") ~
    get[String]("project.name") map {
      case id ~ name => Project(id, name)
    }
  }

  def findById(id:Long):Option[Project] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT id, name from project where id = {id}").on(
        'id -> id
      ).as(projectParse.singleOpt)
    }
  }
}
