// Copyright (c) 2019 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0
package com.digitalasset.daml.lf.data

import scalaz.Equal

import scala.util.matching.Regex

sealed abstract class MatchingStringModule {

  type T <: String

  def fromString(s: String): Either[String, T]

  @throws[IllegalArgumentException]
  final def assertFromString(s: String): T =
    assert(fromString(s))

  def equalInstance: Equal[T]

  // We provide the following array factory instead of a ClassTag
  // because the latter lets people easily reinterpret any string as a T.
  // See
  //  * https://github.com/digital-asset/daml/pull/983#discussion_r282513324
  //  * https://github.com/scala/bug/issues/9565
  val Array: ArrayFactory[T]
}

object MatchingStringModule extends (Regex => MatchingStringModule) {

  override def apply(regex: Regex): MatchingStringModule = new MatchingStringModule {
    type T = String

    private val pattern = regex.pattern

    def fromString(s: String): Either[String, T] =
      Either.cond(pattern.matcher(s).matches(), s, s"""string "$s" does not match regex "$regex"""")

    def equalInstance: Equal[T] = scalaz.std.string.stringInstance

    val Array: ArrayFactory[T] = new ArrayFactory[T]
  }

}
