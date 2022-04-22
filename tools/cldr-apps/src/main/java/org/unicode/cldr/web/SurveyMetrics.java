package org.unicode.cldr.web;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metric;

/**
 * Metrics availble from the /metrics endpoint
 */
@ApplicationScoped
public class SurveyMetrics {
    @Inject
    @Metric(
        name = "exceptions",
        description = "Number of SurveyTool Exceptions that happened")
    Counter surveyExceptions;

    /**
     * Count an exception. Right now there is only one bucket.
     */
    public void countException(Throwable exception) {
        surveyExceptions.inc();
    }

    @Gauge(
        name = "users",
        description = "Number of active users",
        unit = MetricUnits.NONE)
    public int getUsers() {
        return CookieSession.getUserCount();
    }

    public SurveyMetrics() {

    }
}
