package com.loopers.batch.job.demo;

import com.loopers.batch.job.demo.step.DemoTasklet;
import com.loopers.batch.listener.JobListener;
import com.loopers.batch.listener.StepMonitorListener;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = DemoJobConfig.JOB_NAME)
@RequiredArgsConstructor
@Configuration
public class DemoJobConfig {
    public static final String JOB_NAME = "demoJob";
    private static final String STEP_DEMO_SIMPLE_TASK_NAME = "demoSimpleTask";

    private final JobRepository jobRepository;
    private final JobListener jobListener;
    private final StepMonitorListener stepMonitorListener;
    private final DemoTasklet demoTasklet;

    @Bean(JOB_NAME)
    public Job demoJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(categorySyncStep())
                .listener(jobListener)
                .build();
    }

    @JobScope
    @Bean(STEP_DEMO_SIMPLE_TASK_NAME)
    public Step categorySyncStep() {
        return new StepBuilder(STEP_DEMO_SIMPLE_TASK_NAME, jobRepository)
                .tasklet(demoTasklet, new ResourcelessTransactionManager())
                .listener(stepMonitorListener)
                .build();
    }
}
