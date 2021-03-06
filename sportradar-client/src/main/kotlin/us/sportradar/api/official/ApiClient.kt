package us.sportradar.api.official

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import kotlin.reflect.KClass
import kotlin.reflect.full.cast
import kotlin.reflect.full.createInstance

@Component("us.sportradar.api.official.ApiClient")
class ApiClient(val restTemplate: RestTemplate = RestTemplate()) {
  val defaultHeaders = HttpHeaders()
  var basePath = "http://localhost:8080"
  var statusCode = HttpStatus.OK
  var responseHeaders = HttpHeaders()
  var headersEnhancer: (httpHeaders: MultiValueMap<String, String>) -> Unit = {}
  var queryParamsEnhancer: (queryParams: MultiValueMap<String, String>) -> Unit = {}

  fun headers() = HttpHeaders()
  fun params() = LinkedMultiValueMap<String, String>()

  fun <T : Any> invoke(
    path: String,
    httpMethod: HttpMethod,
    responseType: KClass<T>,
    queryParams: MultiValueMap<String, String> = params(),
    body: Any? = null,
    httpHeaders: MultiValueMap<String, String> = headers(),
    formParams: MultiValueMap<String, Any>? = null,
    acceptTypes: List<MediaType>? = null,
    contentType: MediaType? = null
  ): T {
    headersEnhancer(httpHeaders)
    queryParamsEnhancer(queryParams)

    val uri = UriComponentsBuilder.fromHttpUrl(basePath)
      .path(path)
      .queryParams(queryParams)
      .build()
      .toUri()

    val requestBuilder = RequestEntity.method(httpMethod, uri).also {
      if (acceptTypes != null) {
        it.accept(*acceptTypes.toTypedArray())
      }
      if (null != contentType) {
        it.contentType(contentType)
      }
    }

    addHeaders(defaultHeaders, requestBuilder)
    addHeaders(httpHeaders, requestBuilder)

    val requestBody = formParams ?: body ?: ""
    val requestEntity = requestBuilder.body(requestBody)
    val responseEntity = restTemplate.exchange(requestEntity, responseType.java)

    statusCode = responseEntity.statusCode
    responseHeaders = responseEntity.headers

    return if (responseType == Void::class) {
      responseType.cast(Void.TYPE)
    } else if (statusCode == HttpStatus.NO_CONTENT || responseEntity.body == null) {
      responseType.createInstance()
    } else if (statusCode.is2xxSuccessful) {
      responseEntity.body!!
    } else {
      throw HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "API returned $statusCode and it wasn't handled by the RestTemplate error handler")
    }
  }

  private fun addHeaders(httpHeaders: MultiValueMap<String, String>?, requestBuilder: RequestEntity.BodyBuilder) {
    if (httpHeaders != null) {
      for ((key, values) in httpHeaders) {
        for (value in values) {
          if (value != null) {
            requestBuilder.header(key, value)
          }
        }
      }
    }
  }
}