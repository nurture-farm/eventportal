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

import farm.nurture.infra.util.ApplicationConfiguration;


import java.util.HashMap;
import java.util.Map;


public enum EventPortalConfig {
    PORTAL_PORT("portal.port"),
    METRICS_PORT("portal.metrics.port"),
    CLEVERTAP_SCHEME("clevertap.scheme"),
    CLEVERTAP_HOST("clevertap.host"),
    CLEVERTAP_PORT("clevertap.port"),
    MESSAGE_BUS_TARGET("message.bus.target"),
    MESSAGE_BUS_HOST("message.bus.host"),
    MESSAGE_BUS_PORT("message.bus.port"),
    CLEVERTAP_ACCOUNT_ID("clevertap.account.id"),
    CLEVERTAP_ACCOUNT_PASSCODE("clevertap.account.passcode"),
    CLEVERTAP_URI("clevertap.uri"),
    HTTP_CLIENT_MAX_CONNECTION("httpClientConfig.maxConnections"),
    HTTP_CLIENT_MAX_CONNECTION_PER_ROUTE("httpClientConfig.maxConnectionsPerRoute"),
    HTTP_CLIENT_CONNECTION_TIMEOUT("httpClientConfig.connectionTimeout"),
    HTTP_CLIENT_REQUEST_TIMEOUT("httpClientConfig.requestTimeout"),
    HTTP_CLIENT_SO_REUSE_ADDRESS ("httpClientConfig.soReuseAddress"),
    HTTP_CLIENT_SO_LINGER("httpClientConfig.soLinger"),
    HTTP_CLIENT_KEEP_ALIVE("httpClientConfig.keepAlive"),
    HTTP_CLIENT_TCP_NO_DELAY("httpClientConfig.tcpNoDelay"),
    DB_CONNECTION_URL("db.connection.url"),
    DB_USERNAME("db.username"),
    DB_PASSWORD("db.password"),
    DB_DRIVER_CLASS("db.driver.class"),
    DB_CONNECTION_POOL_NAME("db.connection.pool.name"),
    DB_IDLE_CONNECTIONS("db.idle.connections"),
    DB_MAX_CONNECTIONS("db.max.connections"),
    DB_CONNECTION_INCREMENT_BY("db.connection.increment.by"),
    DB_CONNECTION_HEALTH_CHECK_DURATION("db.connection.health.check.duration.ms"),
    GRPC_SERVER_PORT("grpcServer.port"),
    TEMPORAL_ADDRESS("temporal.address"),
    TEMPORAL_NAMESPACE("temporal.namespace"),
    TEMPORAL_TASKQUEUE("temporal.worker.taskqueue");

    private Map<String, String> kafkaProducers = new HashMap<>();

    private static final ApplicationConfiguration appConfig = ApplicationConfiguration.getInstance();

    private final String key;

    EventPortalConfig(String key) {
        this.key = key;
    }

    public final String get() {
        return appConfig.get(key, null);
    }

    public final String get(final String defaultValue) {
        return appConfig.get(key, defaultValue);
    }

    public final int get(final int defaultValue) {
        return appConfig.getInt(key, defaultValue);
    }

    public final double get(final double defaultValue) {
        return appConfig.getDouble(key, defaultValue);
    }

    public final double get(final float defaultValue) {
        return appConfig.getFloat(key, defaultValue);
    }

    public final boolean get(final boolean defaultValue) {
        return appConfig.getBoolean(key, defaultValue);
    }
}
