FROM openjdk:11

RUN apt-get update

RUN apt install nano

RUN apt install -y zip nginx

ARG client_version

ARG crypto_key

ARG healthcheck_url

ARG client_upgrade_server

ARG cert_server_URL

ARG tpm_enabled

ARG db_bootpwd

ENV client_version_env=1.4.1-SNAPSHOT

ENV crypto_key_env=bBQX230Wskq6XpoZ1c+Ep1D+znxfT89NxLQ7P4KFkc4

ENV healthcheck_url_env=${healthcheck_url}

ENV client_upgrade_server_env=${client_upgrade_server}

ENV client_repo_env=${client_repo_url}

ENV client_certificate_env=${client_certificate}

ENV tpm_enabled_env=Y

ENV db_bootpwd_env=bW9zaXAxMjM0NQ

ADD registration-client/target /registration-client/target

ADD registration-libs/target /registration-libs/target

ADD registration-libs/src/main/resources registration-libs/resources

ADD databin_FACETOOLS.bin databin_FACETOOLS.bin

ADD FaceTools_Config.bin FaceTools_Config.bin

ADD initBlock.dat initBlock.dat

ADD QCT.cfg QCT.cfg

ADD opencv_java320.dll opencv_java320.dll

ADD qr.bat qr.bat

ADD qrscanner.jar qrscanner.jar

ADD registration-client/target/MANIFEST.MF MANIFEST.MF

ADD configure.sh configure.sh

ADD maven-metadata-local.xml maven-metadata.xml

RUN chmod a+x configure.sh \
 && rm -f /registration-client/target/registration-client-*-javadoc.jar \
 && rm -f  /registration-client/target/registration-client-*-sources.jar \
 && rm -f  /registration-libs/target/registration-libs-*-javadoc.jar \
 && rm  -f /registration-libs/target/registration-libs-*-sources.jar

ENTRYPOINT ["./configure.sh" ]

#CMD [ "${client_version_env}", "${crypto_key_env}", "${db_bootpwd_env}", "${tpm_enabled_env}", "${client_certificate_env}", "${client_upgrade_server_env}" ]
