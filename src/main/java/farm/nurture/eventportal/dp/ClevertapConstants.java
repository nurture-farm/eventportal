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

public class ClevertapConstants {
    public static final String CLEVERTAP_UNIQUE_ID = "/g";
    public static final String APP_FIELDS = "/af";
    public static final String VERSION_CODE = "/af/Build";
    public static final String VERSION_NAME = "/af/Version";
    public static final String OS_VERSION = "/af/OS Version";
    public static final String PROFILE_USER_ID = "/profile/Identity";
    public static final String PROFILE_USER_TYPE = "/profile/userType";
    public static final String EVENT_TYPE = "/type";
    public static final String EVENT_NAME = "/evtName";
    public static final String SESSION_ID = "/s";
    public static final String TIMESTAMP = "/ep";
    public static final String EVENT_DATA = "/evtData";
    public static final String CLEVERTAP_ACCOUNT_ID = "/id";
    public static final String CLEVERTAP_REDIS_PREFIX = "clevertap_id_";
    public static final String HEADER_CLEVERTAP_ACCOUNT_ID = "X-CleverTap-Account-ID";
    public static final String EVENT_TYPE_PAGE = "page";
}
