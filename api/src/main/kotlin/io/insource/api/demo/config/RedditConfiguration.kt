package io.insource.api.demo.config

import com.reddit.api.ApiClient
import com.reddit.api.RedditApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import javax.annotation.PostConstruct

@Configuration
@PropertySource("classpath:reddit.properties")
class RedditConfiguration(
  @Value("\${basePath}")
  private val basePath: String,
  private val apiClient: ApiClient
) {
  @PostConstruct
  fun init() {
    apiClient.basePath = basePath
    apiClient.defaultHeaders.add("User-Agent", "ios:io.insource.api-demo:v0.0.1 (by /u/sjohnr)")
  }
}