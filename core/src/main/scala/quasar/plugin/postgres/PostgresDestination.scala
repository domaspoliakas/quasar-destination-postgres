/*
 * Copyright 2020 Precog Data
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quasar.plugin.postgres

import scala._, Predef._

import cats.data.NonEmptyList
import cats.effect.{Effect, Timer}

import doobie.Transactor

import quasar.api.destination._
import quasar.connector.MonadResourceErr
import quasar.connector.destination._

final class PostgresDestination[F[_]: Effect: MonadResourceErr: Timer](
    xa: Transactor[F],
    writeMode: WriteMode,
    schema: Option[String])
    extends LegacyDestination[F] {

  val destinationType: DestinationType =
    PostgresDestinationModule.destinationType

  val sinks: NonEmptyList[ResultSink[F, Type]] =
    NonEmptyList.of(
      ResultSink.create(CsvCreateSink(xa, writeMode, schema)),
      ResultSink.upsert(CsvUpsertSink(xa, writeMode)),
      ResultSink.append(CsvAppendSink(xa, writeMode)))
}
