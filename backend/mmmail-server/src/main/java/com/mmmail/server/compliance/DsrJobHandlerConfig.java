package com.mmmail.server.compliance;

import com.mmmail.platform.jobs.TypedJobRunHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DsrJobHandlerConfig {

    @Bean
    public TypedJobRunHandler dsrExportJobHandler(DsrExecutionService service) {
        return service.exportHandler();
    }

    @Bean
    public TypedJobRunHandler dsrErasureJobHandler(DsrExecutionService service) {
        return service.erasureHandler();
    }
}
