/*
 * This software is licensed under the Apache License, Version 2.0
 * (the "License") agreement; you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.force66.correlate;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Checks for correlation id in the header.  If it doesn't exist, 
 * establishes a new correlation id.
 * 
 * <p>Initialization parameters are as follows:</p>
 * <li>correlation.id.header - header name containing the correlation id.  
 * Default is requestCorrelationId.</li>
 * @author D. Ashmore
 *
 */
public class RequestCorrelationFilter implements Filter {
	
	public static final String DEFAULT_CORRELATION_ID_HEADER_NAME="requestCorrelationId";
	public static final String DEFAULT_LOGGER_MDC_NAME="requestId";
	
	public static final String INIT_PARM_CORRELATION_ID_HEADER="correlation.id.header";
	public static final String INIT_PARM_LOGGER_NDC_NAME="logger.mdc.name";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestCorrelationFilter.class);
	
	private String correlationHeaderName;
	private String loggerMdcName;

	public void init(FilterConfig filterConfig) throws ServletException {
		String headerName = filterConfig.getInitParameter(INIT_PARM_CORRELATION_ID_HEADER);
		if (StringUtils.isEmpty(headerName)) {
			correlationHeaderName=DEFAULT_CORRELATION_ID_HEADER_NAME;
		}
		else {
			correlationHeaderName=headerName.trim();
		}
		
		String mdcName = filterConfig.getInitParameter(INIT_PARM_LOGGER_NDC_NAME);
		if (StringUtils.isEmpty(mdcName)) {
			loggerMdcName=DEFAULT_LOGGER_MDC_NAME;
		}
		else {
			loggerMdcName=mdcName.trim();
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		String correlationId = httpServletRequest.getHeader(correlationHeaderName);
		if (StringUtils.isEmpty(correlationId)) {
			correlationId = UUID.randomUUID().toString();
		}
				
		RequestCorrelationContext.getCurrent().setCorrelationId(correlationId);
		MDC.put(loggerMdcName, correlationId);
		LOGGER.debug("Correlation id={} request={}", correlationId, httpServletRequest.getPathInfo());
		
		try {chain.doFilter(httpServletRequest, response);}
		finally {
			MDC.remove(loggerMdcName);
			RequestCorrelationContext.clearCurrent();
		}

	}

	public void destroy() {
		// NoOp

	}

	protected String getCorrelationHeaderName() {
		return correlationHeaderName;
	}

	protected String getLoggerMdcName() {
		return loggerMdcName;
	}

}
