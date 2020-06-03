FROM openjdk:12-alpine
ENV RABBITMQ_HOST=rabbitmq \
    RABBITMQ_PORT=5672 \
    RABBITMQ_VHOST=th2 \
    RABBITMQ_USER="" \
    RABBITMQ_PASS="" \
    EVENT_STORE_HOST=event-store \
    EVENT_STORE_PORT=30003 \
    CODEC_DICTIONARY="" \
    DECODER_PARAMETERS="{}" \
    SF_CODEC_PARAMETERS="{}" \
    CODEC_CLASS_NAME=someClassName
WORKDIR /home
COPY ./ .
ENTRYPOINT ["/home/th2-codec/bin/th2-codec", "run", "com.exactpro.th2.codec.MainKt"]
