# Copyright (c) 2019 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0

bindings_scala_deps = depset([
    "//3rdparty/jvm/com/github/ghik:silencer_lib",
    "//3rdparty/jvm/io/grpc:grpc_core",
    "//3rdparty/jvm/org/scalaz:scalaz_core",
    "//ledger-api/grpc-definitions:ledger-api-scalapb",
])