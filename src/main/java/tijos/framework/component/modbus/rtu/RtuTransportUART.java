// This class was not thoroughly tested! 

package tijos.framework.component.modbus.rtu;

import static tijos.framework.component.modbus.protocol.ModbusConstants.MAX_PDU_SIZE;

import java.io.IOException;

import tijos.framework.component.modbus.protocol.ModbusPdu;
import tijos.framework.component.rs485.TiRS485;
import tijos.framework.util.logging.Logger;

/**
 * MODBUS RTU RS485 Transport 
 *
 */
public class RtuTransportUART  implements ModbusClientTransport {

	TiRS485 rs485;
	
	protected final int timeout;
	protected final int pause;
	protected final byte[] buffer = new byte[MAX_PDU_SIZE + 3]; // ADU: [ID(1), PDU(n), CRC(2)]
	protected int expectedBytes; // for logging

	/**
	 * Initialize with UART and timeout 
	 * @param uart
	 * @param timeout	timeout for receiving data from uart
	 * @param pause		pause after sending data
	 */
	public RtuTransportUART(TiRS485 rs485, int timeout, int pause) {
		this.rs485 = rs485;		
		
		this.timeout = timeout;
		this.pause = pause;
	}

	public void open(int baudRate, int dataBitNum, int stopBitNum, int parity) throws IOException {
		this.rs485.open(baudRate, dataBitNum, stopBitNum, parity);
	}
	
	/**
	 * Close 
	 */
	@Override
	synchronized public void close() {
		try{
		this.rs485.close();
		}
		catch(Exception ie) {
			ie.printStackTrace();
		}
	}


	/**
	 * Send MODBUS request 
	 */
	@Override
	public void sendRequest(ModbusClient modbusClient) throws Exception {

		this.rs485.clearInput();
		
		buffer[0] = modbusClient.getServerId();
		modbusClient.readFromPdu(0, modbusClient.getPduSize(), buffer, 1);
		int size = modbusClient.getPduSize() + 1; // including 1 byte for serverId
		int crc = ModbusPdu.calcCRC16(buffer, 0, size);
		buffer[size] = ModbusPdu.lowByte(crc);
		buffer[size + 1] = ModbusPdu.highByte(crc);
		size = size + 2;

		Logger.info("Modbus", "Write: " + ModbusPdu.toHex(buffer, 0, size));

		this.rs485.write(buffer, 0, size);		
		if (pause > 0)
		{
			Logger.info("Modbus","Pause " + pause);
			Thread.sleep(pause);
		}

	}


	/**
	 * Waiting for response 
	 */
	@Override
	public int waitResponse(ModbusClient modbusClient) throws Exception {

		expectedBytes = modbusClient.getExpectedPduSize() + 3; // id(1), PDU(n), crc(2)

		// read id
		if (!this.rs485.readToBuffer(this.buffer, 0, 1, this.timeout))
			return ModbusClient.RESULT_TIMEOUT;
		if (buffer[0] != modbusClient.getServerId()) {
			logData("bad id", 0, 1);
			Logger.warning("Modbus",
					"waitResponse(): Invalid id: " + buffer[0] + "expected:" + modbusClient.getServerId());
			return ModbusClient.RESULT_BAD_RESPONSE;
		}

		// read function (bit7 means exception)
		if (!this.rs485.readToBuffer(this.buffer, 1, 1, this.timeout))
			return ModbusClient.RESULT_TIMEOUT;
		if ((buffer[1] & 0x7f) != modbusClient.getFunction()) {
			logData("bad function", 0, 2);
			Logger.warning("Modbus",
					"waitResponse(): Invalid function: " + buffer[1] + "expected: " + modbusClient.getFunction());
			return ModbusClient.RESULT_BAD_RESPONSE;
		}

		if ((buffer[1] & 0x80) != 0) {
			// EXCEPTION
			expectedBytes = 5; // id(1), function(1), exception code(1), crc(2)
			if (!this.rs485.readToBuffer(this.buffer, 2, 3, this.timeout)) // exception code + CRC
				return ModbusClient.RESULT_TIMEOUT;
			if (crcValid(3)) {
				logData("exception", 0, expectedBytes);
				modbusClient.setPduSize(2); // function + exception code
				modbusClient.writeToPdu(buffer, 1, modbusClient.getPduSize(), 0);
				return ModbusClient.RESULT_EXCEPTION;
			} else {
				logData("bad crc (exception)", 0, expectedBytes);
				return ModbusClient.RESULT_BAD_RESPONSE;
			}
		} else {
			// NORMAL RESPONSE
			if (!this.rs485.readToBuffer(this.buffer, 2, modbusClient.getExpectedPduSize() + 1, this.timeout)) // data + CRC (without function)
				return ModbusClient.RESULT_TIMEOUT;
			// CRC check of (serverId + PDU)
			if (crcValid(1 + modbusClient.getExpectedPduSize())) {
				logData("normal", 0, expectedBytes);
				modbusClient.setPduSize(modbusClient.getExpectedPduSize());
				modbusClient.writeToPdu(buffer, 1, modbusClient.getPduSize(), 0);
				return ModbusClient.RESULT_OK;
			} else {
				logData("bad crc", 0, expectedBytes);
				return ModbusClient.RESULT_BAD_RESPONSE;
			}
		}
	}

	protected void logData(String kind, int start, int length) {
		Logger.info("Modbus", "Read: " + ModbusPdu.toHex(buffer, start, length));
	}

	/**
	 * CRC validation
	 * @param size
	 * @return
	 */
	protected boolean crcValid(int size) {
		int crc = ModbusPdu.calcCRC16(buffer, 0, size);
		int crc2 = ModbusPdu.bytesToInt16(buffer[size], buffer[size + 1], true);
		if (crc == crc2)
			return true;
		else {
			Logger.warning("Modbus",
					"CRC error calc:" + Integer.toHexString(crc) + " in response: " + Integer.toHexString(crc2));
			return false;
		}
	}
	
}
