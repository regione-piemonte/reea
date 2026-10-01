# =========================
# STAGE 1 - BUILD MAVEN
# =========================
FROM docker-base.ecosis.csi.it/maven:3.9.2-j17-csi-r1 AS builder

COPY . /usr/src/mymaven
WORKDIR /usr/src/mymaven

RUN mvn clean generate-sources install  \
    --settings .mvn/conf/settings.xml \
    -Dmaven.test.skip=true \
    --batch-mode \
    --no-transfer-progress
	
#################################################################################
# STAGE INTERMEDIO PER CODE REVIEW - NON FARA' PARTE DELL'IMMAGINE DI PRODUZIONE
FROM scratch AS code-review

COPY --from=builder /usr/src/mymaven/*/target/classes /target/classes
#COPY --from=builder /usr/src/mymaven/amianto-registry/target/classes /amianto-registry/target/classess
#################################################################################

# =========================
# STAGE 2 - RUNTIME
# =========================
FROM docker-trusted.ecosis.csi.it/eclipse-temurin:17.0.8.1_1-jdk

LABEL it.csi.codice_progetto="reea" \
      it.csi.codice_componente="reeabe"

# ✅ utility linux + tools debug rete
RUN apt-get update \
&& DEBIAN_FRONTEND=noninteractive apt-get install -y \
tzdata \
vim \
mailutils \
netcat-openbsd \
telnet \
dnsutils \
curl \
openssl \
&& ln -snf /usr/share/zoneinfo/Europe/Rome /etc/localtime \
&& echo "Europe/Rome" > /etc/timezone \
&& rm -rf /var/lib/apt/lists/*
 
ENV TZ=Europe/Rome
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Europe/Rome"


# ✅ crea utente PRIMA
RUN useradd -u 1001 -m -s /bin/sh appuser

# ✅ directory
RUN mkdir -p /deployments /scripts \
    && chown -R 1001:root /deployments /scripts \
    && chmod -R g+rwX /deployments /scripts

# ✅ script java
COPY ./run-java.sh /deployments/run-java.sh
RUN chmod 540 /deployments/run-java.sh \
    && chown 1001 /deployments/run-java.sh

# ✅ jar
COPY --from=builder /usr/src/mymaven/amianto-registry/target/*.jar /deployments/app.jar

# ✅ script cron/job
COPY api_job.sh /scripts/api_job.sh
RUN chmod +x /scripts/api_job.sh

EXPOSE 8080

# ✅ usa utente non-root
USER 1001

ENTRYPOINT ["/deployments/run-java.sh"]