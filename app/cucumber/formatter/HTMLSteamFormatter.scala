package cucumber.formatter

import cucumber.runtime.CucumberException
import gherkin.deps.com.google.gson.Gson
import gherkin.deps.com.google.gson.GsonBuilder
import gherkin.formatter.Formatter
import gherkin.formatter.Mappable
import gherkin.formatter.NiceAppendable
import gherkin.formatter.Reporter
import gherkin.formatter.model.Background
import gherkin.formatter.model.Examples
import gherkin.formatter.model.Feature
import gherkin.formatter.model.Match
import gherkin.formatter.model.Result
import gherkin.formatter.model.Scenario
import gherkin.formatter.model.ScenarioOutline
import gherkin.formatter.model.Step
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.UnsupportedEncodingException
import java.util.HashMap
import scala.Predef.String

object HTMLSteamFormatter {
  private final val gson: Gson = new GsonBuilder().setPrettyPrinting.create
  private final val JS_FORMATTER_VAR = "formatter"
  private final val JS_REPORT_FILENAME = "report.js"
  private final val TEXT_ASSETS = List("formatter.js", "index.html", "jquery-1.6.4.min.js", "style.css")
  private final val MIME_TYPES_EXTENSIONS = new HashMap { }
}

class HTMLSteamFormatter(var htmlReportDir: File, stream:OutputStream) extends Formatter with Reporter {

  def uri(uri: String) {
    if (firstFeature) {
      jsOut.append("$(document).ready(function() {").append("var ").append(HTMLSteamFormatter.JS_FORMATTER_VAR).append(" = new CucumberHTML.DOMFormatter($('.cucumber-report'));")
      firstFeature = false
    }
    writeToJsReport("uri", "'" + uri + "'")
  }

  def feature(feature: Feature) {
    writeToJsReport("feature", feature)
  }

  def background(background: Background) {
    writeToJsReport("background", background)
  }

  def scenario(scenario: Scenario) {
    writeToJsReport("scenario", scenario)
  }

  def scenarioOutline(scenarioOutline: ScenarioOutline) {
    writeToJsReport("scenarioOutline", scenarioOutline)
  }

  def examples(examples: Examples) {
    writeToJsReport("examples", examples)
  }

  def step(step: Step) {
    writeToJsReport("step", step)
  }

  def eof {
  }

  def syntaxError(state: String, event: String, legalEvents: java.util.List[String], uri: String, line: Integer) {
  }

  def done {
    if (!firstFeature) {
      jsOut.append("});")
      copyReportFiles
    }
  }

  def close {
    jsOut.close
  }

  private def writeToJsReport(functionName: String, statement: Mappable) {
    val json: String = HTMLSteamFormatter.gson.toJson(statement.toMap)
    writeToJsReport(functionName, json)
  }

  private def writeToJsReport(functionName: String, arg: String) {
    jsOut.append(HTMLSteamFormatter.JS_FORMATTER_VAR + ".").append(functionName).append("(").append(arg).append(");").println
  }

  def result(result: Result) {
    writeToJsReport("result", result)
  }

  def before(`match`: Match, result: Result) {
    writeToJsReport("before", result)
  }

  def after(`match`: Match, result: Result) {
    writeToJsReport("after", result)
  }

  def `match`(`match`: Match) {
    writeToJsReport("match", `match`)
  }

  def embedding(mimeType: String, data: InputStream) {
    val extension: String = HTMLSteamFormatter.MIME_TYPES_EXTENSIONS.get(mimeType)
    if (extension != null) {
      val fileName: StringBuilder = new StringBuilder("embedded").append(({
        embeddedIndex += 1
        embeddedIndex - 1
      })).append(".").append(extension)
      writeBytes(data, reportFileOutputStream(fileName.toString))
      writeToJsReport("embedding", new StringBuilder("'").append(mimeType).append("','").append(fileName).append("'").toString)
    }
  }

  def write(text: String) {
    writeToJsReport("write", HTMLSteamFormatter.gson.toJson(text))
  }

  private def copyReportFiles {
    for (textAsset <- HTMLSteamFormatter.TEXT_ASSETS) {
      val textAssetStream: InputStream = getClass.getResourceAsStream(textAsset)
      writeBytes(textAssetStream, reportFileOutputStream(textAsset))
    }
  }

  private def writeBytes(in: InputStream, out: OutputStream) {
    val buffer: Array[Byte] = new Array[Byte](16 * 1024)
    try {
      var len: Int = in.read(buffer)
      while (len != -1) {
        out.write(buffer, 0, len)
        len = in.read(buffer)
      }
      out.close
    }
    catch {
      case e: IOException => {
        throw new CucumberException("Unable to write to report file item: ", e)
      }
    }
  }

  lazy val jsOut: NiceAppendable = {
      try {
        new NiceAppendable(new OutputStreamWriter(reportFileOutputStream(HTMLSteamFormatter.JS_REPORT_FILENAME), "UTF-8"))
      }
      catch {
        case e: UnsupportedEncodingException => {
          throw new CucumberException(e)
        }
      }
    }

  private def reportFileOutputStream(fileName: String): OutputStream = {
    stream
  }

  private var firstFeature: Boolean = true
  private var embeddedIndex: Int = 0
}

