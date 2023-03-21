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

package farm.nurture.eventportal.server;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import farm.nurture.eventportal.DIModule;
import farm.nurture.eventportal.cache.EventCache;
import farm.nurture.eventportal.cache.EventPropertyCache;
import farm.nurture.eventportal.dp.Consumer;
import farm.nurture.eventportal.grpc.EventPortal;
import farm.nurture.eventportal.grpc.GrpcService;
import farm.nurture.infra.metrics.HealthInfoServerFactory;
import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.infra.util.Logger;
import farm.nurture.infra.util.LoggerFactory;
import farm.nurture.laminar.core.io.sql.dao.DbConfig;
import farm.nurture.laminar.core.io.sql.dao.PoolFactory;
import io.grpc.ServerBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public final class EventPortalServer {

    private static final Logger logger = LoggerFactory.getLogger(EventPortalServer.class);

    /**
     * Starts a portal server which stores the event received from client and
     * forwards the request to the clevertap.
     */
    private static void startPortalServer(Injector injector) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Consumer consumer = injector.getInstance(Consumer.class);
            new Thread(consumer).start();

            //start metrics server
            HealthInfoServerFactory.start(EventPortalConfig.METRICS_PORT.get(1235));

            //start event portal server
            ServerBootstrap b = new ServerBootstrap();
            final int portalServerPort = EventPortalConfig.PORTAL_PORT.get(8081);
            logger.debug("Starting portal server at port: {}", portalServerPort);
            final Channel serverChannel = b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new HttpServerCodec())
                                    .addLast(new HttpObjectAggregator(8388608, true))
                                    .addLast(new FrontendHandler(consumer));
                        }
                    })
                    .childOption(ChannelOption.AUTO_READ, false)
                    .bind(portalServerPort).sync().channel();
            logger.info("Portal to {}:{} started at {}", EventPortalConfig.CLEVERTAP_HOST.get(null),
                    EventPortalConfig.CLEVERTAP_PORT.get(443), portalServerPort);
            serverChannel.closeFuture().sync();
        } catch (Exception e) {
            logger.error("Portal server shutting down {}", e.getMessage(), e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static void startGrpcServer(Injector injector) throws Exception {
        GrpcService grpcService = injector.getInstance(GrpcService.class);
        int port = EventPortalConfig.GRPC_SERVER_PORT.get(8085);
        io.grpc.Server server = ServerBuilder.forPort(port).addService(grpcService).build().start();
        log.info("Event Portal grpc server started, listening on " + port);
        server.awaitTermination();
    }

    public static void startTemporalWorker(Injector injector) {
        // gRPC stubs wrapper that talks to the local docker instance of temporal service.
        String temporalAddress = EventPortalConfig.TEMPORAL_ADDRESS.get("127.0.0.1:7233");
        WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder().setTarget(temporalAddress).build();
        WorkflowServiceStubs service = WorkflowServiceStubs.newInstance(options);

        // client that can be used to start and signal workflows
        String namespace = EventPortalConfig.TEMPORAL_NAMESPACE.get("default");

        WorkflowClientOptions clientOptions = WorkflowClientOptions.newBuilder()
                .setNamespace(namespace).build();
        WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);

        // worker factory that can be used to create workers for specific task lists
        WorkerFactory factory = WorkerFactory.newInstance(client);

        // Worker that listens on a task list and hosts activity implementations.
        Worker worker = factory.newWorker(EventPortalConfig.TEMPORAL_TASKQUEUE.get(null));
        worker.registerActivitiesImplementations(injector.getInstance(EventPortal.class));

        factory.start();
    }

    private static void initializeDatabase() {
        log.info("Initializing database");
        DbConfig dbConfig = new DbConfig();
        dbConfig.connectionUrl = EventPortalConfig.DB_CONNECTION_URL.get(null);
        dbConfig.login = EventPortalConfig.DB_USERNAME.get(null);
        dbConfig.password = EventPortalConfig.DB_PASSWORD.get(null);
        dbConfig.driverClass = EventPortalConfig.DB_DRIVER_CLASS.get("com.mysql.cj.jdbc.Driver");
        dbConfig.poolName = EventPortalConfig.DB_CONNECTION_POOL_NAME.get("event_portal_rw");
        dbConfig.idleConnections = EventPortalConfig.DB_IDLE_CONNECTIONS.get(10);
        dbConfig.maxConnections = EventPortalConfig.DB_MAX_CONNECTIONS.get(10);
        dbConfig.incrementBy = EventPortalConfig.DB_CONNECTION_INCREMENT_BY.get(2);
        dbConfig.healthCheckDurationMillis = EventPortalConfig.DB_CONNECTION_HEALTH_CHECK_DURATION.get(900000);
        dbConfig.testConnectionOnBorrow = true;

        PoolFactory.getInstance().setup(dbConfig);
    }

    private static void initializeInMemoryCache(Injector injector) {

        log.info("Initializing EventCache");
        injector.getInstance(EventCache.class).init();
        injector.getInstance(EventPropertyCache.class).init();
    }

    public static void main(String[] args) throws Exception{

        initializeDatabase();
        Injector injector = Guice.createInjector(new DIModule());
        initializeInMemoryCache(injector);
        startTemporalWorker(injector);

        Thread thread = new Thread(() -> {
                try {
                    startGrpcServer(injector);
                } catch (Exception e) {
                    logger.error("Exception in starting event portal server", e);
                }
            }
        );
        thread.start();
        startPortalServer(injector);
    }
}
