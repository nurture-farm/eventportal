/*
 *  Copyright 2023 NURTURE AGTECH PVT LTD
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package farm.nurture.eventportal.kafka.producer;

import com.google.inject.Singleton;
import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.infra.util.Logger;
import farm.nurture.infra.util.LoggerFactory;
import farm.nurture.kafka.Event;
import farm.nurture.kafka.Producer;
import farm.nurture.kafka.config.KafkaProducerConfig;
import farm.nurture.kafka.impl.KafkaProducer;
import org.apache.kafka.common.header.internals.RecordHeaders;

import java.util.Properties;

@Singleton
public class KafkaProducerWrapperServiceImpl implements IKafkaProducerWrapperService {

    public static final String SERIALIZATION = "serialization";
    public static final String RETRY = "retry";
    public static final String PARTITION_KEY = "partitionKey";
    private final Logger log = LoggerFactory.getLogger(KafkaProducerWrapperServiceImpl.class);

    private static Producer<byte[], byte[]> producer;

    public Producer<byte[], byte[]> getProducerInstance() {
        if (producer == null) {
            synchronized (KafkaProducerWrapperServiceImpl.class.getName()) {
                if (producer == null) {
                    Properties props = new Properties();
                    ApplicationConfiguration configuration = ApplicationConfiguration.getInstance();
                    props.put("bootstrap.servers", configuration.get("kafka.bootstrap.servers", "localhost:9092"));
                    props.put("acks", configuration.get("kafka.producer.acks","all"));
                    props.put("compression.type", configuration.get("kafka.producer.compression.type","none"));
                    props.put("max.in.flight.requests.per.connection", configuration.getInt("kafka.producer.max.in.flight.requests.per.connection",5));
                    props.put("batch.size", configuration.getInt("kafka.producer.batch.size",16384));
                    props.put("linger.ms", configuration.getInt("kafka.producer.linger.ms",5));
                    props.put("key.serializer", configuration.get("kafka.communication.event.key.serializer","org.apache.kafka.common.serialization.ByteArraySerializer"));
                    props.put("value.serializer", configuration.get("kafka.communication.event.value.serializer","org.apache.kafka.common.serialization.ByteArraySerializer"));
                    producer = new KafkaProducer<>(new KafkaProducerConfig(props));
                }
            }
        }
        return producer;
    }

    @Override
    public void pushByteArrayMessage(byte[] message, String topic, String partitionKey, Integer retryCount) {
        RecordHeaders headers = new RecordHeaders();
        headers.add(SERIALIZATION, topic.getBytes());
        headers.add(RETRY, String.valueOf(retryCount).getBytes() );
        headers.add(PARTITION_KEY, partitionKey.getBytes() );
        Long currentTime = System.currentTimeMillis();
        getProducerInstance().send(topic, new Event<>(partitionKey.getBytes(), message, currentTime, headers));
        log.debug("Pushed event to kafka {} partitionKey: {}", topic, partitionKey);
    }
}


