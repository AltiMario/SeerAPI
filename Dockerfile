FROM relateiq/oracle-java8

ADD ./target/seer-api-0.1.0-SNAPSHOT-standalone.jar    /opt/seer/seer-api.jar
ADD ./config/config.edn  /opt/seer/config.edn

EXPOSE 9090

CMD java -jar /opt/seer/seer-api.jar /opt/seer/config.edn
