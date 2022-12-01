// Copyright © Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexerqueue.azure.config;

import org.opengroup.osdu.indexerqueue.azure.util.ExponentialRetryUtil;
import org.opengroup.osdu.indexerqueue.azure.util.RetryUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryConfig {

  @Value("${retry.elongationPoint}")
  private int elongationPoint;
  @Value("${retry.multiplier}")
  private int standardMultiplier;
  @Value("${retry.maxRetryDuration}")
  private int maxRetryDuration;

  @Bean
  public RetryTemplate retryTemplate() {
    RetryTemplate retryTemplate = new RetryTemplate();

    ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
    exponentialBackOffPolicy.setInitialInterval(1000L);
    exponentialBackOffPolicy.setMultiplier(2);
    retryTemplate.setBackOffPolicy(exponentialBackOffPolicy);

    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(6);
    retryTemplate.setRetryPolicy(retryPolicy);

    return retryTemplate;
  }

  @Bean
  public RetryUtil exponentialRetryUtil(){
    return ExponentialRetryUtil.builder()
      .elongationPoint(elongationPoint)
      .multiplier(standardMultiplier)
      .maxRetryDuration(maxRetryDuration)
      .build();
  }
}
