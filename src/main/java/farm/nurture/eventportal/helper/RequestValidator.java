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

package farm.nurture.eventportal.helper;

import com.google.inject.Singleton;
import farm.nurture.core.contracts.common.RequestStatusResult;
import farm.nurture.core.contracts.common.enums.NameSpace;
import farm.nurture.core.contracts.common.enums.RequestStatus;
import farm.nurture.event.portal.proto.*;
import farm.nurture.eventportal.util.EventPortalConstants;

import farm.nurture.infra.metrics.Metrics;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class RequestValidator {

    private final Metrics metrics = Metrics.getInstance();

    private static final String INVALID_FILTER_EVENT_REQUEST = "invalid_filter_events_request";
    private static final String INVALID_UPLOAD_EVENT_REQUEST = "invalid_upload_events_request";
    private static final String INVALID_FILTER_EVENT_PROPERTIES_REQUEST = "invalid_filter_event_properties_request";
    private static final String INVALID_FIND_EVENT_PROPERTY_BY_ID_REQUEST = "invalid_find_event_property_by_id_request";

    public FilterEventsResponse validate(FilterEventsRequest request) {
//        if(request.getNamespace()== NameSpace.NO_NAMESPACE) {
//            metrics.onIncrement(EventPortalConstants.NF_EP, INVALID_FILTER_EVENT_REQUEST);
//            log.error("Namespace or name is not passed. Invalid filterEvents request: {}", request);
//            return FilterEventsResponse.newBuilder()
//                    .setStatus(RequestStatusResult.newBuilder().setStatus(RequestStatus.BAD_INPUT).build())
//                    .build();
//        }
        return null;
    }

    public FilterEventPropertiesResponse validate(FilterEventPropertiesRequest request) {
        if(request.getEventId()==0) {
            metrics.onIncrement(EventPortalConstants.NF_EP, INVALID_FILTER_EVENT_PROPERTIES_REQUEST);
            log.error("EventId is not passed. Invalid filterEventProperties request: {}", request);
            return FilterEventPropertiesResponse.newBuilder()
                    .setStatus(RequestStatusResult.newBuilder().setStatus(RequestStatus.BAD_INPUT).build())
                    .build();
        }
        return null;
    }

    public FindEventPropertyByIdResponse validate(FindEventPropertyByIdRequest request) {
        if(request.getEventId()==0) {
            metrics.onIncrement(EventPortalConstants.NF_EP, INVALID_FIND_EVENT_PROPERTY_BY_ID_REQUEST);
            log.error("EventPropertyId is not passed. Invalid findEventPropertyById request: {}", request);
            return FindEventPropertyByIdResponse.newBuilder()
                    .setStatus(RequestStatusResult.newBuilder().setStatus(RequestStatus.BAD_INPUT).build())
                    .build();
        }
        return null;
    }

    public EventResponse validate(EventRequest request) {
        if(request.getActor() == null || request.getActor().getActorId() == 0L)
        {
            metrics.onIncrement(EventPortalConstants.NF_EP, INVALID_UPLOAD_EVENT_REQUEST);
            log.error("Namespace or name is not passed. Invalid filterEvents request: {}", request);
            return EventResponse.newBuilder()
                    .setStatus(RequestStatusResult.newBuilder().setStatus(RequestStatus.BAD_INPUT).build())
                    .build();
        }
        return null;
    }
}
