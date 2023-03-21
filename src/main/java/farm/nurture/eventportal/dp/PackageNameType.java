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

import farm.nurture.eventportal.util.EventPortalConstants;

import java.util.HashMap;

public class PackageNameType {
    private static final HashMap<String, String> packageNameByAppName = new HashMap<>();
    static {
        packageNameByAppName.put(EventPortalConstants.PACKAGE_NURTURE_FARM, EventPortalConstants.NURTURE_FARM);
        packageNameByAppName.put(EventPortalConstants.PACKAGE_NURTURE_RETAIL, EventPortalConstants.NURTURE_RETAIL);
    }
    public static HashMap<String, String> getPackageNameByAppName() {
        return packageNameByAppName;
    }
}
