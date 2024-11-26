package com.service.runnersmap.config;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@RequiredArgsConstructor
@EnableScheduling
public class JobScheduler {
// 배치 작업의 실행주기 설정

  private final JobLauncher jobLauncher; // Job을 실행시키는 역할

  private final Job rankJob; // 랭킹계산 Job

//  @Scheduled(cron = "*/10 * * * * *") // 10초에 한번(개발용)
  @Scheduled(cron = "0 0 0 * * *") // 매일 자정
//  @Scheduled(cron = "0 0 * * * *") // 정각마다 수행
  public void runJob() {
    try {
      LocalDate now = LocalDate.now();
      // 현재 연월을 "yyyyMM"형식의 문자열로 변환
      String nowMonth = now.format(DateTimeFormatter.ofPattern("yyyyMM"));

      // Job 실행에 필요한 파라미터 설정
      JobParameters jobParameters = new JobParametersBuilder()
          .addLong("time", System.currentTimeMillis())
          .addString("searchMonth", nowMonth)
          .toJobParameters();

      // Job 실행
      jobLauncher.run(rankJob, jobParameters);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}