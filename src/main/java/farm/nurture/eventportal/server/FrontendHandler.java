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

package farm.nurture.eventportal.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import farm.nurture.eventportal.dp.AppNameType;
import farm.nurture.eventportal.dp.Consumer;
import farm.nurture.eventportal.dp.RequestData;
import farm.nurture.eventportal.dp.RequestType;
import farm.nurture.eventportal.util.EventMetrics;
import farm.nurture.eventportal.util.EventPortalConstants;
import farm.nurture.infra.metrics.MetricTracker;
import farm.nurture.infra.util.Logger;
import farm.nurture.infra.util.LoggerFactory;
import farm.nurture.infra.util.StringUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class FrontendHandler extends SimpleChannelInboundHandler<FullHttpMessage> {

    private static final Logger logger = LoggerFactory.getLogger(FrontendHandler.class);
    private static final String PLAYSTORE_APP_REPORTS_URI = "/platform/event-portal/app-install-reports";
    private static final String HEADER_CLEVERTAP_ACCOUNT_ID = "X-CleverTap-Account-ID";
    private static Set<String> clevertapAllowedSet = new HashSet<>();

    private final String scheme;
    private final boolean isHttps;
    private final String remoteHost;
    private final int remotePort;
    private final String remoteHostPort;
    private Channel outboundChannel;
    private final Consumer consumer;
    private Channel inboundChannel;
    private RequestData requestData;

    public FrontendHandler(Consumer consumer) {
        this.scheme = EventPortalConfig.CLEVERTAP_SCHEME.get("https");
        this.isHttps = scheme.equalsIgnoreCase("https");
        this.remoteHost = EventPortalConfig.CLEVERTAP_HOST.get(null);
        this.remotePort = EventPortalConfig.CLEVERTAP_PORT.get(443);
        this.remoteHostPort = remoteHost + ":" + remotePort;
        this.consumer = consumer;
        this.requestData = new RequestData();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        inboundChannel = ctx.channel();
        final Channel inboundChannel = ctx.channel();
        outboundChannel = new Bootstrap()
                .group(inboundChannel.eventLoop())
                .channel(inboundChannel.getClass())
                .handler(new ChannelInitializer<Channel>() {
                    protected void initChannel(Channel ch) throws SSLException {
                        ChannelPipeline pipeline = ch.pipeline();
                        if (isHttps) {
                            pipeline.addLast(new SslHandler(Objects.requireNonNull(
                                    SimpleSslEngine.getSslEngine(remoteHost, remotePort))));
                        }
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new HttpObjectAggregator(8388608, true));
                        pipeline.addLast(new BackendHandler(inboundChannel));
                    }
                })
                .option(ChannelOption.AUTO_READ, false)
                .connect(remoteHost, remotePort)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        // connection complete start to read first data
                        inboundChannel.read();
                    } else {
                        // Close the connection if the connection attempt has failed.
                        inboundChannel.close();
                    }
                }).channel();
    }



    protected void channelRead0(ChannelHandlerContext ctx, FullHttpMessage msg) throws InterruptedException, JsonProcessingException {
        if (outboundChannel.isActive() && msg instanceof FullHttpRequest) {
            EventMetrics eventMetrics = EventMetrics.getInstance();
            eventMetrics.eventsCallReceived.increment();
            FullHttpRequest httpRequest = (FullHttpRequest) msg;
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.uri());

            //Add a handling for App Install Uninstall reports
            if(queryStringDecoder.uri().contains(PLAYSTORE_APP_REPORTS_URI)){
                logger.debug("URI of request : " + queryStringDecoder.uri());
//                logger.debug("Request from temporal service received.: {}", httpRequest.content().copy().toString(StandardCharsets.UTF_8));
                ByteBuf byteBuf = httpRequest.content().copy();
                String data = byteBuf.toString(StandardCharsets.UTF_8);
                byteBuf.release();
                requestData.setRequestType(RequestType.APP_INSTALL_REPORT);
                requestData.setData(data);
                consumer.put(requestData);
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                inboundChannel.writeAndFlush(response);
                close(inboundChannel, outboundChannel);
                return;
            }

            if(queryStringDecoder.uri().contains(EventPortalConstants.APPS_FLYER_BASE_URI)) {
                handleAppsFlyerMessage(httpRequest);
                return;
            }

            //skipping uri request with empty params --> todo : remove this if nginx/prometheus multiple port issue in yaml gets fix
            if(queryStringDecoder.parameters().isEmpty()){
                return;
            }

            ByteBuf byteBuf = httpRequest.content().copy();
            String uriBody = byteBuf.toString(StandardCharsets.UTF_8);
            byteBuf.release();

            logger.debug("Received request from client headers: {}, params: {}, body: {}",
                    httpRequest.headers(), queryStringDecoder.parameters(), uriBody);
            //1.    Push the uriBody to the queue.(so, we can consider this method as a producer)
            //2.    Now at consumer side, we process the json, parse it and filter out the required fields.
            //3.    Finally (at consumer side itself), we send this request to message bus along with the storage_filepath.

            if(StringUtils.isEmpty(uriBody)){
                logger.warn("Empty Body received, ignoring processing in data platform");
            } else {
                requestData.setRequestType(RequestType.EVENT);
                requestData.setData(uriBody);
                consumer.put(requestData);
            }

            //Sending data to clevertap for allowed apps
            String clevertapAccountId = httpRequest.headers().get(HEADER_CLEVERTAP_ACCOUNT_ID);
            if(StringUtils.isNonEmpty(clevertapAccountId) && isClevertapAllowed(clevertapAccountId)) {
                logger.debug("Sending event data to clevertap...");
                String remoteHost = this.remoteHost;
                if(queryStringDecoder.parameters().containsKey(EventPortalConstants.REDIRECT_HOST_KEY)) {
                    remoteHost = queryStringDecoder.parameters().get(EventPortalConstants.REDIRECT_HOST_KEY).get(0);
                }
                String requestUri = scheme + "://" + remoteHost + "?" + queryStringDecoder.rawQuery();
                FullHttpRequest newHttpRequest = httpRequest.setUri(requestUri);
                newHttpRequest.headers().set(HttpHeaderNames.HOST, remoteHostPort);
                eventMetrics.eventRequestsSentToClevertap.increment();
                outboundChannel.writeAndFlush(newHttpRequest.retain());
            } else {
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                response.headers().set(EventPortalConstants.CLEVERTAP_DOMAIN_KEY, EventPortalConstants.CLEVERTAP_DOMAIN_VALUE);
                response.headers().set(EventPortalConstants.CLEVERTAP_SPIKY_DOMAIN_KEY, EventPortalConstants.CLEVERTAP_SPIKY_DOMAIN_VALUE);
                inboundChannel.writeAndFlush(response);
                close(inboundChannel, outboundChannel);
            }
        }
    }

    private boolean isClevertapAllowed(String clevertapAccountId) {
        if(clevertapAllowedSet.isEmpty()){
            initClevertapAllowedSet();
        }
        return clevertapAllowedSet.contains(clevertapAccountId);
    }

    private void initClevertapAllowedSet() {
        for(AppNameType appNameType : AppNameType.values()){
           if(appNameType.isClevertapAllowed() == true){
               clevertapAllowedSet.add(appNameType.getClevertapAccountId());
           }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        close(outboundChannel, ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        close(outboundChannel, ctx.channel());
    }

    private void close(Channel... channels) {
        for (Channel channel : channels) {
            if (null != channel) {
                channel.close();
            }
        }
    }

    private void handleAppsFlyerMessage(FullHttpRequest httpRequest) {
        MetricTracker tracker = new MetricTracker(EventPortalConstants.NF_EP, EventPortalConstants.APPS_FLYER_HANDLE_MSG_TRACKER_NAME);
        try {
            ByteBuf byteBuf = httpRequest.content().copy();
            String data = byteBuf.toString(StandardCharsets.UTF_8);
            byteBuf.release();
            requestData.setRequestType(RequestType.APPS_FLYER_EVENT);
            requestData.setData(data);
            consumer.put(requestData);
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            inboundChannel.writeAndFlush(response);
            close(inboundChannel, outboundChannel);
            tracker.stop(true);
        } catch (Exception e) {
            logger.error("Error while handline Apps Flyer Request, request content : {} ", httpRequest, e);
            tracker.stop(false);
        }
    }
}
