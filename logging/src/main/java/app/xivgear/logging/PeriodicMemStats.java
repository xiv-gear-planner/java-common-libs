package app.xivgear.logging;

import io.micronaut.context.annotation.Context;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

@Singleton
@Context
public class PeriodicMemStats {
	private static final Logger LOG = LoggerFactory.getLogger(PeriodicMemStats.class);

	@Scheduled(fixedRate = "5m")
	public void logMem() {
		MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
		MemoryUsage heap = memory.getHeapMemoryUsage();
		MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();
		LOG.info("Memory: heap [{}]; other [{}]", format(heap), format(nonHeap));
	}

	private static String format(MemoryUsage usage) {
		long usedMi = usage.getUsed() / (1024 * 1024);
		long committedMi = usage.getCommitted() / (1024 * 1024);
		long max = usage.getMax();
		if (max == -1) {
			return usedMi + "Mi / " + committedMi + "Mi (max unknown)";
		}

		long maxMi = max / (1024 * 1024);
		int usedPct = (int) ((double) usage.getUsed() / max * 100);
		int committedPct = (int) ((double) usage.getCommitted() / max * 100);
		return usedMi + "Mi (" + usedPct + "%) / " + committedMi + "Mi (" + committedPct + "%) / " + maxMi + "Mi";
	}
}
