/*
 *  Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.exactpro.th2.codec

import com.exactpro.sf.externalapi.codec.IExternalCodecFactory
import com.exactpro.sf.externalapi.codec.IExternalCodecSettings
import com.exactpro.th2.codec.util.toCodecContext
import com.exactpro.th2.common.grpc.Message
import com.exactpro.th2.common.grpc.MessageBatch
import com.exactpro.th2.common.grpc.RawMessage
import com.exactpro.th2.common.grpc.RawMessageBatch
import com.exactpro.th2.common.grpc.RawMessageMetadata
import com.exactpro.th2.sailfish.utils.ProtoToIMessageConverter
import com.google.protobuf.ByteString
import com.google.protobuf.util.JsonFormat
import mu.KotlinLogging

class EncodeProcessor(
    codecFactory: IExternalCodecFactory,
    codecSettings: IExternalCodecSettings,
    private val converter: ProtoToIMessageConverter
) : AbstractCodecProcessor<MessageBatch, RawMessageBatch>(codecFactory, codecSettings) {

    private val logger = KotlinLogging.logger {  }

    override fun process(source: MessageBatch): RawMessageBatch {
        val rawMessageBatchBuilder = RawMessageBatch.newBuilder()
        for (protoMessage in source.messagesList) {
            val convertedSourceMessage = converter.fromProtoMessage(protoMessage, true).also {
                logger.debug { "converted source message '${it.name}': $it" }
            }
            val encodedMessageData = getCodec().encode(convertedSourceMessage, protoMessage.toCodecContext())
            rawMessageBatchBuilder.addMessages(RawMessage.newBuilder()
                .setBody(ByteString.copyFrom(encodedMessageData))
                .setMetadata(toRawMessageMetadataBuilder(protoMessage).also {
                    logger.debug {
                        val jsonRawMessage = JsonFormat.printer().omittingInsignificantWhitespace().print(it)
                        "message metadata: $jsonRawMessage"
                    }
                })
            )
        }
        return rawMessageBatchBuilder.build()
    }

    private fun toRawMessageMetadataBuilder(sourceMessage: Message): RawMessageMetadata {
        return RawMessageMetadata.newBuilder()
            .setId(sourceMessage.metadata.id)
            .setTimestamp(sourceMessage.metadata.timestamp)
            .putAllProperties(sourceMessage.metadata.propertiesMap)
            .build()
    }
}