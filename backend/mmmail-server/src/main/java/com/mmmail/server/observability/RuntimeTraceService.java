package com.mmmail.server.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Supplier;

@Service
public class RuntimeTraceService {

    private final ObservationRegistry observationRegistry;

    public RuntimeTraceService(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    public TraceScope start(String name, Map<String, String> lowCardinalityTags) {
        Observation observation = Observation.createNotStarted(name, observationRegistry);
        lowCardinalityTags.forEach(observation::lowCardinalityKeyValue);
        observation.start();
        return new TraceScope(observation, observation.openScope());
    }

    public <T> T observe(String name, Map<String, String> lowCardinalityTags, Supplier<T> supplier) {
        TraceScope scope = start(name, lowCardinalityTags);
        try {
            return supplier.get();
        } catch (RuntimeException | Error ex) {
            scope.error(ex);
            throw ex;
        } finally {
            scope.close();
        }
    }

    public void observeVoid(String name, Map<String, String> lowCardinalityTags, Runnable runnable) {
        observe(name, lowCardinalityTags, () -> {
            runnable.run();
            return null;
        });
    }

    public static final class TraceScope implements AutoCloseable {

        private final Observation observation;
        private final Observation.Scope scope;

        private TraceScope(Observation observation, Observation.Scope scope) {
            this.observation = observation;
            this.scope = scope;
        }

        public void tag(String key, String value) {
            observation.lowCardinalityKeyValue(key, value);
        }

        public void error(Throwable error) {
            observation.error(error);
        }

        @Override
        public void close() {
            scope.close();
            observation.stop();
        }
    }
}
