package ledcontrol.connection;

import static com.fazecast.jSerialComm.SerialPort.NO_PARITY;
import static com.fazecast.jSerialComm.SerialPort.ONE_STOP_BIT;
import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_SEMI_BLOCKING;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fazecast.jSerialComm.SerialPort;

public class SerialConnection {

	private final SerialPort serialPort;

	public SerialConnection(String port, int baudrate) throws IOException {
		serialPort = port == null || port.length() == 0 ? firstAvailable() : SerialPort.getCommPort(port);
		serialPort.setBaudRate(baudrate);
		serialPort.setComPortTimeouts(TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
		serialPort.setComPortParameters(baudrate, 8, ONE_STOP_BIT, NO_PARITY);
		if (!serialPort.openPort()) {
			throw new IOException("Could not open port " + port);
		}
	}

	private SerialPort firstAvailable() throws IOException {
		SerialPort[] commPorts = SerialPort.getCommPorts();
		if (commPorts.length == 0) {
			throw new IOException("No serial ports available");
		}
		return commPorts[0];
	}

	public InputStream getInputStream() {
		return serialPort.getInputStream();
	}

	public OutputStream getOutputStream() {
		return serialPort.getOutputStream();
	}

}
