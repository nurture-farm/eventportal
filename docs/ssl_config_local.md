Create a SSL context:
```
    SslContext sslCtx = SslContextBuilder.forServer(
            new File("/Users/shubhendu/.config/mkcertificates/cert.pem"),
            new File("/Users/shubhendu/.config/mkcertificates/key.pem"), null)
            .build();
```
Add to the pipeline
```
channelPipeline.addLast(sslCtx.newHandler(ch.alloc()))
```

