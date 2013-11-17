import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "Clocen"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    javaJdbc,
    javaEbean,
    "mysql" % "mysql-connector-java" % "5.1.26",
    "commons-codec" % "commons-codec" % "1.8",
    "commons-io" % "commons-io" % "2.4",
    "org.apache.httpcomponents" % "httpclient" % "4.3.1",
    "org.apache.httpcomponents" % "httpmime" % "4.3.1",
	"javax.mail" % "mail" % "1.4.7"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    javacOptions += "-Xlint:deprecation"      
  )

}
