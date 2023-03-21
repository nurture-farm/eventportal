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

package farm.nurture.eventportal.dao;

import farm.nurture.eventportal.model.EventProperty;
import farm.nurture.eventportal.util.EventPortalConstants;
import farm.nurture.infra.metrics.Metrics;
import farm.nurture.laminar.core.io.sql.dao.ReadBase;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EventPropertyReadBase extends ReadBase<EventProperty> {

    private static final String populateStringFailed = "populate_record_failed";

    public static final String ID = "id";
    public static final String EVENT_ID = "event_id";
    public static final String TYPE = "type";
    public static final String NAME = "name";
    public static final String VALUE = "value";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";

    private List<EventProperty> records = null;
    private final Metrics metrics = Metrics.getInstance();

    @Override
    protected List<EventProperty> populate() throws SQLException {
        if ( null == this.rs) {
            log.warn("EventProperty ResultSet is not initialized.");
            throw new SQLException("EventProperty ResultSet is not initialized.");
        }

        if ( null == records) records = new ArrayList<EventProperty>();
        this.rs.setFetchSize(1000);
        while (this.rs.next()) {
            recordsCount++;
            records.add(populateEventProperty());
        }
        return records;
    }


    private EventProperty populateEventProperty() {
        EventProperty eventProperty = null;
        try {
            EventProperty.EventPropertyBuilder builder = EventProperty.builder();
            builder.id(rs.getLong(ID));
            builder.eventId(rs.getLong(EVENT_ID));
            builder.type(rs.getString(TYPE));
            builder.name(rs.getString(NAME));
            builder.value(rs.getString(VALUE));
            builder.createdAt(rs.getTimestamp(CREATED_AT));
            builder.updatedAt(rs.getTimestamp(UPDATED_AT));
            eventProperty = builder.build();

        } catch (Exception e) {
            metrics.onIncrement(EventPortalConstants.NF_EP_EVENT_PROPERTY_READ_BASE, populateStringFailed);
            log.error("Unable to populate EventProperty from resultSet : {}", rs, e);
        }
        return eventProperty;
    }

    @Override
    protected EventProperty getFirstRow() throws SQLException {
        if ( null == this.rs) {
            log.error("EventProperty ResultSet is not initialized.");
            throw new SQLException("EventProperty ResultSet is not initialized.");
        }

        EventProperty eventProperty = null;
        this.rs.setFetchSize(1);

        while (this.rs.next()) {
            recordsCount++;
            eventProperty = populateEventProperty();
        }
        return eventProperty;
    }

    @Override
    protected int getRecordsCount() {
        return recordsCount;
    }


}


