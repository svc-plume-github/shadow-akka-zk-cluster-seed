import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm

organization := "com.sclasen"
name := "akka-zk-cluster-seed"
version := "0.1.11-SNAPSHOT"

scalaVersion := "2.12.10"

val akkaVersion = "2.5.25"
val akkaHttpVersion = "10.1.10"

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion
)

val exhibitorOptionalDependencies = Seq(
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "org.slf4j" % "log4j-over-slf4j" % "1.7.28",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
).map(_ % Provided)

val curatorVersion = "4.1.0"
val zookeeperV = "3.4.13"

val zkDependencies = Seq(
"org.apache.curator" % "curator-framework" % curatorVersion exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-log4j12") exclude("org.apache.zookeeper", "zookeeper"),
"org.apache.curator" % "curator-recipes" % curatorVersion exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-log4j12") exclude("org.apache.zookeeper", "zookeeper"),
"org.apache.zookeeper" % "zookeeper" % zookeeperV
)

val testDependencies = Seq(
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.1",
  "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "org.slf4j" % "log4j-over-slf4j" % "1.7.7",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.apache.curator" % "curator-test" % curatorVersion
).map(_ % Test)

lazy val rootProject = (project in file(".")).
  settings(
    libraryDependencies ++= (akkaDependencies ++ exhibitorOptionalDependencies ++ zkDependencies ++ testDependencies),
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint", "-language:postfixOps"),
    javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    parallelExecution in Test := false,

    pomExtra := (
      <url>http://github.com/sclasen/akka-zk-cluster-seed</url>
      <licenses>
        <license>
          <name>The Apache Software License, Version 2.0</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:sclasen/akka-zk-cluster-seed.git</url>
        <connection>scm:git:git@github.com:sclasen/akka-zk-cluster-seed.git</connection>
      </scm>
      <developers>
        <developer>
          <id>sclasen</id>
          <name>Scott Clasen</name>
          <url>http://github.com/sclasen</url>
        </developer>
      </developers>),
    publishConfiguration := publishConfiguration.value.withOverwrite(false),
    publishArtifact in Test := false,
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    publishTo := {
      val nexus = "https://artifactory-artifactory-01.inf.us-west-2.aws.plume.tech/"
      if (isSnapshot.value) {
        Some("snapshots" at nexus + "artifactory/akka-zk-cluster-seed/snapshots")
      } else {
        Some("releases"  at nexus + "artifactory/akka-zk-cluster-seed/releases")
      }
    }
  ).
  settings(Defaults.itSettings:_*).
  settings(SbtMultiJvm.multiJvmSettings:_*).
  settings(compile in MultiJvm := ((compile in MultiJvm) triggeredBy (compile in IntegrationTest)).value).
  settings(executeTests in IntegrationTest := {
    val testResults = (executeTests in Test).value
    val multiNodeResults = (executeTests in MultiJvm).value
    val overall = getOverallTestResult(testResults.overall, multiNodeResults.overall)
    Tests.Output(overall,
      testResults.events ++ multiNodeResults.events,
      testResults.summaries ++ multiNodeResults.summaries)
  }).
  configs(IntegrationTest, MultiJvm)

def getOverallTestResult(a: TestResult, b: TestResult): TestResult = {
  if (a == TestResult.Error || b == TestResult.Error) TestResult.Error
  else if (a == TestResult.Failed || b == TestResult.Failed) TestResult.Failed
  else TestResult.Passed
}
