package cucumber.runtime

import gherkin.formatter.Formatter
import java.util.{ArrayList => JArrayList}

//TODO: Implement profiles.
class DynamicRuntimeOptions extends RuntimeOptions(System.getProperties, "") {
  formatters = new JArrayList[Formatter]
}
