import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.LineMarkerRendererEx;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.SeparatorPlacement;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.paint.LinePainter2D;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Marks the cell containing the caret with a vertical bar in the gutter.
 */
public class CellHighlighter implements EditorFactoryListener {
    private static final Key<RangeHighlighter> HIGHLIGHTER_KEY = Key.create("PythonCellMode.currentCellHighlighter");
    private static final Key<List<RangeHighlighter>> SEPARATORS_KEY = Key.create("PythonCellMode.cellSeparators");

    private static final int BAR_WIDTH = 1;

    /** Subtle colour from the editor scheme : the regular line number colour, dim but visible in any theme. */
    private static Color barColor(Editor editor) {
        EditorColorsScheme scheme = editor.getColorsScheme();
        Color color = scheme.getColor(EditorColors.LINE_NUMBERS_COLOR);
        return color != null ? color : JBColor.GRAY;
    }

    /** Draws a thin vertical bar starting exactly at the gutter's right edge, growing rightwards. */
    private static final LineMarkerRendererEx BAR_RENDERER = new LineMarkerRendererEx() {
        @Override
        public void paint(@NotNull Editor editor, @NotNull Graphics g, @NotNull Rectangle r) {
            int edge = ((EditorGutterComponentEx) editor.getGutter()).getWhitespaceSeparatorOffset();
            g.setColor(barColor(editor));
            g.fillRect(edge, r.y, JBUI.scale(BAR_WIDTH), r.height);
        }

        @Override
        public @NotNull Position getPosition() {
            // CUSTOM hands us the whole gutter width, so we are free to paint at its right edge
            return Position.CUSTOM;
        }
    };

    private final RunCellAction cellFinder = new RunCellAction();

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (file == null || !"py".equals(file.getExtension())) {
            return;
        }

        Disposable disposable = Disposer.newDisposable();
        EditorUtil.disposeWithEditor(editor, disposable);

        editor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent e) {
                updateHighlight(editor);
            }
        }, disposable);

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent e) {
                updateSeparators(editor);
                updateHighlight(editor);
            }
        }, disposable);

        updateSeparators(editor);
        updateHighlight(editor);
    }

    /** Puts a horizontal line above every delimiter line, reaching at most the first vertical ruler. */
    private void updateSeparators(Editor editor) {
        List<RangeHighlighter> previous = editor.getUserData(SEPARATORS_KEY);
        if (previous != null) {
            for (RangeHighlighter h : previous) {
                editor.getMarkupModel().removeHighlighter(h);
            }
            editor.putUserData(SEPARATORS_KEY, null);
        }

        Document document = editor.getDocument();
        List<RangeHighlighter> separators = new ArrayList<>();
        int line = cellFinder.searchForDelimiter(document, 0, 1);
        while (line != -1) {
            int offset = document.getLineStartOffset(line);
            RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(
                    offset, offset, HighlighterLayer.ADDITIONAL_SYNTAX, null, HighlighterTargetArea.EXACT_RANGE);
            highlighter.setLineSeparatorPlacement(SeparatorPlacement.TOP);
            // Colours are looked up at paint time so a theme switch takes effect without reopening the file
            highlighter.setLineSeparatorRenderer((g, x1, x2, y) -> {
                Color color = separatorColor(editor);
                if (color == null) {
                    return;
                }
                int limit = rulerX(editor);
                g.setColor(color);
                LinePainter2D.paint((Graphics2D) g, x1, y, limit == -1 ? x2 : Math.min(x2, limit), y);
            });
            highlighter.setLineMarkerRenderer(STUB_RENDERER);
            separators.add(highlighter);
            line = cellFinder.searchForDelimiter(document, line + 1, 1);
        }
        editor.putUserData(SEPARATORS_KEY, separators);
    }

    /**
     * Short horizontal stub across the gutter's right margin, joining the separator line drawn in the text area
     * with the vertical bar. The separator sits 1px above the delimiter line, hence the y - 1.
     */
    private static final LineMarkerRendererEx STUB_RENDERER = new LineMarkerRendererEx() {
        @Override
        public void paint(@NotNull Editor editor, @NotNull Graphics g, @NotNull Rectangle r) {
            Color color = separatorColor(editor);
            if (color == null) {
                return;
            }
            int edge = ((EditorGutterComponentEx) editor.getGutter()).getWhitespaceSeparatorOffset();
            g.setColor(color);
            // Same primitive the platform uses for separators, so both rasterise on the same pixel row
            LinePainter2D.paint((Graphics2D) g, edge, r.y - 1, r.x + r.width, r.y - 1);
        }

        @Override
        public @NotNull Position getPosition() {
            return Position.CUSTOM;
        }
    };

    private static Color separatorColor(Editor editor) {
        return editor.getColorsScheme().getColor(CodeInsightColors.METHOD_SEPARATORS_COLOR);
    }

    /** x of the first vertical ruler (right margin or soft margin), -1 if none is shown. */
    private static int rulerX(Editor editor) {
        EditorSettings settings = editor.getSettings();
        if (!settings.isRightMarginShown()) {
            return -1;
        }
        int column = settings.getRightMargin(editor.getProject());
        for (Integer soft : settings.getSoftMargins()) {
            column = Math.min(column, soft);
        }
        return editor.visualPositionToXY(new VisualPosition(0, column)).x;
    }

    private void updateHighlight(Editor editor) {
        RangeHighlighter previous = editor.getUserData(HIGHLIGHTER_KEY);
        if (previous != null) {
            editor.getMarkupModel().removeHighlighter(previous);
            editor.putUserData(HIGHLIGHTER_KEY, null);
        }

        Document document = editor.getDocument();
        AbstractRunAction.Block block = cellFinder.findBlock(editor);
        if (block == null) {
            return;
        }
        // No delimiter in the file : nothing to highlight
        if (block.lineStart == -1 && block.lineEnd == document.getLineCount()) {
            return;
        }

        int start = document.getLineStartOffset(Math.max(block.lineStart, 0));
        int end = document.getLineEndOffset(block.lineEnd - 1);

        RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(
                start, end, HighlighterLayer.CARET_ROW - 1, null, HighlighterTargetArea.LINES_IN_RANGE);
        highlighter.setLineMarkerRenderer(BAR_RENDERER);
        editor.putUserData(HIGHLIGHTER_KEY, highlighter);
    }
}
