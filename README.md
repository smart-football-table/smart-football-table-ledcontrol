# smart-football-table-ledcontrol

[![Build Status](https://travis-ci.org/smart-football-table/smart-football-table-ledcontrol.svg?branch=master)](https://travis-ci.org/smart-football-table/smart-football-table-ledcontrol)
[![codecov](https://codecov.io/gh/smart-football-table/smart-football-table-ledcontrol/branch/master/graph/badge.svg)](https://codecov.io/gh/smart-football-table/smart-football-table-ledcontrol)
[![BCH compliance](https://bettercodehub.com/edge/badge/smart-football-table/smart-football-table-ledcontrol?branch=master)](https://bettercodehub.com/)

Java program that receives events via MQTT and displays scenes on a connected LED stripe. 
The LED stripe can be connected using a serial link. The program writes TPM2 frames to the serial link which will then be interpreted by a TPM2 device. The TPM2 device can be a simple µController like an Arduino or an ESP8266. 
However, the Arduino seems to be not fast enough to manage the data send since it is very limited both in processing speed and the size of the serial buffer. 

## Docker
You can either run docker in privileged mode (```--privileged```) or pass in the device(s) available in the container
```docker run --rm --device=/dev/ttyUSB4:/dev/ttyUSB0 -e LEDS=72 -e TTY=/dev/ttyUSB0 -e MQTTHOST=mqtt -e MQTTPORT=1883 ledcontrol```
Because the device numbers depends on the order the devices are connected you should use static links. You can add your own udev rule or just use the already existing links: 
```docker run --rm --device=/dev/serial/by-id/usb-1a86_USB2.0-Serial-if00-port0:/dev/ttyUSB0 -e LEDS=72 -e TTY=/dev/ttyUSB0 -e MQTTHOST=mqtt -e MQTTPORT=1883 ledcontrol```

![Image 1](https://smart-football-table.github.io/modules/smart-football-table-ledcontrol/IMG_20190307_164909.jpg)
LED strip showing score

![Image 2](https://smart-football-table.github.io/modules/smart-football-table-ledcontrol/IMG_20190328_171512830.jpg)
PCB for controlling the LED strip using an ESP8266

![Image 3](https://smart-football-table.github.io/modules/smart-football-table-ledcontrol/IMG_20190404_131424825.jpg)
Showing the ESP8266 module inside a hand crafted case (used to be a RasPi case)
