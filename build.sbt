import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

scalaVersion in ThisBuild := "2.12.6"

lazy val server = (project in file("server")).settings(
  name := "scala-gamecenter",
  version := "0.1",
  scalaJSProjects := Seq(client),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % "10.1.1",
    "com.typesafe.akka" %% "akka-stream" % "2.5.14",
    "com.vmunier" %% "scalajs-scripts" % "1.1.2",
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "org.webjars" % "bootstrap" % "3.3.6" % Provided
  ),
  WebKeys.packagePrefix in Assets := "public/",
  managedClasspath in Runtime += (packageBin in Assets).value,
)
  .enablePlugins(SbtWeb, JavaAppPackaging, SbtTwirl)
  .dependsOn(sharedJvm)


lazy val client = (project in file("client")).settings(
  scalaJSUseMainModuleInitializer := true,
  libraryDependencies ++= Seq(
    "com.github.japgolly.scalajs-react" %%% "core" % "1.2.3",
    "org.scala-js" %%% "scalajs-dom" % "0.9.2",
    "com.github.julien-truffaut" %%% "monocle-core" % "1.4.0",
    "com.github.julien-truffaut" %%% "monocle-macro" % "1.4.0"
  ),
  dependencyOverrides += "org.webjars.npm" % "js-tokens" % "3.0.2",
  jsDependencies ++= Seq(
    "org.webjars.npm" % "react" % "16.2.0"
      / "umd/react.development.js"
      minified "umd/react.production.min.js"
      commonJSName "React",

    "org.webjars.npm" % "react-dom" % "16.2.0"
      / "umd/react-dom.development.js"
      minified "umd/react-dom.production.min.js"
      dependsOn "umd/react.development.js"
      commonJSName "ReactDOM",

    "org.webjars.npm" % "react-dom" % "16.2.0"
      / "umd/react-dom-server.browser.development.js"
      minified "umd/react-dom-server.browser.production.min.js"
      dependsOn "umd/react-dom.development.js"
      commonJSName "ReactDOMServer"
  )
)
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(sharedJs)

lazy val shared = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("shared"))
  .jsConfigure(_ enablePlugins ScalaJSWeb)
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "autowire" % "0.2.6",
      "io.suzaku" %%% "boopickle" % "1.3.0"
    )
  )

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js
