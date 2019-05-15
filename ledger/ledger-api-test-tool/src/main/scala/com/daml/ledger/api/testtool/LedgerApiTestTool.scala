// Copyright (c) 2019 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.api.testtool

import java.io.File
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.digitalasset.daml.lf.UniversalArchiveReader
import com.digitalasset.daml.lf.data.Ref.PackageId
import com.digitalasset.daml.lf.lfpackage.{Ast, Decode}
import com.digitalasset.grpc.adapter.AkkaExecutionSequencerPool
import com.digitalasset.platform.PlatformApplications.RemoteApiEndpoint
import com.digitalasset.platform.common.LedgerIdMode
import com.digitalasset.platform.semantictest.SandboxSemanticTestsLfRunner
import com.digitalasset.platform.services.time.TimeProviderType
import com.digitalasset.platform.testing.LedgerBackend
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object LedgerApiTestToolHelper {
  def runWithTimeout[T](timeout: Duration)(f: => T)(implicit ec: ExecutionContext): Option[T] = {
    Await.result(Future(f), timeout).asInstanceOf[Option[T]]
  }

  def runWithTimeout[T](timeout: Duration, default: T)(f: => T)(
    implicit ec: ExecutionContext): T = {
    runWithTimeout(timeout)(f).getOrElse(default)
  }
}

object LedgerApiTestTool {

  def main(args: Array[String]): Unit = {
    implicit val toolSystem: ActorSystem = ActorSystem("LedgerApiTestTool")
    implicit val toolMaterializer: ActorMaterializer = ActorMaterializer()(toolSystem)
    implicit val ec: ExecutionContext = toolMaterializer.executionContext
    implicit val esf: AkkaExecutionSequencerPool =
      new AkkaExecutionSequencerPool("esf-" + this.getClass.getSimpleName)(toolSystem)

    val testResources = List("/ledger/ledger-api-integration-tests/SemanticTests.dar")

    val toolConfig = Cli
      .parse(args)
      .getOrElse(sys.exit(1))

    if (toolConfig.extract) {
      extractTestFiles(testResources)
      System.exit(0)
    }

    //    if (config.performReset) {
    //      Await.result(ledger.reset(), 10.seconds)
    //    }
    var failed = false

    try {


      val integrationTestResource = testResources.head
      val is = getClass.getResourceAsStream(integrationTestResource)
      if (is == null) sys.error(s"Could not find $integrationTestResource in classpath")
      val targetPath: Path = Files.createTempFile("ledger-api-test-tool-", "-test.dar")
      Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);

      val semanticTestsRunner = new SandboxSemanticTestsLfRunner {
        override def fixtureIdsEnabled: Set[LedgerBackend] =
          Set(LedgerBackend.RemoteApiProxy)

        override implicit lazy val patienceConfig: PatienceConfig = PatienceConfig(Span(60L, Seconds))

        override protected implicit def system: ActorSystem = toolSystem

        override protected implicit def materializer: ActorMaterializer = toolMaterializer

        override protected def config: Config =
          Config
            .default.withTimeProvider(TimeProviderType.WallClock)
            .withLedgerIdMode(LedgerIdMode.Dynamic())
            .withRemoteApiEndpoint(RemoteApiEndpoint.default.withHost(toolConfig.host).withPort(toolConfig.port).withTlsConfigOption(toolConfig.tlsConfig))
            .withDarFile(targetPath)
      }
      LedgerApiTestToolHelper.runWithTimeout(1.seconds) {
        Some(semanticTestsRunner.execute())
//          "Transaction Service when querying ledger end should return the value if ledger Ids match"
      }


    } catch {
      case (t: Throwable) =>
        failed = true
        if (!toolConfig.mustFail) throw t
    } finally {
      toolMaterializer.shutdown()
      val _ = Await.result(toolSystem.terminate(), 5.seconds)
    }

    if (toolConfig.mustFail) {
      if (failed) println("One or more scenarios failed as expected.")
      else
        throw new RuntimeException(
          "None of the scenarios failed, yet the --must-fail flag was specified!")
    }
  }

  private def loadAllPackagesFromResource(resource: String): Map[PackageId, Ast.Package] = {
    // TODO: replace with stream-supporting functions from UniversalArchiveReader when
    // https://github.com/digital-asset/daml/issues/547 is fixed
    val is = getClass.getResourceAsStream(resource)
    if (is == null) sys.error(s"Could not find $resource in classpath")
    val targetPath: Path = Files.createTempFile("ledger-api-test-tool-", "-test.dar")
    Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
    val f: File = targetPath.toFile
    if (f == null) sys.error(s"Could not open $targetPath")
    val packages = UniversalArchiveReader().readFile(f).get
    Map(packages.all.map {
      case (pkgId, pkgArchive) => Decode.readArchivePayloadAndVersion(pkgId, pkgArchive)._1
    }: _*)
  }

  private def extractTestFiles(testResources: List[String]): Unit = {
    val pwd = Paths.get(".").toAbsolutePath
    println(s"Extracting all DAML resources necessary to run the tests into $pwd.")
    testResources
      .foreach { n =>
        val is = getClass.getResourceAsStream(n)
        if (is == null) sys.error(s"Could not find $n in classpath")
        val targetFile = new File(new File(n).getName)
        Files.copy(is, targetFile.toPath, StandardCopyOption.REPLACE_EXISTING)
        println(s"Extracted $n to $targetFile")
      }
  }
}
