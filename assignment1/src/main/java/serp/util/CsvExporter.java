package serp.util;

import serp.model.FrequencyItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CsvExporter {
    public void write(Path path, String labelHeader, List<FrequencyItem> items) throws IOException {
        StringBuilder csv = new StringBuilder("rank,")
                .append(quote(labelHeader)).append(",paper_count,coverage_percent\n");
        for (int index = 0; index < items.size(); index++) {
            FrequencyItem item = items.get(index);
            csv.append(index + 1).append(',')
                    .append(quote(item.label())).append(',')
                    .append(item.count()).append(',')
                    .append(String.format(java.util.Locale.ROOT, "%.1f", item.percentage()))
                    .append('\n');
        }
        Files.writeString(path, csv, StandardCharsets.UTF_8);
    }

    private String quote(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
