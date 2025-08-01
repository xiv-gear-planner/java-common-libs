package app.xivgear.logging

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Context
import io.micronaut.context.propagation.slf4j.MdcPropagationContext
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.core.order.Ordered
import io.micronaut.core.propagation.PropagatedContext
import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import jakarta.inject.Inject
import org.reactivestreams.Publisher
import org.slf4j.MDC

@CompileStatic
@Filter("/**")
@Slf4j
@Context
class RequestLoggingFilter implements Ordered, HttpServerFilter {

	@Inject
	IpAddressResolver ipAddressResolver

	@Override
	Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		// Unfortunately I'm not sure how to do this without reactive
		String ipAddress = ipAddressResolver.resolveIp request
		MDC.put "ip", ipAddress
		long start = System.currentTimeMillis()
		//noinspection GroovyUnusedAssignment
		try (PropagatedContext.Scope _ = (PropagatedContext.get() + new MdcPropagationContext()).propagate()) {
			Publisher<MutableHttpResponse<?>> responsePublisher = chain.proceed request
			return Publishers.<MutableHttpResponse<?>, MutableHttpResponse<?>> map(responsePublisher, { response ->

				HttpMethod method = request.method
				String path = request.path
				int code = response.status.code
				long end = System.currentTimeMillis()
				long delta = end - start

				// Ignore health/ready if successful
				if ((path == "/readyz" || path == "/healthz")
						&& code == 200) {
					log.trace "{} {}: {} ({}ms)",
							method,
							path,
							code,
							delta
				}
				else {
					log.info "{} {}: {} ({}ms)",
							method,
							path,
							code,
							delta
				}
				return response
			})
		}
		finally {
			MDC.remove "ip"
		}

	}

	final int order = -20
}