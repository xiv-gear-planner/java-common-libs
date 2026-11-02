package app.xivgear.logging;

import io.micronaut.context.annotation.Context;
import io.micronaut.http.HttpRequest;

@Context
public class IpAddressResolver {
	public String resolveIp(HttpRequest<?> request) {
		String cloudflareIp = request.getHeaders().get("CF-Connecting-IP");
		if (cloudflareIp != null && !cloudflareIp.isEmpty()) {
			return cloudflareIp;
		}
		return request.getRemoteAddress().getAddress().getHostAddress();
	}
}
