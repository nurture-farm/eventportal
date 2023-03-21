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

import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.eventportal.util.EventPortalConstants;

import java.util.HashMap;
import java.util.Map;


public enum AppNameType{
    DEFAULT_APP("", EventPortalConstants.DEFAULT_APP_NAME, false),
    DEFAULT_APP_CLEVERTAP(EventPortalConstants.DEFAULT_APP_CLEVERTAP_ID, EventPortalConstants.DEFAULT_APP_NAME, true);


    private final String clevertapAccountId;
    private final String appName;
    private final boolean isClevertapAllowed;
    private static Map<String, AppNameType> keyValueMap;

    AppNameType(String clevertapAccountId, String appName, boolean isClevertapAllowed) {
        this.clevertapAccountId = clevertapAccountId;
        this.appName = appName;
        this.isClevertapAllowed = isClevertapAllowed;
    }

    public static AppNameType getAppNameType(String key){
        if(keyValueMap == null){
            initMapping();
        }
        return keyValueMap.get(key);
    }

    private static void initMapping(){
        keyValueMap = new HashMap<>();
        for(AppNameType s : values()){
            keyValueMap.put(s.clevertapAccountId, s);
        }
    }

    public String getClevertapAccountId(){
        return clevertapAccountId;
    }

    public String getAppName(){
        return appName;
    }
    public boolean isClevertapAllowed() {
        return isClevertapAllowed;
    }
}
