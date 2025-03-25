import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import sbt.*
import sbt.Keys.*
import sbt.Keys.libraryDependencies
import scala.collection.Seq
import xerial.sbt.Sonatype.GitHubHosting
import xerial.sbt.Sonatype.sonatypeCentralHost

ThisBuild / tlBaseVersion := "0.0"

// Dependencies versions
val versions = new {
    val scala           = "3.6.3"
    // common
    val fs2             = "3.11.0"
    val iron            = "2.6.0"
    val munit           = "1.1.0"
    val munitCatsEffect = "2.0.0"
    val sttpShared      = "1.5.0"
    val tapir           = "1.11.20"
    // backend
    val pillars         = "0.5.1"
    val postgres        = "42.7.5"
    val flyway          = "11.4.1"
    val circe           = "0.14.12"
    // frontend
    val scalaJSDom      = "2.8.0"
    val laminar         = "17.1.0"
    val rumpel          = "0.0.3"
    val http4s          = "0.23.30"
    val http4sDom       = "0.2.11"
    val sttp            = "3.10.3"
}

scalaVersion := versions.scala

ThisBuild / name                   := "mason"
ThisBuild / homepage               := Some(url(s"https://github.com/FunktionalIO/${(ThisBuild / name).value}"))
ThisBuild / description            := ""
ThisBuild / scalaVersion           := versions.scala
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

val sharedSettings = Seq(
    organization   := "io.funktional",
    scalaVersion   := versions.scala,
    libraryDependencies ++= Seq(
        "org.scalameta" %%% "munit" % versions.munit % Test
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
        libraryDependencies ++=
            Seq("iron", "iron-circe").map("io.github.iltotore" %%% _ % versions.iron) ++
                Seq("tapir-core", "tapir-json-circe", "tapir-iron").map(
                    "com.softwaremill.sttp.tapir" %%% _ % versions.tapir
                ) ++
                Seq("com.softwaremill.sttp.shared" %%% "fs2" % versions.sttpShared) ++
                Seq("co.fs2" %%% "fs2-core" % versions.fs2) ++
                Seq("circe-core", "circe-generic", "circe-parser").map(
                    "io.circe" %%% _ % versions.circe
                )
    )

lazy val backend = project
    .in(file("modules/backend"))
    .enablePlugins(BuildInfoPlugin)
    .enablePlugins(SbtTwirl)
    .enablePlugins(JavaAppPackaging)
    .dependsOn(common.jvm)
    .settings(sharedSettings)
    .settings(
        name                                   := s"${(ThisBuild / name).value}-backend",
        scalaVersion                           := versions.scala,
        buildInfoKeys                          := Seq[BuildInfoKey](name, version, description),
        buildInfoPackage                       := "mason.build",
        buildInfoOptions                       := Seq(BuildInfoOption.Traits("pillars.BuildInfo")),
        Compile / mainClass                    := Some("mason.Main"),
        libraryDependencies ++= Seq(
            "io.funktional"                %% "pillars-core"               % versions.pillars,
            "io.funktional"                %% "pillars-db-skunk"           % versions.pillars,
            "io.funktional"                %% "pillars-db-migration"       % versions.pillars,
            "io.funktional"                %% "pillars-flags"              % versions.pillars,
            "io.funktional"                %% "pillars-http-client"        % versions.pillars,
            "com.softwaremill.sttp.shared" %% "fs2"                        % versions.sttpShared,
            "co.fs2"                       %% "fs2-core"                   % versions.fs2,
            "co.fs2"                       %% "fs2-io"                     % versions.fs2,
            "org.postgresql"                % "postgresql"                 % versions.postgres,
            "org.flywaydb"                  % "flyway-database-postgresql" % versions.flyway,
            "io.circe"                     %% "circe-optics"               % "0.15.0",
            "org.scalameta"                %% "munit"                      % versions.munit           % Test,
            "org.typelevel"                %% "munit-cats-effect"          % versions.munitCatsEffect % Test
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
        scalaVersion                    := versions.scala,
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
            "org.scala-js"                  %%% "scalajs-dom"       % versions.scalaJSDom,
            "com.softwaremill.sttp.tapir"   %%% "tapir-sttp-client" % versions.tapir,
            "com.softwaremill.sttp.client3" %%% "core"              % versions.sttp,
            "com.raquo"                     %%% "laminar"           % versions.laminar,
            "io.funktional"                 %%% "rumpel"            % versions.rumpel,
            "io.github.iltotore"            %%% "iron"              % versions.iron,
            "io.github.iltotore"            %%% "iron-circe"        % versions.iron
        )
    )

lazy val root = project
    .in(file("."))
    .aggregate(backend, frontend)
    .settings(sharedSettings)
    .settings(
        name           := (ThisBuild / name).value,
        publish / skip := true
    )
