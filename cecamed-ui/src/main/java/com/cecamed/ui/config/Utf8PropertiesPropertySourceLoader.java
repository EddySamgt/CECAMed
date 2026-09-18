package com.cecamed.ui.config;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Reads properties syntax as UTF-8, including extensionless .env imports. */
public class Utf8PropertiesPropertySourceLoader implements PropertySourceLoader {

    @Override
    public String[] getFileExtensions() {
        return new String[]{"utf8"};
    }

    @Override
    public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        return List.of(new ResourcePropertySource(name,
                new EncodedResource(resource, StandardCharsets.UTF_8)));
    }
}
