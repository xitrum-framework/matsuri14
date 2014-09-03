package matsuri.demo.action

import xitrum.Action

trait DefaultLayout extends Action with LangFilter {
  override def layout = renderViewNoLayout[DefaultLayout]()
}
