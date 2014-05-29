#using gnuplot
awk '/respo/{ sub(/ms/,"",$4); print $1,$4; }' $1_load*  | sort -n > load.csv
awk '/respo/{ sub(/ms/,"",$4); print $1,$4; }' $1_fastservice.txt   | sort -n > service.csv
cat > gp <<EOF
set terminal png size 1000,600
set output '$1.png'
set style data lines
#set style data points
set title ""
set xlabel "Time [minutes:seconds]"
set xdata time
set xtics rotate 
set timefmt "%H:%M:%S"
set format x "%M:%S"
set ylabel "response time [millis]"
set yrange [ 0 : 30000.0]
set grid
plot "load.csv" using 1:2 title "a+b", "service.csv" using 1:2 title "a"
EOF
gnuplot gp
#open $1.png
