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

package farm.nurture.eventportal.dp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import farm.nurture.eventportal.util.EventMetrics;
import farm.nurture.infra.util.Logger;
import farm.nurture.infra.util.LoggerFactory;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomDeserializer {

    private static final Logger logger = LoggerFactory.getLogger(CustomDeserializer.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Function name : findExternalId, extracts the externalId and externalSource from incoming Json data
     * @param firstNode : Jackson parser tree first node
     * @param eventObject : event object for the ith json object
     * @return : boolean result
     */
    private static boolean setExternalId(JsonNode firstNode, Event eventObject) {
        boolean result = true;
        if (firstNode.at(ClevertapConstants.CLEVERTAP_UNIQUE_ID).isMissingNode()) {
            result = false;
        } else {
            JsonNode keyNode = firstNode.at(ClevertapConstants.CLEVERTAP_UNIQUE_ID);
            eventObject.setExternalId(keyNode.asText());
            eventObject.setExternalSource(Message.ExternalSource.CLEVERTAP);
        }
        return result;
    }

    private static boolean setAppFields(JsonNode firstNode, Event eventObject) {
        boolean result = true;
        if (firstNode.at(ClevertapConstants.APP_FIELDS).isMissingNode()) {
            result = false;
        } else {
            JsonNode keyNode = firstNode.at(ClevertapConstants.VERSION_CODE);
            eventObject.setVersionCode(keyNode.asInt());

            keyNode = firstNode.at(ClevertapConstants.VERSION_NAME);
            eventObject.setVersionName(keyNode.asText());

            keyNode = firstNode.at(ClevertapConstants.OS_VERSION);
            eventObject.setOsVersion(keyNode.asText());
        }

        return result;
    }

    private static boolean setAppName(JsonNode firstNode, Event eventObject) {
        boolean result = false;
        if (!firstNode.at(ClevertapConstants.CLEVERTAP_ACCOUNT_ID).isMissingNode()) {
            JsonNode keyNode = firstNode.at(ClevertapConstants.CLEVERTAP_ACCOUNT_ID);
            String id = keyNode.asText();
            if (AppNameType.getAppNameType(id) == null) {
                logger.warn("No match found with any app name for the received id : {}", id);
            } else {
                eventObject.setAppNameType(AppNameType.getAppNameType(id));
                result = true;
            }
        }
        return result;
    }

    private static void setPageEvents(JsonNode currentNode) {
        JsonNode eventType = currentNode.get(ClevertapConstants.EVENT_TYPE);
        if(null != eventType && eventType.asText().equals(ClevertapConstants.EVENT_TYPE_PAGE)) {
            //this is a page event from client we are logging it in to handle in next release
            logger.info("Received Page event {}", currentNode);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setEventData(JsonNode currentNode, Event eventObject, List<Event> eventList) {
        //messageType, sessionId, timestamp
        if (!currentNode.at(ClevertapConstants.EVENT_NAME).isMissingNode()) {
            eventObject.setMessageType(Message.MessageType.EVENT);

            JsonNode keyNode = currentNode.at(ClevertapConstants.EVENT_NAME);
            eventObject.setEventName(keyNode.asText().replace(' ', '_').toUpperCase());

            keyNode = currentNode.at(ClevertapConstants.SESSION_ID);
            eventObject.setSessionId(keyNode.asLong());

            keyNode = currentNode.at(ClevertapConstants.TIMESTAMP);
            eventObject.setTimestamp(keyNode.asLong() * 1000L);

            keyNode = currentNode.at(ClevertapConstants.EVENT_DATA);
            Map<String, Object> mp = mapper.convertValue(keyNode, Map.class);
            eventObject.setEventData(mp);

            //set default values or warn if we have some missing data
            if(StringUtils.isEmpty(eventObject.getUserId())) {
                logger.info("userId not found in event Properties of event: {}, appName : {}", eventObject.getEventName(), eventObject.getAppNameType().name());
                EventMetrics.getInstance().eventsWithoutUserID.increment();
            }

            if(StringUtils.isEmpty(eventObject.getDeviceId())){
                logger.info("device Id missing for the the event : {}, appName : {}", eventObject.getEventName(), eventObject.getAppNameType().name());
                EventMetrics.getInstance().eventsWithoutDeviceID.increment();
            }
            eventList.add(eventObject);
            EventMetrics.getInstance().totalEventsReceived.increment();
        }
    }

    public static List<Event> deserialize(String json) throws IOException {
        JsonNode node = mapper.readTree(json);
        List<Event> eventList = new ArrayList<>();
        if(node == null){
            logger.debug("JSON read parser node is null");
            return eventList;
        }
        else if(node.isObject()){
            logger.debug("JSON received is an object");
        }
        else if(node.isArray()){
            logger.debug("JSON received is an array");
        }


        //Firstly extract the common data from the json(externalId, appName, externalSource, versionName. versionCode, osVersion)
        Event eventCommonData = new Event();
        JsonNode firstNode = node.get(0);
        if(!setExternalId(firstNode, eventCommonData)){
            logger.warn("Incoming data does not contain mandatory data : external Id. cannot send to s3");
            return eventList;
        }
        if(!setAppFields(firstNode, eventCommonData)){
            logger.warn("Incoming data does not contain mandatory data : app_fields. cannot send to s3");
            return eventList;
        }
        if(!setAppName(firstNode, eventCommonData)){
            logger.warn("Incoming data does not contain listed app name. cannot send to s3");
            return eventList;
        }

        //Now, since we can have multiple events in a single request, we create a list of events that share the common data.
        for(int i = 0; i < node.size(); i++) {
            Event eventObject = new Event(eventCommonData);
            JsonNode currentNode = node.get(i);
            setPageEvents(currentNode);
            setEventData(currentNode, eventObject, eventList);
        }
        return eventList;
    }
}
