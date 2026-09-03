package serp.data;

import serp.model.Paper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Loads the reproducible, tab-separated SERP snapshot bundled with the app. */
public final class PaperRepository {
    private static final String RESOURCE = "/data/papers.tsv";

    public List<Paper> load() {
        InputStream stream = PaperRepository.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Missing classpath resource: " + RESOURCE);
        }

        List<Paper> papers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                if (fields.length != 8) {
                    throw new IllegalStateException(
                            "Expected 8 TSV fields at line " + lineNumber + ", found " + fields.length);
                }
                papers.add(new Paper(
                        fields[0],
                        Paper.Category.valueOf(fields[1]),
                        fields[2],
                        fields[3],
                        Integer.parseInt(fields[4]),
                        fields[5],
                        parseHeadings(fields[6]),
                        fields[7]));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + RESOURCE, exception);
        }
        return List.copyOf(papers);
    }

    private List<String> parseHeadings(String field) {
        if (field.isBlank() || field.equals("-")) {
            return List.of();
        }
        return Arrays.stream(field.split("\\|\\|"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }
}
