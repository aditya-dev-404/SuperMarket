package com.aditya.supermarket.notification.service;

import com.aditya.supermarket.notification.config.NotificationProperties;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Renders an email template twice: an HTML version (templates/email/html/NAME.html) and a
 * plain-text version (templates/email/text/NAME.txt). Variables are HTML-escaped by Thymeleaf
 * in the HTML version, so user-supplied text (names, product titles) cannot inject markup.
 *
 * <p>The engines are private to this class on purpose: exposing them as beans would clash with
 * the web layer's own Thymeleaf auto-configuration.
 */
@Component
public class EmailTemplateRenderer {

    private final TemplateEngine htmlEngine = engine(TemplateMode.HTML, "templates/email/html/", ".html");
    private final TemplateEngine textEngine = engine(TemplateMode.TEXT, "templates/email/text/", ".txt");
    private final NotificationProperties properties;

    public EmailTemplateRenderer(NotificationProperties properties) {
        this.properties = properties;
    }

    public RenderedEmail render(String templateName, Map<String, Object> model) {
        Context context = new Context(Locale.ENGLISH, variables(model));
        return new RenderedEmail(
            htmlEngine.process(templateName, context),
            textEngine.process(templateName, context).strip());
    }

    private Map<String, Object> variables(Map<String, Object> model) {
        Map<String, Object> variables = new HashMap<>(model);
        variables.putIfAbsent("appName", properties.fromName());
        variables.putIfAbsent("homeUrl", properties.link("/"));
        return variables;
    }

    private static TemplateEngine engine(TemplateMode mode, String prefix, String suffix) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode(mode);
        resolver.setPrefix(prefix);
        resolver.setSuffix(suffix);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCheckExistence(true);
        resolver.setCacheable(true);
        // SpringTemplateEngine evaluates ${...} with Spring EL, so the optional OGNL library is not needed.
        TemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
