if [ $# -lt 4 ]; then
    echo usage: $0 hostname path sleep loadrate
    echo \t: $0 localhost api/service sleep loadrate
    exit
fi
hostname=$1
path=$2
sleeptime=$3
loadrate=$4

# Warmup
for rate in 100 500 1000 $loadrate
do
    java -jar photon.jar  -name Warmup${rate} -rate $rate http://${hostname}:8080/${path}\?sleep=50 -duration 5 -print 0
done

# Tested service
java -jar photon.jar -name ${path}FastService -rate 50 http://${hostname}:8080/${path} -duration 60 -stats -print 5000 & 

# Load service
java -jar photon.jar -name ${path}Load1 -rate $loadrate http://${hostname}:8080/${path}\?sleep=1 -duration 5 -stats -print 5000
java -jar photon.jar -name ${path}Load2 -rate $loadrate http://${hostname}:8080/${path}\?sleep=${sleeptime} -duration 50 -stats -print 5000
