import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes every cell foldable : the delimiter line stays visible, the cell body folds.
 */
public class CellFoldingBuilder extends FoldingBuilderEx implements DumbAware {
    private final RunCellAction cellFinder = new RunCellAction();

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        ASTNode node = root.getNode();
        CharSequence text = document.getCharsSequence();
        int lineCount = document.getLineCount();

        int line = cellFinder.searchForDelimiter(document, 0, 1);
        while (line != -1) {
            int next = cellFinder.searchForDelimiter(document, line + 1, 1);
            int lastLine = (next == -1 ? lineCount : next) - 1;

            int start = document.getLineEndOffset(line);
            // Leave trailing blank lines out of the fold so the spacing between cells stays visible
            int end = document.getLineEndOffset(lastLine);
            while (end > start && Character.isWhitespace(text.charAt(end - 1))) {
                end--;
            }
            if (end > start) {
                descriptors.add(new FoldingDescriptor(node, new TextRange(start, end), null, " ..."));
            }
            line = next;
        }
        return descriptors.toArray(FoldingDescriptor.EMPTY_ARRAY);
    }

    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node) {
        return " ...";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return false;
    }
}
