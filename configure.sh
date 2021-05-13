#!/bin/bash

set -e

echo "Started with args"

client_version_env="$client_version_env" #We should pick this from the jar not as an argument.
crypto_key_env="$crypto_key_env" #key to encrypt the jar files
client_tpm_enabled="$tpm_enabled_env" #Not used as of now
client_certificate="$client_certificate_env" # Not used as of now
client_upgrade_server="$client_upgrade_server_env" #docker hosted url
reg_client_sdk_url="$reg_client_sdk_url_env"

echo "initalized variables"

#download the certificate at runtime from the certificate store
#wget -O mosip_cer.cer "${client_certificate_url}"

mkdir -p /registration-libs/target/props

echo "mosip.reg.app.key=${crypto_key_env}" > /registration-libs/target/props/mosip-application.properties
echo "mosip.reg.version=${client_version_env}" >> /registration-libs/target/props/mosip-application.properties
echo "mosip.reg.client.url=${client_upgrade_server}/registration-client/" >> /registration-libs/target/props/mosip-application.properties
echo "mosip.reg.healthcheck.url=${healthcheck_url_env}" >> /registration-libs/target/props/mosip-application.properties
echo "mosip.reg.rollback.path=../BackUp" >> /registration-libs/target/props/mosip-application.properties
echo "mosip.reg.cerpath=/cer/mosip_cer.cer" >> /registration-libs/target/props/mosip-application.properties
echo "mosip.reg.dbpath=db/reg" >> /registration-libs/target/props/mosip-application.properties
echo "mosip.reg.xml.file.url=${client_upgrade_server}/registration-client/maven-metadata.xml" >> /registration-libs/target/props/mosip-application.properties
echo "mosip.reg.client.tpm.availability=${client_tpm_enabled}" >> /registration-libs/target/props/mosip-application.properties
echo "mosip.client.upgrade.server.url=${client_upgrade_server}" >> /registration-libs/target/props/mosip-application.properties

echo "created mosip-application.properties"

cd /registration-libs/target
jar uf registration-libs-${client_version_env}.jar props/mosip-application.properties
cd /

cp databin_FACETOOLS.bin /registration-client/target/
cp FaceTools_Config.bin  /registration-client/target/
cp initBlock.dat /registration-client/target/
cp QCT.cfg /registration-client/target/
cp opencv_java320.dll /registration-client/target/
cp qr.bat /registration-client/target/
cp qrscanner.jar /registration-client/target/
cp maven-metadata.xml /registration-client/target/

#mkdir -p /sdkjars

#if [ "$reg_client_sdk_url" ]
#then
#	echo "Found thirdparty SDK"
#	wget "$reg_client_sdk_url"
#	/usr/bin/unzip /sdkDependency.zip
#	cp /sdkDependency/*.jar /sdkjars/
#else
#	echo "Downloading MOCK SDK..."
#	wget https://repo1.maven.org/maven2/io/mosip/mock/sdk/mock-sdk/0.9/mock-sdk-0.9.jar -P /sdkjars/
#fi

## download Open JRE 11 + FX
#wget https://cdn.azul.com/zulu/bin/zulu11.41.23-ca-fx-jre11.0.8-win_x64.zip -O zulu11.41.23-ca-fx-jre11.0.8-win_x64.zip

#cp /registration-libs/resources/zulu11.41.23-ca-fx-jre11.0.8-win_x64.zip /

#unzip Jre to be bundled
#/usr/bin/unzip /zulu11.41.23-ca-fx-jre11.0.8-win_x64.zip
#mkdir -p /registration-libs/resources/jre
#mv /zulu11.41.23-ca-fx-jre11.0.8-win_x64/* /registration-libs/resources/jre/
#chmod -R a+x /registration-libs/resources/jre

## temp fix - for class loader issue
#rm /registration-libs/resources/rxtx/bcprov-jdk14-138.jar

#/usr/local/openjdk-11/bin/java -cp /registration-libs/target/*:/registration-client/target/lib/* io.mosip.registration.cipher.ClientJarEncryption "/registration-client/target/registration-client-${client_version_env}.jar" "${crypto_key_env}" "${client_version_env}" "/registration-libs/target/" "/build_files/${client_certificate}" "/registration-libs/resources/db/reg" "/registration-client/target/registration-client-${client_version_env}.jar" "/registration-libs/resources/rxtx" "/registration-libs/resources/jre" "/registration-libs/resources/batch/run.bat" "/registration-libs/target/props/mosip-application.properties"

echo "encryption completed"

cd /registration-client/target/
mv "mosip-sw-${client_version_env}.zip" reg-client.zip
/usr/bin/unzip reg-client.zip
cp qr.bat jre/
cp qrscanner.jar jre/
mkdir -p /registration-client/target/bin
cp /registration-client/target/lib/mosip-client.jar /registration-client/target/bin/
cp /registration-client/target/lib/mosip-services.jar /registration-client/target/bin/
ls -ltr lib | grep bc

/usr/bin/zip -r reg-client.zip jre
/usr/bin/zip -r reg-client.zip bin
/usr/bin/zip -r reg-client.zip lib
/usr/bin/zip reg-client.zip MANIFEST.MF
/usr/bin/zip reg-client.zip databin_FACETOOLS.bin
/usr/bin/zip reg-client.zip FaceTools_Config.bin
/usr/bin/zip reg-client.zip initBlock.dat
/usr/bin/zip reg-client.zip opencv_java320.dll
/usr/bin/zip reg-client.zip QCT.cfg


echo "setting up nginx static content"

mkdir -p /var/www/html/registration-client
mkdir -p /var/www/html/registration-client/${client_version_env}
mkdir -p /var/www/html/registration-client/${client_version_env}/lib

cp /registration-client/target/lib/* /var/www/html/registration-client/${client_version_env}/lib
cp qr.bat /var/www/html/registration-client/${client_version_env}/lib
cp qrscanner.jar /var/www/html/registration-client/${client_version_env}/lib
cp opencv_java320.dll /var/www/html/registration-client/${client_version_env}/lib
cp databin_FACETOOLS.bin /var/www/html/registration-client/${client_version_env}/lib
cp FaceTools_Config.bin /var/www/html/registration-client/${client_version_env}/lib
cp initBlock.dat /var/www/html/registration-client/${client_version_env}/lib
cp QCT.cfg /var/www/html/registration-client/${client_version_env}/lib
cp /registration-client/target/MANIFEST.MF /var/www/html/registration-client/${client_version_env}/
cp maven-metadata.xml /var/www/html/registration-client/
cp reg-client.zip /var/www/html/registration-client/${client_version_env}/

echo "setting up nginx static content - completed"

/usr/sbin/nginx -g "daemon off;"
