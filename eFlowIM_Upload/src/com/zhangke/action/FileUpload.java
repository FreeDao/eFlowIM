package com.zhangke.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.struts2.ServletActionContext;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;


import com.baidu.yun.channel.auth.ChannelKeyPair;
import com.baidu.yun.channel.client.BaiduChannelClient;
import com.baidu.yun.channel.exception.ChannelClientException;
import com.baidu.yun.channel.exception.ChannelServerException;
import com.baidu.yun.channel.model.PushBroadcastMessageRequest;
import com.baidu.yun.channel.model.PushBroadcastMessageResponse;
import com.baidu.yun.core.log.YunLogEvent;
import com.baidu.yun.core.log.YunLogHandler;
import com.opensymphony.xwork2.ActionSupport;

import freemarker.template.utility.StringUtil;

/***
 * �ļ��ϴ�����   resource code encoding is utf-8
 * <br>��ҪΪ��android�ͻ���ʵ�ֹ���   ����д����   ���Ҽ���
 *
 */
public class FileUpload extends ActionSupport {

	private String savePath;
	/**��������ֺ�html�����ֱ���Գ�*/
	private File img;
	/**Ҫ�ϴ����ļ�����*/
	private String imgContentType;                                       
	/**�ļ�������*/
	private String imgFileName;
	
	private String orderId, tihao, teacher, answer;
	/**
	 * ָ�����ϴ�����   zip ��   ͼƬ��ʽ���ļ�
	 */
	private static final String[] types = { "application/octet-stream",
			"ZIP", "image/pjpeg","image/x-png" };  //"application/octet-stream; charset=utf-8",

	
	
	String url = "jdbc:sqlserver://121.199.3.19:1433;DatabaseName=openfire;useunicode=true;characterEncoding=UTF-8";
	String classforname = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	String uid = "admin";
	String pwd = "admin";
	
	
	
	/***
	 * �ж��ļ��������Ƿ�Ϊָ�����ļ�����
	 * @return
	 */
	public boolean filterType() {
		boolean isFileType = false;
		String fileType = getImgContentType();
//		System.out.println(fileType);
		for (String type : types) {
			if (type.equals(fileType)) {
				isFileType = true;
				break;
			}
		}
		return true;
	}

	public String getSavePath() {
		String realPath = ServletActionContext.getRequest().getRealPath(savePath);
//		System.out.println("savePaht -- " + realPath);
		if ( !new File(savePath).exists() ){
			new File(savePath).mkdir();
		}
		
		return savePath;
	}

	public File getImg() {
		return img;
	}

	public String getImgFileName() {
		return imgFileName;
	}

	public void setSavePath(String value) {
		this.savePath = value;
	}

	public void setImgFileName(String imgFileName) {
		this.imgFileName = imgFileName;
	}

	public void setImg(File img) {
		this.img = img;
	}

	public String getImgContentType() {
		return imgContentType;
	}

	public void setImgContentType(String imgContentType) {
		this.imgContentType = imgContentType;
	}

	/**
	 * ȡ���ļ��д�С
	 * 
	 * @param f
	 * @return
	 * @throws Exception
	 */
	public long getFileSize(File f) throws Exception {
		return f.length();
	}

	public String FormetFileSize(long fileS) {// ת���ļ���С
		DecimalFormat df = new DecimalFormat("#.00");
		String fileSizeString = "";
		if (fileS < 1024) {
			fileSizeString = df.format((double) fileS) + "B";
		} else if (fileS < 1048576) {
			fileSizeString = df.format((double) fileS / 1024) + "K";
		} else if (fileS < 1073741824) {
			fileSizeString = df.format((double) fileS / 1048576) + "M";
		} else {
			fileSizeString = df.format((double) fileS / 1073741824) + "G";
		}
		return fileSizeString;
	}

