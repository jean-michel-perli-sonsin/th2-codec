package com.exactpro.th2.codec

import com.exactpro.sf.common.messages.IMessage
import com.exactpro.sf.externalapi.codec.IExternalCodec
import com.exactpro.th2.IMessageToProtoConverter
import com.exactpro.th2.codec.util.toHexString
import com.exactpro.th2.infra.grpc.*
import mu.KotlinLogging

class DecodeProcessor(
    private val codec: IExternalCodec,
    private val messageToProtoConverter: IMessageToProtoConverter
) : MessageProcessor<RawMessageBatch, MessageBatch> {

    private val logger = KotlinLogging.logger {  }

    override fun process(source: RawMessageBatch): MessageBatch {
        val batchData = joinBatchData(source)
        val messageBatchBuilder = MessageBatch.newBuilder()
        val decodedMessageList = codec.decode(batchData)
        if (checkSizeAndContext(source, decodedMessageList)) {
            for (pair in source.messagesList.zip(decodedMessageList)) {
                val metadataBuilder = toMessageMetadataBuilder(pair.first)
                val protoMessageBuilder = messageToProtoConverter.toProtoMessage(pair.second)
                protoMessageBuilder.metadata = metadataBuilder.setMessageType(pair.second.name).build()
                messageBatchBuilder.addMessages(protoMessageBuilder)
            }
        }
        return messageBatchBuilder.build()
    }

    private fun checkSizeAndContext(source: RawMessageBatch, decodedMessageList: List<IMessage>): Boolean {
        if (source.messagesCount != decodedMessageList.size) {
            logger.error { "messages size mismatch: source count = ${source.messagesCount}," +
                    " decoded count = ${decodedMessageList.size}" }
            return false
        }
        for ((index, pair) in source.messagesList.zip(decodedMessageList).withIndex()) {
            val sourceMessageData = pair.first.body.toByteArray()
            val decodedMessageData = pair.second.metaData.rawMessage
            if (!(sourceMessageData contentEquals decodedMessageData)) {
                logger.error { "content mismatch by position $index in the batch: " +
                        "source hex: '${sourceMessageData.toHexString()}', decoded hex: '${decodedMessageData.toHexString()}'" }
                return false
            }
        }
        return true
    }

    private fun toMessageMetadataBuilder(sourceMessage: RawMessage): MessageMetadata.Builder {
        return MessageMetadata.newBuilder()
            .setId(sourceMessage.metadata.id)
            .setTimestamp(sourceMessage.metadata.timestamp)
    }

    private fun joinBatchData(rawMessageBatch: RawMessageBatch): ByteArray {
        var batchData= ByteArray(0)
        for (rawMessage in rawMessageBatch.messagesList) {
            batchData += rawMessage.body.toByteArray()
        }
        return batchData
    }
}