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

package com.github.cssxfire.action;

import com.github.cssxfire.CssXFireSettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public class AutoExpand extends ToggleAction {
    @Nullable
    protected CssXFireSettings getProjectSettings(AnActionEvent event) {
        Project project = CommonDataKeys.PROJECT.getData(event.getDataContext());
        if (project == null) {
            return null;
        }
        return CssXFireSettings.getInstance(project);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        CssXFireSettings cssXFireSettings = getProjectSettings(e);
        return cssXFireSettings != null && cssXFireSettings.isAutoExpand();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        CssXFireSettings cssXFireSettings = getProjectSettings(e);
        if (cssXFireSettings != null) {
            cssXFireSettings.setAutoExpand(state);
        }
    }
}
