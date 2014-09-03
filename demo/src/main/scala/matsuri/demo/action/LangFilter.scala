package matsuri.demo.action

import xitrum.{Action, i18n}


trait LangFilter {
  this: Action =>

  beforeFilter {
    autosetLanguage("en","ja")
    true
  }
}
