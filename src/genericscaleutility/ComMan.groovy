/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package genericscaleutility

import gnu.io.*
/**
 *
 * @author SYSTEM
 */
class ComMan implements SerialPortEventListener {

    private CommPortIdentifier m_PortIdPrinter;
    private SerialPort serialScale;

    private String m_sPortScale;
    private OutputStream m_out;
    private InputStream m_in;

    private static final int SCALE_READY = 0;
    private static final int SCALE_READING = 1;
    private static final int SCALE_READINGDECIMALS = 2;

    private double m_dWeightBuffer;
    private double m_dWeightDecimals;
    private int m_iStatusScale;
    def buffer = "", tempBuffer = ""
    def miniDriver = [:]

    /** Creates a new instance of ScaleComm */
    public def ComMan(String sPortPrinter) {
        m_sPortScale = sPortPrinter;
        m_out = null;
        m_in = null;

        m_iStatusScale = SCALE_READY;
        //m_dWeightBuffer = 0.0;
        //m_dWeightDecimals = 1.0;
    }

    public def readWeight(command, miniDriver) {

        this.miniDriver = miniDriver
        synchronized(this) {

            if (m_iStatusScale != SCALE_READY) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                }
                if (m_iStatusScale != SCALE_READY) {
                    // bascula tonta.
                    m_iStatusScale = SCALE_READY;
                }
            }

            // Ya estamos en SCALE_READY
            //m_dWeightBuffer = 0.0;
            //m_dWeightDecimals = 1.0;
            write(command); // $
            flush();

            // Esperamos un ratito
            try {
                wait(1000);
            } catch (InterruptedException e) {
            }

