/*
 * Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.th2.codec.util

import com.exactpro.sf.externalapi.codec.IExternalCodecContext
import com.exactpro.sf.externalapi.codec.IExternalCodecContext.Role
import com.exactpro.sf.externalapi.codec.impl.ExternalCodecContext
import com.exactpro.th2.infra.grpc.Direction
import com.exactpro.th2.infra.grpc.MessageBatch
import com.exactpro.th2.infra.grpc.RawMessageBatch

private val RECEIVER_CONTEXT = ExternalCodecContext(Role.RECEIVER)
private val SENDER_CONTEXT = ExternalCodecContext(Role.SENDER)

private fun Direction.toRole(): Role = when (this) {
    Direction.FIRST -> Role.RECEIVER
    Direction.SECOND -> Role.SENDER
    else -> throw IllegalStateException("Unsupported direction: $this")
}

private fun Role.toContext(): IExternalCodecContext = when (this) {
    Role.RECEIVER -> RECEIVER_CONTEXT
    Role.SENDER -> SENDER_CONTEXT
}

val RawMessageBatch.codecContext: IExternalCodecContext
    // assuming that we wouldn't have messages with different directions in a single batch
    get() = messagesList.first().metadata.id.direction.toRole().toContext()

val MessageBatch.codecContext: IExternalCodecContext
    // assuming that we wouldn't have messages with different directions in a single batch
    get() = messagesList.first().metadata.id.direction.toRole().toContext()
