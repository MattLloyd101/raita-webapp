package controllers

import play.api._
import play.api.mvc._
import cucumber.runtime._
import cucumber.io.{ResourceLoader, MultiLoader, ClasspathResourceLoader}
import java.net.{URL, URLClassLoader}
import java.io.{IOException, File}
import org.xeustechnologies.jcl.JarClassLoader

object Application extends Controller {
  def loadBackends(resourceLoader: ResourceLoader, classLoader: ClassLoader) = {
    val crl =  new ClasspathResourceLoader(classLoader)
    crl.instantiateSubclasses(Predef.classOf[Backend], "cucumber.runtime", Array(Predef.classOf[ResourceLoader]), Array(resourceLoader))
  }

  object using {

    def apply[A <: {def close(): Unit}, B](obj: A)(f : A => B): B = {
      try {
        f(obj)
      } finally {
        obj.close()
      }
    }
  }

  def addFile(s:String):ClassLoader = {
    addFile(new File(s))
  }

  def addFile(f:File):ClassLoader = {
    addURL(f.toURL())
  }

  def addURL(u:URL):ClassLoader = {
//    val sysloader = ClassLoader.getSystemClassLoader().asInstanceOf[URLClassLoader]
//    val sysclass = Predef.classOf[URLClassLoader]
//    val parameters = Predef.classOf[URL]
//    println(u)
//    try {
//      val method = sysclass.getDeclaredMethod("addURL", parameters)
//      method.setAccessible(true)
//      method.invoke(sysloader, u)
//    } catch {
//      case t =>
//      t.printStackTrace()
//      throw new IOException("Error, could not add URL to system classloader")
//    }
    URLClassLoader.newInstance(Array(u), Thread.currentThread.getContextClassLoader)
  }


  def runCucumber = {
    val argv: Array[String] = Array("test.feature")
    val runtimeOptions: RuntimeOptions = new RuntimeOptions(System.getProperties, argv:_*)
    runtimeOptions.glue.add("steps")

    val classLoader = addFile("test-features-hacked.jar")
    val classpath = new MultiLoader(classLoader)

    val tmp = loadBackends(classpath, classLoader)
    println("wat > ")
    tmp.toArray.foreach { item =>
      try {
        val backend = item.asInstanceOf[HackedScalaBackend]

        println("item > " + backend)
        println("item > " + backend.getStepDefinitions)
        backend.getStepDefinitions.foreach { step =>
          println("step > " + step.toString)
        }
      } catch {
        case _ =>
      }
    }


    val runtime: Runtime = new Runtime(classpath, classLoader, runtimeOptions)

    runtime.writeStepdefsJson
    runtime.run
  }
  
  def index = Action {
    runCucumber

    Ok(views.html.index("Your new application is ready."))
  }
  
}