package mason

import cats.effect.*
import cats.syntax.all.*
import fs2.Pipe
import fs2.Stream
import fs2.io.readOutputStream
import fs2.io.writeOutputStream
import io.github.iltotore.iron.*
import java.nio.file.StandardOpenOption.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import pillars.Pillars

object generator:
    case class FileEntry(name: FileName, data: Stream[IO, Byte]):
        def asZipEntry: ZipEntry = new ZipEntry(name)
    end FileEntry

    def zipPipe(chunkSize: Int = 4096): Pipe[IO, FileEntry, Byte] = (files: Stream[IO, FileEntry]) =>
        readOutputStream[IO](chunkSize): outputStream =>
            Resource.fromAutoCloseable(IO(new ZipOutputStream(outputStream))).use: zipOut =>
                val writeOutput = writeOutputStream[IO](zipOut.pure[IO], closeAfterUse = false)
                files
                    .evalMap: (archive: FileEntry) =>
                        for
                            _ <- IO(zipOut.putNextEntry(archive.asZipEntry))
                            _ <- archive.data.through(writeOutput).compile.drain
                            _ <- IO(zipOut.closeEntry())
                        yield ()
                    .compile.drain

    def generateFiles(project: Project, config: Config)(using Pillars[IO]): Stream[IO, FileEntry] =
        val srcDir           = s"src/main/scala/${project.packagePath}"
        val restDir          = s"src/main/rest"
        val testDir          = s"src/test/scala/${project.packagePath}"
        val resourcesDir     = s"src/main/resources/"
        val observabilityDir = "observability"
        Stream.emits:
            val commonFiles    = Map(
                ".gitignore"                                                 -> txt.gitignore(),
                ".scalafmt.conf"                                             -> txt.scalafmtconf(),
                "build.sbt"                                                  -> txt.buildsbt(project, config),
                "index.html"                                                 -> txt.index_html(project),
                "README.md"                                                  -> txt.readme(project),
                "project/plugins.sbt"                                        -> txt.pluginssbt(),
                s"$observabilityDir/otel-collector-config.yaml"              -> txt.otel_collector_config_yaml(),
                s"$observabilityDir/clickhouse/cluster.xml"                  -> txt.ch_cluster_xml(),
                s"$observabilityDir/clickhouse/config.xml"                   -> txt.ch_config_xml(),
                s"$observabilityDir/clickhouse/custom-function.xml"          -> txt.ch_custom_function_xml(),
                s"$observabilityDir/clickhouse/storage.xml"                  -> txt.ch_storage_xml(),
                s"$observabilityDir/clickhouse/users.xml"                    -> txt.ch_users_xml(),
                s"$observabilityDir/clickhouse/user_scripts/.gitkeep"        -> txt.gitkeep(),
                s"$observabilityDir/signoz/prometheus.yml"                   -> txt.signoz_prometheus_yml(),
                s"$observabilityDir/signoz/otel-collector-opamp-config.yaml" -> txt
                    .signoz_otel_collector_opamp_config_yaml(),
                s"$restDir/admin.http"                                       -> txt.adminhttp(project),
                s"$restDir/app.http"                                         -> txt.apphttp(project),
                s"$srcDir/codec.scala"                                       -> txt.codec(project),
                s"$srcDir/Config.scala"                                      -> txt.Config(project),
                s"$srcDir/controllers.scala"                                 -> txt.controllers(project),
                s"$srcDir/endpoints.scala"                                   -> txt.endpoints(project),
                s"$srcDir/errors.scala"                                      -> txt.errors(project),
                s"$srcDir/Main.scala"                                        -> txt.Main(project),
                s"$srcDir/Metrics.scala"                                     -> txt.Metrics(project),
                s"$srcDir/model.scala"                                       -> txt.model(project),
                s"$srcDir/services.scala"                                    -> txt.services(project),
                s"$resourcesDir/config.yaml"                                 -> txt.configyaml(project),
                s"$testDir/ServiceSuite.scala"                               -> txt.ServicesSuite(project)
            )
            val dockerFiles    =
                if project.generateDocker then Map("docker-compose.yml" -> txt.dockercomposeyaml(project, config))
                else Map.empty
            val migrationFiles =
                if project.hasModule(Module.DBMigration) then
                    Map(
                        s"$resourcesDir/db-migrations/V1__init.sql" -> txt.initdbsql()
                    )
                else Map.empty
            (commonFiles ++ dockerFiles ++ migrationFiles)
                .map: (name, content) =>
                    FileEntry(name.assume, Stream.emit(content.body).through(fs2.text.utf8.encode))
                .toSeq

    end generateFiles
end generator
