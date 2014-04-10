cat > sysctl.conf <<EOF
net.ipv4.tcp_tw_recycle = 1
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_fin_timeout = 1
net.ipv4.tcp_timestamps = 1
net.ipv4.tcp_syncookies = 0
net.ipv4.ip_local_port_range = 1024 65535
EOF
sudo sh -c "cat sysctl.conf >> /etc/sysctl.conf" 
sudo sysctl -p /etc/sysctl.conf
cat <<EOF
USAGE: 
$ sudo bash
$ ulimit -n 200000

Enjoy
EOF
