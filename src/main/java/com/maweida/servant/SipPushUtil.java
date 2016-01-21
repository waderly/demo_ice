package com.maweida.servant;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @ClassName: SipPushUtil
 * @Description: sip推送消息
 * @author jerome_s@qq.com
 * @date 2015年11月11日 下午7:27:59
 * @see 参考波特推送文档
 */
public class SipPushUtil {
	
	private static final Logger LOG  = LoggerFactory.getLogger(SipPushUtil.class);
	public static void main(String[] args){
//		System.out.println(push("0", "990860100000000003", "{'msg_type':'MSG_UNLOCK_PHONE_REG'}"));
		System.out.println(push("9999999", "9188", "{'description':{'value':'','strategyUrl':'/get_card_number','insertUrl':'','time':1452742216.0974929,'Action':'card_number','password':'','MD5':''},'title':'send'}"));
	}

	/**
	 * 推送
	 * @param fromNumber
	 *            推送发起号码(不重要)
	 * @param toNumber
	 *            推送接收号码
	 * @param content
	 *            推送内容
	 *            1. {'msg_type':'MSG_UNLOCK_PHONE_REG'} 一件开门
	 * @return 0：成功 1：失败
	 */
	public static int push(String fromNumber, String toNumber, String content){
		
		String pushId = UUID.randomUUID().toString().substring(0, 4) + new Date().getTime(); // 推送标识,这里使用随机4位数+时间戳
		StringBuffer sbPushData = new StringBuffer();
		sbPushData.append("PUSHMESSAGE\r\nCLGNUMBER:").append(fromNumber);
		sbPushData.append("\r\nCLDNUMBER:").append(toNumber);
		sbPushData.append("\r\nCONTENT:").append(content);
		sbPushData.append("\r\nPUSHID:").append(pushId);

		return send(sbPushData.toString());
	}

	/**
	 * 发送数据
	 * 
	 * @param pushData
	 *            发送的数据
	 * @return 0:表示服务器处理成功; -1:表示服务器处理失败 ; -2:波特服务器异常没响应
	 */
	public static int send(String pushData) {
		int result = -1; // 结果默认失败
		OutputStream out = null; // 发送流
		InputStream in = null;  // 接收流
		Socket clientSocket = null; // socket
		
		try {
			String pushDataAscii = toASCII(pushData); // 将字符编码转换成US-ASCII码
			LOG.info("发送的数据为： \r\n" + pushDataAscii);
			//System.out.println("发送的数据为：\r\n" + pushDataAscii);

			// 设置包头
			byte[] byteLength = hexInt(pushDataAscii.length()); // 将数据长度转换成byte数组
			byte head[] = { (byte) 0xFF, (byte) 0xFE, (byte) 0xFF, (byte) 0xFE, (byte) 0xFF, (byte) 0xFE,
					(byte) 0xFF, (byte) 0xFE, byteLength[3], byteLength[2], byteLength[1], byteLength[0] };
			
			// 包体
			byte body[] = pushDataAscii.getBytes();
			byte[] pushDataByte = new byte[head.length + body.length]; // 声明固定长度要推送的字节数组
			// 将两个字节数组合并到pushDataByte
			System.arraycopy(head, 0, pushDataByte, 0, head.length);
			System.arraycopy(body, 0, pushDataByte, head.length, body.length);
			//LOG.info("发送数据的长度：" + pushDataByte.length + "\r\n" );
			//System.out.println("发送数据的长度：" + pushDataByte.length + "\r\n" );
			
			// 向服务器端发送信息
			clientSocket = new Socket("siponets.lz-qs.com",1920); 
			out = clientSocket.getOutputStream(); 
			out.write(pushDataByte);
			
			// 接收服务器端响应(非阻塞)
			byte[] buf = new byte[2048];  
			in = clientSocket.getInputStream();  
			int i = in.read(buf);  
			clientSocket.setSoTimeout(5000); // 超时时间五秒
			String response = new String(buf,12,i);
			//LOG.info("实际接收到的数据为：" + response);
			//System.out.println("接收到的数据为：" + response);  
	
			// 解析结果
			if (response.contains("CODE")) {
				result = Integer.parseInt(response.substring(
						response.indexOf("CODE") + 5, response.indexOf("CODE") + 6));
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketTimeoutException e) { // 很可能是波特服务器有问题没响应
			result = -2;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (clientSocket != null) {
					clientSocket.close();
				}
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		LOG.info("波特服务器接收到消息的返回码为：" + result);
		LOG.info("---------------------end----------------------\r\n");
		return result;

	}

	/**
	 * 将int转换成byte数组
	 * 
	 * @param hexint
	 * @return
	 */
	public static byte[] hexInt(int hexint) {
		byte[] a = new byte[4];
		a[0] = (byte) (0xff & hexint);
		a[1] = (byte) ((0xff00 & hexint) >> 8);
		a[2] = (byte) ((0xff0000 & hexint) >> 16);
		a[3] = (byte) ((0xff000000 & hexint) >> 24);
		return a;

	}

	/**
	 * 将字符编码转换成US-ASCII码
	 */
	public static String toASCII(String str) throws UnsupportedEncodingException {
		if (str != null) {
			// 用默认字符编码解码字符串。
			byte[] bs = str.getBytes();
			// 用新的字符编码生成字符串
			return new String(bs, "US-ASCII");
		}
		return null;
	}
}
