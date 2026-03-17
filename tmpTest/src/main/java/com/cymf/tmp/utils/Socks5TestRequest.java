package com.cymf.tmp.utils;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;


public class Socks5TestRequest extends Thread {

    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private String hostPort = null;
    private Socks5TestRequestListener listener = null;

    public Socks5TestRequest(String hostPort) {
        this.hostPort = hostPort;
    }

    public void setListener(Socks5TestRequestListener listener) {
        this.listener = listener;
    }

    public void run() {
        String host = "";
        int port = 30000;

        try {
            String[] split = hostPort.split(":");
            host = split[0];
            port = Integer.parseInt(split[1]);
            this.listener.callbackLog(df.format(new Date()) + " host:" + host + " port:" + port);
        } catch(Exception ex) {
            this.listener.callbackLog(df.format(new Date()) + " 输入host:形式，IP用30000，域名用40000，当前格式不对。");
            return;
        }

        // GET_HTTP
        try {
            this.listener.callbackLog(
                    df.format(new Date()) + " GET_HTTP\n" + get("http://" + host + ":" + port + "/test?http=") + "\n");
        } catch(Exception ex) {
            this.listener.callbackLog(
                    df.format(new Date()) + " GET_HTTP ERROR. " + ExceptionUtil.toString(ex) + "\n");
        }

        // GET_HTTPS
        try {
            if (host.contains(".com")) {
                this.listener.callbackLog(
                        df.format(new Date()) + " GET_HTTPS\n" + get("https://" + host + ":" + (port + 1) + "/test?https=") + "\n");
            } else {
                this.listener.callbackLog(df.format(new Date()) + " GET_HTTPS\nHost为IP形式，跳过\n");
            }
        } catch(Exception ex) {
            this.listener.callbackLog(
                    df.format(new Date()) + " GET_HTTPS ERROR. " + ExceptionUtil.toString(ex) + "\n");
        }

        // POST_HTTP
        try {
            this.listener
                    .callbackLog(df.format(new Date()) + " POST_HTTP\n" + post("http://" + host + ":" + (port + 2) + "/http") + "\n");
        } catch(Exception ex) {
            this.listener.callbackLog(
                    df.format(new Date()) + " POST_HTTP ERROR. " + ExceptionUtil.toString(ex) + "\n");
        }

        // POST_HTTPS
        try {
            if (host.contains(".com")) {
                this.listener.callbackLog(
                        df.format(new Date()) + " POST_HTTPS\n" + post("https://" + host + ":" + (port + 3) + "/https") + "\n");
            } else {
                this.listener.callbackLog(df.format(new Date()) + " POST_HTTPS\nHost为IP形式，跳过\n");
            }
        } catch(Exception ex) {
            this.listener.callbackLog(
                    df.format(new Date()) + " POST_HTTPS ERROR. " + ExceptionUtil.toString(ex) + "\n");
        }

        // UDP
        try {
            this.listener.callbackLog(df.format(new Date()) + " UDP\n" + udp(host, port + 4) + "\n");
        } catch(Exception ex) {
            this.listener.callbackLog(
                    df.format(new Date()) + " UDP ERROR. " + ExceptionUtil.toString(ex) + "\n");
        }

        // TCP SOCKET
        try {
            this.listener.callbackLog(df.format(new Date()) + " TCP SOCKET\n" + tcpSocket(host, port + 5) + "\n");
        } catch(Exception ex) {
            this.listener.callbackLog(
                    df.format(new Date()) + " TCP SOCKET ERROR. " + ExceptionUtil.toString(ex) + "\n");
        }

        // UDP SOCKET
        try {
            this.listener.callbackLog(df.format(new Date()) + " UDP SOCKET\n" + udpSocket(host, port + 6) + "\n");
        } catch(Exception ex) {
            this.listener.callbackLog(
                    df.format(new Date()) + " UDP SOCKET ERROR. " + ExceptionUtil.toString(ex) + "\n");
        }

        // THRIFT SOCKET
        try {
            this.listener.callbackLog(df.format(new Date()) + " THRIFT SOCKET\n" + thriftSocket(host, port + 7) + "\n");
        } catch(Exception ex) {
            this.listener.callbackLog(
                    df.format(new Date()) + " THRIFT SOCKET ERROR. " + ExceptionUtil.toString(ex) + "\n");
        }

//		// WEB SOCKET
//		try {
//			this.listener.callbackLog(df.format(new Date()) + " WEB SOCKET\n" + webSocket(host, port + 7) + "\n");
//		} catch(Exception ex) {
//			this.listener.callbackLog(
//					df.format(new Date()) + " WEB SOCKET ERROR. " + ExceptionUtil.toString(ex) + "\n");
//		}
    }

