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

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.AbstractFileTreeTable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Map;

import static com.intellij.util.ui.FormBuilder.createFormBuilder;

public class CssXFireConfigurable implements SearchableConfigurable {

  private Project myProject;
  private JCheckBox myAutoClearCb;
  private JCheckBox myMediaReduceCb;
  private JCheckBox myFileNameReduceCb;
  private JCheckBox myRoutesReduceCb;
  private JCheckBox myOpenedFilesReduceCb;
  private JCheckBox myResolveVariablesCb;
  private JCheckBox myResolveMixinsCb;
  private FileTreeTable myRoutesTable;
  private JButton mySetRootButton;

  public CssXFireConfigurable(@NotNull Project project) {
    myProject = project;
  }

  @NotNull
  public String getId() {
    return getClass().getName();
  }

  public Runnable enableSearch(String option) {
    return null;
  }

  public JComponent createComponent() {
    myAutoClearCb = new JBCheckBox("Clear pending changes when leaving or reloading page");
    JPanel generalPanel = createFormBuilder().addComponent(myAutoClearCb).getPanel();
    generalPanel.setBorder(IdeBorderFactory.createTitledBorder("General"));

    myResolveVariablesCb = new JBCheckBox("Resolve variables");
    myResolveMixinsCb = new JBCheckBox("Resolve mixins");
    JPanel lessSassPanel = createFormBuilder().addComponent(myResolveVariablesCb).addComponent(myResolveMixinsCb).getPanel();
    lessSassPanel.setBorder(IdeBorderFactory.createTitledBorder("Less / Sass"));

    myMediaReduceCb = new JBCheckBox("Match CSS3 media queries");
    myOpenedFilesReduceCb = new JBCheckBox("Currently opened files");
    myFileNameReduceCb = new JBCheckBox("Match filename");
    myRoutesReduceCb = new JBCheckBox("Use routes");
    myRoutesTable = new FileTreeTable(myProject);
    myRoutesReduceCb.addChangeListener(event -> UIUtil.setEnabled(myRoutesTable, myRoutesReduceCb.isSelected(), true));
    mySetRootButton = new JButton("Set as web root");

    final JPanel reduceStrategyPanel = new FormBuilder() {
      @Override
      protected int getFill(JComponent component) {
        if (component instanceof JButton) {
          return GridBagConstraints.NONE;
        }
        else if (component instanceof FileTreeTable) {
          return GridBagConstraints.BOTH;
        }
        return super.getFill(component);
      }
    }
      .addComponent(myMediaReduceCb)
      .addComponent(myOpenedFilesReduceCb)
      .addComponent(myFileNameReduceCb)
      .addComponent(myRoutesReduceCb)
      .setFormLeftIndent(UIUtil.getCheckBoxTextHorizontalOffset(myRoutesReduceCb))
      .addComponent(ScrollPaneFactory.createScrollPane(myRoutesTable))
      .addComponent(mySetRootButton)
      .getPanel();
    reduceStrategyPanel.setBorder(IdeBorderFactory.createTitledBorder("Reduce strategy"));

    myRoutesTable.getSelectionModel().addListSelectionListener(e -> updateWebRootButton());
    mySetRootButton.addActionListener(e -> updateWebRoot());

    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.anchor = GridBagConstraints.NORTHWEST;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 0.0;
    c.weighty = 0.0;
    c.gridwidth = GridBagConstraints.REMAINDER;

    panel.add(generalPanel, c);
    panel.add(lessSassPanel, c);
    c.weightx = 1.0;
    c.weighty = 1.0;
    panel.add(reduceStrategyPanel, c);

    return panel;
  }

  public void disposeUIResources() {
    UIUtil.dispose(myAutoClearCb);
    UIUtil.dispose(myMediaReduceCb);
    UIUtil.dispose(myFileNameReduceCb);
    UIUtil.dispose(myRoutesReduceCb);
    UIUtil.dispose(myOpenedFilesReduceCb);
    UIUtil.dispose(myResolveVariablesCb);
    UIUtil.dispose(myResolveMixinsCb);
    UIUtil.dispose(myRoutesTable);
    UIUtil.dispose(mySetRootButton);
  }

