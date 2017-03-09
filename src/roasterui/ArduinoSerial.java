/*
 *
 *          Class to handle serial connection to arduino
 *
*/


package roasterui;

import java.util.ArrayList;
import javafx.application.Platform;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class ArduinoSerial{
    private String portName;
    private String incoming;
    private static SerialPort serialPort;
    public ArrayList commandQueue;
    
    void connect(){
        portName = "/dev/tty.usbmodemFD121";
        serialPort = new SerialPort(portName);
        try {
            System.out.println("connecting");
            serialPort.openPort();
            serialPort.setParams(SerialPort.BAUDRATE_9600,
                                 SerialPort.DATABITS_8,
                                 SerialPort.STOPBITS_1,
                                 SerialPort.PARITY_NONE);

            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | 
                                  SerialPort.FLOWCONTROL_RTSCTS_OUT);

            
            serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);

            
            
            
            
        } catch (SerialPortException ex) {
            System.out.println("There are an error on writing string to port т: " + ex);
        }
    }
    
    void send(int command,int value){
        try {
            serialPort.writeString(command+","+value+"\n");
            //System.out.println("data sent");
        } catch (SerialPortException ex) {
            System.out.println("Error sending command: " + ex);
        }
    }
    
    public String checkQueue(){
            if (commandQueue.size() == 0) {
                return "";
            }
            String message = (String) commandQueue.get(0);
            commandQueue.remove(0); 
            return message;
    }
    
    void close(){
        if (serialPort != null && serialPort.isOpened ()) {
            try {
                serialPort.closePort();//Close serial port
            } catch (SerialPortException ex) {
                System.out.println("Failed to close port.");
            }
        }
    }
    
    
    public class PortReader implements SerialPortEventListener {
        StringBuilder message = new StringBuilder();
        PortReader(){
            commandQueue = new ArrayList ();
        }
        @Override
        public void serialEvent(SerialPortEvent event) {
            
            if(event.isRXCHAR() && event.getEventValue() > 0){
                try {
                    byte buffer[] = serialPort.readBytes();
                    for (byte b: buffer) {
                            if ( (b == '\r' || b == '\n') && message.length() > 0) {
                                String toProcess = message.toString();
                                processMessage(toProcess);
                                message.setLength(0);
                            }
                            else if (b != '\r' && b != '\n'){
                                message.append((char)b);   
                            }
                    }                
                }
                catch (SerialPortException ex) {
                    System.out.println(ex);
                    System.out.println("serialEvent Error");
                }
            }
        }
        public void processMessage(String msg){
            commandQueue.add(msg);
        }
        
        
    }
    
    
    
}
