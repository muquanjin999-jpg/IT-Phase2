// @GENERATOR:play-routes-compiler
// @SOURCE:D:/work/IT-Phase2/ITSD-DT2025-26-Template/conf/routes
// @DATE:Fri Mar 06 14:33:07 GMT 2026


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
