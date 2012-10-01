import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "raita-webapp"
    val appVersion      = "1.0-SNAPSHOT"

    val sonatypeRepo = "Sonatype Central M1" at "https://repository.sonatype.org/content/shadows/centralm1"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "info.cukes" % "cucumber-scala" % "1.0.9"
      //"org.xeustechnologies" % "jcl-core" % "2.2.2"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
