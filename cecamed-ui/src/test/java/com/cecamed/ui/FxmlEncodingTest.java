package com.cecamed.ui;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FxmlEncodingTest {
    @ParameterizedTest
    @ValueSource(strings = {"login", "main-layout", "dashboard", "patient-list",
            "patient-form-dialog", "medical-record", "appointment-calendar",
            "appointment-dialog", "reception", "settings"})
    void viewTextIsUtf8WithoutLostSpanishCharacters(String view) throws Exception {
        try (var resource = getClass().getResourceAsStream("/fxml/" + view + ".fxml")) {
            assertThat(resource).isNotNull();
            // The strict decoder rejects malformed UTF-8 instead of silently replacing bytes.
            String xml = StandardCharsets.UTF_8.newDecoder()
                    .decode(ByteBuffer.wrap(resource.readAllBytes())).toString();
            assertThat(xml).doesNotContain("\uFFFD", "Ã", "Â");
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var document = factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(
                    xml.getBytes(StandardCharsets.UTF_8)));
            var elements = document.getElementsByTagName("*");
            for (int i = 0; i < elements.getLength(); i++) {
                var attributes = elements.item(i).getAttributes();
                for (String name : new String[]{"text", "promptText"}) {
                    var attribute = attributes.getNamedItem(name);
                    if (attribute != null) {
                        assertThat(attribute.getNodeValue()).as("%s: %s", view, name)
                                .doesNotContainPattern("\\p{L}\\?\\p{L}|(?<!\\p{L})\\?\\p{L}");
                    }
                }
            }
        }
    }
}
