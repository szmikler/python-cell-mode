import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class PythonCellRunLineMarker extends RunLineMarkerContributor {

    protected final Preferences prefs = new Preferences();

    @Nullable
    @Override
    public Info getInfo(@NotNull PsiElement element) {
        if (element instanceof PsiComment) {
            PsiComment comment = (PsiComment) element;
            String value = comment.getText();
            Pattern pattern = Pattern.compile(prefs.getDelimiterRegexp());
            int lineNumber = getLineNumber(pattern, comment);
            if (lineNumber >= 0) {
                return new Info(
                        AllIcons.RunConfigurations.TestState.Run,
                        getActions(lineNumber),
                        (PsiElement e) -> "Run Cell");
            }
        }
        return null;
    }

    /** Line number of the element if its line matches the delimiter pattern, -1 otherwise. */
    private static int getLineNumber(Pattern pattern, PsiElement element) {
        Document document = PsiDocumentManager.getInstance(element.getProject()).getDocument(element.getContainingFile());
        if (document != null) {
            int lineNumber = document.getLineNumber(element.getTextRange().getStartOffset());

            int start = document.getLineStartOffset(lineNumber);
            int end = document.getLineEndOffset(lineNumber);
            CharSequence text = document.getCharsSequence().subSequence(start, end);
            if (pattern.matcher(text).matches()) {
                return lineNumber;
            }
        }
        return -1;
    }

    private AnAction[] getActions(int lineNumber) {
        return new RunCellAction[]{new RunCellAction(lineNumber), new RunCellMoveNextAction(lineNumber)};
    }
}
