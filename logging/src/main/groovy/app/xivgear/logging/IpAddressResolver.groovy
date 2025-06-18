package app.xivgear.logging

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Context
import io.micronaut.http.HttpRequest
import jakarta.inject.Singleton

@CompileStatic
@Slf4j
@Context
class IpAddressResolver {
	public IpAddressResolver() {
		log.info("Foo")
	}

	String resolveIp(HttpRequest<?> request) {
		return request.headers
				.get("CF-Connecting-IP")
				?: request.remoteAddress.address.hostAddress.toString()
	}

}