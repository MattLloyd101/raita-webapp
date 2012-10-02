package controllers

import play.api.mvc._
import cucumber.runtime._
import cucumber.io._
import model.CucumberFeature
import scalax.io.{Resource => SResource}
import java.io.{ByteArrayOutputStream, PipedInputStream, PipedOutputStream, File}
import java.util.zip.{ZipEntry, ZipFile}
import lang.using
import gherkin.formatter.{Formatter, Reporter}
import cucumber.formatter.HTMLSteamFormatter
import play.api.libs.iteratee.Enumerator
import scala.collection.JavaConversions._
import java.util

object Application extends Controller {

  def loadFromJar(path: String, parentLoader:ClassLoader, stepPkg:String) {
    class ZipLoader(zf: ZipFile) extends ClassLoader(parentLoader) {
      def loadEntry(entry: ZipEntry) = {
        val clsName = entry.getName.replace("/", ".").dropRight(6)
        val data = SResource.fromInputStream(zf.getInputStream(entry)).byteArray
        defineClass(clsName, data, 0, data.length)
      }
    }

    using(new ZipFile(new File(path), ZipFile.OPEN_READ)) { zf =>
      val enum = zf.entries()
      val tmpItr = new Iterator[ZipEntry]() {
        def hasNext = enum.hasMoreElements
        def next() = enum.nextElement()
      }

      val Feature = """^.*\.feature$""".r
      val Cls = """^.*\.class$""".r
      val (features, classes) = tmpItr.foldLeft((List[ZipEntry](), List[ZipEntry]())) {
        (out, entry) =>
          entry.getName match {
            case Feature() => (entry :: out._1, out._2)
            case Cls() => (out._1, entry :: out._2)
            case _ => out
          }
      }

      def isScalaDsl(cls:Class[_]): Boolean = List(cls.getInterfaces:_*).exists { _ == classOf[ScalaDsl] }

      //TODO: Handle Objects with MODULE$
      val zipLoader = new ZipLoader(zf)
      DynamicScalaBackend.dynamicInstances = (classes.map { cls => zipLoader.loadEntry(cls) }).filter(isScalaDsl).map { _.newInstance().asInstanceOf[ScalaDsl] }
    }
  }

  def runCucumber = {
    val argv: Array[String] = Array("test.feature")
    val classLoader = Thread.currentThread().getContextClassLoader
    loadFromJar("temp-features_2.9.1-1.0-SNAPSHOT.jar", classLoader, "steps")

    val runtimeOptions: RuntimeOptions = new RuntimeOptions(System.getProperties, argv: _*)
    val resourceLoader = new MultiLoader(classLoader)
    val runtime: Runtime = new Runtime(resourceLoader, classLoader, runtimeOptions)
    runtimeOptions.formatters = new util.ArrayList[Formatter]
    val outstr = new PipedOutputStream
    val instr = new PipedInputStream(outstr)
    val buff = new ByteArrayOutputStream()
    using(new HTMLSteamFormatter(buff)) { formatter =>
      val reporter: Reporter = runtimeOptions.reporter(classLoader)
      for(cucumberFeature <- runtimeOptions.cucumberFeatures(resourceLoader).toList)
        cucumberFeature.run(formatter, reporter, runtime)

      formatter.done

      Ok(views.html.cucumberOutput("Test.js"))
    }
  }

  def index = Action {
    runCucumber
  }

}