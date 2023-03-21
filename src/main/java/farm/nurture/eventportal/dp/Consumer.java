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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import farm.nurture.core.contracts.common.ActorID;
import farm.nurture.core.contracts.common.AppNameType;
import farm.nurture.core.contracts.common.DataPlatformMessage;
import farm.nurture.core.contracts.common.enums.*;
import farm.nurture.eventportal.cache.EventCache;
import farm.nurture.eventportal.cache.EventCacheKey;
import farm.nurture.eventportal.cache.EventPropertyCache;
import farm.nurture.eventportal.kafka.producer.IKafkaProducerWrapperService;
import farm.nurture.eventportal.model.EventProperty;
import farm.nurture.eventportal.util.CommonUtilities;
import farm.nurture.eventportal.util.EventPortalConstants;
import farm.nurture.infra.metrics.MetricTracker;
import farm.nurture.infra.metrics.Metrics;
import farm.nurture.infra.util.Logger;
import farm.nurture.infra.util.LoggerFactory;
import farm.nurture.util.serializer.Serializer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

public class Consumer implements Runnable {

    public static final String CMS_TOPIC_NAME = "user_events"; // For content management service
    public static final String SEGMENTATON_TOPIC_NAME = "user_click_events"; // For Segmentation service
    public static final String RETAIL = "retail";
    public static final String FARM = "farm";
    public static final String SUSTAIN = "sustain";
    public static final String TRADE = "trade";
    public static final String PARTNER = "partner";
    public static final String NO_NAMESPACE_MAPPING = "no_namespace_mapping";
    public static final String NAMESPACE_MAPPING = "namespace_mapping";
    public static final String LABEL_APP_NAME = "appName";
    public static final String LABEL_NAMESPACE = "namespace";
    public static final String[] LABEL_NO_NAMESPACE_MAPPING = {LABEL_APP_NAME};
    public static final String[] LABEL_NAMESPACE_MAPPING = {LABEL_NAMESPACE, LABEL_APP_NAME};

    @Inject
    IKafkaProducerWrapperService kafkaProducerWrapperService;

    @Inject
    private EventCache eventCache;

    @Inject
    private EventPropertyCache eventPropertyCache;

