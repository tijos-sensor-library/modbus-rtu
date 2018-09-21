# modbus-rtu
TiJOS driver for Modbus RTU protocol



| 条目       | 说明                                  |
| ---------- | ------------------------------------- |
| 驱动名称   | MODBUS RTU Client                     |
| 适用       | 该驱动适用于符合MODBUS RTU 协议的设备 |
| 通讯方式   | I2C                                   |
| Java Class | ModbusClient.java                     |
| 图片       |                                       |



## 主要接口

| 函数                                                         | 说明                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| ModbusClient(TiRS485 rs485,  int timeout, int pause)         | 实初化， timout: 通讯超时，pause: 发送命令后等待时间后开始读取数据 |
| InitReadCoilsRequest(int serverId, int startAddress, int count) | 初始化Read Coils 请求                                        |
| InitWriteCoilRequest(int serverId, int coilAddress, boolean value) | 初始化WRITE COIL register 请求- 单寄存器操作                 |
| InitWriteCoilsRequest(int serverId, int startAddress, boolean[] values) | 初始化WRITE MULTIPLE COILS registers 请求- 多寄存器操作      |
| InitReadHoldingsRequest(int serverId, int startAddress, int count) | 初始化READ HOLDING REGISTERs 请求                            |
| InitReadDInputsRequest(int serverId, int startAddress, int count) | 初始化READ DISCRETE INPUT REGISTERs 请求                     |
| InitReadAInputsRequest(int serverId, int startAddress, int count) | 初始化READ INPUT REGISTERs 请求                              |
| InitWriteRegisterRequest(int serverId, int regAddress, int value) | 初始化WRITE SINGLE REGISTER 请求 - 单寄存器操作              |
| InitWriteRegistersRequest(int serverId, int startAddress, int[] values) | 初始化WRITE MULTIPLE 请求 - 多寄存器操作                     |
| int execRequest()                                            | 执行MODBUS 请求并获得响应                                    |
| int getExceptionCode()                                       | 获得返回的MODBUS异常码                                       |
| int getResponseAddress()                                     | 获取返回数据的开始地址                                       |
| int getResponseCount()                                       | 获取返回数据寄存器个数                                       |
| boolean getResponseBit(int address)                          | 获取指定地址COIL寄存器值                                     |
| int getResponseRegister(int address, boolean unsigned)       | 获取指定地址InputRegister/HoldingRegister的值， unsigned: 返回值 为无符号或有符号 |



## 使用方法

### 第一步 ：RS485 初始化

创建RS485对象， 指定UART ID, 以及用于RS485半双工切换的GIPOPIN , 并设置通讯参数

```java
		// 485端口 - UART 1, GPIO PORT 2 PIN 4
		TiRS485 rs485 = new TiRS485(1, 2, 4);
		
		// 通讯参数 9600，8，1，N
		rs485.open(9600, 8, 1, TiUART.PARITY_NONE);
```
### 第二步:  MODBUS  客户端设置

创建ModbusClient对象， 设置RS485及通讯参数

```java
		// Modbus 客户端
		// 通讯超时2000 ms 读取数据前等待5ms
		ModbusClient mc = new ModbusClient(rs485, 2000, 5);
```
### 第三步：操作寄存器

进行寄存器操作，步骤：

1. 通过InitXXXRequst初始化参数，
2. execRequest执行请求，并获取响应
3. getResponseRegister

```java
//初始读取Holding Register参数， 设备地址， 寄存器开始地址， 个数
mc.InitReadHoldingsRequest(serverId, startAddr, count);	
//执行请求
int result = mc.execRequest();
//执行成功
if (result == ModbusClient.RESULT_OK) {
    	//解析寄存器地址及值(无符号或有符号)
		int humdity = mc.getResponseRegister(mc.getResponseAddress(), false);
		int temperature  = mc.getResponseRegister(mc.getResponseAddress() + 1, false);
}
```
