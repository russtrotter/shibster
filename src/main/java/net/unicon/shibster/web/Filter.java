package net.unicon.shibster.web;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Filter implements javax.servlet.Filter {
	private ServletContext context;
	private final Map<String, String> headers = new HashMap<String, String>();
	private String remoteUser;
	private boolean active = true;

	public void init(FilterConfig filterConfig) throws ServletException {
		this.context = filterConfig.getServletContext();
		active = determineActive(filterConfig.getInitParameter("contexts"));
		log((active ? "enabled" : "DISABLED") + " for "
				+ filterConfig.getServletContext().getContextPath());
		if (active) {
			configureHeaders(filterConfig.getInitParameter("headers"));
			if (headers.size() == 0) {
				log("warning, no headers specified in filter config");
			}
			remoteUser = headers.get("REMOTE_USER");
		}
	}

	private boolean determineActive(String str) {
		if (str == null) {
			return false;
		}
		String[] parts = str.split(",");
		for (String p : parts) {
			if (context.getContextPath().equals(p)) {
				return true;
			}
		}
		return false;
	}

	private void configureHeaders(String str) throws ServletException {
		if (str == null) {
			return;
		}
		Properties props = new Properties();
		try {
			props.load(new StringReader(str));
		} catch (IOException ioe) {
			throw new ServletException(ioe.getMessage());
		}
		for (Map.Entry<Object, Object> e : props.entrySet()) {
			headers.put(e.getKey().toString(), e.getValue().toString());
		}
		log("loaded " + headers.size() + " header(s) from filter config");
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		filterHttp((HttpServletRequest) request,
				(HttpServletResponse) response, chain);
	}

	private void filterHttp(HttpServletRequest request,
			HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (active) {
			MutableHttpServletRequest mreq = new MutableHttpServletRequest(
					request);
			mreq.setRemoteUser(remoteUser);
			for (Map.Entry<String, String> e : headers.entrySet()) {
				mreq.addHeader(e.getKey(), e.getValue());
			}
			mreq.addHeader("Shib-Session-ID", "SHIBSTER_IN_EFFECT");
			request = mreq;
		}
		chain.doFilter(request, response);

	}

	public void destroy() {

	}

	private void log(String msg) {
		context.log("SHIBSTER: " + msg);
	}
}