  private void updateWebRoot() {
    int selectedRow = myRoutesTable.getSelectedRow();
    if (selectedRow != -1) {
      Map<VirtualFile, String> currentValues = myRoutesTable.getValues();
      Object file = myRoutesTable.getValueAt(selectedRow, 0);
      if (file instanceof VirtualFile) {
        for (VirtualFile key : new HashSet<>(currentValues.keySet())) {
          if ("/".equals(currentValues.get(key))) {
            currentValues.remove(key);
          }
        }
        currentValues.put((VirtualFile)file, "/");
      }
      myRoutesTable.reset(currentValues);
      updateWebRootButton();
    }
  }

  private void updateWebRootButton() {
    int selectedRow = myRoutesTable.getSelectedRow();
    if (selectedRow == -1) {
      mySetRootButton.setEnabled(false);
      return;
    }
    Object file = myRoutesTable.getValueAt(selectedRow, 0);
    Object route = myRoutesTable.getValueAt(selectedRow, 1);
    mySetRootButton.setEnabled(!"/".equals(route) && file instanceof VirtualFile && ((VirtualFile)file).isDirectory()
                               && myRoutesTable.isValueEditableForFile((VirtualFile)file));
  }

  public boolean isModified() {
    CssXFireSettings settings = CssXFireSettings.getInstance(myProject);
    return !settings.getRoutes().getMappings().equals(myRoutesTable.getValues())
           || settings.isAutoClear() != myAutoClearCb.isSelected()
           || settings.isMediaReduce() != myMediaReduceCb.isSelected()
           || settings.isFileReduce() != myFileNameReduceCb.isSelected()
           || settings.isUseRoutes() != myRoutesReduceCb.isSelected()
           || settings.isCurrentDocumentsReduce() != myOpenedFilesReduceCb.isSelected()
           || settings.isResolveVariables() != myResolveVariablesCb.isSelected()
           || settings.isResolveMixins() != myResolveMixinsCb.isSelected();
  }

  public void apply() {
    CssXFireSettings settings = CssXFireSettings.getInstance(myProject);
    settings.getRoutes().setMappings(myRoutesTable.getValues());
    settings.setAutoClear(myAutoClearCb.isSelected());
    settings.setMediaReduce(myMediaReduceCb.isSelected());
    settings.setFileReduce(myFileNameReduceCb.isSelected());
    settings.setCurrentDocumentsReduce(myOpenedFilesReduceCb.isSelected());
    settings.setResolveVariables(myResolveVariablesCb.isSelected());
    settings.setResolveMixins(myResolveMixinsCb.isSelected());
    settings.setUseRoutes(myRoutesReduceCb.isSelected());
  }

  public void reset() {
    CssXFireSettings settings = CssXFireSettings.getInstance(myProject);
    myRoutesTable.reset(settings.getRoutes().getMappings());
    UIUtil.setEnabled(myRoutesTable, settings.isUseRoutes(), true);
    myAutoClearCb.setSelected(settings.isAutoClear());
    myFileNameReduceCb.setSelected(settings.isFileReduce());
    myMediaReduceCb.setSelected(settings.isMediaReduce());
    myOpenedFilesReduceCb.setSelected(settings.isCurrentDocumentsReduce());
    myResolveVariablesCb.setSelected(settings.isResolveVariables());
    myResolveMixinsCb.setSelected(settings.isResolveMixins());
    myRoutesReduceCb.setSelected(settings.isUseRoutes());
    updateWebRootButton();
  }

  @Nls
  public String getDisplayName() {
    return "CSS-X-Fire";
  }

  public String getHelpTopic() {
    return null;
  }

  private static class FileTreeTable extends AbstractFileTreeTable<String> {
    private final VirtualFile myProjectBaseDir;

    private FileTreeTable(@NotNull Project project) {
      super(project, String.class, "Route", VirtualFileFilter.ALL, false);
      myProjectBaseDir = project.getBaseDir();
    }

    @Override
    protected boolean isValueEditableForFile(VirtualFile virtualFile) {
      if (virtualFile == null) {
        return false;
      }
      return !virtualFile.getUrl().startsWith(getSettingsUrl());
    }

    @Override
    protected boolean isNullObject(String value) {
      return value == null || !value.equals(value.trim()) || !StringUtil.startsWithChar(value, '/');
    }

    @NotNull
    private String getSettingsUrl() {
      return (myProjectBaseDir != null ? myProjectBaseDir.getUrl() : "") + "/.idea";
    }
  }
}
