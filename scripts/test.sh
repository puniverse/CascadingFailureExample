hostname="localhost"
path="regular"

if [ $# -lt 2 ]; then
    echo "Your command line contains $# arguments"
    echo usage: $0 hostname regular/fiber
    exit
fi
hostname=$1
path=$2

# Warmup
for rate in 10 20 100
do
    java -jar photon.jar  -name Warmup -rate $rate http://${hostname}:8080/${path}\?sleep=500\&callService\=true -duration 5
done

# Tested service
java -jar photon.jar  -name SmallService -rate 10 http://${hostname}:8080/${path}\?sleep=500\&callService\=false -duration 30 &

# Load service
for rate in 100 200 300 400
do
    java -jar photon.jar  -name Load -rate $rate http://${hostname}:8080/${path}\?sleep=500\&callService\=true -duration 5
done


