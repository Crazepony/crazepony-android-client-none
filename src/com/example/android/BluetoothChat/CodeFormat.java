package com.example.android.BluetoothChat;

import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.R.integer;

public class CodeFormat {
	
  static String dataOne ;
  /* 
	* 16进制数字字符集 
	*/ 
	private static String hexString="0123456789ABCDEF"; 
	/* 
	* 将字符串编码成16进制数字,适用于所有字符（包括中文） 
	*/ 
	public static String encode(String str) 
	{  
		dataOne = str;
	//根据默认编码获取字节数组 
	byte[] bytes=str.getBytes(); 
	StringBuilder sb=new StringBuilder(bytes.length*2); 
	//将字节数组中每个字节拆解成2位16进制整数 
	for(int i=0;i<bytes.length;i++) 
	{ 
	sb.append(hexString.charAt((bytes[i]&0xf0)>>4)); 
	sb.append(hexString.charAt((bytes[i]&0x0f)>>0)+" "); 
	} 
	 
	  return sb.toString(); 
	  
	} 
	/* 
	* 将16进制数字解码成字符串,适用于所有字符（包括中文） 
	*/ 
	public static String decode(String bytes)
	{
		
	ByteArrayOutputStream baos=new ByteArrayOutputStream(bytes.length()/2);
	//将每2位16进制整数组装成一个字节
	for(int i=0;i<bytes.length();i+=2)
	baos.write((hexString.indexOf(bytes.charAt(i))<<4 |hexString.indexOf(bytes.charAt(i+1))));
	return new String(baos.toByteArray());
	
	}
	
	 public   static   String StringFilter(String   str)   throws   PatternSyntaxException   {      
		               // 只允许字母和数字        
		               // String   regEx  =  "[^a-zA-Z0-9]";                      
		               // 清除掉所有特殊字符   
		          String regEx="[`~!@#$%^&*()+=|{}':;',//[//].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";   
		         Pattern   p   =   Pattern.compile(regEx);      
		          Matcher   m   =   p.matcher(str);      
		         return   m.replaceAll("").trim();      
    }
	 /** 
	　　* Convert byte[] to hex string.这里我们可以将byte转换成int，然后利用Integer.toHexString(int)来转换成16进制字符串。

	　　* @param src byte[] data

	　　* @return hex string

	　　*/

	public static String bytesToHexString(byte[] src){

	StringBuilder stringBuilder = new StringBuilder("");
	if (src == null || src.length <= 0) {

	 return null;

	}

	for (int i = 0; i < 20; i++) {
    
	int v = src[i] & 0xFF;

	String hv = Integer.toHexString(v);

	if (hv.length() < 2) {

	 stringBuilder.append(0);
     System.out.println(stringBuilder);
	}

	 stringBuilder.append(hv);

	}

	return stringBuilder.toString();

}

	/** *//** 
	    * 把字节数组转换成16进制字符串 
	    * @param bArray 
	    * @return 
	    */ 
	public static final String bytesToHexStringTwo(byte[] bArray,int count) { 
	    StringBuffer sb = new StringBuffer(bArray.length); 
	    String sTemp; 
	    for (int i = 0; i < count; i++) { 
	     sTemp = Integer.toHexString(0xFF & bArray[i]); 
	     if (sTemp.length() < 2) 
	      sb.append(0); 
	     sb.append(sTemp.toUpperCase()); 
	    } 
	    return sb.toString(); 
	}


	 
	 //分割字符串
      public static  String  Stringspace(String str){
    	 
			String temp=""; 
			String temp2="";
			for(int i=0;i<str.length();i++) 
			{  
				
				if (i%2==0) {
				  temp=str.charAt(i)+"";
				  temp2+=temp;
			      System.out.println(temp);
				}else {
					temp2+=str.charAt(i)+" ";
				}
				
			}  
    	 return temp2;
      }
      /**
  	 * Byte -> Hex
  	 * 
  	 * @param bytes
  	 * @return
  	 */
  	public static String byteToHex(byte[] bytes, int count) {
  		StringBuffer sb = new StringBuffer();
  		for (int i = 0; i < count; i++) {
  			String hex = Integer.toHexString(bytes[i] & 0xFF);
  			if (hex.length() == 1) {
  				hex = '0' + hex;
  			}
  			sb.append(hex).append(" ");
  		}
  		return sb.toString();
  	}
  	/**
	 * String -> Hex
	 * 
	 * @param s
	 * @return
	 */
	public static String stringToHex(String s) {
		String str = "";
		for (int i = 0; i < s.length(); i++) {
			int ch = (int) s.charAt(i);
			String s4 = Integer.toHexString(ch);
			if (s4.length() == 1) {
				s4 = '0' + s4;
			}
			str = str + s4 + " ";
		}
		return str;
	}
	
 }
    