    private final Metrics metrics = Metrics.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    public static final String DEFAULT_TIMEZONE = "Asia/Kolkata";
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of(DEFAULT_TIMEZONE);
    public static final String DEFAULT_PARTITION = "defaultUserPartition";

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static StorageFolderPath folderPath = new StorageFolderPath();
    private static final String CONST_APP_INSTALL_REPORTS = "APP_INSTALL_REPORTS";
    private static final Pattern ALPHA_NUMERIC_REGEX_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]*$");

    private final BlockingQueue<RequestData> queue;

    private final ExecutorService executorService;
    public Consumer(BlockingQueue<RequestData> queue , ExecutorService executorService) {
        this.queue = queue;
        this.executorService = executorService;
    }

    public void put(RequestData request) throws InterruptedException {
        this.queue.put(request);
    }
    /**
     function name: isAlphaNumeric()
     args: string
     desc: Makes sure the string only contains alphanumeric characters or underscore(_) only.
     returns false if anything else apart from chars mentioned above are found.
     e.g.:
     HELLO_World --> returns true
     HELLO_&!_world --> returns false
     **/
    private boolean isAlphaNumeric(String s) {
        return s != null && ALPHA_NUMERIC_REGEX_PATTERN.matcher(s).matches();
    }

    @Override
    public void run() {
        while (true) {
            try {
                RequestData requestBody = queue.take();
                if (requestBody.requestType == RequestType.APP_INSTALL_REPORT) {
                    sendAppInstallReportsToMessageBus(requestBody.getData());
                } else if (requestBody.requestType == RequestType.APPS_FLYER_EVENT) {
                    sendAppsFlyerEventsToMessageBus(requestBody.getData());
                } else {
                    List<Event> eventList = CustomDeserializer.deserialize(requestBody.getData());

                    for (Event event : eventList) {
                        final String eventName = event.getEventName();
                        //ignoring events having special chars as hive partitions for such events in AWS glue are not allowed
                        if (!isAlphaNumeric(eventName)){
                            logger.info("ignoring invalid event : {}", eventName);
                            continue;
                        }
                        NameSpace nameSpace = NameSpace.NO_NAMESPACE;
                        farm.nurture.eventportal.model.Event modelEvent = null;
                        boolean success = false;
                        MetricTracker tracker = new MetricTracker(EventPortalConstants.NF_EP, EventPortalConstants.consumerEvents);
                        try {

                            final String appName = event.getAppNameType().getAppName();
                            nameSpace = getEventNamespace(appName);
                            modelEvent = eventCache.getEvent(EventCacheKey.builder().namespace(nameSpace.name()).name(eventName).build());

                            final String eventString = Serializer.DEFAULT_JSON_SERIALIZER.serialize(event);
                            byte[] dataAsBytesArray = eventString.getBytes(StandardCharsets.UTF_8);
                            //folderpath
                            folderPath.setAppName(appName);
                            folderPath.setEventName(eventName);
                            long date = event.getTimestamp();
                            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(date), DEFAULT_ZONE_ID);
                            String formattedDate = dateTime.format(dateFormat);
                            folderPath.setDate(formattedDate);
                            String finalStorageFolderPath = folderPath.getFinalStorageFolderPath();
                            //what happens if a request fails
                            MessageBusService.getInstance().sendMessage(dataAsBytesArray, finalStorageFolderPath);
                            logger.debug("Sent event {} to s3 path : {}", eventString, finalStorageFolderPath);
                            success = true;

                        } catch (Exception e) {
                            logger.error("Error in processing event : {}", event, e);
                        } finally {
                            tracker.stop(success);
                            pushToKafkaAsync(event, nameSpace, modelEvent);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error in processing event list", e);
            }
        }
    }

    private void pushToKafkaAsync(Event event, NameSpace namespace, farm.nurture.eventportal.model.Event modelEvent) {

        executorService.submit(() -> {
            MetricTracker tracker = new MetricTracker(EventPortalConstants.NF_EP, EventPortalConstants.pushToKafka);
            boolean success = false;
            try {
                pushToKafka(event, namespace, modelEvent);
                success = true;
            } catch (Exception e) {
                logger.error("Exception in pushing event to Kafka", e);
            }finally {
                tracker.stop(success);
            }
        });
    }

    private NameSpace getEventNamespace(String appName) {
        return NameSpace.NO_NAMESPACE;
    }

    private void pushToKafka(Event event, NameSpace namespace, farm.nurture.eventportal.model.Event modelEvent) {

        final String appName = event.getAppNameType().getAppName();
        final String eventName = event.getEventName();

        for (Map.Entry<String, Object> entry : event.getEventData().entrySet()) {
            eventPropertyCache.updateEventPropertySet(EventProperty.builder()
                    .eventId(modelEvent.getId().longValue())
                    .name(entry.getKey())
                    .value(entry.getValue().toString())
                    .type(EventPortalConstants.EVENT_TYPE_EVENT_PARAMETER)
                    .build());
        }
        final String eventDataString = Serializer.DEFAULT_JSON_SERIALIZER.serialize(event.getEventData());
        logger.debug("Event name: {}, Event Data : {}", eventName, eventDataString);

        //Building the DPMessage
        DataPlatformMessage.Builder dataPlatformMessageBuilder = DataPlatformMessage.newBuilder()
                .setEventName(eventName).setExternalId(event.getExternalId())
                .setDpSource(DPSource.CLEVERTAP).setVersionCode(event.getVersionCode())
                .setVersionName(event.getVersionName()).setOsVersion(event.getOsVersion())
                .setAppNameType(AppNameType.newBuilder().setValue(appName)
                        .setKey(event.getAppNameType().getClevertapAccountId())) //app name type
                .setEventType(EventType.EVENT) // always event
                .setSessionId(event.getSessionId()).setTimestamp(Timestamp.newBuilder().setSeconds(event.getTimestamp()))
                .setEventData(ByteString.copyFrom(eventDataString.getBytes()))  //converted to Byte String
                .setNamespace(namespace)
                .setEventIndex(modelEvent.getIndex());
        if (event.getEventSubType() != null && event.getEventSubType() != EventSubType.NO_EVENT_SUBTYPE) {
            dataPlatformMessageBuilder.setEventSubType(event.getEventSubType());
        }
        String partitionKey = DEFAULT_PARTITION;

        if (event.getUserId() != null) {
            dataPlatformMessageBuilder.setActor(ActorID.newBuilder()
                    .setActorId(Long.parseLong(event.getUserId()))
                    .setActorType(ActorType.valueOf(event.getUserType())));
            partitionKey = event.getUserId();
        }
        kafkaProducerWrapperService.pushByteArrayMessage(dataPlatformMessageBuilder.build().toByteArray(), SEGMENTATON_TOPIC_NAME, partitionKey, 3);
        logger.debug("Successfully pushed to Kafka. eventName : {}", eventName);
    }

    private void sendAppsFlyerEventsToMessageBus(String requestBody) {
        MetricTracker tracker = new MetricTracker(EventPortalConstants.NF_EP, EventPortalConstants.APPS_FLYER_SEND_MSG_BUS_TRACKER_NAME);
        try {
            JsonObject requestJson = JsonParser.parseString(requestBody).getAsJsonObject();

            folderPath.setAppName(EventPortalConstants.APPS_FLYER_ROOT_DIRECTORY + PackageNameType.getPackageNameByAppName().getOrDefault(requestJson.get(EventPortalConstants.APPS_FLYER_APP_NAME_KEY).getAsString(), EventPortalConstants.APPS_FLYER_DEFAULT_APP_NAME));
            folderPath.setDate(CommonUtilities.convertDateFormat(requestJson.get(EventPortalConstants.APPS_FLYER_EVENT_TIME_KEY).getAsString(), EventPortalConstants.APPS_FLYER_DATE_FORMATTER, EventPortalConstants.OUTPUT_DATE_FORMATTER));
            folderPath.setEventName(EventPortalConstants.APPS_FLYER_EVENTS);

            String finalStorageFolderPath = folderPath.getFinalStorageFolderPath();
            byte[] dataAsBytesArray = requestBody.getBytes(StandardCharsets.UTF_8);

            MessageBusService.getInstance().sendMessage(dataAsBytesArray, finalStorageFolderPath);
            tracker.stop(true);
        } catch (Exception e) {
            tracker.stop(false);
            logger.error("Error while processing Apps Flyer Data, request content : {} ", requestBody, e);
        }
    }

    private void sendAppInstallReportsToMessageBus(String uriBody) {
        try {
            AppInstallData appInstallData = objectMapper.readValue(uriBody, AppInstallData.class);
            folderPath.setAppName(PackageNameType.getPackageNameByAppName().get(appInstallData.getPackageName()));
            folderPath.setDate(appInstallData.getDate());
            folderPath.setEventName(CONST_APP_INSTALL_REPORTS);
            String finalStorageFolderPath = folderPath.getFinalStorageFolderPath();
            final String eventString = Serializer.DEFAULT_JSON_SERIALIZER.serialize(appInstallData);
            byte[] dataAsBytesArray = eventString.getBytes(StandardCharsets.UTF_8);

            logger.info("Sending app install reports to data platform at path: {}", finalStorageFolderPath);
            MessageBusService.getInstance().sendMessage(dataAsBytesArray, finalStorageFolderPath);
        } catch (Exception e) {
            logger.error("Error while processing App Install/Uninstall Reports, request content : {} ", uriBody, e);
        }
    }
}
