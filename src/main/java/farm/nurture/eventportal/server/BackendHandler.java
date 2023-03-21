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

import farm.nurture.eventportal.util.EventMetrics;
import farm.nurture.infra.util.Logger;
import farm.nurture.infra.util.LoggerFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpResponse;

public class BackendHandler extends SimpleChannelInboundHandler<FullHttpMessage> {

    private static final Logger logger = LoggerFactory.getLogger(BackendHandler.class);
    private final Channel inboundChannel;

    public BackendHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.read();
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, FullHttpMessage msg) {
        inboundChannel.writeAndFlush(msg.retain())
                .addListener((ChannelFutureListener) future -> close(inboundChannel, future.channel()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        close(inboundChannel, ctx.channel());
    }

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        if(msg instanceof FullHttpResponse) {
            FullHttpResponse message = (FullHttpResponse) msg;
            EventMetrics.getInstance().eventRequestsSentToClevertapResponse.increment(String.valueOf(message.status().code()));
            if(message.status().code() < 400) {
                logger.debug(" Received success response from remote status: {} contentLength: {}",
                        message.status(), message.content().readableBytes());
            } else {
                logger.error(" Received error response from remote status: {} contentLength: {}",
                        message.status(), message.content().readableBytes());
            }
        }
        return super.acceptInboundMessage(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        close(inboundChannel, ctx.channel());
    }

    private void close(Channel... channels) {
        for(Channel channel : channels) {
            if(null != channel) {
                channel.close();
            }
        }
    }
}
