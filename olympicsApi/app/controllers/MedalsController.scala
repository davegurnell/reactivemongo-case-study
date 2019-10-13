package controllers

import javax.inject.Inject
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

class MedalsController @Inject() (components: ControllerComponents) extends AbstractController(components) {
  def search(): Action[AnyContent] =
    Action {
      Ok("TODO")
    }
}