    /**
     * GET请求
     *
     * @param u
     * @return
     */
    private String get(String u) {
        try {
            // 格式化当前时间
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String encodedTime = URLEncoder.encode(now, "UTF-8");
            // 构造 URL
            String urlString = u + encodedTime;
            URL url = new URL(urlString);
            // 3. 发起 HTTP GET 请求
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            // 设置超时时间 (15秒)
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            // 4. 读取返回结果
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return u + "\nRESPONSE: " + response.toString();
            } else {
                return "ERROR " + responseCode;
            }
        } catch (Exception e) {
            return ExceptionUtil.toString(e);
        }
    }

    private String post(String u) {
        try {
            // 2. 准备 POST 数据
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            // 构造 body 格式：time=2026-02-09+20%3A10%3A00
            String postData = "post time=" + URLEncoder.encode(now, "UTF-8");
            byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);

            // 3. 配置连接
            URL url = new URL(u);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true); // 必须设置为 true 才能发送 Body

            // 设置超时 (15秒)
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            // 设置 Content-Type (表单格式)
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));

            // 4. 写入 Body 数据
            try (OutputStream os = conn.getOutputStream()) {
                os.write(postDataBytes);
                os.flush();
            }

            // 5. 读取响应
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String result = in.lines().collect(Collectors.joining("\n"));
                    return u + "\n" + postData + "\nRESPONSE: " + result;
                }
            } else {
                return "ERROR " + responseCode;
            }

        } catch (Exception e) {
            return ExceptionUtil.toString(e);
        }
    }

    private String udp(String host, int port) {
        // 稍等片刻确保服务端线程已就绪
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }

        // 2. 准备 UDP 通信
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            // 设置超时时间 15 秒 (15000ms)
            clientSocket.setSoTimeout(15000);

            // 3. 构造请求数据
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String content = "udp time=" + now;
            byte[] sendData = content.getBytes(StandardCharsets.UTF_8);

            // 4. 发送数据包
            InetAddress serverAddress = InetAddress.getByName(host);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, port);
            clientSocket.send(sendPacket);

            // 5. 接收响应
            byte[] receiveBuffer = new byte[2048];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            try {
                clientSocket.receive(receivePacket);
                String result = new String(receivePacket.getData(), 0, receivePacket.getLength(),
                        StandardCharsets.UTF_8);
                return "udp send " + host + ":" + port + "\nRESPONSE: " + result;
            } catch (java.net.SocketTimeoutException e) {
                return "udp send error. " + ExceptionUtil.toString(e);
            }

        } catch (Exception e) {
            return "udp send error. " + ExceptionUtil.toString(e);
        }
    }

    private String tcpSocket(String host, int port) {
        // 2. 发起连接
        Socket socket = new Socket();
        try {
            int timeout = 15000;
            // 连接超时
            socket.connect(new InetSocketAddress(host, port), timeout);
            // 读取超时
            socket.setSoTimeout(timeout);

            // 3. 发送数据
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // Java 8 的 DateTimeFormatter 是可以用的
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String content = "tcp socket time=" + now;
            out.println(content);
            // 4. 接收响应
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            String response = in.readLine();
            return "tcp socket send " + host + ":" + port + "\nRESPONSE: " + response;
        } catch (Exception e) {
            return "tcp socket error. " + ExceptionUtil.toString(e);
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    private String udpSocket(String host, int port) {
        DatagramSocket clientSocket = null;
        try {
            clientSocket = new DatagramSocket();
            // 设置超时时间 15 秒
            clientSocket.setSoTimeout(15000);

            // 2. 构造请求内容 (不进行 URLEncode，按你要求直接传 time=yyyy-MM-dd HH:mm:ss)
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String content = "udp socket time=" + now;
            byte[] sendData = content.getBytes("UTF-8");

            // 3. 发送数据包
            InetAddress serverAddr = InetAddress.getByName(host);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddr, port);
            clientSocket.send(sendPacket);

            // 4. 接收响应
            byte[] receiveBuf = new byte[2048];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);

            try {
                clientSocket.receive(receivePacket);
                String result = new String(receivePacket.getData(), 0, receivePacket.getLength(), "UTF-8");
                return "udp socket send " + host + ":" + port + "\nRESPONSE: " + result;
            } catch (java.net.SocketTimeoutException e) {
                return "udp socket error. " + ExceptionUtil.toString(e);
            }

        } catch (Exception e) {
            return "udp socket error. " + ExceptionUtil.toString(e);
        } finally {
            if (clientSocket != null) {
                clientSocket.close();
            }
        }
    }

    private String webSocket(String host, int port) {
        // 1. 初始化 Socket
        Socket socket = new Socket();
        try {
            // 设置连接和读取超时（15秒）
            socket.connect(new InetSocketAddress(host, port), 15000);
            socket.setSoTimeout(15000);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // 2. 发送 WebSocket 握手请求 (HTTP GET)
            // Sec-WebSocket-Key 只要是 16 字节 Base64 即可
            String handshake = "GET /chat HTTP/1.1\r\n" + "Host: 127.0.0.1\r\n" + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n" + "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n"
                    + "Sec-WebSocket-Version: 13\r\n\r\n";
            out.write(handshake.getBytes("UTF-8"));
            out.flush();

            // 3. 读取并跳过服务端的响应头 (直到读取到 \r\n\r\n)
            // 这里我们用一个简单的方式读完 HTTP 响应
            byte[] buffer = new byte[1024];
            int read = in.read(buffer);
            String headerResponse = new String(buffer, 0, read, "UTF-8");
            if (!headerResponse.contains("101 Switching Protocols")) {
                return "WebSocket握手失败: " + headerResponse;
            }

            // 4. 准备请求内容
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String content = "web socket time=" + now;
            byte[] payload = content.getBytes("UTF-8");

            // 5. 构造并发送 WebSocket 文本帧 (必须带 Mask)
            // 第1字节: 0x81 (FIN=1, Opcode=1 文本)
            out.write(0x81);
            // 第2字节: 0x80 | 长度 (0x80 表示启用 Mask)
            out.write(0x80 | payload.length);

            // 第3-6字节: 4字节掩码 (这里固定用 1,2,3,4)
            byte[] mask = new byte[] { 1, 2, 3, 4 };
            out.write(mask);

            // 写入经过掩码异或处理的数据
            for (int i = 0; i < payload.length; i++) {
                out.write(payload[i] ^ mask[i % 4]);
            }
            out.flush();

            // 6. 接收服务端返回的帧
            // 读取第1字节 (期望 0x81)
            int firstByte = in.read();
            if (firstByte == 0x81) {
                // 读取第2字节 (长度)
                int resLen = in.read();
                // 如果服务端返回了大于 125 的内容，这里逻辑会更复杂，目前 IP+时间 足够了
                byte[] resPayload = new byte[resLen];

                // 确保完整读取响应体
                int received = 0;
                while (received < resLen) {
                    int r = in.read(resPayload, received, resLen - received);
                    if (r == -1)
                        break;
                    received += r;
                }

                String result = new String(resPayload, "UTF-8");
                return "web socket send " + host + ":" + port + "\nRESPONSE: " + result;
            } else {
                return "web socket send " + host + ":" + port + "\n收到无效帧头: " + firstByte;
            }

        } catch (Exception e) {
            return "web socket error. " + ExceptionUtil.toString(e);
        } finally {
            try {
                // 给系统一点处理缓冲区的时间再关闭
                Thread.sleep(100);
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    private String thriftSocket(String host, int port) {
        TTransport transport = null;
        TProtocol protocol = null;
        SocketDataService.Client client = null;
        try {
            transport = new TSocket(host, port);
            transport.open();
            protocol = new TCompactProtocol(transport);
            client = new SocketDataService.Client(protocol);
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String response = client.addData("thrift socket ", new SocketData("time=" + now));
            return "thrift socket send " + host + ":" + port + "\nRESPONSE: " + response;
        } catch (Exception ex) {
            return "thrift socket error. " + ExceptionUtil.toString(ex);
        } finally {
            if(transport != null) {
                transport.close();
            }
        }
    }


    /**
     * 生成Exception的str
     */
    public static String format2String(Exception ex) throws NullPointerException {
        StringBuffer LogText = new StringBuffer();
        LogText.append(ex.toString()).append("\n");
        for (int i = 0; i < ex.getStackTrace().length; i++)
            LogText.append("        at ").append(ex.getStackTrace()[i])
                    .append("\n");
        return LogText.toString();
    }
}
