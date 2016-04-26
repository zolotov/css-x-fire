/*
 * Copyright 2010 Ronnie Kolehmainen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.cssxfire;

import com.github.cssxfire.resolve.GotoDeclarationResolver;
import com.intellij.ide.browsers.BrowserFamily;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.ide.browsers.WebBrowserManager;
import com.intellij.lang.Language;
import com.intellij.lang.css.CSSLanguage;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.css.*;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

public class CssUtils {
    /**
     * See {@link #isDynamicCssLanguage(PsiElement)}
     */
    private static final Collection<FileType> DYNAMIC_CSS_FILETYPES = Arrays.asList(
            FileTypeManager.getInstance().getStdFileType("LESS"),
            FileTypeManager.getInstance().getStdFileType("SASS"),
            FileTypeManager.getInstance().getStdFileType("SCSS")
    );

    public static CssDeclaration createDeclaration(Project project, String selector, String property, String value, boolean important) {
        CSSLanguage cssLanguage = Language.findInstance(CSSLanguage.class);
        String text = selector + " {" + property + ":" + value + (important ? " !important" : "") + ";}\n";
        PsiFile dummyFile = PsiFileFactory.getInstance(project).createFileFromText("dummy.css", cssLanguage, text);
        return PsiTreeUtil.findChildOfType(dummyFile, CssDeclaration.class);
    }

    public static CssRuleset createRuleset(Project project, String selector) {
        CSSLanguage cssLanguage = Language.findInstance(CSSLanguage.class);
        PsiFile dummyFile = PsiFileFactory.getInstance(project).createFileFromText("dummy.css", cssLanguage, selector + " {\n\n}\n");
        return PsiTreeUtil.findChildOfType(dummyFile, CssRuleset.class);
    }

    public static CssTerm createTerm(Project project, String value) {
        return PsiTreeUtil.findChildOfType(createTermList(project, value), CssTerm.class);
    }

    public static CssTermList createTermList(Project project, String value) {
        CSSLanguage cssLanguage = Language.findInstance(CSSLanguage.class);
        PsiFile dummyFile = PsiFileFactory.getInstance(project).createFileFromText("dummy.css", cssLanguage, ".foo { color: " + value + " }");
        return PsiTreeUtil.findChildOfType(dummyFile, CssTermList.class);
    }

    @Nullable
    public static CssRulesetList findFirstCssRulesetList(@NotNull PsiFile file) {
        final Ref<CssRulesetList> ref = new Ref<CssRulesetList>();
        PsiTreeUtil.processElements(file, new PsiElementProcessor() {
            public boolean execute(@NotNull PsiElement element) {
                if (element instanceof CssRulesetList) {
                    ref.set((CssRulesetList) element);
                    return false;
                }
                return true;
            }
        });
        return ref.get();
    }

    public static PsiSearchHelper getPsiSearchHelper(Project project) {
        return ServiceManager.getService(project, PsiSearchHelper.class);
    }

    public static boolean processParents(@NotNull PsiElement element, @NotNull PsiElementProcessor<PsiElement> processor) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (parent instanceof PsiFile) {
                break;
            }
            if (!processor.execute(parent)) {
                return false;
            }
            parent = parent.getParent();
        }
        return true;
    }

    @Nullable
    public static PsiElement resolveVariableAssignment(@NotNull CssDeclaration cssDeclaration) {
        CssTermList termList = cssDeclaration.getValue();
        if (termList == null) {
            return null;
        }
        CssTerm[] terms = termList.getTerms();
        if (terms.length != 1) {
            return null; // not an explicit variable reference
        }

        final Ref<PsiElement> resolved = new Ref<PsiElement>(null);
        PsiTreeUtil.processElements(terms[0], new PsiElementProcessor() {
            public boolean execute(@NotNull PsiElement element) {
                PsiElement[] targets = GotoDeclarationResolver.INSTANCE.getGotoDeclarationTargets(element, null);
                if (targets != null && targets.length == 1) {
                    resolved.set(targets[0]);
                    return false;
                }
                return true;
            }
        });
        return resolved.get();
    }

    public static boolean processCssDeclarations(@Nullable CssBlock block, final PsiElementProcessor<CssDeclaration> declarationProcessor) {
        if (block == null) {
            return false;
        }
        CssDeclaration[] declarations = PsiTreeUtil.getChildrenOfType(block, CssDeclaration.class);
        if (declarations != null) {
            for (CssDeclaration declaration : declarations) {
                if (!declarationProcessor.execute(declaration)) {
                    return false;
                }
            }
        }
        if (isDynamicCssLanguage(block) && CssXFireSettings.getInstance(block.getProject()).isResolveMixins()) {
            return PsiTreeUtil.processElements(block, new PsiElementProcessor() {
                public boolean execute(@NotNull PsiElement element) {
                    PsiElement[] targets = GotoDeclarationResolver.INSTANCE.getGotoDeclarationTargets(element, null);
                    if (targets != null && targets.length == 1) {
                        PsiElement resolved = targets[0];
                        if (resolved instanceof CssRuleset) {
                            if (!processCssDeclarations(((CssRuleset) resolved).getBlock(), declarationProcessor)) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
            });
        }
        return true;
    }

    /**
     * Checks if the element is contained in a Less or Sass language file
     *
     * @param element an element
     * @return <tt>true</tt> if the element is a file or is contained in a file of type Less/Sass
     */
    public static boolean isDynamicCssLanguage(@NotNull PsiElement element) {
        PsiFile file = element instanceof PsiFile ? (PsiFile) element : element.getContainingFile();
        FileType fileType = file.getFileType();
        return !(fileType instanceof PlainTextFileType) && DYNAMIC_CSS_FILETYPES.contains(fileType);
    }

    @Nullable
    public static CssMediumList findMediumList(@Nullable PsiElement element) {
        CssMedia media = PsiTreeUtil.getNonStrictParentOfType(element, CssMedia.class);
        return media != null ? media.getMediumList() : null;
    }

    public static void openInFirefox(@NotNull String url) {
        WebBrowser firefox = WebBrowserManager.getInstance().getFirstBrowser(BrowserFamily.FIREFOX);
        BrowserLauncher.getInstance().browse(url, WebBrowserManager.getInstance().isActive(firefox) ? firefox : null);
    }
}
