// Copyright (c) 2019 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.digitalasset.ledger.api.testing.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.Await
import scala.concurrent.duration._

trait AkkaBeforeAndAfterAll extends BeforeAndAfterAll { self: Suite =>

  protected implicit def system: ActorSystem = ActorSystem(this.getClass.getSimpleName)

  protected implicit def materializer: ActorMaterializer = ActorMaterializer()

  override protected def afterAll(): Unit = {
    materializer.shutdown()
    Await.result(system.terminate(), 30.seconds)
    super.afterAll()
  }
}
