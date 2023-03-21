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

import farm.nurture.core.contracts.common.RequestStatusResult;
import farm.nurture.core.contracts.common.enums.RequestStatus;
import farm.nurture.event.portal.proto.*;
import farm.nurture.eventportal.cache.EventPropertyCache;
import farm.nurture.eventportal.cache.EventPropertyCacheKey;
import farm.nurture.eventportal.dto.CleverTapUploadRequest;
import farm.nurture.eventportal.helper.RequestValidator;
import farm.nurture.eventportal.helper.ResponseMapper;
import farm.nurture.eventportal.model.Event;
import farm.nurture.eventportal.model.EventProperty;
import farm.nurture.eventportal.repository.EventPropertyRepository;
import farm.nurture.eventportal.repository.EventRepository;
import farm.nurture.eventportal.service.CleverTapService;
import farm.nurture.eventportal.util.EventPortalConstants;
import farm.nurture.infra.metrics.MetricTracker;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Slf4j
public class EventPortalImpl implements EventPortal {

    private static RequestStatusResult successResult;
    private static RequestStatusResult errorResult;

    static {
        successResult = RequestStatusResult.newBuilder().setStatus(RequestStatus.SUCCESS).build();
        errorResult = RequestStatusResult.newBuilder().setStatus(RequestStatus.INTERNAL_ERROR).build();
    }

    @Inject
    private RequestValidator requestValidator;

    @Inject
    private ResponseMapper responseMapper;

    @Inject
    private EventRepository eventRepository;

    @Inject
    private EventPropertyRepository eventPropertyRepository;

    @Inject
    private CleverTapService cleverTapService;

    @Inject
    private EventPropertyCache eventPropertyCache;

    @Override
    public FilterEventsResponse executeFilterEvents(FilterEventsRequest request) {
        log.info("Serving filterEvents request. Request: {}", request);
        FilterEventsResponse response = requestValidator.validate(request);
        if (response != null) return response;
        boolean success = false;
        MetricTracker tracker = new MetricTracker(EventPortalConstants.NF_EP, EventPortalConstants.filterEvents);
        try {
            List<Event> eventList = eventRepository.filterEvents(request.getNamespace().name(), request.getName(), request.getPageNumber(), request.getLimit());
            log.info("Successfully fetched events for request: {} number of events: {}", request, eventList.size());
            response = responseMapper.mapToFilterEventsResponse(successResult, eventList);
            success = true;
        } catch (Exception e) {
            log.error("Error in filtering Events, request : {}", request, e);
            response = responseMapper.mapToFilterEventsResponse(errorResult, null);
        } finally {
            tracker.stop(success);
        }
        return response;
    }

    @Override
    public EventResponse uploadEvent(EventRequest request) {
        log.info("Upload event to clevertap {} ", request);
        EventResponse response = requestValidator.validate(request);
        if (response != null) {
            log.error("Invalid request for sending event to clevertap {} ", request);
            return response;
        }
        CleverTapUploadRequest cleverTapUploadRequest = responseMapper.convertEventRequestToCleverTapUploadRequest(request);
        boolean isEventSent = cleverTapService.sendEventToCleverTap(cleverTapUploadRequest);
        if (!isEventSent){
            response = responseMapper.mapToEventsResponse(errorResult);
        }else {
            response = responseMapper.mapToEventsResponse(successResult);
        }
        return response;
    }

    @Override
    public FilterEventPropertiesResponse executeFilterEventProperties(FilterEventPropertiesRequest request) {
        log.info("Serving filterEventProperties request. Request: {}", request);
        FilterEventPropertiesResponse response = requestValidator.validate(request);
        if (response != null) return response;
        boolean success = false;
        MetricTracker tracker = new MetricTracker(EventPortalConstants.NF_EP, EventPortalConstants.filterEventProperties);
        try {
            List<EventProperty> eventList = eventPropertyRepository.filterEventProperties(request.getEventId(), request.getPageNumber(), request.getLimit());
            log.info("Successfully fetched eventProperties for request: {} number of events: {}", request, eventList.size());
            response = responseMapper.mapToFilterEventPropertiesResponse(successResult, eventList);
            success = true;
        } catch (Exception e) {
            log.error("Error in filtering EventProperties, request : {}", request, e);
            response = responseMapper.mapToFilterEventPropertiesResponse(errorResult, null);
        } finally {
            tracker.stop(success);
        }
        return response;

    }

    @Override
    public FindEventPropertyByIdResponse executeFindEventPropertyById(FindEventPropertyByIdRequest request) {

        log.info("Serving findEventPropertyById request. Request: {}", request);
        FindEventPropertyByIdResponse response = requestValidator.validate(request);
        if(response!=null) return response;
        boolean success = false;
        MetricTracker tracker = new MetricTracker(EventPortalConstants.NF_EP, EventPortalConstants.findEventPropertyById);
        try {
            Set<EventProperty> eventProperties = eventPropertyCache.get(EventPropertyCacheKey.builder().eventId(request.getEventId()).name(request.getName()).build());
            if(null == eventProperties || eventProperties.isEmpty()) {
                eventProperties = eventPropertyRepository.findEventPropertyByEventIdAndName(request.getEventId(), request.getName());
            }

            if(eventProperties.size() >= EventPortalConstants.MAX_PROPERTIES_SET_SIZE_PER_KEY) {
                eventProperties = new HashSet<>();
            }
            List<EventProperty> eventList = new ArrayList<>(eventProperties);
            log.info("Successfully fetched eventProperties for findEventPropertyById request: {} number of events: {}", request, eventList.size());
            response = responseMapper.mapToFindEventPropertyByIdResponse(successResult, eventList);
            success = true;
        } catch (Exception e) {
            log.error("Error in FindEventPropertyByIdResponse, request : {}", request, e);
            response = responseMapper.mapToFindEventPropertyByIdResponse(errorResult, null);
        } finally {
            tracker.stop(success);
        }
        return response;
    }

}
