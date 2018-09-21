package tijos.framework.component.rs485;

import java.io.IOException;

import tijos.framework.devicecenter.TiGPIO;
import tijos.framework.devicecenter.TiUART;
import tijos.framework.util.Delay;
import tijos.framework.util.Formatter;
import tijos.framework.util.logging.Logger;

/**
 * RS485 based on UART for TiJOS 
 * @author TiJOS
 *
 */
public class TiRS485 {

	private TiUART uart;
	private TiGPIO gpio;
	private int gpioPin;

	/**
	 * Initialize RS485 with UART and GPIO
	 * @param uartPort  UART port id
	 * @param gpioPort  GPIO port id 
	 * @param gpioPin   GPIO pin id 
	 * @throws IOException
	 */
	public TiRS485(int uartPort, int gpioPort, int gpioPin) throws IOException {

		// RS485使用UART1 根据外设进行初始化
		uart = TiUART.open(uartPort);

		if(gpioPort >= 0) {
			// RS485 使用GPIO Port3 Pin4 进行半双工切换
			gpio = TiGPIO.open(gpioPort, gpioPin);
			gpio.setWorkMode(gpioPin, TiGPIO.OUTPUT_PP);

			this.gpioPin = gpioPin;
		}
	}

	/**
	 * Open with communication parameters
	 * @param baudRate  
	 * @param dataBitNum
	 * @param stopBitNum
	 * @param parity
	 * @throws IOException
	 */
	public void open(int baudRate, int dataBitNum, int stopBitNum, int parity) throws IOException {

		// UART通讯参数
		uart.setWorkParameters(dataBitNum, stopBitNum, parity, baudRate);
	}
	
	/**
	 * Close 
	 * @throws IOException
	 */
	public void close() throws IOException {
		this.uart.close();
	}
	
	/**
	 * Clear UART buffer
	 * @throws IOException
	 */
	public void clearInput() throws IOException {
			
		this.uart.clear(TiUART.BUFF_READ);
	}

	/**
	 * Write data to the uart 
	 * @param buffer 
	 * @param start
	 * @param length
	 * @throws IOException
	 */
	public void write(byte [] buffer ,int start ,int length) throws IOException {
		
		if(gpio != null)
		{
			Logger.info("TiRS485", "sendReqeust GPIO high");
			this.gpio.writePin(this.gpioPin, 1);
		}
		
		this.uart.write(buffer, start, length);
	}

	/**
	 * Read data into buffer from the UART
	 * 
	 * @param start
	 * @param length
	 * @param modbusClient
	 * @return
	 * @throws IOException
	 */
	public boolean readToBuffer(byte[] buffer, int start, int length, int timeOut) throws IOException {

		if(gpio != null)
		{
			Logger.info("TiRS485", "readResponse GPIO low");
			this.gpio.writePin(this.gpioPin, 0);
		}
		
		long now = System.currentTimeMillis();
		long deadline = now + timeOut;
		int offset = start;
		int bytesToRead = length;
		int res;
		while ((now < deadline) && (bytesToRead > 0)) {
			res = this.uart.read(buffer, offset, bytesToRead);
			if (res <= 0) {
				Delay.msDelay(10);
				now = System.currentTimeMillis();
				continue;
			}

			offset += res;
			bytesToRead -= res;
			if (bytesToRead > 0) // only to avoid redundant call of System.currentTimeMillis()
				now = System.currentTimeMillis();
		}
		res = length - bytesToRead; // total bytes read
		if (res < length) {
			Logger.info("TiRS485",
					"Read timeout(incomplete): " + Formatter.toHexString(buffer, offset, start + res, ""));

			return false;
		} else
			return true;
	}

	
}
