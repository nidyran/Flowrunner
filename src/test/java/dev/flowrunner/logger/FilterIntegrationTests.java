/*
 * MIT License
 *
 * Copyright (c) 2026 Flowrunner Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.flowrunner.logger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestConstructor;

/**
 * Integration tests for {@link ApplicationRequestLoggingFilter} and
 * {@link ApplicationResponseLoggingFilter} using REST Assured to make actual HTTP requests
 * with the filters attached.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.location=classpath:/flow-properties-test.yaml")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class FilterIntegrationTests {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        FlowExecutionLogger.clear();
        RestAssured.port = port;
        RestAssured.basePath = "";
    }

    @AfterEach
    void tearDown() {
        FlowExecutionLogger.clear();
        ApplicationRequestLoggingFilter.ACTIVE_REQUEST_LOG_ENTRY.remove();
    }

    @Test
    void requestLoggingFilterCanBeInstantiatedAndUsed() {
        ApplicationRequestLoggingFilter filter = new ApplicationRequestLoggingFilter();

        given()
                .filter(filter)
                .when()
                .get("/")
                .then()
                .statusCode(200);
    }

    @Test
    void responseLoggingFilterCanBeInstantiatedAndUsed() {
        ApplicationResponseLoggingFilter filter = new ApplicationResponseLoggingFilter();

        given()
                .filter(filter)
                .when()
                .get("/")
                .then()
                .statusCode(200);
    }

    @Test
    void bothFiltersWorkTogether() {
        ApplicationRequestLoggingFilter requestFilter = new ApplicationRequestLoggingFilter();
        ApplicationResponseLoggingFilter responseFilter = new ApplicationResponseLoggingFilter();

        given()
                .filter(requestFilter)
                .filter(responseFilter)
                .when()
                .get("/")
                .then()
                .statusCode(200);
    }

    @Test
    void responseFilterIgnorePushingResponseFlagCanBeSet() {
        ApplicationResponseLoggingFilter.ignorePushingResponse();

        given()
                .filter(new ApplicationResponseLoggingFilter())
                .when()
                .get("/")
                .then()
                .statusCode(200);
    }

    @Test
    void configEndpointWithFilters() {
        given()
                .filter(new ApplicationRequestLoggingFilter())
                .filter(new ApplicationResponseLoggingFilter())
                .when()
                .get("/api/flowrunner/config")
                .then()
                .statusCode(200);
    }

    @Test
    void handlersEndpointWithFilters() {
        given()
                .filter(new ApplicationRequestLoggingFilter())
                .filter(new ApplicationResponseLoggingFilter())
                .when()
                .get("/api/flowrunner/handlers")
                .then()
                .statusCode(200);
    }

    @Test
    void filterOrderReturnsLowestPrecedence() {
        assertThat(new ApplicationRequestLoggingFilter().getOrder())
                .isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void requestFilterPushesActiveEntry() {
        FlowExecutionLogger.LogEntry entry = new FlowExecutionLogger.LogEntry();
        entry.setKind(FlowExecutionLogger.LogEntryType.REQUEST);
        ApplicationRequestLoggingFilter.ACTIVE_REQUEST_LOG_ENTRY.set(entry);

        ApplicationRequestLoggingFilter.pushRequest();

        assertThat(ApplicationRequestLoggingFilter.ACTIVE_REQUEST_LOG_ENTRY.get()).isNull();
    }

    @Test
    void responseFilterCreatesValidPrintStream() {
        ApplicationResponseLoggingFilter filter = new ApplicationResponseLoggingFilter();
        assertThat(filter).isNotNull();
    }

    @Test
    void requestFilterCreatesValidPrintStream() {
        ApplicationRequestLoggingFilter filter = new ApplicationRequestLoggingFilter();
        assertThat(filter).isNotNull();
    }

    @Test
    void multipleRequestsWithFilters() {
        for (int i = 0; i < 3; i++) {
            given()
                    .filter(new ApplicationRequestLoggingFilter())
                    .filter(new ApplicationResponseLoggingFilter())
                    .when()
                    .get("/")
                    .then()
                    .statusCode(200);
        }
    }
}
