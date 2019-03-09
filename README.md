# smart-football-table-ledcontrol

Java program that receives events via MQTT and displays scenes on a connected LED stripe. 
The LED stripe can be connected using a serial link. The program writes TPM2 frames to the serial link which will then be interpreted by a TPM2 device. The TPM2 device can be a simple µController like an Arduino or an ESP8266. 
However, the Arduino seems to be not fast enough to manage the data send since it is very limited both in processing speed and the size of the serial buffer. 

## MQTT messages
| topic           | Description                                  | Example payload        |  Implemented |
| --------------- | -------------------------------------------- |----------------------- |------------- |
| backgroundlight | Sets the background light                    | #CC11DD                | ✔            |
| score           | The teams' scores                            | { "score": [ 0, 3 ] }  | ✔            |
| foul            | Some foul has happened                       | -                      | ✔            |
| gameover        | A match ended                                | { "winners": [ 0 ] }   | ✔            |
| idle            | Is there action on the table                 | { "idle": true }       | ✔            |
| foregroundlight | Sets the foreground light (#000000 for none) | #111111                | ✔            |

## Docker
You can either run docker in privileged mode (```--privileged```) or pass in the device(s) available in the container
```docker run --rm --device=/dev/ttyUSB4:/dev/ttyUSB0 -e LEDS=72 -e TTY=/dev/ttyUSB0 -e MQTTHOST=mqtt -e MQTTPORT=1883 ledcontrol```
Because the device numbers depends on the order the devices are connected you should use static links. You can add your own udev rule or just use the already existing links: 
```docker run --rm --device=/dev/serial/by-id/usb-1a86_USB2.0-Serial-if00-port0:/dev/ttyUSB0 -e LEDS=72 -e TTY=/dev/ttyUSB0 -e MQTTHOST=mqtt -e MQTTPORT=1883 ledcontrol```

