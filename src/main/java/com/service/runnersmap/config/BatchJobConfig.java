package com.service.runnersmap.config;

import ch.qos.logback.core.util.StringUtil;
import com.service.runnersmap.service.RankService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class BatchJobConfig {
// 스프링배치 Job 설정 담당

  private final String JOB_NAME = "rankJob";

  private final String STEP_NAME = "rankStep";

  private final RankService rankService;

  /**
   * Job 등록
   */
  @Bean
  public Job rankJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new JobBuilder(JOB_NAME, jobRepository)
        // 각 Job 실행마다 순차적으로 고유 id 부여
        .incrementer(new RunIdIncrementer())
        // Job에 포함될 step 설정
        .start(rankStep(jobRepository, transactionManager))
        .build();
  }

  /**
   * Step 등록
   */
  @Bean
  @JobScope // Job 실행 시점에 Bean이 생성되도록 설정
  public Step rankStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder(STEP_NAME, jobRepository)
        // 스텝에서 실행할 tasklet 설정
        .tasklet(testTasklet(), transactionManager)
        .build();
  }

  /**
   * Tasklet 수행
   */
  @Bean
  @StepScope // step 실행 시점에 Bean이 생성되도록 설정
  public Tasklet testTasklet() {
    return new Tasklet() {
      @Override
      public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
        throws Exception {

        log.info("[Runners-Batch] rankJob 시작");
        // Job 파라미터 가져오기
        JobParameters jobParameters = chunkContext.getStepContext().getStepExecution()
            .getJobParameters();
        String searchMonth = jobParameters.getString("searchMonth");
        LocalDate batchExecutedDate = LocalDate.now();

        if(!StringUtil.isNullOrEmpty(searchMonth)) {

          // searchMonth에서 연도, 월 추출하기
          int year = Integer.parseInt(searchMonth.substring(0, 4)); //ex. 202411 -> 2024
          int month = Integer.parseInt(searchMonth.substring(4, 6)); // ex. 202411 -> 11

          log.info("[Runners-Batch] {}년 {}월 기준 데이터 집계 ", year, month);
          // 랭킹 저장 로직 실행
          rankService.saveRankingByMonth(year, month, batchExecutedDate);

        } else {
          log.info("[Runners-Batch] searchMonth 파라미터 null ");
        }

        log.info("[Runners-Batch] rankJob 종료");
        return RepeatStatus.FINISHED; // 작업 완료상태 반환
      }
    };
  }
}