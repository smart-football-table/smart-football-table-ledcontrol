# smart-football-table-ledcontrol

[![Build Status](https://travis-ci.org/smart-football-table/smart-football-table-ledcontrol.svg?branch=master)](https://travis-ci.org/smart-football-table/smart-football-table-ledcontrol)
[![BCH compliance](https://bettercodehub.com/edge/badge/smart-football-table/smart-football-table-ledcontrol?branch=master)](https://bettercodehub.com/)
[![codecov](https://codecov.io/gh/smart-football-table/smart-football-table-ledcontrol/branch/master/graph/badge.svg)](https://codecov.io/gh/smart-football-table/smart-football-table-ledcontrol)
[![Maintainability](https://api.codeclimate.com/v1/badges/4e3ba0582647e20a63f7/maintainability)](https://codeclimate.com/github/smart-football-table/smart-football-table-ledcontrol/maintainability)
[![Known Vulnerabilities](https://snyk.io/test/github/smart-football-table/smart-football-table-ledcontrol/badge.svg?targetFile=pom.xml)](https://snyk.io/test/github/smart-football-table/smart-football-table-ledcontrol?targetFile=pom.xml)
[![GitLicense](https://gitlicense.com/badge/smart-football-table/smart-football-table-ledcontrol)](https://gitlicense.com/license/smart-football-table/smart-football-table-ledcontrol)


Java program that receives events via MQTT and displays scenes on a connected LED stripe. 
The LED stripe can be connected using a serial link. The program writes TPM2 frames to the serial link which will then be interpreted by a TPM2 device. The TPM2 device can be a simple µController like an Arduino or an ESP8266. 
However, the Arduino seems to be not fast enough to manage the data send since it is very limited both in processing speed and the size of the serial buffer. 

Scenes visualized using the stripe: 
* Goals: Whole stripe blinks in the goal getting team's color)
* Fouls: Whole stripe blinks yellow
* Score: The score of both teams in visualized during the game
* Game over: Whole stripe blinks in the winning team's color)
* Idle: Plays some animation if non one is playing

The stripe can also be used to lighten the football table. 
* Foreground color: No matter what else should be displayed, only the foreground color is shown. This can be used to have the brightest possible light on the table by selecting white as color (#FFFFFF) or any other RGB color
* Background color: All not active (black) leds can be turned on, also any RGB color is supported

## Docker
You can either run docker in privileged mode (```--privileged```) or pass in the device(s) available in the container
```docker run --rm --device=/dev/ttyUSB4:/dev/ttyUSB0 -e LEDS=72 -e TTY=/dev/ttyUSB0 -e MQTTHOST=mqtt -e MQTTPORT=1883 ledcontrol```
Because the device numbers depends on the order the devices are connected you should use static links. You can add your own udev rule or just use the already existing links: 
```docker run --rm --device=/dev/serial/by-id/usb-1a86_USB2.0-Serial-if00-port0:/dev/ttyUSB0 -e LEDS=72 -e TTY=/dev/ttyUSB0 -e MQTTHOST=mqtt -e MQTTPORT=1883 ledcontrol```

![Score](https://smart-football-table.github.io/modules/smart-football-table-ledcontrol/IMG_20190307_164909.jpg)
LED strip showing score

![PCB top](https://smart-football-table.github.io/modules/smart-football-table-ledcontrol/esp8266-tpm2-top.png) ![PCB bottom](https://smart-football-table.github.io/modules/smart-football-table-ledcontrol/esp8266-tpm2-bottom.png)
<br>LED controller module using an ESP8266, top and bottom layout view

![Soldered](https://smart-football-table.github.io/modules/smart-football-table-ledcontrol/IMG_20190328_171512830.jpg)
The soldered PCB

![Case](https://smart-football-table.github.io/modules/smart-football-table-ledcontrol/IMG_20190404_131424825.jpg)
Showing the ESP8266 module inside a hand crafted case (used to be a RasPi case)

# Part list

Beside the RGB LED stripe, the three wire (white/green/red) connection cable (JST) and a micro USB cable we used the following parts to build the TPM2 box. 

| Amount | Description             | Price (china)  |
| ------ | ----------------------- | -------------- |
| 1      | Resistor 470Ω           | 0,10€          |
| 1      | Capacitor 1,0mF         | 0,50€          |
| 1      | WeMos D1 mini (ESP8266) | 2,00€          |
| 1      | 5V Power supply 6A/30W<br>(One RBG LED drains upto 60mA) | 8,00€ |
| 1      | PCB mount power socket connector<br>(matching power supply connector) | 0,05€ |
| 1      | PCB 15"x15"             | <1€            |
| (2)    | (Optional) 8 pins single row female pin header | 0,10€ |

