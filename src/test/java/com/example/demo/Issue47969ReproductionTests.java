package com.example.demo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"management.metrics.data.repository.autotime.enabled=true",
		"management.observations.annotations.enabled=true"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Issue47969ReproductionTests {

	@Autowired
	private Issue47969NoteRepository noteRepository;

	@Autowired
	private Issue47969ObservedService observedService;

	@Autowired
	private MeterRegistry meterRegistry;

	@Autowired
	private ObservationRegistry observationRegistry;

	private final RecordingObservationHandler observationHandler = new RecordingObservationHandler();

	@BeforeAll
	void registerObservationHandler() {
		this.observationRegistry.observationConfig().observationHandler(this.observationHandler);
	}

	@BeforeEach
	void setUp() {
		this.noteRepository.deleteAll();
		clearRepositoryInvocationMeters();
		this.observationHandler.clear();
	}

	@Test
	void observedOnServiceCreatesObservation() {
		this.observedService.invoke();

		assertThat(this.observationHandler.observationNames()).contains("issue.47969.service");
	}

	@Test
	void observedOnRepositoryCreatesObservationAndMetricIsRecorded() {
		assertThat(this.meterRegistry.find("spring.data.repository.invocations").timers()).isEmpty();

		this.noteRepository.findAll();

		assertThat(this.meterRegistry.find("spring.data.repository.invocations").timers()).isNotEmpty();
		assertThat(this.observationHandler.observationNames()).contains("issue.47969.repository");
	}

	@Test
	void observedOnRepositoryDurationIsInSimilarRangeToRepositoryMetricDuration() {
		int iterations = 300;
		for (int i = 0; i < iterations; i++) {
			this.noteRepository.findAll();
		}

		long metricDurationNanos = this.meterRegistry.find("spring.data.repository.invocations")
			.timers()
			.stream()
			.filter((timer) -> "findAll".equals(timer.getId().getTag("method")))
			.mapToLong((timer) -> (long) timer.totalTime(TimeUnit.NANOSECONDS))
			.sum();
		long observedDurationNanos = this.observationHandler.totalDurationNanos("issue.47969.repository");
		System.out.println("metricDurationNanos=" + metricDurationNanos + ", observedDurationNanos=" + observedDurationNanos);

		assertThat(this.observationHandler.observationNames()).contains("issue.47969.repository");
		double ratio = (double) metricDurationNanos / (double) observedDurationNanos;
		assertThat(ratio).isBetween(0.5d, 5d);
	}

	private void clearRepositoryInvocationMeters() {
		List<Meter> repositoryMeters = List.copyOf(this.meterRegistry.find("spring.data.repository.invocations").meters());
		repositoryMeters.forEach(this.meterRegistry::remove);
	}

	private static class RecordingObservationHandler implements ObservationHandler<Observation.Context> {

		private final List<String> observationNames = new CopyOnWriteArrayList<>();

		private final Map<Observation.Context, Long> startTimesNanos = new ConcurrentHashMap<>();

		private final Map<String, Long> totalDurationByNameNanos = new ConcurrentHashMap<>();

		@Override
		public void onStart(Observation.Context context) {
			this.observationNames.add(context.getName());
			this.startTimesNanos.put(context, System.nanoTime());
		}

		@Override
		public void onStop(Observation.Context context) {
			Long startTimeNanos = this.startTimesNanos.remove(context);
			if (startTimeNanos == null) {
				return;
			}
			long durationNanos = System.nanoTime() - startTimeNanos;
			this.totalDurationByNameNanos.merge(context.getName(), durationNanos, Long::sum);
		}

		@Override
		public boolean supportsContext(Observation.Context context) {
			return true;
		}

		void clear() {
			this.observationNames.clear();
			this.startTimesNanos.clear();
			this.totalDurationByNameNanos.clear();
		}

		List<String> observationNames() {
			return this.observationNames;
		}

		long totalDurationNanos(String name) {
			return this.totalDurationByNameNanos.getOrDefault(name, 0L);
		}

	}

}
