#wget https://github.com/puniverse/photon/releases/download/v0.2.0/photon.jar
git clone https://github.com/puniverse/photon.git
cd photon
./gradlew fullCapsule
cp build/libs/photon.jar ..
cd ..
java -jar photon.jar -help
