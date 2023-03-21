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

import farm.nurture.core.contracts.common.enums.NameSpace;

import farm.nurture.event.portal.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GrpcClient {

    private static final String LOCAL_URL = "127.0.0.1:8085";
    private static final String DEV_URL = "internal-a2d376e6948514a73a514e3584160823-409853754.ap-south-1.elb.amazonaws.com";
    private static final String STAGE_URL = "internal-a49872e2b408d43d28ce6000e575e84b-437549400.ap-south-1.elb.amazonaws.com";
    private static final String PROD_URL = "internal-a85f3a2f550f84f578459537c8ae3304-113299092.ap-south-1.elb.amazonaws.com";
    private static ManagedChannel channel;
    private static EventPortalGrpc.EventPortalBlockingStub stub;

    static {
        channel = ManagedChannelBuilder.forTarget(LOCAL_URL).usePlaintext().build();
        stub = EventPortalGrpc.newBlockingStub(channel);
    }

    public static void main(String[] args) throws Exception {

        FilterEvents();
    }

    private static void FilterEvents() {

        FilterEventsRequest filterEventsRequest = FilterEventsRequest.newBuilder().setNamespace(NameSpace.FARM).build();

        try {
            log.info("Calling API");
            FilterEventsResponse response = stub.executeFilterEvents(filterEventsRequest);
            log.info("response : {}", response);
        } catch (Exception e) {
            log.error("Error request : {}", filterEventsRequest, e);
        }
    }

    private static void FilterEventProperties() {

        FilterEventPropertiesRequest filterEventPropertiesRequest = FilterEventPropertiesRequest.newBuilder().setEventId(1).build();

        try {
            log.info("Calling API");
            FilterEventPropertiesResponse response = stub.executeFilterEventProperties(filterEventPropertiesRequest);
            log.info("response : {}", response);
        } catch (Exception e) {
            log.error("Error request : {}", filterEventPropertiesRequest, e);
        }
    }

    private static void FindEventProperty() {

        FindEventPropertyByIdRequest findEventPropertyByIdRequest = FindEventPropertyByIdRequest.newBuilder().setEventId(1).build();

        try {
            log.info("Calling API");
            FindEventPropertyByIdResponse response = stub.executeFindEventPropertyById(findEventPropertyByIdRequest);
            log.info("response : {}", response);
        } catch (Exception e) {
            log.error("Error request : {}", findEventPropertyByIdRequest, e);
        }
    }
}
