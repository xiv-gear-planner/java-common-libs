package app.xivgear.logging

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Context
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton

import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.MemoryUsage

@Slf4j
@CompileStatic
@Singleton
@Context
class PeriodicMemStats {

	@Scheduled(fixedRate = "5m")
	void logMem() {
		MemoryMXBean memory = ManagementFactory.memoryMXBean
		MemoryUsage heap = memory.heapMemoryUsage
		MemoryUsage nonHeap = memory.nonHeapMemoryUsage
		log.info("Memory: heap [${format(heap)}]; other [${format(nonHeap)}]")
	}

	private static String format(MemoryUsage mu) {
		long usedMi = mu.used.intdiv(1024 * 1024)
		long commitMi = mu.committed.intdiv(1024 * 1024)
		long max = mu.max
		if (max == -1) {
			return "${usedMi}Mi / ${commitMi}Mi (max unknown)"
		}
		else {
			long maxMi = max.intdiv(1024 * 1024)
			int usedPct = mu.used / max * 100 as int
			int commitPct = mu.committed / max * 100 as int
			return "${usedMi}Mi (${usedPct}%) / ${commitMi}Mi (${commitPct}%) / ${maxMi}Mi"
		}
	}
}
