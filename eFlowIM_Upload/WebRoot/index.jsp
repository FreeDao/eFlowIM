<%@ page language="java" import="java.util.*" pageEncoding="utf-8"%>
<%@taglib prefix="s" uri="/struts-tags"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
	<head>
		<base href="<%=basePath%>">

		<title>My JSP 'index.jsp' starting page</title>
		<meta http-equiv="pragma" content="no-cache">
		<meta http-equiv="cache-control" content="no-cache">
		<meta http-equiv="expires" content="0">
		<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
		<meta http-equiv="description" content="This is my page">
		<!--
	<link rel="stylesheet" type="text/css" href="styles.css">
	-->
	</head>



<script language="javascript">
function ale()
{//这个基本没有什么说的，就是弹出一个提醒的对话框
    alert("我敢保证，你现在用的是演示一");
}
function firm()
{//利用对话框返回的值 （true 或者 false）
    if(confirm("你确信要转去 天轰穿的博客？"))
    {//如果是true ，那么就把页面转向thcjp.cnblogs.com
        location.href="http://thcjp.cnblogs.com";
    }
    else
    {//否则说明下了，赫赫
        alert("你按了取消，那就是返回false");
    }
}
function prom()
{
    var name=prompt("请输入您的名字","");//将输入的内容赋给变量 name ，
    //这里需要注意的是，prompt有两个参数，前面是提示的话，后面是当对话框出来后，在对话框里的默认值
    if(name)//如果返回的有内容
    {
        alert("欢迎您："+ name)
    }
}
</script>



	<body>
		<div style="color: red">
			<s:fielderror />
		</div>
<!--		<form action="p/file!upload" method="post"	enctype="multipart/form-data">-->
<!--			订单号码：<input type="text" name="orderId" />-->
<!--			<br>-->
<!--			选择文件：<input type="file" name="img" />-->
<!--			<br>-->
<!--			<input type="submit" value="上传" />-->
<!--		</form>-->
		


		
		<form action="p/formCau" method="get">
		           教师：<input type="text" name="teacher" /><br>
			题号：<input type="text" name="tihao" /><br><br>
			内容：<br>
			<textarea rows="10" cols="30" name="orderId" id='cn1'></textarea>  <br>
			答案：<input type="text" name="answer" /><br><br>
			
			<input type="submit" value="确定" />
		</form>
		
		
		
	</body>
</html>
