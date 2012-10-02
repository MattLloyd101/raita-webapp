package cucumber.runtime

import cucumber.io.ResourceLoader

object DynamicScalaBackend {
  var dynamicInstances:List[ScalaDsl] = Nil

}
class DynamicScalaBackend(loader: ResourceLoader) extends ScalaBackend(loader) {

  override def getStepDefinitions = super.getStepDefinitions ++ DynamicScalaBackend.dynamicInstances.flatMap(_.stepDefinitions)

  override def getBeforeHooks = super.getBeforeHooks ++ DynamicScalaBackend.dynamicInstances.flatMap(_.beforeHooks)

  override def getAfterHooks = super.getAfterHooks ++ DynamicScalaBackend.dynamicInstances.flatMap(_.afterHooks)

  override def disposeWorld() {
    super.disposeWorld()
    DynamicScalaBackend.dynamicInstances = Nil
  }
}
