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

package farm.nurture.eventportal.repository;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import farm.nurture.eventportal.dao.EventPropertyReadBase;
import farm.nurture.eventportal.dao.EventReadBase;
import farm.nurture.eventportal.model.Event;
import farm.nurture.eventportal.model.EventProperty;
import farm.nurture.eventportal.util.EventPortalConstants;
import farm.nurture.infra.metrics.MetricTracker;
import farm.nurture.laminar.core.io.sql.dao.WriteBase;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
@Slf4j
public class EventPropertyRepository {

    @Inject
    private QueryBuilder queryBuilder;

    private static final String filterEventProperties = "filter_event_properties";

    private static final String findEventPropertyByEventIdAndName = "find_event_property_by_event_id_and_name";

    private static final String selectEventPropertyByEventIdAndNameAndValue = "select_event_property_by_event_id_and_name_value";

    private static final String insertEventProperty = "insert_event_property";

    protected static final String selectEventPropertyByEventId = "SELECT id,event_id,type,name,value,created_at,updated_at FROM event_properties WHERE event_id = ? GROUP BY event_id, name ";

    private static final String selectEventPropertyByEventIdAndName = "SELECT id,event_id,type,name,value,created_at,updated_at FROM event_properties WHERE event_id = ? AND name = ?";

    private static final String insertEventPropertySql = "INSERT into event_properties(event_id,type,name,value) VALUES(?,?,?,?)";

    private static final String selectEventPropertyByEventIdAndNameAndValueSql = "SELECT id,event_id,type,name,`value`,created_at,updated_at FROM event_properties WHERE event_id = ? " +
            "AND name = ? AND value = ?";

    public List<EventProperty> filterEventProperties(Long eventId, int pageNumber, int limit) {

        boolean success = false;
        MetricTracker metricTracker = new MetricTracker(EventPortalConstants.NF_EP_MYSQL,filterEventProperties);
        EventPropertyReadBase readBase = new EventPropertyReadBase();
        List<EventProperty> eventProperties = null;
        QueryBuilder.Query query = queryBuilder.buildFilterEventPropertiesQuery(eventId, true, pageNumber, limit);

        try {
            eventProperties = readBase.execute(query.getQuery(), Arrays.asList(query.getParams().toArray()));
            success = true;
        } catch (Exception e) {
            log.error("Error in fetching eventProperties. EventId : {}, Pagenumber : {}, Limit : {}", e, eventId, pageNumber, limit);
        } finally {
            metricTracker.stop(success);
        }
        return eventProperties;
    }

    public Set<EventProperty> findEventPropertyByEventIdAndName(Long eventId, String name) {

        boolean success = false;
        MetricTracker metricTracker = new MetricTracker(EventPortalConstants.NF_EP_MYSQL,findEventPropertyByEventIdAndName);
        EventPropertyReadBase readBase = new EventPropertyReadBase();
        Set<EventProperty> eventProperties = null;
        try {
            eventProperties = new HashSet<>(readBase.execute(selectEventPropertyByEventIdAndName, Arrays.asList(eventId, name)));
            success = true;
        } catch (Exception e) {
            log.error("Error in fetching findEventPropertyByEventIdAndName. EventId : {}, name : {}", e, eventId, name);
        } finally {
            metricTracker.stop(success);
        }
        return eventProperties;
    }

    public EventProperty findEventPropertyByEventIdAndNameAndValue(Long eventId, String name, String value) {

        boolean success = false;
        MetricTracker metricTracker = new MetricTracker(EventPortalConstants.NF_EP_MYSQL,selectEventPropertyByEventIdAndNameAndValue);
        EventPropertyReadBase readBase = new EventPropertyReadBase();
        EventProperty eventProperty = null;

        try {
            eventProperty = readBase.selectByUniqueKey(selectEventPropertyByEventIdAndNameAndValueSql, Arrays.asList(eventId,name,value).toArray());
            success = true;
        } catch (Exception e) {
            log.error("Error in selecting EventProperty By EventId and Name and Value ", e);
        } finally {
            metricTracker.stop(success);
        }
        return eventProperty;
    }

    public boolean insertEventProperty(Long eventId, String type, String name, String value) {
        boolean success = false;
        MetricTracker metricTracker = new MetricTracker(EventPortalConstants.NF_EP_MYSQL,insertEventProperty);
        WriteBase writeBase = new WriteBase();
        try {
            writeBase.execute(insertEventPropertySql, Arrays.asList(eventId,type,name,value));
            success = true;
        } catch (SQLIntegrityConstraintViolationException e) {
            //
        }  catch (Exception e) {
            log.error("Error in inserting event property. EventId : {}, type : {}, name : {}, value : {}", e, eventId, type, name, value);
        } finally {
            metricTracker.stop(success);
        }
        return success;
    }
}
