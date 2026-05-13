package com.interview.application.service;

import com.interview.domain.model.PromptTemplate;
import com.interview.domain.repository.PromptTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PromptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateService.class);
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");
    private static final Pattern SECTION_PATTERN = Pattern.compile("\\{\\{#(\\w+)}}(.*?)\\{\\{/\\1}}", Pattern.DOTALL);

    private final PromptTemplateRepository repository;

    public PromptTemplateService(PromptTemplateRepository repository) {
        this.repository = repository;
    }

    public String resolveTemplate(String templateKey, Map<String, Object> variables) {
        PromptTemplate template = repository.findActiveByKey(templateKey).orElse(null);
        if (template == null) {
            log.warn("No active template found for key={}, returning empty", templateKey);
            return "";
        }
        return render(template.userTemplate(), variables);
    }

    String render(String templateText, Map<String, Object> variables) {
        if (templateText == null || templateText.isEmpty()) {
            return "";
        }
        Map<String, Object> vars = variables != null ? variables : Map.of();

        // Expand sections: {{#list}}...{{/list}} repeats for each item in a list
        String expanded = expandSections(templateText, vars);

        // Replace simple {{var}} placeholders
        Matcher matcher = VAR_PATTERN.matcher(expanded);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = vars.get(varName);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String expandSections(String template, Map<String, Object> vars) {
        Matcher matcher = SECTION_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String innerTemplate = matcher.group(2);
            Object value = vars.get(varName);

            String replacement;
            if (value instanceof List<?> list && !list.isEmpty()) {
                StringBuilder expanded = new StringBuilder();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> itemMap) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemVars = (Map<String, Object>) itemMap;
                        expanded.append(render(innerTemplate, itemVars));
                    } else {
                        expanded.append(render(innerTemplate, Map.of("value", item != null ? item.toString() : "")));
                    }
                }
                replacement = expanded.toString();
            } else if (value instanceof Boolean b && b) {
                replacement = render(innerTemplate, vars);
            } else if (value == null || (value instanceof Boolean)) {
                replacement = "";
            } else if (value instanceof List<?> list && list.isEmpty()) {
                replacement = "";
            } else {
                replacement = render(innerTemplate, vars);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public void activateVersion(String templateKey, int version) {
        // Deactivate current active
        PromptTemplate current = repository.findActiveByKey(templateKey).orElse(null);
        if (current != null) {
            repository.updateStatusByKeyAndVersion(templateKey, current.version(), "ARCHIVED");
        }
        repository.updateStatusByKeyAndVersion(templateKey, version, "ACTIVE");
    }
}
