package cucumber.runtime

import java.util.{List => JList}
import gherkin.formatter.model.Step
import java.lang.reflect.Modifier
import snippets.SnippetGenerator
import scala.collection.JavaConversions._
import cucumber.io.{ClasspathResourceLoader, ResourceLoader}
import java.io.File
import collection.mutable
import org.reflections.Reflections

object HackedScalaBackend {
  var dynamicallyLoadedClasses:List[Class[_]] = Nil

  var hackedClassLoader = Thread.currentThread().getContextClassLoader
}
class HackedScalaBackend(someResources: ResourceLoader) extends Backend {
  private var snippetGenerator = new SnippetGenerator(new ScalaSnippetGenerator())
  private var instances: Seq[ScalaDsl] = Nil

  def getStepDefinitions = instances.flatMap(_.stepDefinitions)

  def getBeforeHooks = instances.flatMap(_.beforeHooks)

  def getAfterHooks = instances.flatMap(_.afterHooks)

  def disposeWorld() {
    instances = Nil
  }

  def getSnippet(step: Step) = snippetGenerator.getSnippet(step)

  def buildWorld() {
    //I don't believe scala has to do anything to clean out it's world
  }

  def loadGlue(glue: Glue, gluePaths: JList[String]) {
    val cl = new ClasspathResourceLoader(HackedScalaBackend.hackedClassLoader)
    val packages = gluePaths map {
      cucumber.io.MultiLoader.packageName(_)
    }
    println("packages > " + packages)
    val dslClasses = packages flatMap { pkg =>
      println("cl > " + cl)
      val decs = cl.getDescendants(classOf[ScalaDsl], pkg)
      println("decs >" + decs)
      decs
    } filter {
      cls =>
        try {
          cls.getDeclaredConstructor()
          true
        } catch {
          case e => false
        }
    }

    println("dslClasses > " + dslClasses)
    val (clsClasses, objClasses) = dslClasses partition {
      cls =>
        println("cls> "+cls)
        try {
          Modifier.isPublic(cls.getConstructor().getModifiers)
        } catch {
          case e => false
        }
    }
    val objInstances = objClasses map {
      cls =>
        val instField = cls.getDeclaredField("MODULE$")
        instField.setAccessible(true)
        instField.get(null).asInstanceOf[ScalaDsl]
    }
    println("dynamicallyLoadedClasses > " + HackedScalaBackend.dynamicallyLoadedClasses)
    val clsInstances = ((clsClasses ++ HackedScalaBackend.dynamicallyLoadedClasses) map {
      _.newInstance().asInstanceOf[ScalaDsl]
    })

    instances = objInstances ++ clsInstances

    getStepDefinitions map {
      glue.addStepDefinition(_)
    }
    getBeforeHooks map {
      glue.addBeforeHook(_)
    }
    getAfterHooks map {
      glue.addAfterHook(_)
    }
  }

  def setUnreportedStepExecutor(executor: UnreportedStepExecutor) {}
}
