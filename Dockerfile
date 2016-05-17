FROM relateiq/oracle-java8

ADD ./target/seer-api-standalone.jar  /opt/seer/seer-api.jar
ADD ./docker/config.edn.docker        /opt/seer/config.edn
ADD ./docker/seerCore                 /opt/seer/seerCore

EXPOSE 9090

CMD java -jar /opt/seer/seer-api.jar /opt/seer/config.edn
