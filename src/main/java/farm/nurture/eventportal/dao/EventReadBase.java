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

import farm.nurture.eventportal.model.Event;
import farm.nurture.eventportal.util.EventPortalConstants;
import farm.nurture.infra.metrics.Metrics;
import farm.nurture.laminar.core.io.sql.dao.ReadBase;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EventReadBase extends ReadBase<Event> {

    private static final String populateStringFailed = "populate_record_failed";

    public static final String ID = "id";
    public static final String NAMESPACE = "namespace";
    public static final String NAME = "name";
    public static final String INDEX = "index";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";

    private List<Event> records = null;
    private final Metrics metrics = Metrics.getInstance();

    @Override
    protected List<Event> populate() throws SQLException {
        if ( null == this.rs) {
            log.warn("Event ResultSet is not initialized.");
            throw new SQLException("Event ResultSet is not initialized.");
        }

        if ( null == records) records = new ArrayList<Event>();
        this.rs.setFetchSize(1000);
        while (this.rs.next()) {
            recordsCount++;
            records.add(populateEvent());
        }
        return records;
    }


    private Event populateEvent() {
        Event event = null;
        try {
            Event.EventBuilder builder = Event.builder();
            builder.id(rs.getLong(ID));
            builder.namespace(rs.getString(NAMESPACE));
            builder.name(rs.getString(NAME));
            builder.index(rs.getInt(INDEX));
            builder.createdAt(rs.getTimestamp(CREATED_AT));
            builder.updatedAt(rs.getTimestamp(UPDATED_AT));
            event = builder.build();

        } catch (Exception e) {
            metrics.onIncrement(EventPortalConstants.NF_EP_EVENT_READ_BASE, populateStringFailed);
            log.error("Unable to populate Event from resultSet : {}", rs, e);
        }
        return event;
    }

    @Override
    protected Event getFirstRow() throws SQLException {
        if ( null == this.rs) {
            log.error("Event ResultSet is not initialized.");
            throw new SQLException("Event ResultSet is not initialized.");
        }

        Event event = null;
        this.rs.setFetchSize(1);

        while (this.rs.next()) {
            recordsCount++;
            event = populateEvent();
        }
        return event;
    }

    @Override
    protected int getRecordsCount() {
        return recordsCount;
    }


}
