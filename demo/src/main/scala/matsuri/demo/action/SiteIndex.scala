package matsuri.demo.action

import xitrum.annotation.GET

@GET("")
class SiteIndex extends DefaultLayout {
  def execute() {
    respondView()
  }
}