	/**
	 * �ϴ��ļ�����
	 * 
	 * @return
	 * @throws Exception
	 */
	public String upload() throws Exception {
		
		String ct  =  ServletActionContext.getRequest().getHeader("Content-Type");
//		System.out.println("Content-Type="+ct);
		String result = "unknow error";
//		System.out.println("orderId="+getOrderId());
		PrintWriter out = ServletActionContext.getResponse().getWriter();
		if (!filterType()) {
//			System.out.println("�ļ����Ͳ���ȷ");
			ServletActionContext.getRequest().setAttribute("typeError",	"��Ҫ�ϴ����ļ����Ͳ���ȷ");
			result = "error:" + getImgContentType() + " type not upload file type";
		} else {
//			System.out.println("��ǰ�ļ���СΪ��"	+ FormetFileSize(getFileSize(getImg())));
			FileOutputStream fos = null;
			FileInputStream fis = null;
			try {
				// �����ļ���һ��·��
				fos = new FileOutputStream(getSavePath() + "\\"	+ getImgFileName());
				fis = new FileInputStream(getImg());
				byte[] buffer = new byte[1024];
				int len = 0;
				while ((len = fis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				//result = "�ϴ��ɹ�!";
				result = "Upload File Success !";
			} catch (Exception e) {
				result = "Upload File Failed ! ";
				e.printStackTrace();
			} finally {
				fos.close();
				fis.close();
			}
		}
		out.print(result);
		return null;
	}
	
	/*
	 * ����¼��绰����ӿ�
	*/
	public void updatePersonalInfo() throws Exception{
		HttpServletRequest request = ServletActionContext.getRequest();
		request.setCharacterEncoding("UTF-8");
		
		HttpServletResponse response = ServletActionContext.getResponse();
		response.setCharacterEncoding("UTF-8");
		
		String myres = "";
		PrintWriter out = response.getWriter();
		String commString = request.getParameter("command");
		if ( "telnumber".equals(commString.split(",")[0]) ){
			//��绰����
			myres = selectRecord(commString.split(",")[1]);
			
		}else if ( "updatetel".equals(commString.split(",")[0]) ) {
			//�ĵ������
			myres = updateRecord(commString.split(",")[1], commString.split(",")[2]);
			
		}else if ( "password".equals(commString.split(",")[0]) ) {
			//�����룬�鲻�����½�һ��
			myres = passwordRecord(commString.split(",")[1]);
			
		}else if ( "changepw".equals(commString.split(",")[0]) ) {
			//������
			myres = changePWRecord(commString.split(",")[1], commString.split(",")[2]);
		}
		
		out.print(myres);
		out.flush();
	}
	
	
	public String changePWRecord(String username, String newPWord) {
		String rss= "";
		Connection conn = null;

		try {
			Class.forName(classforname);
			if (conn == null || conn.isClosed())
				conn = DriverManager.getConnection(url, uid, pwd);
			
			PreparedStatement c2 = conn.prepareStatement("UPDATE personInfo SET password=? WHERE name=?");
			c2.setString(1, newPWord);
			c2.setString(2, username);
			c2.execute();
			rss = "ok";
			
			return rss;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			rss = "no";
		}

		return rss;
	}
	
	
	
	public String passwordRecord(String username) {
		String rss= "";
		Connection conn = null;

		try {
			Class.forName(classforname);
			if (conn == null || conn.isClosed())
				conn = DriverManager.getConnection(url, uid, pwd);
			
			PreparedStatement c2 = conn.prepareStatement("SELECT * FROM personInfo WHERE name=?");
			c2.setString(1, username);
			ResultSet c2RS = c2.executeQuery();
			if ( c2RS.next() ){
				rss = c2RS.getString("password");
			}else {
				PreparedStatement psce = conn.prepareStatement("insert into personInfo(name,tel,password) values(?,?,?)");
				psce.setString(1, username);
				psce.setString(2, "");
				psce.setString(3, "123456");
				psce.executeUpdate();
				rss = "123456";
			}
			
			return rss;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		return "none";
	}
	
	
	public String selectRecord(String chen) {
		String rss= "";
		Connection conn = null;
		Statement stmt;
		ResultSet rs = null;

		try {
			Class.forName(classforname);
			if (conn == null || conn.isClosed())
				conn = DriverManager.getConnection(url, uid, pwd);
			stmt = conn.createStatement();
			
			if ( chen == null || "".equals(chen) ){
				rs = stmt.executeQuery("SELECT name, tel FROM personInfo");
			}else {
				rs = stmt.executeQuery("SELECT name, tel FROM personInfo WHERE name = '"+ chen + "'");
			}
			
			if (rs.next()) {
				rss = rs.getString("tel");
			}else {
				rss = "";
			}
			
			
			return rss;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		return "none";
	}
	
	public String updateRecord(String name, String tel) {
		String rss= "";
		Connection conn = null;

		try {
			Class.forName(classforname);
			if (conn == null || conn.isClosed())
				conn = DriverManager.getConnection(url, uid, pwd);
			
			
			PreparedStatement queryPI = conn.prepareStatement("SELECT * FROM personInfo WHERE name=?");
			queryPI.setString(1, name);
			ResultSet queryRS = queryPI.executeQuery();
			if ( queryRS.next() ){
				//���������˾�update
				PreparedStatement updateContent = conn.prepareStatement("UPDATE personInfo SET tel=? WHERE name=?");
				updateContent.setString(1, tel);
				updateContent.setString(2, name);
				updateContent.execute();
				rss = "updated";
			}else {
				PreparedStatement psce = conn.prepareStatement("insert into personInfo(name,tel) values(?,?)");
				psce.setString(1, name);
				psce.setString(2, tel);
				psce.executeUpdate();
				rss = "insert";
			}
			
			return rss;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		return "none";
	}
	
	public void doWeinXin() throws Exception {
		HttpServletRequest request = ServletActionContext.getRequest();
		request.setCharacterEncoding("UTF-8");

		HttpServletResponse response = ServletActionContext.getResponse();
		response.setCharacterEncoding("UTF-8");

		PrintWriter out = response.getWriter();
		 

		/*��δ�����ע�Ὺ���ߵ�ʱ����
		String signature = request.getParameter("signature");
		String timestamp = request.getParameter("timestamp");
		String nonce = request.getParameter("nonce");
		String echostr = request.getParameter("echostr");
		response.getWriter().write(echostr);*/
		
		
		try {
			InputStream is = request.getInputStream();
            // ȡHTTP����������
            int size = request.getContentLength();
            // ���ڻ���ÿ�ζ�ȡ������
            byte[] buffer = new byte[size];
            // ���ڴ�Ž��������
            byte[] xmldataByte = new byte[size];
            int count = 0;
            int rbyte = 0;
            // ѭ����ȡ
            while (count < size) { 
                // ÿ��ʵ�ʶ�ȡ���ȴ���rbyte��
                rbyte = is.read(buffer); 
                for(int i=0;i<rbyte;i++) {
                    xmldataByte[count + i] = buffer[i];
                }
                count += rbyte;
            }
            is.close();
            String requestStr = new String(xmldataByte, "UTF-8");
            Document doc = DocumentHelper.parseText(requestStr);
            Element rootElt = doc.getRootElement();
            
            //��content��Сд��ȥ�ո�
            String content = rootElt.elementText("Content").trim().toLowerCase();
            String toUserName = rootElt.elementText("ToUserName");
            String fromUserName = rootElt.elementText("FromUserName");
            
            
            //////////////���ݿ�
            String dbUrl = "jdbc:mysql://sqld.duapp.com:4050/CFkjviRxklYLmPNPoFhQ";
    		String username2 = request.getHeader("BAE_ENV_AK");
    		String password = request.getHeader("BAE_ENV_SK");
    		
    		Connection connection = null;
    		String resultString = "";
    		
    		Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection(dbUrl, username2,password);
			

			
			if ( content.equalsIgnoreCase("a") || 	content.equalsIgnoreCase("b") ||
					content.equalsIgnoreCase("c") ||  content.equalsIgnoreCase("d") ) {
				
				PreparedStatement queryCPath = connection.prepareStatement("SELECT dati FROM cPath WHERE username=?");
				queryCPath.setString(1, fromUserName);
				ResultSet queryRS = queryCPath.executeQuery();
				if ( queryRS.next() ){
					String currentTi = queryRS.getString("dati");
					
					PreparedStatement updateContent = connection.prepareStatement("UPDATE profile SET feedback=? WHERE username=? and ti=?");
					updateContent.setString(1, content);
					updateContent.setString(2, fromUserName);
					updateContent.setString(3, currentTi);
					updateContent.execute();
				}
				
			}else {
				//�������������
				//ͬʱ�ж�������Ƿ�Ϊ��Ч����� 
				PreparedStatement queryContent = connection.prepareStatement("SELECT * FROM `tiku` WHERE bianhao=?");
				queryContent.setString(1, content);
				ResultSet queryRS = queryContent.executeQuery();
				if (queryRS.next()) {
					
					PreparedStatement c2 = connection.prepareStatement("SELECT * FROM cPath WHERE username=?");
					c2.setString(1, fromUserName);
					ResultSet c2RS = c2.executeQuery();
					if ( !c2RS.next() ){
						PreparedStatement insertName = connection.prepareStatement("insert into cPath(username,dati) values(?,?)");
						insertName.setString(1, fromUserName);
						insertName.setString(2, content);
						insertName.executeUpdate();
					}else {
						PreparedStatement updateCPath = connection.prepareStatement("UPDATE cPath SET dati=? where username=?");
						updateCPath.setString(1, content);
						updateCPath.setString(2, fromUserName);
						updateCPath.execute();
					}
					
					
					
					//�����˷���Ϣ����ʱ ����ʱ�Ƿ�������profile��û�����½�
					PreparedStatement queryProfile = connection.prepareStatement("SELECT * FROM `profile` WHERE username=? and ti=?");
					queryProfile.setString(1, fromUserName);
					queryProfile.setString(2, content);
					ResultSet rsqp = queryProfile.executeQuery();
					if ( !rsqp.next() ){
						//���û�ҵ����Ϊn����Ŀ
						PreparedStatement insertName = connection.prepareStatement("insert into profile(username,ti,feedback) values(?,?,?)");
						insertName.setString(1, fromUserName);
						insertName.setString(2, content);
						insertName.setString(3, "");
						insertName.executeUpdate();
					}else {
					}
					
					
					resultString = queryRS.getString("content");
					String responseStr = "<xml>";
	                responseStr += "<ToUserName><![CDATA[" + fromUserName  + "]]></ToUserName>";
	                responseStr += "<FromUserName><![CDATA[" + toUserName   + "]]></FromUserName>";
	                responseStr += "<CreateTime>" + System.currentTimeMillis() + "</CreateTime>";
	                responseStr += "<MsgType><![CDATA[text]]></MsgType>";
	                responseStr += "<Content>" + resultString + "</Content>";
	                responseStr += "<FuncFlag>0</FuncFlag>";
	                responseStr += "</xml>";
	                response.getWriter().write(responseStr);
				}else {
					if ( content.startsWith("tj") || content.startsWith("ͳ��") ) {
						int an=0, bn=0, cn=0, dn=0;
						String ti = content.substring(2);
						PreparedStatement tiPS = connection.prepareStatement("SELECT * FROM profile WHERE ti=?");
						tiPS.setString(1, ti);
						ResultSet tiRS = tiPS.executeQuery();
						while ( tiRS.next() ) {
							if ( "a".equals(tiRS.getString("feedback")) ) {
								an++;
								
							}else if ( "b".equals(tiRS.getString("feedback")) ) {
								bn++;
								
							}else if ( "c".equals(tiRS.getString("feedback")) ) {
								cn++;
								
							}else if ( "d".equals(tiRS.getString("feedback")) ) {
								dn++;
								
							}
						}
						
						int total = an+bn+cn+dn;
						String pera = (int)((float)an / total * 100) + "%";
						String perb = (int)((float)bn / total * 100) + "%";
						String perc = (int)((float)cn / total * 100) + "%";
						String perd = (int)((float)dn / total * 100) + "%";
						
						PreparedStatement duiPS = connection.prepareStatement("SELECT * FROM tiku WHERE bianhao=?");
						duiPS.setString(1, ti);
						ResultSet duiRS = duiPS.executeQuery();
						String dui = "";
						if ( duiRS.next() ){
							dui = duiRS.getString("answer");
						}
						
						String fd = "��"+ti+"��  ��ȷ��: "+dui.toUpperCase() +"\n\r�ֲ�: "+ pera+", "+perb+", "+perc+", "+perd+
						"\n\r���յ�"+ total +"������";
//						PreparedStatement insertName = connection.prepareStatement("insert into testtable(test) values(?)");
//						insertName.setString(1, fd);
//						insertName.executeUpdate();
						
						
						String responseStr = "<xml>";
		                responseStr += "<ToUserName><![CDATA[" + fromUserName  + "]]></ToUserName>";
		                responseStr += "<FromUserName><![CDATA[" + toUserName   + "]]></FromUserName>";
		                responseStr += "<CreateTime>" + System.currentTimeMillis() + "</CreateTime>";
		                responseStr += "<MsgType><![CDATA[text]]></MsgType>";
		                responseStr += "<Content>" + fd + "</Content>";
		                responseStr += "<FuncFlag>0</FuncFlag>";
		                responseStr += "</xml>";
		                response.getWriter().write(responseStr);
		                
					}else if ( "?".equals(content) ) {
						String responseStr = "<xml>";
		                responseStr += "<ToUserName><![CDATA[" + fromUserName  + "]]></ToUserName>";
		                responseStr += "<FromUserName><![CDATA[" + toUserName   + "]]></FromUserName>";
		                responseStr += "<CreateTime>" + System.currentTimeMillis() + "</CreateTime>";
		                responseStr += "<MsgType><![CDATA[text]]></MsgType>";
		                responseStr += "<Content>������Ŀ���ѡ�����</Content>";
		                responseStr += "<FuncFlag>0</FuncFlag>";
		                responseStr += "</xml>";
		                response.getWriter().write(responseStr);
					}
				}
			}
			
			
			
			
			
			
			
            
//            //�ı���Ϣ
//            if ( content != null  && "text".equals(content)) {
//                String responseStr = "<xml>";
//                responseStr += "<ToUserName><![CDATA[" + fromUserName  + "]]></ToUserName>";
//                responseStr += "<FromUserName><![CDATA[" + toUserName   + "]]></FromUserName>";
//                responseStr += "<CreateTime>" + System.currentTimeMillis() + "</CreateTime>";
//                responseStr += "<MsgType><![CDATA[text]]></MsgType>";
//                responseStr += "<Content>" + resultString + "</Content>";
//                responseStr += "<FuncFlag>0</FuncFlag>";
//                responseStr += "</xml>";
//                response.getWriter().write(responseStr);
//            }
//            //ͼ����Ϣ
//            else if ( content != null && "news".equals(content)) {
//                String responseStr = "<xml>";
//                responseStr += "<ToUserName><![CDATA[" + fromUserName   + "]]></ToUserName>";
//                responseStr += "<FromUserName><![CDATA[" + toUserName   + "]]></FromUserName>";
//                responseStr += "<CreateTime>" + System.currentTimeMillis()   + "</CreateTime>";
//                responseStr += "<MsgType><![CDATA[news]]></MsgType>";
//                responseStr += "<Content><![CDATA[]]></Content>";
// 
//                responseStr += "<ArticleCount>2</ArticleCount>";
// 
//                responseStr += "<Articles>";
//                responseStr += "<item>";
//                responseStr += "<Title><![CDATA[ͼ����Ϣ������ɫʯͷ11]]></Title>";
//                responseStr += "<Discription><![CDATA[ͼ����Ϣ���ġ�����ɫʯͷ11]]></Discription>";
//                responseStr += "<PicUrl><![CDATA[http://redstones.sinaapp.com/res/images/redstones_wx_258.jpg]]></PicUrl>";
//                responseStr += "<Url><![CDATA[http://redstones.sinaapp.com/]]></Url>";
//                responseStr += "</item>";
// 
//                responseStr += "<item>";
//                responseStr += "<Title><![CDATA[ͼ����Ϣ������ɫʯͷ22]]></Title>";
//                responseStr += "<Discription><![CDATA[ͼ����Ϣ���ġ�����ɫʯͷ22]]></Discription>";
//                responseStr += "<PicUrl><![CDATA[]]></PicUrl>";
//                responseStr += "<Url><![CDATA[]]></Url>";
//                responseStr += "</item>";
// 
//                responseStr += "</Articles>";
//                responseStr += "<FuncFlag>1</FuncFlag>";
//                responseStr += "</xml>";
//                response.getWriter().write(responseStr);
//            }
//            //����ʶ��
//            else {
//                String responseStr = "<xml>";
//                responseStr += "<ToUserName><![CDATA[" + fromUserName  + "]]></ToUserName>";
//                responseStr += "<FromUserName><![CDATA[" + toUserName   + "]]></FromUserName>";
//                responseStr += "<CreateTime>" + System.currentTimeMillis()   + "</CreateTime>";
//                responseStr += "<MsgType><![CDATA[text]]></MsgType>";
//                responseStr += "<Content>����text����news������Ӧ���͵���Ϣ�������Ƽ����ע '��ɫʯͷ'����ȫ����Java��ɣ��������ͽ����뵽http://wzwahl36.net</Content>";
//                responseStr += "<FuncFlag>0</FuncFlag>";
//                responseStr += "</xml>";
//                response.getWriter().write(responseStr);
//            }
            
            
            
            
		} catch (Exception e) {
			// TODO: handle exception
		}

	}
	
	public String getOrderId() {
		return orderId;
	}
	
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	
	public String getTihao() {
		return tihao;
	}
	
	public void setTihao(String tihao) {
		this.tihao = tihao;
	}
	
	public String getTeacher() {
		return teacher;
	}
	
	public void setTeacher(String t) {
		this.teacher = t;
	}
	
	public String getAnswer() {
		return answer;
	}
	
	public void setAnswer(String a) {
		this.answer = a;
	}
	
	
	
	public void doWeinXinTest(){
		String url = "https://api.weixin.qq.com/cgi-bin/menu/create?access_token=qwertyuiop";
        /**
         * ���ò˵�
         * ��Ϊʲô��\"�㶮��,����java����
         */
        String responeJsonStr = "{"+
                "\"button\":["+
                    "{\"name\":\"�ҵ���Ϣ\","+
                    "\"type\":\"click\"," +
                    "\"key\":\"V01_S01\"" +
                    "},"+
                    "{\"name\":\"�����趨\","+
                    "\"type\":\"click\"," +
                    "\"key\":\"V02_S01\"" +
                    "},"+
                    "{\"name\":\"����\","+
                    "\"type\":\"click\"," +
                    "\"key\":\"V03_S01\"" +
                    "}"+
                "]"+
            "}";
         
                     
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(url);
        post.setRequestBody(responeJsonStr);
        post.getParams().setContentCharset("utf-8");
        //����http����
        String respStr = "";
        try {
            client.executeMethod(post);
            respStr = post.getResponseBodyAsString();
        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(responeJsonStr);
        System.out.println(respStr);
	}
	
	
	//��������������͵�
	public void formCau(){
		HttpServletRequest requestBC = ServletActionContext.getRequest();
		String myres = "";
		String message = requestBC.getParameter("message");
		
		HttpServletResponse responseBC = ServletActionContext.getResponse();
		responseBC.setCharacterEncoding("UTF-8");
		PrintWriter out = null;
		try {
			out = responseBC.getWriter();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// 1. ����developerƽ̨��ApiKey/SecretKey
		String apiKey = "guoydgcnaBmr3SS3FFEWl6cA";
		String secretKey = "BQ1iZF61IeMAnYBxSzsbOXc6QQQ3q2Dv";
		ChannelKeyPair pair = new ChannelKeyPair(apiKey, secretKey);
		
		// 2. ����BaiduChannelClient����ʵ��
		BaiduChannelClient channelClient = new BaiduChannelClient(pair);
		
		// 3. ��Ҫ�˽⽻��ϸ�ڣ���ע��YunLogHandler��
		channelClient.setChannelLogHandler(new YunLogHandler() {
			public void onHandle(YunLogEvent event) {
				System.out.println(event.getMessage());
			}
		});
		
		try {
			
			// 4. �������������
			PushBroadcastMessageRequest request = new PushBroadcastMessageRequest();
			request.setDeviceType(3);	// device_type => 1: web 2: pc 3:android 4:ios 5:wp	
			
			//"��"+message.split(",")[0]+ " ����"+ message.split(",")[1]
			request.setMessage(message);
			// ��Ҫ֪ͨ��
			//			request.setMessageType(1);
			//			request.setMessage("{\"title\":\"Notify_title_danbo\",\"description\":\"Notify_description_content\"}");
 			
			// 5. ����pushMessage�ӿ�
			PushBroadcastMessageResponse response = channelClient.pushBroadcastMessage(request);
				
			// 6. ��֤���ͳɹ�
			System.out.println("push amount : " + response.getSuccessAmount());
			out.print("ok");
		} catch (ChannelClientException e) {
			// ����ͻ��˴����쳣
			e.printStackTrace();
		} catch (ChannelServerException e) {
			// �������˴����쳣
			System.out.println(
					String.format("request_id: %d, error_code: %d, error_message: %s" , 
						e.getRequestId(), e.getErrorCode(), e.getErrorMsg()
						)
					);
		}
		
	
	}
}

