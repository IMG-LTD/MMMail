package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.dto.EvaluateSheetsFormulaCellInput;
import com.mmmail.server.model.entity.SheetsWorkbook;
import com.mmmail.server.model.vo.SheetsDependencyGraphVo;
import com.mmmail.server.model.vo.SheetsFormulaCellResultVo;
import com.mmmail.server.model.vo.SheetsFormulaEvaluationVo;
import com.mmmail.server.model.vo.SheetsFormulaGraphNodeVo;
import com.mmmail.server.model.vo.SheetsWorkbookDetailVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SheetsFormulaApiService {

    private static final int ROW_BASE = 1;
    private static final int COLUMN_BASE = 26;
    private static final String FORMULA_PREFIX = "=";
    private static final Pattern CELL_REF_PATTERN = Pattern.compile("^\\$?([A-Z]{1,3})\\$?([1-9][0-9]*)$");
    private static final Pattern FORMULA_REF_PATTERN = Pattern.compile("\\$?([A-Z]{1,3})\\$?([1-9][0-9]*)(?::\\$?([A-Z]{1,3})\\$?([1-9][0-9]*))?");

    private final SheetsAccessService sheetsAccessService;
    private final SheetsWorkbookStateService sheetsWorkbookStateService;
    private final SheetsFormulaService sheetsFormulaService;
    private final SheetsService sheetsService;

    public SheetsFormulaApiService(
            SheetsAccessService sheetsAccessService,
            SheetsWorkbookStateService sheetsWorkbookStateService,
            SheetsFormulaService sheetsFormulaService,
            SheetsService sheetsService
    ) {
        this.sheetsAccessService = sheetsAccessService;
        this.sheetsWorkbookStateService = sheetsWorkbookStateService;
        this.sheetsFormulaService = sheetsFormulaService;
        this.sheetsService = sheetsService;
    }

    public SheetsFormulaEvaluationVo evaluate(Long userId, Long workbookId, List<EvaluateSheetsFormulaCellInput> cells) {
        LoadedSheet loadedSheet = loadActiveSheet(userId, workbookId);
        List<List<String>> grid = gridWithInputs(loadedSheet.sheet(), cells);
        FormulaGraph graph = buildFormulaGraph(grid, loadedSheet.sheet().rowCount(), loadedSheet.sheet().colCount());
        assertNoCircularReferences(graph.expandedDependencies());
        SheetsFormulaService.ComputationResult computation = sheetsFormulaService.compute(
                grid,
                loadedSheet.sheet().rowCount(),
                loadedSheet.sheet().colCount()
        );
        return new SheetsFormulaEvaluationVo(buildResults(cells, computation, graph.tokenDependencies()));
    }

    public SheetsDependencyGraphVo dependencyGraph(Long userId, Long workbookId) {
        LoadedSheet loadedSheet = loadActiveSheet(userId, workbookId);
        FormulaGraph graph = buildFormulaGraph(
                loadedSheet.sheet().grid(),
                loadedSheet.sheet().rowCount(),
                loadedSheet.sheet().colCount()
        );
        assertNoCircularReferences(graph.expandedDependencies());
        return new SheetsDependencyGraphVo(buildNodes(graph), topologicalOrder(graph.expandedDependencies()));
    }

    public SheetsWorkbookDetailVo recalculate(Long userId, Long workbookId) {
        return sheetsService.get(userId, workbookId);
    }

    private LoadedSheet loadActiveSheet(Long userId, Long workbookId) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireAccessible(userId, workbookId);
        SheetsWorkbook workbook = context.workbook();
        SheetsWorkbookStateService.WorkbookState state = sheetsWorkbookStateService.readState(workbook);
        return new LoadedSheet(workbook, sheetsWorkbookStateService.activeSheet(state));
    }

    private List<List<String>> gridWithInputs(
            SheetsWorkbookStateService.SheetState sheet,
            List<EvaluateSheetsFormulaCellInput> cells
    ) {
        List<List<String>> grid = mutableGrid(sheet.grid(), sheet.rowCount(), sheet.colCount());
        for (EvaluateSheetsFormulaCellInput cell : cells) {
            CellRef ref = parseCellRef(cell.ref());
            validateWithinSheet(ref, sheet.rowCount(), sheet.colCount());
            grid.get(ref.rowIndex()).set(ref.colIndex(), normalizeFormula(cell.formula()));
        }
        return grid;
    }

    private List<List<String>> mutableGrid(List<List<String>> source, int rowCount, int colCount) {
        List<List<String>> grid = new ArrayList<>(rowCount);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            List<String> sourceRow = rowIndex < source.size() ? source.get(rowIndex) : List.of();
            List<String> row = new ArrayList<>(colCount);
            for (int colIndex = 0; colIndex < colCount; colIndex++) {
                row.add(colIndex < sourceRow.size() ? sourceRow.get(colIndex) : "");
            }
            grid.add(row);
        }
        return grid;
    }

    private FormulaGraph buildFormulaGraph(List<List<String>> grid, int rowCount, int colCount) {
        Map<String, String> formulas = new LinkedHashMap<>();
        Map<String, List<String>> tokenDependencies = new LinkedHashMap<>();
        Map<String, List<String>> expandedDependencies = new LinkedHashMap<>();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            for (int colIndex = 0; colIndex < colCount; colIndex++) {
                addFormulaNode(grid, rowIndex, colIndex, formulas, tokenDependencies, expandedDependencies);
            }
        }
        return new FormulaGraph(formulas, tokenDependencies, expandedDependencies);
    }

    private void addFormulaNode(
            List<List<String>> grid,
            int rowIndex,
            int colIndex,
            Map<String, String> formulas,
            Map<String, List<String>> tokenDependencies,
            Map<String, List<String>> expandedDependencies
    ) {
        String value = cellValue(grid, rowIndex, colIndex);
        if (!isFormula(value)) {
            return;
        }
        String ref = toCellRef(rowIndex, colIndex);
        List<FormulaReference> references = extractFormulaReferences(value);
        formulas.put(ref, value);
        tokenDependencies.put(ref, references.stream().map(FormulaReference::token).toList());
        expandedDependencies.put(ref, expandReferences(references));
    }

    private List<SheetsFormulaCellResultVo> buildResults(
            List<EvaluateSheetsFormulaCellInput> cells,
            SheetsFormulaService.ComputationResult computation,
            Map<String, List<String>> tokenDependencies
    ) {
        List<SheetsFormulaCellResultVo> results = new ArrayList<>(cells.size());
        for (EvaluateSheetsFormulaCellInput input : cells) {
            CellRef ref = parseCellRef(input.ref());
            String normalizedRef = toCellRef(ref.rowIndex(), ref.colIndex());
            TypedFormulaValue value = toTypedValue(cellValue(computation.computedGrid(), ref.rowIndex(), ref.colIndex()));
            results.add(new SheetsFormulaCellResultVo(normalizedRef, value.value(), value.type(), tokenDependencies.get(normalizedRef)));
        }
        return results;
    }

    private List<SheetsFormulaGraphNodeVo> buildNodes(FormulaGraph graph) {
        Map<String, LinkedHashSet<String>> dependents = buildDependents(graph.expandedDependencies());
        Set<String> refs = new LinkedHashSet<>(graph.formulas().keySet());
        graph.expandedDependencies().values().forEach(refs::addAll);
        return sortRefs(refs).stream()
                .map(ref -> new SheetsFormulaGraphNodeVo(
                        ref,
                        graph.formulas().get(ref),
                        graph.tokenDependencies().getOrDefault(ref, List.of()),
                        sortRefs(dependents.getOrDefault(ref, new LinkedHashSet<>()))
                ))
                .toList();
    }

    private Map<String, LinkedHashSet<String>> buildDependents(Map<String, List<String>> dependencies) {
        Map<String, LinkedHashSet<String>> dependents = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : dependencies.entrySet()) {
            for (String dependency : entry.getValue()) {
                dependents.computeIfAbsent(dependency, ignored -> new LinkedHashSet<>()).add(entry.getKey());
            }
        }
        return dependents;
    }

    private List<String> topologicalOrder(Map<String, List<String>> dependencies) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        Set<String> visited = new LinkedHashSet<>();
        for (String ref : dependencies.keySet()) {
            addTopologicalRef(ref, dependencies, visited, ordered);
        }
        return new ArrayList<>(ordered);
    }

    private void addTopologicalRef(
            String ref,
            Map<String, List<String>> dependencies,
            Set<String> visited,
            LinkedHashSet<String> ordered
    ) {
        if (!visited.add(ref)) {
            return;
        }
        for (String dependency : dependencies.getOrDefault(ref, List.of())) {
            addTopologicalRef(dependency, dependencies, visited, ordered);
        }
        ordered.add(ref);
    }

    private void assertNoCircularReferences(Map<String, List<String>> dependencies) {
        Map<String, VisitState> states = new LinkedHashMap<>();
        Deque<String> path = new ArrayDeque<>();
        for (String ref : dependencies.keySet()) {
            visitFormulaRef(ref, dependencies, states, path);
        }
    }

    private void visitFormulaRef(
            String ref,
            Map<String, List<String>> dependencies,
            Map<String, VisitState> states,
            Deque<String> path
    ) {
        VisitState state = states.get(ref);
        if (state == VisitState.VISITING) {
            throw circularReference(path, ref);
        }
        if (state == VisitState.VISITED) {
            return;
        }
        states.put(ref, VisitState.VISITING);
        path.addLast(ref);
        for (String dependency : dependencies.getOrDefault(ref, List.of())) {
            if (dependencies.containsKey(dependency)) {
                visitFormulaRef(dependency, dependencies, states, path);
            }
        }
        path.removeLast();
        states.put(ref, VisitState.VISITED);
    }

    private BizException circularReference(Deque<String> path, String ref) {
        List<String> cycle = new ArrayList<>();
        boolean collecting = false;
        for (String item : path) {
            collecting = collecting || item.equals(ref);
            if (collecting) {
                cycle.add(item);
            }
        }
        cycle.add(ref);
        return new BizException(ErrorCode.SHEETS_CIRCULAR_REF, "Circular sheets formula reference: " + String.join(" -> ", cycle));
    }

    private List<FormulaReference> extractFormulaReferences(String formula) {
        Matcher matcher = FORMULA_REF_PATTERN.matcher(formula.toUpperCase(Locale.ROOT));
        List<FormulaReference> references = new ArrayList<>();
        while (matcher.find()) {
            references.add(toFormulaReference(matcher));
        }
        return dedupeReferences(references);
    }

    private FormulaReference toFormulaReference(Matcher matcher) {
        CellRef start = parseCellRef(matcher.group(1) + matcher.group(2));
        if (!StringUtils.hasText(matcher.group(3))) {
            return new FormulaReference(toCellRef(start.rowIndex(), start.colIndex()), start, start);
        }
        CellRef end = parseCellRef(matcher.group(3) + matcher.group(4));
        return new FormulaReference(toCellRef(start.rowIndex(), start.colIndex()) + ":" + toCellRef(end.rowIndex(), end.colIndex()), start, end);
    }

    private List<FormulaReference> dedupeReferences(List<FormulaReference> references) {
        Map<String, FormulaReference> deduped = new LinkedHashMap<>();
        for (FormulaReference reference : references) {
            deduped.putIfAbsent(reference.token(), reference);
        }
        return new ArrayList<>(deduped.values());
    }

    private List<String> expandReferences(List<FormulaReference> references) {
        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        for (FormulaReference reference : references) {
            expandReference(reference, expanded);
        }
        return new ArrayList<>(expanded);
    }

    private void expandReference(FormulaReference reference, LinkedHashSet<String> expanded) {
        int startRow = Math.min(reference.start().rowIndex(), reference.end().rowIndex());
        int endRow = Math.max(reference.start().rowIndex(), reference.end().rowIndex());
        int startCol = Math.min(reference.start().colIndex(), reference.end().colIndex());
        int endCol = Math.max(reference.start().colIndex(), reference.end().colIndex());
        for (int rowIndex = startRow; rowIndex <= endRow; rowIndex++) {
            for (int colIndex = startCol; colIndex <= endCol; colIndex++) {
                expanded.add(toCellRef(rowIndex, colIndex));
            }
        }
    }

    private CellRef parseCellRef(String rawRef) {
        Matcher matcher = CELL_REF_PATTERN.matcher(rawRef.trim().toUpperCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Sheets cell reference is invalid");
        }
        return new CellRef(Integer.parseInt(matcher.group(2)) - ROW_BASE, columnToIndex(matcher.group(1)));
    }

    private int columnToIndex(String letters) {
        int value = 0;
        for (int index = 0; index < letters.length(); index++) {
            value = value * COLUMN_BASE + (letters.charAt(index) - 'A' + 1);
        }
        return value - 1;
    }

    private String toCellRef(int rowIndex, int colIndex) {
        return indexToColumn(colIndex) + (rowIndex + ROW_BASE);
    }

    private String indexToColumn(int colIndex) {
        int value = colIndex + 1;
        StringBuilder builder = new StringBuilder();
        while (value > 0) {
            int remainder = (value - 1) % COLUMN_BASE;
            builder.insert(0, (char) ('A' + remainder));
            value = (value - 1) / COLUMN_BASE;
        }
        return builder.toString();
    }

    private void validateWithinSheet(CellRef ref, int rowCount, int colCount) {
        if (ref.rowIndex() >= rowCount || ref.colIndex() >= colCount) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Sheets cell reference is outside the sheet");
        }
    }

    private String normalizeFormula(String formula) {
        String normalized = formula.trim();
        if (!normalized.startsWith(FORMULA_PREFIX)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Sheets formula must start with =");
        }
        return normalized;
    }

    private TypedFormulaValue toTypedValue(String displayValue) {
        if (displayValue == null || displayValue.isBlank()) {
            return new TypedFormulaValue("", "BLANK");
        }
        if (displayValue.startsWith("#")) {
            return new TypedFormulaValue(displayValue, "ERROR");
        }
        if ("TRUE".equals(displayValue) || "FALSE".equals(displayValue)) {
            return new TypedFormulaValue(Boolean.valueOf(displayValue.toLowerCase(Locale.ROOT)), "BOOLEAN");
        }
        return parseNumber(displayValue);
    }

    private TypedFormulaValue parseNumber(String displayValue) {
        try {
            return new TypedFormulaValue(new BigDecimal(displayValue), "NUMBER");
        } catch (NumberFormatException ex) {
            return new TypedFormulaValue(displayValue, "TEXT");
        }
    }

    private List<String> sortRefs(Collection<String> refs) {
        return refs.stream().sorted(Comparator.comparing(this::parseCellRef)).toList();
    }

    private String cellValue(List<List<String>> grid, int rowIndex, int colIndex) {
        if (rowIndex >= grid.size() || colIndex >= grid.get(rowIndex).size()) {
            return "";
        }
        return grid.get(rowIndex).get(colIndex);
    }

    private boolean isFormula(String value) {
        return StringUtils.hasText(value) && value.startsWith(FORMULA_PREFIX) && value.length() > 1;
    }

    private enum VisitState {
        VISITING,
        VISITED
    }

    private record LoadedSheet(SheetsWorkbook workbook, SheetsWorkbookStateService.SheetState sheet) {
    }

    private record FormulaGraph(
            Map<String, String> formulas,
            Map<String, List<String>> tokenDependencies,
            Map<String, List<String>> expandedDependencies
    ) {
    }

    private record FormulaReference(String token, CellRef start, CellRef end) {
    }

    private record TypedFormulaValue(Object value, String type) {
    }

    private record CellRef(int rowIndex, int colIndex) implements Comparable<CellRef> {

        @Override
        public int compareTo(CellRef other) {
            int rowCompare = Integer.compare(rowIndex, other.rowIndex);
            return rowCompare != 0 ? rowCompare : Integer.compare(colIndex, other.colIndex);
        }
    }
}
