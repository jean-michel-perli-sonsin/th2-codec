# How it works

The th2 Codec component is responsible for encoding and decoding the messages. It is operating two instances of encoder/decoder pairs, one used for operational purposes and the other one for general conversion.

Encoding and decoding are performed according to the scheme "one or more input pins and one or more output pins". Both types of pins may have filters, depending on which of the input / output of the encoder and decoder can be partially or entirely filtered out. The metadata of the message and its fields are used as filter parameters.

One instance of the codec implements the logic for encoding and decoding one protocol of one version. The version-specific protocol messages are described in a separate xml file called "dictionary".
Codec operates with arrays of messages (parsed batch to raw batch in case of encoding and raw batch to parsed batch upon decoding).

# Running

To start a codec it is required to place an external codec api implementation jar with dependencies to 'codec_implementation' folder.

# Configuration

Codec have four types of connection: stream and general for encode and decode functions.

* stream encode / decode connections are used for work 24 / 7
* general encode / decode connections are used for requests on demand

Messages in stream and general connections are never mixed. 

Decoding can work in two different modes:
+ CUMULATIVE (default) - all raw messages in batch will be joined together and decoded. After decoding, the content and the count of the decoded messages will be compared with the original messages in the batch.
+ SEQUENTIAL - each message in the batch will be decoded as a separate message.

This setting can be overridden in a custom config for the application using the parameter `decodeProcessorType`.

## Requried pins

Every type of connections is presented with two subscribe and publish pins. 
Configuration should include at least one pin at a time, for every type (the minimal number is 8)

```yaml
apiVersion: th2.exactpro.com/v1
kind: Th2GenericBox
metadata:
  name: codec
spec:
  pins:
    # encoder
    - name: in_codec_encode
      connection-type: mq
      attributes: ['encoder_in', 'parsed', 'subscribe']
    - name: out_codec_encode
      connection-type: mq
      attributes: ['encoder_out', 'raw', 'publish']
    # decoder
    - name: in_codec_decode
      connection-type: mq
      attributes: ['decoder_in', 'raw', 'subscribe']
    - name: out_codec_decode
      connection-type: mq
      attributes: ['decoder_out', 'parsed', 'publish']
    # encoder general (technical)
    - name: in_codec_general_encode
      connection-type: mq
      attributes: ['general_encoder_in', 'parsed', 'subscribe']
    - name: out_codec_general_encode
      connection-type: mq
      attributes: ['general_encoder_out', 'raw', 'publish']
    # decoder general (technical)
    - name: in_codec_general_decode
      connection-type: mq
      attributes: ['general_decoder_in', 'raw', 'subscribe']
    - name: out_codec_general_decode
      connection-type: mq
      attributes: ['general_decoder_out', 'parsed', 'publish']
```

## Message routing

The schema API allows to configure routing streams of messages via links between connections and filters on pins.
Let's consider some examples of routing in codec box.

### Split on 'publish' pins

For example, you got a big source data stream, and you want to split them into some pins via session alias.
You can declare multiple pins with attributes ['decoder_out', 'parsed', 'publish'] and filters instead of common pin or in addition to it.
Every decoded messages will be direct to all declared pins and will send to MQ only if pass filter.

```yaml
apiVersion: th2.exactpro.com/v1
kind: Th2GenericBox
metadata:
  name: codec
spec:
  pins:
    # decoder
    - name: out_codec_decode_first_session_alias
      connection-type: mq
      attributes: ['decoder_out', 'parsed', 'publish', 'first_session_alias']
      filters:
        - metadata:
            - field-name: session_alias
              expected-value: first_session_alias
              operation: EQUAL
    - name: out_codec_decode_secon_session_alias
      connection-type: mq
      attributes: ['decoder_out', 'parsed', 'publish', 'second_session_alias']
      filters:
        - metadata:
            - field-name: session_alias
              expected-value: second_session_alias
              operation: EQUAL
```
