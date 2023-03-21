FROM alpine:latest

ENV JAVA_HOME="/usr/lib/jvm/default-jvm/"
RUN apk add --no-cache libstdc++
RUN apk add openjdk11

# Has to be set explictly to find binaries
ENV PATH=$PATH:${JAVA_HOME}/bin

ARG BUILD_FOR=dev

LABEL Description="This image is used to proxy events received from app to clevertap." Vendor="nurture.farm" Version="1.0"

# Allow event-portal to know time for better work management
RUN apk update && apk add tzdata
RUN cp /usr/share/zoneinfo/Asia/Kolkata  /etc/localtime
RUN echo "Asia/Kolkata" >  /etc/timezone

# Create a folder for configuring portal for events.
RUN mkdir -p /event-portal

# Build the current project and add dependency jars, app jar, config and starter script
COPY target/dependency-jars /event-portal/dependency-jars
COPY target/event-portal-1.0-SNAPSHOT.jar /event-portal/event-portal-1.0-SNAPSHOT.jar
COPY config/config_$BUILD_FOR.properties /event-portal/config.properties
COPY start_docker.sh /event-portal/
RUN chmod a+x /event-portal/start_docker.sh

# Key to the portal
EXPOSE 8080

# Activate portal
CMD ["sh", "/event-portal/start_docker.sh"]