            if (m_iStatusScale == SCALE_READY) {
                // hemos recibido cositas o si no hemos recibido nada estamos a 0.0
                /*double dWeight = m_dWeightBuffer / m_dWeightDecimals;
                m_dWeightBuffer = 0.0;
                m_dWeightDecimals = 1.0;
                return new Double(dWeight);
                */
                return buffer
            } else {
                m_iStatusScale = SCALE_READY;
                //m_dWeightBuffer = 0.0;
                //m_dWeightDecimals = 1.0;
                //return new Double(0.0);
                return "0.0"
            }
        }
    }

    private void flush() {
        try {
            m_out.flush();
        } catch (IOException e) {
        }
    }

    private void write(byte[] data) {
        try {
            if (m_out == null) {
                m_PortIdPrinter = CommPortIdentifier.getPortIdentifier(miniDriver.port); // Tomamos el puerto
                serialScale = (SerialPort) m_PortIdPrinter.open("PORTID", 2000); // Abrimos el puerto

                m_out = serialScale.getOutputStream(); // Tomamos el chorro de escritura
                m_in = serialScale.getInputStream();

                serialScale.addEventListener(this);
                serialScale.notifyOnDataAvailable(true);

                def bits, stopBits, parity
                switch(miniDriver.bits) {
                    case "5": bits = SerialPort.DATABITS_5; break
                    case "6": bits = SerialPort.DATABITS_6; break
                    case "7": bits = SerialPort.DATABITS_7; break
                    case "8": bits = SerialPort.DATABITS_8; break
                }
                switch(miniDriver.stopBits) {
                    case "1"  : stopBits = SerialPort.STOPBITS_1  ; break
                    case "1.5": stopBits = SerialPort.STOPBITS_1_5; break
                    case "2"  : stopBits = SerialPort.STOPBITS_2  ; break
                }
                switch(miniDriver.parity) {
                    case "None" : parity  = SerialPort.PARITY_NONE; break
                    case "Odd"  : parity  = SerialPort.PARITY_ODD;  break
                    case "Even" : parity  = SerialPort.PARITY_EVEN; break
                    case "Mark" : parity  = SerialPort.PARITY_MARK; break
                    case "Space": parity  = SerialPort.PARITY_SPACE;break
                }

                serialScale.setSerialPortParams(miniDriver.baud, bits, stopBits, parity); // Configuramos el puerto
                serialScale.setDTR(false);
                serialScale.setRTS(false);
            }
            m_out.write(data);
        } catch (NoSuchPortException e) {
            e.printStackTrace();
        } catch (PortInUseException e) {
            e.printStackTrace();
        } catch (UnsupportedCommOperationException e) {
            e.printStackTrace();
        } catch (TooManyListenersException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serialEvent(SerialPortEvent e) {

        // Determine type of event.
        switch (e.getEventType()) {
            case SerialPortEvent.BI:
            case SerialPortEvent.OE:
            case SerialPortEvent.FE:
            case SerialPortEvent.PE:
            case SerialPortEvent.CD:
            case SerialPortEvent.CTS:
            case SerialPortEvent.DSR:
            case SerialPortEvent.RI:
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                break;
            case SerialPortEvent.DATA_AVAILABLE:
                try {
                    tempBuffer = ""
                    while (m_in.available() > 0) {
                        println "Dat Disponible"
                        int b = m_in.read();
                        println "leído -> $b "
                        if (b == ((miniDriver.stopChar[0] as int) as char)) { // CR ASCII
                            // Fin de lectura
                            synchronized (this) {
                                m_iStatusScale = SCALE_READY;
                                println "scale ready"
                                buffer = tempBuffer
                                notifyAll();
                            }
                            
                        } else {
                            synchronized(this) {
                            tempBuffer += (b as char)
                            
                            }
                            /*
                        } else if ((b > 0x002F && b < 0x003A) || b == 0x002E){
                            synchronized(this) {
                                if (m_iStatusScale == SCALE_READY) {
                                    m_dWeightBuffer = 0.0; // se supone que esto debe estar ya garantizado
                                    m_dWeightDecimals = 1.0;
                                    m_iStatusScale = SCALE_READING;
                                    println "leyendo..."
                                }
                                if (b == 0x002E) {
                                    m_iStatusScale = SCALE_READINGDECIMALS;
                                    println "leyendo decimales"
                                } else {
                                    m_dWeightBuffer = m_dWeightBuffer * 10.0 + b - 0x0030;
                                    if (m_iStatusScale == SCALE_READINGDECIMALS) {
                                        m_dWeightDecimals *= 10.0;
                                        println "leyendo decimales y almacenando en buffer"
                                    }
                                }
                            }
                        } else {
                            // caracteres invalidos, reseteamos.
                            println "======= Caractér inválido"
                            //m_dWeightBuffer = 0.0; // se supone que esto debe estar ya garantizado
                            m_dWeightDecimals = 1.0;
                            m_iStatusScale = SCALE_READY;
                            println "de nuevo lista"
                        }
                        */
                    }
                    }
                } catch (IOException eIO) { println "[Excepción IO: ${eIO.getMessage()}]"}
                break;
        }
    }

    /*
    CommPortIdentifier portId1 = CommPortIdentifier.getPortIdentifier("COM1");
    InputStream inputStream;
    OutputStream outputStream;
    SerialPort serialPort1;
    Thread readThread;
    protected String divertCode = "10";
    def lastInput
    static String TimeStamp;

    def ComMan() {
        try {
            TimeStamp = new java.util.Date().toString();
            serialPort1 = (SerialPort) portId1.open("ComControl", 2000);
            System.out.println(TimeStamp + ": " + portId1.getName() + " opened for scanner input");

        } catch (PortInUseException e) {}
        try {
            inputStream = serialPort1.getInputStream();
        } catch (IOException e) {}
        try {
            serialPort1.addEventListener(this);
        } catch (TooManyListenersException e) {}
        serialPort1.notifyOnDataAvailable(true);
        try {

            serialPort1.setSerialPortParams(9600,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);

            serialPort1.setDTR(false);
            serialPort1.setRTS(false);


        } catch (UnsupportedCommOperationException e) {}

    }
    def write(inp) {
        outputStream = serialPort1.getOutputStream();
        outputStream.write(inp.getBytes());
        System.out.println(TimeStamp + ": command sent");
        outputStream.close();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) { println "InterruptedException" }
        "->"+lastInput

    }

    
    public void serialEvent(SerialPortEvent event) {
        switch(event.getEventType()) {
        case SerialPortEvent.BI:
        case SerialPortEvent.OE:
        case SerialPortEvent.FE:
        case SerialPortEvent.PE:
        case SerialPortEvent.CD:
        case SerialPortEvent.CTS:
        case SerialPortEvent.DSR:
        case SerialPortEvent.RI:
        case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
            break;
        case SerialPortEvent.DATA_AVAILABLE:
            StringBuffer readBuffer = new StringBuffer();
            int c;
            try {
                 while ((c=inputStream.read()) != 10){
                   if(c!=13)  readBuffer.append((char) c);
                   println "--->"+c+";--->"+readBuffer
                 }
                 String scannedInput = readBuffer.toString();
                 lastInput = scannedInput
                 TimeStamp = new java.util.Date().toString();
                 System.out.println(TimeStamp + ": scanned input received:" + scannedInput);
                 inputStream.close();

            } catch (IOException e) {}

            break;
        }
    }
    */
}


