package controllers

import play.api.mvc._
import cucumber.runtime._
import cucumber.io._
import scalax.io.{Resource => SResource}
import java.net.{URL, URLClassLoader}
import java.io.File
import java.util.zip.{ZipEntry, ZipFile}
import collection.JavaConversions._

object Application extends Controller {
  def loadBackends(resourceLoader: ResourceLoader, classLoader: ClassLoader) = {
    val crl = new ClasspathResourceLoader(classLoader)
    crl.instantiateSubclasses(Predef.classOf[Backend], "cucumber.runtime", Array(Predef.classOf[ResourceLoader]), Array(resourceLoader))
  }

  object using {

    def apply[A <: {def close() : Unit}, B](obj: A)(f: A => B): B = {
      try {
        f(obj)
      } finally {
        obj.close()
      }
    }
  }

  def addFile(s: String): ClassLoader = {
    addFile(new File(s))
  }

  def addFile(f: File): ClassLoader = {
    addURL(f.toURL())
  }

  def addURL(u: URL): ClassLoader = {
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

  def index = Action {

    class ZipLoader extends ClassLoader(Thread.currentThread().getContextClassLoader) {
      def loadEntry(zf: ZipFile, entry: ZipEntry) = {
        val clsName = entry.getName.replace("/", ".").dropRight(6)
        val data = SResource.fromInputStream(zf.getInputStream(entry)).byteArray
        defineClass(clsName, data, 0, data.length)
      }
    }

    def loadFromJar(path: String, zipLoader:ZipLoader, stepPkg:String) {
      using(new ZipFile(new File(path), ZipFile.OPEN_READ)) { zf =>
          val enum = zf.entries()
          val tmpItr = new Iterator[ZipEntry]() {
            def hasNext = enum.hasMoreElements
            def next() = enum.nextElement()
          }

          val Feature = """^.*\.feature$""".r
          val Cls = """^%s/.*\.class$""".format(stepPkg).r
          val (features, classes) = tmpItr.foldLeft((List[ZipEntry](), List[ZipEntry]())) {
            (out, entry) =>
              entry.getName match {
                case Feature() => (entry :: out._1, out._2)
                case Cls() =>
                  println("Matched > " + entry.getName)
                  (out._1, entry :: out._2)
                case _ => out
              }
          }

          def isScalaDsl(cls:Class[_]): Boolean = List(cls.getInterfaces:_*).exists { _ == classOf[ScalaDsl] }

          //TODO: Handle Objects with MODULE$
          DynamicScalaBackend.dynamicInstances = (classes.map { cls => zipLoader.loadEntry(zf, cls) }).filter(isScalaDsl).map { _.newInstance().asInstanceOf[ScalaDsl] }
      }
    }

    def runCucumber(zipLoader:ZipLoader) = {
      val argv: Array[String] = Array("test.feature")
      val runtimeOptions: RuntimeOptions = new RuntimeOptions(System.getProperties, argv: _*)

      loadFromJar("temp-features_2.9.1-1.0-SNAPSHOT.jar", zipLoader, "steps")
      val classLoader = zipLoader

      val runtime: Runtime = new Runtime(new MultiLoader(classLoader), classLoader, runtimeOptions)

      runtime.writeStepdefsJson
      runtime.run
    }

    runCucumber(new ZipLoader)

    Ok(views.html.index("Your new application is ready."))
  }

}