package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.vo.StandardNoteChecklistItemVo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StandardNotesChecklistCodec {

    private static final Pattern CHECKLIST_PATTERN = Pattern.compile("^(\\s*[-*]\\s+\\[)( |x|X)(\\]\\s+)(.*)$");

    public List<StandardNoteChecklistItemVo> parse(String content) {
        String[] lines = normalizeContent(content).split("\\n", -1);
        List<StandardNoteChecklistItemVo> items = new ArrayList<>();
        int itemIndex = 0;
        for (String line : lines) {
            Matcher matcher = CHECKLIST_PATTERN.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            items.add(new StandardNoteChecklistItemVo(itemIndex++, matcher.group(4), isCompleted(matcher.group(2))));
        }
        return List.copyOf(items);
    }

    public ChecklistStats summarize(String content) {
        int taskCount = 0;
        int completedTaskCount = 0;
        for (StandardNoteChecklistItemVo item : parse(content)) {
            taskCount++;
            if (item.completed()) {
                completedTaskCount++;
            }
        }
        return new ChecklistStats(taskCount, completedTaskCount);
    }

    public String toggle(String content, int itemIndex, boolean completed) {
        String[] lines = normalizeContent(content).split("\\n", -1);
        int currentIndex = 0;
        for (int index = 0; index < lines.length; index++) {
            Matcher matcher = CHECKLIST_PATTERN.matcher(lines[index]);
            if (!matcher.matches()) {
                continue;
            }
            if (currentIndex == itemIndex) {
                lines[index] = matcher.group(1) + (completed ? "x" : " ") + matcher.group(3) + matcher.group(4);
                return String.join("\n", lines);
            }
            currentIndex++;
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Checklist item not found");
    }

    private boolean isCompleted(String marker) {
        return "x".equalsIgnoreCase(marker);
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content;
    }

    public record ChecklistStats(int taskCount, int completedTaskCount) {
    }
}
