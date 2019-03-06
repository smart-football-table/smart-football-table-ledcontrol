# ledcontrol

Java program that receives events via MQTT and displays scenes on a connected LED stripe. 
The LED stripe can be connected using a serial link. The program writes TPM2 frames to the serial link which will then be interpreted by a TPM2 device. The TPM2 device can be a simple µController like an Arduino or an ESP8266. 
However, the Arduino seems to be not fast enough to manage the data send since it is very limited both in processing speed and the size of the serial buffer. 

## MQTT messages
| topic      | Description                  | Example payload       |  Implemented |
| ---------- | ---------------------------- |---------------------- |------------- |
| score      | The teams' scores            | { "score": [ 0, 3 ] } | ✔            |
| foul       | Some foul has happened       | -                     | ✔            |
| gameover   | A match ended                | { "winners": [ 0 ] }  | ✔            |
| velocity   | current ball velocity        | { "velocity": 46.3 }  | -            |
| position   | current ball position        | { "x": 0, "y": 0 }    | -            |
| idle       | Is there action on the table | { "idle": true }      | ✔            |

## Docker
You can either run docker in privileged mode (```--privileged```) or pass in the device(s) available in the container
```docker run --rm --device=/dev/ttyUSB4 -e LEDS=120 -e TTY=/dev/ttyUSB4 -e MQTTHOST=mqtt -e MQTTPORT=1880 ledcontrol```

