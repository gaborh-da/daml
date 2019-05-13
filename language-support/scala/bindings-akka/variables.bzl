# Copyright (c) 2019 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0

bindings_akka_deps = depset([
    "//3rdparty/jvm/ch/qos/logback:logback_classic",
    "//3rdparty/jvm/com/github/pureconfig",
    "//3rdparty/jvm/com/typesafe/akka:akka_stream",
    "//3rdparty/jvm/com/typesafe/scala_logging",
    "//3rdparty/jvm/io/grpc:grpc_netty",
    "//3rdparty/jvm/io/netty:netty_handler",
    "//3rdparty/jvm/io/netty:netty_tcnative_boringssl_static",
    "//3rdparty/jvm/org/scalaz:scalaz_core",
    "//language-support/java/bindings:bindings-java",
    "//language-support/scala/bindings",
    "//ledger-api/rs-grpc-akka",
    "//ledger/ledger-api-client",
])