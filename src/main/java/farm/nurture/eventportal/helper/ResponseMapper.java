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
import farm.nurture.core.contracts.common.Attribs;
import farm.nurture.core.contracts.common.RequestStatusResult;
import farm.nurture.core.contracts.common.enums.EventPropertyType;
import farm.nurture.core.contracts.common.enums.NameSpace;
import farm.nurture.core.contracts.common.enums.RequestStatus;
import farm.nurture.event.portal.proto.*;
import farm.nurture.eventportal.dto.CleverTapUploadRequest;
import farm.nurture.eventportal.model.Event;
import farm.nurture.eventportal.model.EventProperty;
import org.apache.commons.collections4.CollectionUtils;
import farm.nurture.infra.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ResponseMapper {

    public FilterEventsResponse mapToFilterEventsResponse(RequestStatusResult requestStatusResult, List<Event> recordList) {

        FilterEventsResponse.Builder builder = FilterEventsResponse.newBuilder();
        builder.setStatus(requestStatusResult);

        if(recordList!=null) {
            builder.setCount(recordList.size());
            recordList.forEach(record -> builder.addRecord(convert(record)));
        }
        return builder.build();
    }

    public FilterEventPropertiesResponse mapToFilterEventPropertiesResponse(RequestStatusResult requestStatusResult, List<EventProperty> recordList) {

        FilterEventPropertiesResponse.Builder builder = FilterEventPropertiesResponse.newBuilder();
        builder.setStatus(requestStatusResult);

        if(recordList!=null) {
            builder.setCount(recordList.size());
            recordList.forEach(record -> builder.addEventProperties(convert(record)));
        }
        return builder.build();
    }

    public FindEventPropertyByIdResponse mapToFindEventPropertyByIdResponse(RequestStatusResult requestStatusResult, List<EventProperty> recordList) {

        FindEventPropertyByIdResponse.Builder builder = FindEventPropertyByIdResponse.newBuilder();
        builder.setStatus(requestStatusResult);

        if(recordList!=null) {
            builder.setFindEventPropertyByIdRecord(FindEventPropertyByIdRecord.newBuilder()
                    .setEventPropertyType(EventPropertyType.valueOf(recordList.get(0).getType()))
                    .setEventParameterMetadata(convertEventParameterMetadata(recordList))
                    .build()).build();
        }
        return builder.build();
    }

    private FilterEventsResponseRecord convert(Event event) {
        FilterEventsResponseRecord.Builder builder = FilterEventsResponseRecord.newBuilder();
        if(event.getId()!=null) builder.setId(event.getId());
        if(event.getIndex()!=null) builder.setIndex(event.getIndex());
        if(StringUtils.isNonEmpty(event.getName())) builder.setName(event.getName());
        if(StringUtils.isNonEmpty(event.getNamespace())) builder.setNamespace(NameSpace.valueOf(event.getNamespace()));
        return builder.build();
    }

    private farm.nurture.event.portal.proto.EventProperty convert(EventProperty eventProperty) {
        farm.nurture.event.portal.proto.EventProperty.Builder builder = farm.nurture.event.portal.proto.EventProperty.newBuilder();
        if(eventProperty.getEventId()!=null) builder.setId(eventProperty.getId());
        if(StringUtils.isNonEmpty(eventProperty.getType())) builder.setEventPropertyType(EventPropertyType.valueOf(eventProperty.getType()));
        if(StringUtils.isNonEmpty(eventProperty.getName())) builder.setName(eventProperty.getName());
        return builder.build();
    }

    private EventParameterMetadata convertEventParameterMetadata(List<EventProperty> recordList) {

        List<String> values = new ArrayList<>();
        for(EventProperty record: recordList) {
            values.add(record.getValue());
        }
        return EventParameterMetadata.newBuilder().setEventParameterValues(EventParameterValues.newBuilder().addAllValues(values).build()).build();
    }

    public EventResponse mapToEventsResponse(RequestStatusResult requestStatusResult) {

        EventResponse.Builder builder = EventResponse.newBuilder();
        builder.setStatus(requestStatusResult);
        if(requestStatusResult.getStatus() == RequestStatus.SUCCESS){
            builder.setCount(1);
        }
        return builder.build();
    }


    public CleverTapUploadRequest convertEventRequestToCleverTapUploadRequest(EventRequest request) {
        Map<String, String> eventData = new HashMap<>();
        if (CollectionUtils.isNotEmpty(request.getAttributesList())) {
            eventData = request.getAttributesList().stream().collect(Collectors.toMap(Attribs::getKey, Attribs::getValue));
        }
        CleverTapUploadRequest.Event event = CleverTapUploadRequest.Event.builder()
                .evtName(request.getEventName())
                .ts(request.getEventTime().getSeconds())
                .type(request.getEventType().name().toLowerCase())
                .identity(request.getActor().getActorId())
                .evtData(eventData)
                .build();
        return CleverTapUploadRequest.builder().d(Collections.singletonList(event)).build();
    }
}
