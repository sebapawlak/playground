FROM airhacks/wildfly
COPY ./build/libs/playground.war ${DEPLOYMENT_DIR}
