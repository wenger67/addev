package com.vinson.addev.serialport;

import android.serialport.SerialPort;

import com.socks.library.KLog;

import java.io.IOException;
import java.security.InvalidParameterException;

public class SerialPortWrapper {

    private static SerialPort SERIAL_PORT = null;

    public static SerialPort getSerialPort() {
        if (SERIAL_PORT == null) {
            try {
                SERIAL_PORT = getSerialPortInner();
            } catch (Exception e) {
                e.printStackTrace();
                KLog.d(e.getMessage());
                return null;
            }
        }
        return SERIAL_PORT;
    }


    public static SerialPort getSerialPortInner()
            throws SecurityException, IOException, InvalidParameterException {
        /* Read serial port parameters */

        String path = SerialConfig.getSerialPath();
        int baudrate = SerialConfig.getSerialBau();

        /* Check parameters */
        if ((path.length() == 0) || (baudrate == -1)) {
            throw new InvalidParameterException();
        }
        /* Open the serial port */
        //mSerialPort = new SerialPort(new File(path), baudrate, 0);

        return SerialPort //
                .newBuilder(path, baudrate) // 串口地址地址，波特率
                .parity(0) // 校验位；0:无校验位(NONE，默认)；1:奇校验位(ODD);2:偶校验位(EVEN)
                .dataBits(8) // 数据位,默认8；可选值为5~8
                .stopBits(1) // 停止位，默认1；1:1位停止位；2:2位停止位
                .build();
    }

    public void closeSerialPort() {
        if (SERIAL_PORT != null) {
            SERIAL_PORT.close();
            SERIAL_PORT = null;
        }
    }

}
