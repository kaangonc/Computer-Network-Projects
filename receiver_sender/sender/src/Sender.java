import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

/**
 * Authors:
 * Kaan Gonc - 21602670
 * Ahmet Ayrancioglu - 21601206
 */

public class Sender {
    private final static int SEGMENT_SIZE = 1024;
    private final static int HEADER_SIZE = 2;

    private static class SegmentThread extends Thread {
        DatagramSocket socket;
        int segmentNo;
        byte[] segment;
        long timeout;
        int port;

        SegmentThread(DatagramSocket socket, int segmentNo, byte[] segment, long timeout, int port) {
            this.socket = socket;
            this.segmentNo = segmentNo;
            this.segment = segment;
            this.timeout = timeout;
            this.port = port;
        }


        @Override
        public void run() {

            while(true) {
                sendPacket(socket, segment, port, SEGMENT_SIZE);
//                System.out.println("Sent packet: " + segmentNo);
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    return;
                }
            }

        }
    }

    private static void sendPacket(DatagramSocket socket, byte[] segment, int port, int size) {
        try {
            InetAddress addr = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(segment, size, addr, port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int receiveACK(DatagramSocket socket) {
        byte[] ackBuffer = new byte[HEADER_SIZE];
        DatagramPacket ackPacket = new DatagramPacket(ackBuffer, HEADER_SIZE);
        try {
            socket.receive(ackPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int ackNo = byteArrayToInt(ackBuffer);
        return ackNo;

    }

    private static int byteArrayToInt(byte[] b)
    {
        return   b[1] & 0xFF |
                (b[0] & 0xFF) << 8;
    }

    private static byte[] intToByteArray(int i) {
        byte[] arr = new byte[2];
        arr[0] = (byte) (i >>> 8);
        arr[1] = (byte) i;
        return arr;
    }
    private static ArrayList<byte[]> readFileAndCreateSegments(String filePath) {
        ArrayList<byte[]> segments = new ArrayList<>();
        try {
            int sequenceNumber = 1;
            FileInputStream in = new FileInputStream(filePath);
            byte[] data = new byte[SEGMENT_SIZE-HEADER_SIZE];
            while(in.read(data) > -1) {
                byte[] seq = intToByteArray(sequenceNumber);
                byte[] segment = new byte[SEGMENT_SIZE];
                segment[0] = seq[0];
                segment[1] = seq[1];
                for(int i = 0; i < (SEGMENT_SIZE-HEADER_SIZE); i++) {
                    segment[i+2] = data[i];
                }
                segments.add(segment);
                data = new byte[SEGMENT_SIZE-HEADER_SIZE];
                sequenceNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return segments;

    }

    private static void selectiveRepeat(DatagramSocket socket, ArrayList<byte[]> segments, int windowsSize, long timeout, int port) {
        int nextSeqNum = 1;
        int sendBase = 1;
        int segmentCount = segments.size();
        SegmentThread[] threads = new SegmentThread[segmentCount];
        boolean[] segmentAcks = new boolean[segmentCount];
        int noOfAcksArrived = 0;
        while(noOfAcksArrived < segmentCount) {
            int limit = sendBase + windowsSize;
            if(limit > segmentCount + 1) {
                limit = segmentCount + 1;
            }
            for(int segmentNo = nextSeqNum; segmentNo < limit; segmentNo++) {
                SegmentThread st = new SegmentThread(socket, segmentNo, segments.get(segmentNo-1), timeout, port);
                st.start();
                threads[segmentNo-1] = st;
            }
            nextSeqNum = sendBase + windowsSize;
            int ackNo = receiveACK(socket);
            if(ackNo >= sendBase && ackNo < (sendBase + windowsSize)) {
                if(threads[ackNo-1].isAlive()) {
                    noOfAcksArrived++;
                    segmentAcks[ackNo-1] = true;
                    threads[ackNo-1].interrupt();
                    while(sendBase <= segmentCount && segmentAcks[sendBase-1]) {
                        sendBase++;
                    }
                }
            }
        }
        terminate(socket, port);
        for(int i = 0; i < segmentCount; i++) {
            if (threads[i].isAlive()) {
                threads[i].interrupt();
            }
        }

    }

    private static void terminate(DatagramSocket socket, int port) {
        byte[] terminationData = intToByteArray(0);
        sendPacket(socket, terminationData, port, 2);
    }

    private static DatagramSocket createSocket(int port) {
        try {
            return new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        String filePath = args[0];
        int receiverPort =  Integer.parseInt(args[1]);
        int windowSize = Integer.parseInt(args[2]);
        long retransmissionTimeout = Long.parseLong(args[3]);

        DatagramSocket socket = createSocket(receiverPort);
        ArrayList<byte[]> segments = readFileAndCreateSegments(filePath);
        selectiveRepeat(socket, segments, windowSize, retransmissionTimeout, receiverPort);

        assert socket != null;
        socket.close();
    }
}
