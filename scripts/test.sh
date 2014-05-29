if [ $# -lt 4 ]; then
    echo usage: $0 hostname path sleep loadrate
    exit
fi
hostname=$1
path=$2
sleeptime=$3
loadrate=$4
serverconf=$5

# Warmup
for rate in 50 100 200 $loadrate
do
    java -jar photon.jar  -name Warmup${rate} -rate $rate http://${hostname}:8080/${path}\?sleep=50 -duration 5 -print 0
done

# Tested service
java -jar photon.jar -name stat_${serverconf}_${sleeptime}_${loadrate}_fastservice -rate 10 http://${hostname}:8080/${path} -duration 60 -stats -print 5000 & 

# Load service
java -jar photon.jar -name stat_${serverconf}_${sleeptime}_${loadrate}_load1 -rate $loadrate http://${hostname}:8080/${path}\?sleep=1 -duration 5 -stats -print 5000 &
sleep 5
java -jar photon.jar -name stat_${serverconf}_${sleeptime}_${loadrate}_load2 -rate $loadrate http://${hostname}:8080/${path}\?sleep=${sleeptime} -duration 50 -stats -print 5000
