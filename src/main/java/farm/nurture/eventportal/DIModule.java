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

package farm.nurture.eventportal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import farm.nurture.eventportal.cache.EventCache;
import farm.nurture.eventportal.cache.EventPropertyCache;
import farm.nurture.eventportal.dp.Consumer;
import farm.nurture.eventportal.grpc.EventPortal;
import farm.nurture.eventportal.grpc.EventPortalImpl;
import farm.nurture.eventportal.grpc.GrpcService;
import farm.nurture.eventportal.helper.RequestValidator;
import farm.nurture.eventportal.helper.ResponseMapper;
import farm.nurture.eventportal.kafka.producer.KafkaProducerWrapperServiceImpl;
import farm.nurture.eventportal.kafka.producer.IKafkaProducerWrapperService;
import farm.nurture.eventportal.repository.EventPropertyRepository;
import farm.nurture.eventportal.repository.EventRepository;
import farm.nurture.eventportal.repository.QueryBuilder;
import farm.nurture.eventportal.server.EventPortalConfig;
import farm.nurture.eventportal.service.CleverTapService;
import farm.nurture.util.http.HttpClientConfig;
import farm.nurture.util.http.HttpClientFactory;
import farm.nurture.util.http.client.NFHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;

@Slf4j
public class DIModule extends AbstractModule {

    @Override
    protected void configure() {

       // bind(KafkaProducerWrapperServiceImpl.class).in(Singleton.class);
        bind(IKafkaProducerWrapperService.class).toInstance(new KafkaProducerWrapperServiceImpl());
        bind(ObjectMapper.class).toInstance(buildObjectMapper());
        bind(Consumer.class).toInstance(new Consumer(new ArrayBlockingQueue<>(10000) , Executors.newFixedThreadPool(10)));
        bind(GrpcService.class).in(Singleton.class);
        bind(EventPortal.class).toInstance(new EventPortalImpl());
        bind(RequestValidator.class).in(Singleton.class);
        bind(ResponseMapper.class).in(Singleton.class);

        bind(EventRepository.class).in(Singleton.class);
        bind(EventPropertyRepository.class).in(Singleton.class);
        bind(QueryBuilder.class).in(Singleton.class);
        bind(EventCache.class).in(Singleton.class);
        bind(EventPropertyCache.class).in(Singleton.class);
        bind(CleverTapService.class).in(Singleton.class);
        bind(NFHttpClient.class).toInstance(buildHttpClient());
    }

    private NFHttpClient buildHttpClient() {
        HttpClientConfig config = createHttpClientConfig();
        HttpClient httpClient = new HttpClientFactory(config).createClient();
        return new NFHttpClient(httpClient);
    }

    private HttpClientConfig createHttpClientConfig() {
        HttpClientConfig config = new HttpClientConfig();
        config.setMaxTotalConnections(EventPortalConfig.HTTP_CLIENT_MAX_CONNECTION.get(150));
        config.setMaxConnectionsPerRoute(EventPortalConfig.HTTP_CLIENT_MAX_CONNECTION_PER_ROUTE.get(50));
        config.setConnectionTimeout(EventPortalConfig.HTTP_CLIENT_CONNECTION_TIMEOUT.get(7000));
        config.setSoTimeout(EventPortalConfig.HTTP_CLIENT_REQUEST_TIMEOUT.get(60000));
        config.setSoReuseAddress(EventPortalConfig.HTTP_CLIENT_SO_REUSE_ADDRESS.get( true));
        config.setSoLinger(EventPortalConfig.HTTP_CLIENT_SO_LINGER.get(0));
        config.setSoKeepAlive(EventPortalConfig.HTTP_CLIENT_KEEP_ALIVE.get(false));
        config.setTcpNoDelay(EventPortalConfig.HTTP_CLIENT_TCP_NO_DELAY.get(false));
        return config;
    }
    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        return mapper;
    }
}
