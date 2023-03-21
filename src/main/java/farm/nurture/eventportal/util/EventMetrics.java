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

package farm.nurture.eventportal.util;

import farm.nurture.infra.metrics.IMetricCounter;
import farm.nurture.infra.metrics.IMetricSummary;
import farm.nurture.infra.metrics.MetricFactory;

public class EventMetrics {
    static EventMetrics instance;
    public IMetricCounter eventsCallReceived;
    public IMetricCounter totalEventsReceived;
    public IMetricCounter eventsWithoutUserID;
    public IMetricCounter addNewEventFailures;
    public IMetricCounter eventsWithoutDeviceID;
    public IMetricCounter eventRequestsSentToClevertap;
    public IMetricCounter eventRequestsSentToClevertapResponse;

    private EventMetrics () {
        eventsCallReceived = MetricFactory.getCounter("", "event_requests");
        totalEventsReceived = MetricFactory.getCounter("", "total_events_received");
        eventsWithoutUserID = MetricFactory.getCounter("","events_without_user_id");
        addNewEventFailures = MetricFactory.getCounter("","add_new_event_failures");
        eventsWithoutDeviceID = MetricFactory.getCounter("","events_without_device_id");
        eventRequestsSentToClevertap = MetricFactory.getCounter("","events_sent_to_clevertap");
        eventRequestsSentToClevertapResponse = MetricFactory.getCounter("","events_sent_to_clevertap_response","status_code");
    }

    public static EventMetrics getInstance() {
        if ( null != instance) return instance;
        synchronized ( EventMetrics.class.getName()) {
            if ( null != instance) return instance;
            instance = new EventMetrics();
        }
        return instance;
    }
}
