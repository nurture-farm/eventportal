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

import java.text.SimpleDateFormat;

public class EventPortalConstants {

    public static final String DEFAULT_APP_NAME = "";

    public static final String DEFAULT_APP_CLEVERTAP_ID = "";
    public static final String REDIRECT_HOST_KEY = "rt";
    public static final String EVENT_PORTAL_TENANT = "event_portal";
    public static final String EVENT_SUB_TYPE = "eventSubType";
    public static final String EVENT_USER_ID = "userId";
    public static final String EVENT_USER_ID_2 = "user_id";
    public static final String EVENT_USER_TYPE = "userType";
    public static final String EVENT_DEVICE_ID = "deviceId";
    public static final String NURTURE_FARM = "nurture_farm";
    public static final String NURTURE_FARM_DS = "nurture_farm_ds";
    public static final String NURTURE_AFS_ADMIN_DS = "nurture_afs_admin_ds";
    public static final String NURTURE_AFS_ADMIN = "nurture_afs_admin";
    public static final String NURTURE_AFS_OPERATOR = "nurture_afs_operator";
    public static final String NURTURE_AFS_OPERATOR_DS = "nurture_afs_operator_ds";
    public static final String NURTURE_AFS_TECHNICIAN = "nurture_afs_technician";
    public static final String NURTURE_AFS_TECHNICIAN_DS = "nurture_afs_technician_ds";
    public static final String NURTURE_RETAIL = "nurture_retail";
    public static final String NURTURE_RETAIL_DS= "nurture_retail_ds";
    public static final String NURTURE_AFS_UNIMART = "nurture_afs_unimart";
    public static final String NURTURE_AFS_UNIMART_DS = "nurture_afs_unimart_ds";
    public static final String PACKAGE_NURTURE_FARM = "nurture.farm";
    public static final String PACKAGE_NURTURE_RETAIL = "com.nurture.retail";
    public static final String NURTURE_PARTNER = "nurture_partner";
    public static final String NURTURE_PARTNER_DS= "nurture_partner_ds";
    public static final String NURTURE_TRADE = "nurture_trade";
    public static final String NURTURE_TRADE_DS= "nurture_trade_ds";
    public static final String CLEVERTAP_DOMAIN_KEY = "X-WZRK-RD";
    public static final String CLEVERTAP_DOMAIN_VALUE = "wzrkt.com";
    public static final String CLEVERTAP_SPIKY_DOMAIN_KEY = "X-WZRK-SPIKY-RD";
    public static final String CLEVERTAP_SPIKY_DOMAIN_VALUE = "spiky.wzrkt.com";
    public static final String CLEVERTAP_ACCOUNT_ID ="X-CleverTap-Account-Id";
    public static final String CLEVERTAP_ACCOUNT_PASSCODE = "X-CleverTap-Passcode";
    public static final String ACCEPT = "Accept";
    public static final String ACCEPT_VALUE = "**";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String NF_EP = "NF_EP";
    public static final String NF_EP_MYSQL = "NF_EP_mysql";
    public static final String NF_EP_EVENT_READ_BASE = "NF_EP_event_read_base";
    public static final String NF_EP_EVENT_PROPERTY_READ_BASE = "NF_EP_event_property_read_base";
    public static final String EMPTY_STRING = "";
    public static final String filterEvents = "filter_events";
    public static final String uploadEvents = "upload_events";
    public static final String filterEventProperties = "filter_event_properties";
    public static final String findEventPropertyById = "find_event_property_by_id";

    public static final String pushToKafka = "push_to_kafka";

    public static final String consumerEvents = "consumer_event";

    public static final String getEvents = "get_events_cache";

    public static final String updateCache = "update_cache";
    public static final String APPS_FLYER_BASE_URI = "/apps-flyer/listen";
    public static final String APPS_FLYER_EVENT_TIME_KEY = "event_time";
    public static final String APPS_FLYER_APP_NAME_KEY = "app_name";
    public static final String APPS_FLYER_DEFAULT_APP_NAME = "not_defined";
    public static final String APPS_FLYER_ROOT_DIRECTORY = "APPS_FLYER_DATA/";
    public static final String APPS_FLYER_EVENTS = "APPS_FLYER_EVENTS";
    public static final String APPS_FLYER_HANDLE_MSG_TRACKER_NAME = "HandleAppsFlyerMessage";
    public static final String APPS_FLYER_SEND_MSG_BUS_TRACKER_NAME = "SendAppsFlyerEventsToMessageBus";

    public static final SimpleDateFormat APPS_FLYER_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    public static final SimpleDateFormat OUTPUT_DATE_FORMATTER = new SimpleDateFormat("yyyy/MM/dd");
    public static final String EVENT_SENT_MESSAGE_BUS = "event_sent_message_bus";
    public static final int MAX_PROPERTIES_SET_SIZE_PER_KEY = 50;
    public static final String EVENT_TYPE_EVENT_PARAMETER = "EVENT_PARAMETER";
}
