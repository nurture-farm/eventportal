export CLASSPATH=/event-portal/event-portal-1.0-SNAPSHOT.jar
java -server -Dfile.encoding=UTF-8 -classpath $CLASSPATH -Xms${Xms} -Xmx${Xmx} -Dfile.encoding=UTF-8 \
-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=2 -XX:ConcGCThreads=2 -XX:InitiatingHeapOccupancyPercent=70 \
-DLog4j2ContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector \
-Darchaius.configurationSource.additionalUrls=file:///event-portal/config.properties \
farm.nurture.eventportal.server.EventPortalServer