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

package farm.nurture.eventportal.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import farm.nurture.eventportal.dto.CleverTapUploadRequest;
import farm.nurture.eventportal.dto.CleverTapUploadResponse;
import farm.nurture.eventportal.dto.HttpClientRequest;
import farm.nurture.eventportal.server.EventPortalConfig;
import farm.nurture.eventportal.util.EventPortalConstants;
import farm.nurture.infra.metrics.MetricTracker;
import farm.nurture.util.http.HttpUtils;
import farm.nurture.util.http.client.NFAsyncHttpClient;
import farm.nurture.util.http.client.NFHttpClient;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static farm.nurture.eventportal.util.EventPortalConstants.*;

@Slf4j
@Singleton
public class CleverTapService {

    @Inject
    private NFHttpClient nfHttpClient;


    public static HttpClientRequest<CleverTapUploadRequest> createHttpRequest(CleverTapUploadRequest cleverTapUploadRequest) {
        HttpClientRequest<CleverTapUploadRequest> httpClientRequest = new HttpClientRequest<>();
        httpClientRequest.setRequestBody(cleverTapUploadRequest);
        httpClientRequest.setMethod(HttpUtils.HttpMethod.POST);
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, APPLICATION_JSON);
        headers.put(ACCEPT, ACCEPT_VALUE);
        headers.put(CLEVERTAP_ACCOUNT_ID, EventPortalConfig.CLEVERTAP_ACCOUNT_ID.get());
        headers.put(CLEVERTAP_ACCOUNT_PASSCODE, EventPortalConfig.CLEVERTAP_ACCOUNT_PASSCODE.get());
        httpClientRequest.setHeaders(headers);
        httpClientRequest.setUrl(EventPortalConfig.CLEVERTAP_URI.get());
        return httpClientRequest;

    }

    public boolean sendEventToCleverTap(CleverTapUploadRequest cleverTapUploadRequest) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(NF_EP, uploadEvents);
        try {
            HttpClientRequest<CleverTapUploadRequest> httpClientRequest = createHttpRequest(cleverTapUploadRequest);
            log.info("Sending nfHttpClient Request to cleverTap {} " , httpClientRequest);
            CleverTapUploadResponse response = nfHttpClient.sendMessage(httpClientRequest.getMethod(), httpClientRequest.getUrl(),
                   null, httpClientRequest.getHeaders(),
                    httpClientRequest.getRequestBody(), CleverTapUploadResponse.class);
            log.info("CleverTap Upload Response are {} ", response);
            if(response.getStatus().equals("success")){
                success = true;
                log.info("Successfully sent request to cleverTap");
            }

        } catch (Exception e) {
            log.error("Error in sending cleverTap upload event : {}",cleverTapUploadRequest);
        } finally {
            tracker.stop(success);
        }
        return success;
    }
}
