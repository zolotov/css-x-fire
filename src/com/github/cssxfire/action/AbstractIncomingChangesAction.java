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

import com.github.cssxfire.IncomingChangesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class AbstractIncomingChangesAction extends AnAction {
  public AbstractIncomingChangesAction() {
  }

  public AbstractIncomingChangesAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
    super(text, description, icon);
  }

  @Nullable
  protected IncomingChangesComponent getIncomingChangesComponent(AnActionEvent event) {
    Project project = CommonDataKeys.PROJECT.getData(event.getDataContext());
    return project != null ? IncomingChangesComponent.getInstance(project) : null;
  }

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(getIncomingChangesComponent(e) != null &&
                                   IncomingChangesComponent.TOOLWINDOW_ID.equals(e.getPlace()));
  }
}
