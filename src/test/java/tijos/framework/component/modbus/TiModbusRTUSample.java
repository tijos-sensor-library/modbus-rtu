package tijos.framework.component.modbus;

import tijos.framework.component.modbus.rtu.ModbusClient;
import tijos.framework.component.rs485.TiRS485;
import tijos.framework.devicecenter.TiUART;
import tijos.framework.util.Delay;

/**
 * MODBUS RTU Client Driver
 *
 */
public class TiModbusRTUSample {
	public static void main(String[] args) {
		System.out.println("Hello Modbus RTU!");

		try {

			// 485端口 - UART 1, GPIO PORT 2 PIN 4
			TiRS485 rs485 = new TiRS485(1, 2, 4);

			// 通讯参数
			rs485.open(9600, 8, 1, TiUART.PARITY_NONE);

			// Modbus 客户端
			// 通讯超时2000 ms 读取数据前等待5ms
			ModbusClient mc = new ModbusClient(rs485, 2000, 5);

			// Modbus Server 设备地址
			int serverId = 1;

			// Input Register 开始地址
			int startAddr = 0;

			// Read 2 registers from start address 读取个数
			int count = 2;

			int number = 10;
			while (number-- > 0) {
				// 读取Holding Register
				mc.InitReadHoldingsRequest(serverId, startAddr, count);

				int result = mc.execRequest();

				if (result == ModbusClient.RESULT_OK) {

					int humdity = mc.getResponseRegister(mc.getResponseAddress(), false);
					int temperature  = mc.getResponseRegister(mc.getResponseAddress() + 1, false);

					System.out.println("temp = " + temperature + " humdity = " + humdity);

				} else {
					System.out.println("Modbus Error: result = " + result);
				}
				
				Delay.msDelay(5000);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
