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

import com.fasterxml.jackson.annotation.JsonInclude;
import farm.nurture.core.contracts.common.enums.EventSubType;
import farm.nurture.eventportal.util.EventPortalConstants;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Event extends Message {

    String eventName;
    Map<String, Object> eventData;

    EventSubType eventSubType;

    public Event() {

    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Map<String, Object> getEventData() {
        return eventData;
    }

    public void setEventData(Map<String, Object> eventData) {
        this.eventData = eventData;
        //extract eventSubType and remove from properties
        if(eventData.containsKey(EventPortalConstants.EVENT_SUB_TYPE)) {
            this.setEventSubType(EventSubType.valueOf(eventData.get(EventPortalConstants.EVENT_SUB_TYPE).toString()));
            this.eventData.remove(EventPortalConstants.EVENT_SUB_TYPE);
        }

        //extract userId and remove from properties
        if(eventData.containsKey(EventPortalConstants.EVENT_USER_ID)) {
            this.setUserId(eventData.get(EventPortalConstants.EVENT_USER_ID).toString());
            this.eventData.remove(EventPortalConstants.EVENT_USER_ID);
        } else if(eventData.containsKey(EventPortalConstants.EVENT_USER_ID_2)) {
            this.setUserId(eventData.get(EventPortalConstants.EVENT_USER_ID_2).toString());
            this.eventData.remove(EventPortalConstants.EVENT_USER_ID_2);
        }

        //extract userType and remove from properties
        if(eventData.containsKey(EventPortalConstants.EVENT_USER_TYPE)) {
            this.setUserType(eventData.get(EventPortalConstants.EVENT_USER_TYPE).toString());
            this.eventData.remove(EventPortalConstants.EVENT_USER_TYPE);
        }

        //extract deviceId and remove from properties
        if(eventData.containsKey(EventPortalConstants.EVENT_DEVICE_ID)) {
            this.setDeviceId(eventData.get(EventPortalConstants.EVENT_DEVICE_ID).toString());
            this.eventData.remove(EventPortalConstants.EVENT_DEVICE_ID);
        }
    }

    public EventSubType getEventSubType() {
        return eventSubType;
    }

    public void setEventSubType(EventSubType eventSubType) {
        this.eventSubType = eventSubType;
    }

    public Event(Event event){
        this.setExternalId(event.getExternalId());
        this.setExternalSource(event.getExternalSource());
        this.setVersionCode(event.getVersionCode());
        this.setOsVersion(event.getOsVersion());
        this.setVersionName(event.getVersionName());
        this.setAppNameType(event.getAppNameType());
        if(!StringUtils.isEmpty(event.getEventName())) {
            this.setEventName(event.getEventName());
        }
        if(null != event.getEventData()) {
            this.setEventData(event.getEventData());
        }
    }

    @Override
    public String toString() {
        return "\nEvent{" +
                "eventName='" + getEventName() + '\'' +
                ", eventData=" + getEventData() +
                ", externalId='" + getExternalId() + '\'' +
                ", externalSource=" + getExternalSource() +
                ", versionCode=" + getVersionCode() +
                ", versionName='" + getVersionName() + '\'' +
                ", osVersion='" + getOsVersion() + '\'' +
                ", appName='" + getAppNameType() + '\'' +
                ", messageType=" + getMessageType() +
                ", sessionId=" + getSessionId() +
                ", timestamp=" + getTimestamp() +
                '}';
    }


}
