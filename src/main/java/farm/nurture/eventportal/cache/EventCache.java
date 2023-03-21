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
import farm.nurture.core.contracts.common.enums.NameSpace;
import farm.nurture.eventportal.dp.Consumer;
import farm.nurture.eventportal.model.Event;
import farm.nurture.eventportal.repository.EventRepository;
import farm.nurture.eventportal.util.EventPortalConstants;
import farm.nurture.infra.metrics.MetricTracker;
import farm.nurture.infra.util.Logger;
import farm.nurture.infra.util.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class EventCache {

    @Inject
    private EventRepository eventRepository;
    private static final Logger logger = LoggerFactory.getLogger(EventCache.class);

    private LoadingCache<EventCacheKey, Event> eventCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .refreshAfterWrite(1, TimeUnit.HOURS)
            .build(key -> eventRepository.filterEvent(key.getNamespace(), key.getName()));

    public void init() {
        for(NameSpace nameSpace:NameSpace.values()) {
            if(nameSpace!=NameSpace.NO_NAMESPACE) {
                eventRepository.filterEvents(nameSpace.name(), EventPortalConstants.EMPTY_STRING, 1, 100000).
                        forEach(event -> eventCache.put(new EventCacheKey(event.getNamespace(),event.getName()), event));
            }
        }
    }

    public Event getEvent(EventCacheKey eventCacheKey) {

        Event event = eventCache.get(eventCacheKey);
        MetricTracker tracker = new MetricTracker(EventPortalConstants.NF_EP, EventPortalConstants.getEvents);
        boolean success = false;
        try {
            if (event == null) {
                synchronized (EventCache.class.getName()) {
                    int count = 3; //numOfTries
                    while (count > 0) {
                        event = updateCache(eventCacheKey);
                        if (event != null) {
                            eventCache.put(eventCacheKey, event);
                            break;
                        }
                        --count;
                    }
                }
            }
            success = true;
        } catch (Exception e) {
            logger.error("Error in getEvent() , eventCacheKey : {}", eventCacheKey, e);
        } finally {
            tracker.stop(success);
        }
        return event;
    }

    private Event updateCache(EventCacheKey eventCacheKey) {

        String namespace = eventCacheKey.getNamespace();
        Event lastEvent = eventRepository.selectLastEventByNamespace(namespace);
        int index = lastEvent!=null ? lastEvent.getIndex()+1:1;
        MetricTracker tracker = new MetricTracker(EventPortalConstants.NF_EP, EventPortalConstants.updateCache);
        boolean success = false;

        boolean result = eventRepository.insertEvent(eventCacheKey.getNamespace(), eventCacheKey.getName(), index);

        try {
            if (!result) {
                List<Event> events = eventRepository.filterEvents(namespace, EventPortalConstants.EMPTY_STRING, 1, 100000);

                index = 1;

                for (Event event : events) {
                    if (event.getIndex() != index) {
                        index = event.getIndex();
                        break;
                    }
                    ++index;
                }
                result = eventRepository.insertEvent(eventCacheKey.getNamespace(), eventCacheKey.getName(), index);
            }
            success = true;
        }catch (Exception e) {
            logger.error("Error in Update Cache , eventCacheKey : {}", eventCacheKey, e);
        } finally {
            tracker.stop(success);
        }

        return result ? Event.builder().name(eventCacheKey.getName()).namespace(eventCacheKey.getNamespace()).index(index).build():null;

    }
}
