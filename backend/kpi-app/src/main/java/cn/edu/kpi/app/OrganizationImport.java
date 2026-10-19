package cn.edu.kpi.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Development importer for the supplied organization register; production will read snapshots from MySQL. */
public final class OrganizationImport {
    private OrganizationImport() { }

    public static List<Unit> load() {
        String configured = System.getenv("KPI_ORGANIZATION_CSV");
        if (configured == null || configured.isBlank()) return List.of(new Unit("unit-demo-001", "示例学院", "待确认", "是"));
        try {
            List<List<String>> rows = parseCsv(Files.readString(Path.of(configured), StandardCharsets.UTF_8));
            var units = new ArrayList<Unit>();
            for (int i = 1; i < rows.size(); i++) {
                List<String> fields = rows.get(i);
                if (fields.size() >= 7) units.add(new Unit("unit-" + fields.get(0), fields.get(3).replace("\n", " "), fields.get(4), fields.get(6)));
            }
            return List.copyOf(units);
        } catch (IOException e) { throw new IllegalStateException("cannot load organization register", e); }
    }

    private static List<List<String>> parseCsv(String source) {
        var rows = new ArrayList<List<String>>(); var row = new ArrayList<String>(); var cell = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < source.length() && source.charAt(i + 1) == '"') { cell.append(c); i++; }
                else quoted = !quoted;
            } else if (c == ',' && !quoted) { row.add(cell.toString().replace("\uFEFF", "")); cell.setLength(0); }
            else if ((c == '\n' || c == '\r') && !quoted) {
                if (c == '\r' && i + 1 < source.length() && source.charAt(i + 1) == '\n') i++;
                row.add(cell.toString().replace("\uFEFF", "")); cell.setLength(0);
                if (!row.stream().allMatch(String::isEmpty)) rows.add(row);
                row = new ArrayList<>();
            } else cell.append(c);
        }
        if (!row.isEmpty() || cell.length() > 0) { row.add(cell.toString().replace("\uFEFF", "")); rows.add(row); }
        return rows;
    }

    public record Unit(String id, String name, String category, String workPlanStatus) { }
}
