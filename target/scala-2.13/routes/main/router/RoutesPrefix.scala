// @GENERATOR:play-routes-compiler
// @SOURCE:D:/work/IT-Phase2/ITSD-DT2025-26-Template/conf/routes
// @DATE:Sat Feb 28 20:58:30 GMT 2026


package router {
  object RoutesPrefix {
    private var _prefix: String = "/"
    def setPrefix(p: String): Unit = {
      _prefix = p
    }
    def prefix: String = _prefix
    val byNamePrefix: Function0[String] = { () => prefix }
  }
}
