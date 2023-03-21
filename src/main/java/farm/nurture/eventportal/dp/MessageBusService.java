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

package farm.nurture.eventportal.dp;

import com.google.protobuf.ByteString;
import farm.nurture.core.contracts.common.enums.RequestStatus;
import farm.nurture.core.contracts.mb.MessageBusGrpc;
import farm.nurture.core.contracts.mb.MessageBusRequest;
import farm.nurture.core.contracts.mb.MessageBusResponse;
import farm.nurture.eventportal.server.EventPortalConfig;
import farm.nurture.eventportal.util.EventPortalConstants;
import farm.nurture.infra.metrics.MetricTracker;
import farm.nurture.infra.util.Logger;
import farm.nurture.infra.util.LoggerFactory;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.UUID;

public class MessageBusService {
    private static final Logger logger = LoggerFactory.getLogger(MessageBusService.class);

    private static MessageBusService instance;
    private MessageBusGrpc.MessageBusBlockingStub messageBusBlockingStub;

    public static MessageBusService getInstance() {
        if (null != instance) {
            return instance;
        } else {
            synchronized(MessageBusService.class.getName()) {
                if (null != instance) {
                    return instance;
                }
                instance = new MessageBusService();
            }
            return instance;
        }
    }

    private MessageBusService(){
        initializeMessageBusService();
    }

    private void initializeMessageBusService() {
        ManagedChannelBuilder<?> builder;
        String target = EventPortalConfig.MESSAGE_BUS_TARGET.get(null);
        if(null == target) {
            String host = EventPortalConfig.MESSAGE_BUS_HOST.get();
            int port = EventPortalConfig.MESSAGE_BUS_PORT.get(80);
            builder = ManagedChannelBuilder.forAddress(host, port);
        } else {
            builder = ManagedChannelBuilder.forTarget(target);
        }
        ManagedChannel channel = builder.usePlaintext()
                .build();
        messageBusBlockingStub = MessageBusGrpc.newBlockingStub(channel);
    }

    public void sendMessage(byte[] message, String storageFolderPath){
        boolean success = false;
        MetricTracker tracker = new MetricTracker(EventPortalConstants.NF_EP, EventPortalConstants.EVENT_SENT_MESSAGE_BUS);
        try {
            MessageBusRequest request = MessageBusRequest.newBuilder()
                    .setTraceId(UUID.randomUUID().toString())
                    .setTenantId(EventPortalConstants.EVENT_PORTAL_TENANT)
                    .setMessage(ByteString.copyFrom(message))
                    .setStorageFilepath(storageFolderPath)
                    .build();

            MessageBusResponse response = messageBusBlockingStub.upload(request);
            if (response.getStatus().getStatus() != RequestStatus.SUCCESS) {
                logger.info("Message bus response status : {}", response.getStatus().getStatus());
            }
            success = true;
        }catch (Exception e) {
            logger.error("Error in sending event to message bus having storagePath: {}", storageFolderPath, e);
        } finally {
            tracker.stop(success);
        }
    }
}
