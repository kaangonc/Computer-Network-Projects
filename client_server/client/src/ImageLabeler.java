import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/*
Authors:
Kaan Gönç           21602670
Ahmet Ayrancıoğlu   21601206

The images will be saved into the same directory as the .class file located.
Authentication, asking images, and exiting are automated, sending labels is manual and should be provided by the user.
The client will ask for images four times from the server.
 */
public class ImageLabeler {

    private static int IMAGE_AMOUNT = 3;

    private static Socket createSocket(String address, int port) {
        try {
            Socket socket = new Socket(address, port);
            return socket;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String read_command() {
        Scanner scan = new Scanner(System.in);
        String command = scan.nextLine();
        return command;
    }

    private static void sendRequest(Socket socket, String request) {
        try {
            assert socket != null;
            request = request + "\r\n";
            DataOutputStream out;
            out = new DataOutputStream(socket.getOutputStream());
            out.write(request.getBytes(StandardCharsets.US_ASCII));
            //out.flush();
            //socket.shutdownOutput();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String getMessageResponse(Socket socket) {
        try {
            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String message = br.readLine();
            return message;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean readData(DataInputStream inputStream, byte[] data) {
        try {
            if (inputStream.read(data) <= 0) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private static boolean getImageResponse(Socket socket) {
        byte[] data;
        String code, fileName;
        int size, codeLength = 4, sizeLength = 3;
        DataInputStream inputStream = null;
        try {
            inputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            return false;
        }

        for (int i = 0; i < IMAGE_AMOUNT; i++) {
            data = new byte[codeLength];
            if (!readData(inputStream, data)) {
                System.out.println("Error while getting response!");
                return false;
            }
            code = new String(data);
            if (code.equals("INVA")) {
                System.out.println("Server Response: " + code + getMessageResponse(socket));
                return false;
            }
//            if (code.equals("")) {
//                System.out.println("ENTERED");
//                sendRequest(socket, "IGET");
//            }
            if(!code.equals("ISND")) {
                String c;
                while (true) {
                    data = new byte[1];
                    if (!readData(inputStream, data)) {
                        System.out.println("Error while getting response!");
                        return false;
                    }
                    c = new String(data);
                    if (c.equals("I")) {
                        String c1;
                        data = new byte[3];
                        if (!readData(inputStream, data)) {
                            System.out.println("Error while getting response!");
                            return false;
                        }
                        c1 = new String(data);
                        if (c1.equals("SND")) {
                            break;
                        }
                    }
                }
            }

            data = new byte[sizeLength];
            if (!readData(inputStream, data)) {
                System.out.println("Error while getting response!");
                return false;
            }
            size = byteArrayToInt(data);

            data = new byte[size];
            if (!readData(inputStream, data)) {
                System.out.println("Error while getting response!");
                return false;
            }

            fileName = "image" + i + ".jpg";
            if (!writeImage(data, fileName)) {
                System.out.println("Error while writing the image!");
                return false;
            }

//            if (i == IMAGE_AMOUNT - 1) {
////                data = new byte[1];
//                while(getMessageResponse(socket) != null);
////                {
////                    System.out.println(data);
////                }
//            }
        }

        return true;
    }

    private static int byteArrayToInt(byte[] b)
    {
        return   b[2] & 0xFF |
                (b[1] & 0xFF) << 8 |
                (b[0] & 0xFF) << 16;
    }

    private static boolean writeImage(byte[] image, String fileName) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(image);
            BufferedImage bImage = ImageIO.read(bis);
            ImageIO.write(bImage, "jpg", new File(fileName));

        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private static boolean authenticate(Socket socket){
        System.out.println("Authenticating");
        String command;
        String[] asks = {"Username: ", "Password: "};
        String[] codes = {"USER ", "PASS "};
        for (int i = 0; i < 2; i++) {
            command = i == 0 ? "USER bilkentstu" : "PASS cs421f2019";
//            System.out.print(asks[i]);
//            command = read_command();;
//            sendRequest(socket, codes[i] + command);
            sendRequest(socket, command);
            String response = getMessageResponse(socket);

            if (response == null) {
                System.out.println("NO RESPONSE FROM SERVER!");
                return false;
            }

            if (!response.equals("OK")) {
                System.out.println("Server Response: " + response);
                return false;
            }
        }
        return true;

    }

    private static boolean askImages(Socket socket) {
        sendRequest(socket, "IGET");
        return getImageResponse(socket);
    }

    private static String[] askLablesFromUser() {
        String[] messages = {"Label of first image: ", "Label of second image: ", "Label of third image: "};
        String[] results = new String[IMAGE_AMOUNT];
        for (int i = 0; i < IMAGE_AMOUNT; i++) {
            System.out.print(messages[i]);
            results[i] = read_command();
        }
        return results;
    }

    private static boolean sendLabels(Socket socket, String[] labels) {
        String command = "ILBL " + labels[0] + "," + labels[1] + "," + labels[2];
        sendRequest(socket, command);
        String response = getMessageResponse(socket);

        if (response != null && response.contains("INVALID")) {
            System.out.println("Server Response: " + response);
            return false;
        }
        return  true;
    }

    private static void closeSocket(Socket socket) {
        try {
            assert socket != null;
            socket.close();
            System.out.println("Connection closed!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Socket socket = createSocket(args[0], Integer.parseInt(args[1]));
        boolean isAuth = authenticate(socket);
        if (!isAuth) {
            closeSocket(socket);
            return;
        }
        System.out.println("Authentication successful!");

        int askCount = 0;
        boolean imageResult;
        String[] labels;
        while (true) {
            System.out.println("");
            if(askCount >= 4) {
                break;
            }
            System.out.println("Getting Images...");
            imageResult = askImages(socket);
            if(!imageResult) {
                continue;
            }
            System.out.println("Images were saved successfully!");
            labels = askLablesFromUser();
            while(!sendLabels(socket, labels)) {
                System.out.println("Wrong Labels! Please enter again:");
                labels = askLablesFromUser();
            }
            System.out.println("True Labeling!");
            askCount++;
        }

        sendRequest(socket, "EXIT");
        closeSocket(socket);
    }
}