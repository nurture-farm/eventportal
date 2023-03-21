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

package farm.nurture.eventportal.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import farm.nurture.eventportal.model.Event;
import farm.nurture.eventportal.model.EventProperty;
import farm.nurture.eventportal.repository.EventPropertyRepository;
import farm.nurture.eventportal.repository.EventRepository;
import farm.nurture.eventportal.util.EventPortalConstants;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class EventPropertyCache {

    @Inject
    private EventRepository eventRepository;

    @Inject
    private EventPropertyRepository eventPropertyRepository;

    private LoadingCache<EventPropertyCacheKey, Set<EventProperty>> eventPropertyCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .refreshAfterWrite(1, TimeUnit.HOURS)
            .build(key -> eventPropertyRepository.findEventPropertyByEventIdAndName(key.getEventId(),key.getName()));

    public void init() {
        Event event = eventRepository.selectLastEventId();
        if(event!=null) {
            Long lastId = event.getId();
            while(lastId>0) {
                List<EventProperty> eventProperties = eventPropertyRepository.filterEventProperties(lastId,1,10000);
                eventProperties.forEach(eventProperty -> {
                    EventPropertyCacheKey eventPropertyCacheKey = new EventPropertyCacheKey(eventProperty.getEventId(),eventProperty.getName());
                    Set<EventProperty> set = eventPropertyCache.get(eventPropertyCacheKey);
                    if(null == set) {
                        set = new HashSet<>();
                        eventPropertyCache.put(eventPropertyCacheKey, set);
                    }
                    set.addAll(eventPropertyRepository.findEventPropertyByEventIdAndName(eventPropertyCacheKey.getEventId(), eventPropertyCacheKey.getName()).stream().collect(Collectors.toSet()));
                });
                lastId--;
            }
        }
    }

    public void updateEventPropertySet(EventProperty eventProperty) {
        EventPropertyCacheKey eventPropertyCacheKey = EventPropertyCacheKey.builder().eventId(eventProperty.getEventId()).name(eventProperty.getName()).build();
        Set<EventProperty> eventProperties = eventPropertyCache.get(eventPropertyCacheKey);

        if (null == eventProperties) {
            eventProperties = new HashSet<>();
            eventPropertyCache.put(eventPropertyCacheKey, eventProperties);
        }

        if(!eventProperties.contains(eventProperty) && eventProperties.size() < EventPortalConstants.MAX_PROPERTIES_SET_SIZE_PER_KEY) {
            synchronized (EventPropertyCache.class.getName()) {
                eventProperty = updateCache(eventProperty);
                if (null != eventProperty) {
                    eventProperties.add(eventProperty);
                }
            }
        }
    }

    private EventProperty updateCache(EventProperty eventProperty) {

        boolean result = eventPropertyRepository.insertEventProperty(eventProperty.getEventId(),
                EventPortalConstants.EVENT_TYPE_EVENT_PARAMETER,eventProperty.getName(), eventProperty.getValue());

        return result ? EventProperty.builder().eventId(eventProperty.getEventId()).type(EventPortalConstants.EVENT_TYPE_EVENT_PARAMETER)
                .name(eventProperty.getName())
                .value(eventProperty.getValue())
                .build():null;

    }

    public Set<EventProperty> get(EventPropertyCacheKey eventPropertyCacheKey) {
        return eventPropertyCache.get(eventPropertyCacheKey);
    }
}
