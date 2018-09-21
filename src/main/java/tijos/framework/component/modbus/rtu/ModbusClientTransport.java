package tijos.framework.component.modbus.rtu;

public interface ModbusClientTransport {
	
	public void sendRequest(ModbusClient modbusClient) throws Exception, InterruptedException;
	
	public int waitResponse(ModbusClient modbusClient) throws Exception, InterruptedException;
	
	public void close();
}
