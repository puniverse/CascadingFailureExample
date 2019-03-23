# Cascading Failures HTTP Service Benchmark

This project contains the code for the benchmarks discussed in the post [Scalable, Robust - and Standard - Java Web Services with Fibers](http://blog.paralleluniverse.co/2014/05/29/cascading-failures/)

## Installation

Clone the repository. Then copy and run the [`install.sh` script](https://github.com/puniverse/CascadingFailureExample/blob/master/scripts/install.sh) on a clean Ubunto EC2 instance, which will install Java, clone and build the project, and configure the OS (see the next section).

The [`install_photon.sh` script](https://github.com/puniverse/CascadingFailureExample/blob/master/scripts/install_photon.sh) will build [Photon](https://github.com/puniverse/photon) , and place the Photon [capsule](https://github.com/puniverse/capsule) (Java executable) in this project's home directory.

## Running

To run the server:

    ./run.sh -server <jetty|tomcat|undertow> -threads <max-threads> [-fibers]

The `-fibers` flag turns on the use of fibers.

The load generator runs the code in [this script](https://github.com/puniverse/CascadingFailureExample/blob/master/scripts/test.sh).

## License

MIT
