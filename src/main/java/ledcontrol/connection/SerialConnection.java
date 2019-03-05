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

	public SerialConnection(String port, int baudrate) {
		serialPort = SerialPort.getCommPort(port);
		serialPort.setBaudRate(baudrate);
		serialPort.setComPortTimeouts(TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
		serialPort.setComPortParameters(baudrate, 8, ONE_STOP_BIT, NO_PARITY);
		serialPort.openPort();
	}

	public InputStream getInputStream() throws IOException {
		return serialPort.getInputStream();
	}

	public OutputStream getOutputStream() throws IOException {
		return serialPort.getOutputStream();
	}

}
