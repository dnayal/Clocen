import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "ServiceNode"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    javaJdbc,
    javaEbean,
    "mysql" % "mysql-connector-java" % "5.1.26",
    "commons-codec" % "commons-codec" % "1.8",
	"javax.mail" % "mail" % "1.4.7"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
