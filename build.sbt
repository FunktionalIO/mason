import org.scalajs.linker.interface.ModuleSplitStyle
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import sbt.*
import sbt.Keys.*
import scala.collection.Seq
import xerial.sbt.Sonatype.GitHubHosting
import xerial.sbt.Sonatype.sonatypeCentralHost

ThisBuild / tlBaseVersion := "0.0"
val scala3Version = "3.5.2"

ThisBuild / name                   := "mason"
ThisBuild / homepage               := Some(url(s"https://github.com/FunktionalIO/${(ThisBuild / name).value}"))
ThisBuild / description            := ""
ThisBuild / scalaVersion           := scala3Version
ThisBuild / organization           := "io.funktional"
ThisBuild / organizationName       := "Funktional"
ThisBuild / organizationHomepage   := Some(url("https://funktional.io"))
ThisBuild / startYear              := Some(2024)
ThisBuild / licenses               := Seq("EPL-2.0" -> url("https://opensource.org/licenses/EPL-2.0"))
ThisBuild / developers             := List(
  Developer(
    id = "rlemaitre",
    name = "Raphaël Lemaitre",
    email = "github.com.lushly070@passmail.net",
    url = url("https://rlemaitre.com")
  )
)
// Publication
ThisBuild / sonatypeCredentialHost := sonatypeCentralHost
ThisBuild / sonatypeProjectHosting := Some(GitHubHosting(
  "FunktionalIO",
  (ThisBuild / name).value,
  "github.com.lushly070@passmail.net"
))
ThisBuild / scmInfo                := Some(
  ScmInfo(
    url(s"https://github.com/FunktionalIO/${(ThisBuild / name).value}"),
    s"scm:git:git@github.com:FunktionalIO/${(ThisBuild / name).value}.git"
  )
)

// Github actions
ThisBuild / githubWorkflowOSes         := Seq("ubuntu-latest")
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"))

// Dependencies versions
val versions = new {
    val munit = "1.0.3"
}

val sharedSettings = Seq(
  organization   := "io.funktional",
  scalaVersion   := "3.3.4",
  libraryDependencies ++= Seq(
    "org.scalameta" %%% "munit" % "1.0.2" % Test
  ),
  // Headers
  headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
  headerLicense  := Some(HeaderLicense.Custom(
    """|Copyright (c) 2024-2024 by Raphaël Lemaitre and Contributors 
           |This software is licensed under the Eclipse Public License v2.0 (EPL-2.0).
           |For more information see LICENSE or https://opensource.org/license/epl-2-0
           |""".stripMargin
  ))
)

lazy val common = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("modules/common"))
    .settings(sharedSettings)
    .settings(
      name := s"${(ThisBuild / name).value}-common",
      libraryDependencies ++= Seq("io.github.iltotore" %%% "iron" % "2.6.0")
    )

lazy val backend = project
    .in(file("modules/backend"))
    .enablePlugins(BuildInfoPlugin)
    .dependsOn(common.jvm)
    .settings(sharedSettings)
    .settings(
      name                                   := s"${(ThisBuild / name).value}-backend",
      scalaVersion                           := scala3Version,
      buildInfoKeys                          := Seq[BuildInfoKey](name, version, description),
      buildInfoPackage                       := "mason.build",
      buildInfoOptions                       := Seq(BuildInfoOption.Traits("pillars.BuildInfo")),
      libraryDependencies ++= Seq(
        "com.rlemaitre" %% "pillars-core" % "0.3.23",
        "org.scalameta" %% "munit"        % versions.munit % Test
      ),
      libraryDependencySchemes += "io.circe" %% "circe-yaml" % VersionScheme.Always
    )

lazy val frontend = project
    .in(file("modules/frontend"))
    .enablePlugins(ScalaJSPlugin, SbtVitePlugin)
    .dependsOn(common.js)
    .settings(sharedSettings)
    .settings(
      name                            := s"${(ThisBuild / name).value}-frontend",
      scalaJSUseMainModuleInitializer := true,
      viteOtherSources                := Seq(
        Location.FromProject(file("src/main/entrypoint")),
        Location.FromProject(file("src/main/styles"))
      ),
      scalaJSLinkerConfig ~= {
          _.withModuleKind(ModuleKind.CommonJSModule)
//              .withModuleSplitStyle(
//                ModuleSplitStyle.SmallModulesFor(List("main"))
//              )
      },
      libraryDependencies ++= Seq(
        "org.scala-js"       %%% "scalajs-dom" % "2.8.0",
        "com.raquo"          %%% "laminar"     % "17.1.0",
        "io.github.iltotore" %%% "iron"        % "2.6.0"
      )
    )

lazy val root = project
    .in(file("."))
    .aggregate(backend)
    .settings(sharedSettings)
    .settings(
      name           := (ThisBuild / name).value,
      publish / skip := true
    )
