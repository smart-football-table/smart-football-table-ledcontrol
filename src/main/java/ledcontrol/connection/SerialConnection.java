package ledcontrol.connection;

import static gnu.io.SerialPort.DATABITS_8;
import static gnu.io.SerialPort.PARITY_NONE;
import static gnu.io.SerialPort.STOPBITS_1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

public class SerialConnection {

	private final SerialPort serialPort;

	public SerialConnection(String port, int baudrate)
			throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException {
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(port);
		checkState(!portIdentifier.isCurrentlyOwned(), "Port %s is currently in use", port);
		this.serialPort = serialPort(null, portIdentifier, baudrate);
	}

	private static void checkState(boolean state, String message, Object... args) {
		if (!state) {
			throw new IllegalStateException(String.format(message, args));
		}
	}

	private static SerialPort serialPort(Object config, CommPortIdentifier portIdentifier, int baudrate)
			throws PortInUseException, UnsupportedCommOperationException {
		SerialPort serialPort = (SerialPort) portIdentifier.open("LedControl", 2000);
		serialPort.setSerialPortParams(baudrate, DATABITS_8, STOPBITS_1, PARITY_NONE);
		return serialPort;
	}

	public InputStream getInputStream() throws IOException {
		return serialPort.getInputStream();
	}

	public OutputStream getOutputStream() throws IOException {
		return serialPort.getOutputStream();
	}

}
