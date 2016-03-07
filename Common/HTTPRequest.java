package Common;

public class HTTPRequest {
	private String method;
	private String url;
	private String Server;
	private String headerUserAgent;
	private String headerIfModifiedSince;
	private String headerContentType;
	private String headerContentLength;
	private String query;
	public String getMsgBody() {
		return msgBody;
	}
	public void setMsgBody(String msgBody) {
		this.msgBody = msgBody;
	}
	private String msgBody;
	
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getServer() {
		return Server;
	}
	public void setServer(String server) {
		Server = server;
	}
	public String getHeaderUserAgent() {
		return headerUserAgent;
	}
	public void setHeaderUserAgent(String headerUserAgent) {
		this.headerUserAgent = headerUserAgent;
	}
	public String getHeaderIfModifiedSince() {
		return headerIfModifiedSince;
	}
	public void setHeaderIfModifiedSince(String headerIfModifiedSince) {
		this.headerIfModifiedSince = headerIfModifiedSince;
	}
	public String getHeaderContentType() {
		return headerContentType;
	}
	public void setHeaderContentType(String headerContentType) {
		this.headerContentType = headerContentType;
	}
	public String getHeaderContentLength() {
		return headerContentLength;
	}
	public void setHeaderContentLength(String headerContentLength) {
		this.headerContentLength = headerContentLength;
	}
}
