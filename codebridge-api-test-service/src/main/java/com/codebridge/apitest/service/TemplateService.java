package com.codebridge.apitest.service;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache;
import com.github.jknack.handlebars.cache.TemplateCache;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.helper.StringHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for template-based request generation.
 * Uses Handlebars.java for template processing.
 */
@Service
public class TemplateService {
    
    private static final Logger logger = LoggerFactory.getLogger(TemplateService.class);
    
    private final Handlebars handlebars;
    private final Map<String, Template> templateCache = new ConcurrentHashMap<>();
    
    /**
     * Constructor for TemplateService.
     * Initializes Handlebars with helpers and cache.
     */
    public TemplateService() {
        // Initialize Handlebars with template cache
        TemplateCache cache = new ConcurrentMapTemplateCache();
        this.handlebars = new Handlebars().with(cache);
        
        // Register built-in helpers
        registerHelpers();
    }
    
    /**
     * Registers Handlebars helpers.
     */
    private void registerHelpers() {
        // Register conditional helpers (if, unless, etc.)
        ConditionalHelpers.each(helper -> handlebars.registerHelper(helper.name(), helper));
        
        // Register string helpers (uppercase, lowercase, etc.)
        StringHelpers.each(helper -> handlebars.registerHelper(helper.name(), helper));
        
        // Register custom helpers
        handlebars.registerHelper("json", (context, options) -> {
            if (context == null) {
                return "null";
            }
            try {
                return context.toString();
            } catch (Exception e) {
                logger.error("Error converting object to JSON: {}", e.getMessage());
                return "null";
            }
        });
        
        handlebars.registerHelper("concat", (context, options) -> {
            StringBuilder result = new StringBuilder();
            if (context != null) {
                result.append(context);
            }
            
            if (options.params.length > 0) {
                for (Object param : options.params) {
                    if (param != null) {
                        result.append(param);
                    }
                }
            }
            
            return result.toString();
        });
        
        handlebars.registerHelper("default", (context, options) -> {
            if (context == null || "".equals(context)) {
                return options.param(0, "");
            }
            return context;
        });
    }
    
    /**
     * Processes a template with the given context.
     *
     * @param templateContent the template content
     * @param context the context for template processing
     * @return the processed template
     */
    public String processTemplate(String templateContent, Map<String, Object> context) {
        if (templateContent == null || templateContent.isEmpty()) {
            return templateContent;
        }
        
        try {
            // Get or compile template
            Template template = getOrCompileTemplate(templateContent);
            
            // Apply template with context
            return template.apply(context);
        } catch (IOException e) {
            logger.error("Error processing template: {}", e.getMessage());
            return templateContent; // Return original content on error
        }
    }
    
    /**
     * Gets a compiled template from cache or compiles it if not found.
     *
     * @param templateContent the template content
     * @return the compiled template
     * @throws IOException if an error occurs during template compilation
     */
    private Template getOrCompileTemplate(String templateContent) throws IOException {
        // Use content hash as cache key
        String cacheKey = String.valueOf(templateContent.hashCode());
        
        // Check cache first
        Template template = templateCache.get(cacheKey);
        if (template == null) {
            // Compile template and cache it
            template = handlebars.compileInline(templateContent);
            templateCache.put(cacheKey, template);
        }
        
        return template;
    }
    
    /**
     * Clears the template cache.
     */
    public void clearCache() {
        templateCache.clear();
        logger.info("Template cache cleared");
    }
    
    /**
     * Gets the current template cache size.
     *
     * @return the cache size
     */
    public int getCacheSize() {
        return templateCache.size();
    }
}

