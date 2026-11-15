package app.salescopilot.shared;

import org.apache.catalina.core.StandardHost;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TomcatErrorConfiguration {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> problemErrorReports() {
        return factory -> factory.addContextCustomizers(context -> {
            if (context.getParent() instanceof StandardHost host) {
                host.setErrorReportValveClass(ProblemErrorReportValve.class.getName());
            }
        });
    }
}
