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
import farm.nurture.eventportal.dao.EventReadBase;
import farm.nurture.eventportal.model.Event;
import farm.nurture.eventportal.util.EventPortalConstants;
import farm.nurture.infra.metrics.MetricTracker;
import farm.nurture.laminar.core.io.sql.dao.WriteBase;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Singleton
@Slf4j
public class EventRepository {

    @Inject
    private QueryBuilder queryBuilder;

    private static final String filterEvent = "filter_event";
    private static final String filterEvents = "filter_events";
    private static final String selectLastEventByNamespace = "select_last_event_by_namespace";
    private static final String insertEvent = "insert_event";
    private static final String selectLastEvent = "select_last_event";

    protected static final String partialFilterEventSql = "Select id,namespace,name,`index`,created_at,updated_at from events where true and deleted_at is NULL ";

    private static final String selectLastEventByNamespaceSql = "Select id,namespace,name,`index`,created_at,updated_at from events where namespace = ? and deleted_at is NULL order by id desc limit 1";

    private static final String insertEventSql = "Insert into events(namespace,name,`index`) values(?,?,?)";

    private static final String selectLastEventIdSql = "Select id,namespace,name,`index`,created_at,updated_at from events order by id desc limit 1";


    public List<Event> filterEvents(String namespace, String name, int pageNumber, int limit) {

        boolean success = false;
        MetricTracker metricTracker = new MetricTracker(EventPortalConstants.NF_EP_MYSQL,filterEvents);
        EventReadBase readBase = new EventReadBase();
        List<Event> events = null;

        try {
            QueryBuilder.Query query = queryBuilder.buildFilterEventsQuery(namespace, name, true, pageNumber, limit);
            events = readBase.execute(query.getQuery(), query.getParams().toArray());
            success = true;
        } catch (Exception e) {
            log.error("Error in fetching events. Namespace : {}, Name : {}", e, namespace, name);
        } finally {
            metricTracker.stop(success);
        }
        return events;
    }

    public Event filterEvent(String namespace, String name) {

        boolean success = false;
        MetricTracker metricTracker = new MetricTracker(EventPortalConstants.NF_EP_MYSQL,filterEvent);
        EventReadBase readBase = new EventReadBase();
        Event event = null;

        try {
            QueryBuilder.Query query = queryBuilder.buildFilterEventsQuery(namespace, name, false,0, 0);
            Object[] params = query.getParams().toArray();
            event = readBase.selectByUniqueKey(query.getQuery(), params);
            success = true;
        } catch (Exception e) {
            log.error("Error in fetching event. Namespace : {}, Name : {}", e, namespace, name);
        } finally {
            metricTracker.stop(success);
        }
        return event;
    }

    public Event selectLastEventByNamespace(String namespace) {

        boolean success = false;
        MetricTracker metricTracker = new MetricTracker(EventPortalConstants.NF_EP_MYSQL,selectLastEventByNamespace);
        EventReadBase readBase = new EventReadBase();
        Event event = null;

        try {
            event = readBase.selectByUniqueKey(selectLastEventByNamespaceSql, Arrays.asList(namespace).toArray());
            success = true;
        } catch (Exception e) {
            log.error("Error in fetching last event by namespace. Namespace : {}", e, namespace);
        } finally {
            metricTracker.stop(success);
        }
        return event;
    }

    public boolean insertEvent(String namespace, String name, Integer index) {
        boolean success = false;
        MetricTracker metricTracker = new MetricTracker(EventPortalConstants.NF_EP_MYSQL,insertEvent);
        WriteBase writeBase = new WriteBase();
        try {
            writeBase.execute(insertEventSql, Arrays.asList(namespace, name, index));
            success = true;
        } catch (Exception e) {
            log.error("Error in inserting event. Namespace : {], name : {}, index : {}", e, namespace, name, index);
        } finally {
            metricTracker.stop(success);
        }
        return success;
    }

    public Event selectLastEventId() {

        boolean success = false;
        MetricTracker metricTracker = new MetricTracker(EventPortalConstants.NF_EP_MYSQL,selectLastEvent);
        EventReadBase readBase = new EventReadBase();
        Event event = null;

        try {
            event = readBase.selectByUniqueKey(selectLastEventIdSql, Arrays.asList().toArray());
            success = true;
        } catch (Exception e) {
            log.error("Error in fetching last event", e);
        } finally {
            metricTracker.stop(success);
        }
        return event;
    }
}
