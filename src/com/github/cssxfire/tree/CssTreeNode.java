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

package com.github.cssxfire.tree;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public abstract class CssTreeNode extends DefaultMutableTreeNode {
    @Nullable
    public abstract Icon getIcon();

    /**
     * Get the text to display in the tree view
     * @return the text
     */
    public abstract String getText();

    /**
     * Get the name of the node (used for sorting the tree).
     * @return the name to compare
     */
    public abstract String getName();

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public Object getUserObject() {
        return super.getUserObject();
    }

    @Nullable
    public abstract ActionGroup getActionGroup();

    @NotNull
    public SimpleTextAttributes getTextAttributes() {
        return SimpleTextAttributes.REGULAR_ATTRIBUTES;
    }
}
