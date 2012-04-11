/*
 * Copyright 2012 Ronnie Kolehmainen
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

package com.github.cssxfire.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.css.CssBlock;
import com.intellij.psi.css.CssRuleset;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.less.psi.LESSVariableDeclarationImpl;

/**
 * Created by IntelliJ IDEA.
 * User: Ronnie
 */
public class LessVariableProcessor extends CssResolveProcessor {
    public LessVariableProcessor(String name) {
        super(name);
    }

    @Override
    public boolean executeInScope(@NotNull PsiElement base) {
        // If the variable is used within a mixin, check if the variable is declared as mixin parameter or
        // locally within the same block
        CssRuleset ruleset = PsiTreeUtil.getParentOfType(base, CssRuleset.class);
        if (ruleset != null) {
            if (!executeChildren(ruleset)) {
                return false;
            }
            CssBlock[] blocks = PsiTreeUtil.getChildrenOfType(ruleset, CssBlock.class);
            if (blocks != null) {
                for (CssBlock block : blocks) {
                    if (!executeChildren(block)) {
                        return false;
                    }
                }
            }
        }
        return super.executeInScope(base);
    }
    
    private boolean executeChildren(@Nullable PsiElement element) {
        if (element == null) {
            return true;
        }
        LESSVariableDeclarationImpl[] localVariableDeclarations = PsiTreeUtil.getChildrenOfType(element, LESSVariableDeclarationImpl.class);
        if (localVariableDeclarations != null) {
            for (LESSVariableDeclarationImpl localVariableDeclaration : localVariableDeclarations) {
                if (!executeInternal(localVariableDeclaration)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean executeInternal(@NotNull PsiElement element) {
        if (element instanceof LESSVariableDeclarationImpl) {
            LESSVariableDeclarationImpl lessVariableDeclaration = (LESSVariableDeclarationImpl) element;
            PsiElement namedElement = lessVariableDeclaration.getNamedElement();
            if (namedElement != null && namedElement.getText().equals(name)) {
                if (PsiTreeUtil.findChildOfType(lessVariableDeclaration.getParent(), PsiErrorElement.class) != null) {
                    // Error in PSI tree - skip
                    return true;
                }
                result.set(element);
                return false;
            }
        }
        return true;
    }
}