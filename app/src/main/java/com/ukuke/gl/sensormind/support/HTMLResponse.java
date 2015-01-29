package com.ukuke.gl.sensormind.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HTMLResponse {

	private int HTMLStatusCode;
	private String content;
	private HashMap<String, String> HTMLHeader;
	

	
	public HTMLResponse() {
		HTMLStatusCode = -1;
		content = "";
		HTMLHeader = new HashMap<String, String>();
	}
	public int getHTMLStatusCode() {
		return HTMLStatusCode;
	}
	public void setHTMLStatusCode(int hTMLStatusCode) {
		HTMLStatusCode = hTMLStatusCode;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public HashMap<String, String> getHTMLHeader() {
		return HTMLHeader;
	}
	public void setHTMLHeader(HashMap<String, String> a) {
		HTMLHeader = a;
	}
	public String getHTMLHeaderAsString() {
		String ret = "";
	
		 Iterator it = HTMLHeader.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pairs = (Map.Entry)it.next();
		        ret += pairs.getKey() + " : " + pairs.getValue() + "\r\n";
		        it.remove(); // avoids a ConcurrentModificationException
		    }
		    return ret;
	}
	@Override
	public String toString() {
		return "HTMLResponse [HTMLStatusCode=" + HTMLStatusCode + ", content="
				+ content + "]";
	}
	
	
}
