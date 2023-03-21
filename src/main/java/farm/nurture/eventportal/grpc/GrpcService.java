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

package farm.nurture.eventportal.grpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import farm.nurture.event.portal.proto.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class GrpcService extends EventPortalGrpc.EventPortalImplBase {

    @Inject EventPortal eventPortal;

    public void executeFilterEvents(FilterEventsRequest request, StreamObserver<FilterEventsResponse> responseObserver) {
        FilterEventsResponse response = eventPortal.executeFilterEvents(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void uploadEvent(EventRequest request, StreamObserver<EventResponse> responseObserver) {
        EventResponse response = eventPortal.uploadEvent(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void executeFilterEventProperties(FilterEventPropertiesRequest request, StreamObserver<FilterEventPropertiesResponse> responseObserver) {
        FilterEventPropertiesResponse response = eventPortal.executeFilterEventProperties(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void executeFindEventPropertyById(FindEventPropertyByIdRequest request, StreamObserver<FindEventPropertyByIdResponse> responseObserver) {
        FindEventPropertyByIdResponse response = eventPortal.executeFindEventPropertyById(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
