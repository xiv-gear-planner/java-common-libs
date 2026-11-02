package app.xivgear.logging;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.propagation.slf4j.MdcPropagationContext;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.function.Function;

@Filter("/**")
@Context
public class ReqLog implements HttpServerFilter {
	private static final Logger LOG = LoggerFactory.getLogger(ReqLog.class);

	private final IpAddressResolver ipAddressResolver;

	@Inject
	public ReqLog(IpAddressResolver ipAddressResolver) {
		this.ipAddressResolver = ipAddressResolver;
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		String ipAddress = ipAddressResolver.resolveIp(request);
		MDC.put("ip", ipAddress);
		long start = System.currentTimeMillis();

		try (PropagatedContext.Scope ignored = (PropagatedContext.get()
				.plus(new MdcPropagationContext())).propagate()) {
			Publisher<MutableHttpResponse<?>> responsePublisher = chain.proceed(request);
			Function<MutableHttpResponse<?>, MutableHttpResponse<?>> logger = response -> {
				String responseIpAddress = ipAddressResolver.resolveIp(request);
				MDC.put("ip", responseIpAddress);

				HttpMethod method = request.getMethod();
				String path = request.getPath();
				String rawQuery = request.getUri().getRawQuery();
				String fullPath = request.getUri().getRawPath()
						+ (rawQuery == null || rawQuery.isEmpty() ? "" : "?" + rawQuery);
				int code = response.getStatus().getCode();
				long delta = System.currentTimeMillis() - start;

				if (("/readyz".equals(path) || "/healthz".equals(path)) && code == 200) {
					LOG.trace("{} {} {} ({}ms)", method, fullPath, code, delta);
				}
				else {
					LOG.info("{} {} {} ({}ms)", method, fullPath, code, delta);
				}
				return response;
			};
			return Publishers.map(responsePublisher, logger);
		}
		finally {
			MDC.remove("ip");
		}
	}

	@Override
	public int getOrder() {
		return -20;
	}
}
