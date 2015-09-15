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

package com.github.cssxfire.ui;

import com.github.cssxfire.CssXFireConfigurable;
import com.github.cssxfire.CssXFireSettings;
import com.github.cssxfire.IncomingChangesComponent;
import com.github.cssxfire.tree.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.*;
import com.intellij.ide.actions.NextOccurenceToolbarAction;
import com.intellij.ide.actions.PreviousOccurenceToolbarAction;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SideBorder;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.DialogUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;

public class CssToolWindow extends SimpleToolWindowPanel implements TreeViewModel, Disposable {
  private final CssChangesTreeModel myTreeModel;
  private final JTree myTree;
  private final Project myProject;
  private final JButton myCancelButton;
  private final JButton myApplyButton;

  public CssToolWindow(final Project project) {
    super(false, true);
    myProject = project;
    myTreeModel = new CssChangesTreeModel(project);

    myTree = new Tree(myTreeModel);
    myTree.setCellRenderer(new MyTreeCellRenderer());
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    UIUtil.setLineStyleAngled(myTree);

    myTree.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(@NotNull MouseEvent e) {
        // todo reimplement with EditSourceOnDoubleClickHandler.install(tree);
        if ((e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
            || (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON2)) {
          TreePath selPath = myTree.getPathForLocation(e.getX(), e.getY());
          navigateTo(selPath);
        }
        else
          // todo reimplement with PopupHandler.installPopupHandler(tree, group, "CssXFire", ActionManager.getInstance());
          if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON3) {
            TreePath selPath = myTree.getPathForLocation(e.getX(), e.getY());
            Point point = new Point(e.getXOnScreen(), e.getYOnScreen());
            showMenu(selPath, point);
          }
      }
    });
    myTree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(@NotNull KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          TreePath path = myTree.getSelectionPath();
          navigateTo(path);
        }
      }
    });

    JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    buttonsPanel.setBorder(IdeBorderFactory.createBorder(SideBorder.TOP));

    myCancelButton = createButton("&Cancel changes");
    myCancelButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent event) {
        clearTree();
      }
    });
    myApplyButton = createButton("&Apply changes");
    myApplyButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent event) {
        applyPending();
      }
    });
    buttonsPanel.add(myCancelButton);
    buttonsPanel.add(myApplyButton);

    JPanel content = new JPanel(new BorderLayout());
    content.add(ScrollPaneFactory.createScrollPane(myTree), BorderLayout.CENTER);
    content.add(buttonsPanel, BorderLayout.SOUTH);
    setContent(content);
    setToolbar(createToolbar());

    myTreeModel.addTreeModelListener(new TreeModelListener() {
      @Override
      public void treeNodesChanged(TreeModelEvent e) {
        updateButtons();
      }

      @Override
      public void treeNodesInserted(TreeModelEvent e) {
        updateButtons();
      }

      @Override
      public void treeNodesRemoved(TreeModelEvent e) {
        updateButtons();
      }

      @Override
      public void treeStructureChanged(TreeModelEvent e) {
        updateButtons();
      }

      private void updateButtons() {
        boolean hasPendingChanges = ((CssTreeNode)myTreeModel.getRoot()).getChildCount() > 0;
        myApplyButton.setEnabled(hasPendingChanges);
        myCancelButton.setEnabled(hasPendingChanges);
      }
    });
  }

  private JButton createButton(@NotNull  String name) {
    JButton button = new JButton(UIUtil.replaceMnemonicAmpersand(name));
    DumbService.getInstance(myProject).makeDumbAware(button, this);
    DialogUtil.registerMnemonic(button);
    button.setEnabled(false);
    return button;
  }

  public void dispose() {
    UIUtil.dispose(myTree);
    UIUtil.dispose(myCancelButton);
    UIUtil.dispose(myApplyButton);
  }

  private JComponent createToolbar() {
    DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(new AnAction("Help", "Show the CSS-X-Fire help page", AllIcons.Actions.Help) {
      @Override
      public void actionPerformed(AnActionEvent anActionEvent) {
        BrowserUtil.browse("http://localhost:6776/files/about.html");
      }
    });
    actionGroup.addSeparator();

    OccurenceNavigator occurenceNavigator = new MyOccurenceNavigator(myTree, myTreeModel);
    actionGroup.add(new PreviousOccurenceToolbarAction(occurenceNavigator));
    actionGroup.add(new NextOccurenceToolbarAction(occurenceNavigator));
    actionGroup.addSeparator();

    TreeExpander expander = new DefaultTreeExpander(myTree);
    CommonActionsManager actionsManager = CommonActionsManager.getInstance();
    actionGroup.add(new AutoExpandToggleAction());
    actionGroup.add(actionsManager.createExpandAllAction(expander, myTree));
    actionGroup.add(actionsManager.createCollapseAllAction(expander, myTree));
    actionGroup.addSeparator();
    actionGroup.add(new ShowSettingsAction());

    return ActionManager.getInstance().createActionToolbar(IncomingChangesComponent.TOOLWINDOW_ID, actionGroup, false).getComponent();
  }

  public CssChangesTreeModel getTreeModel() {
    return myTreeModel;
  }

  private static void navigateTo(@Nullable TreePath path) {
    if (path != null) {
      Object source = path.getLastPathComponent();
      if (source instanceof Navigatable) {
        ((Navigatable)source).navigate();
      }
    }
  }

  private void showMenu(@Nullable TreePath path, @NotNull Point point) {
    if (path != null) {
      myTree.setSelectionPath(path);
      Object source = path.getLastPathComponent();
      ActionGroup actionGroup = source instanceof CssTreeNode ? ((CssTreeNode)source).getActionGroup() : null;

      if (actionGroup != null) {
        ListPopup listPopup = JBPopupFactory.getInstance().createActionGroupPopup(null,
                                                                                  actionGroup,
                                                                                  createDataContext(),
                                                                                  JBPopupFactory.ActionSelectionAid.MNEMONICS,
                                                                                  true,
                                                                                  IncomingChangesComponent.TOOLWINDOW_ID);

        listPopup.showInScreenCoordinates(myTree, point);
      }
    }
  }

  private DataContext createDataContext() {
    return DataManager.getInstance().getDataContext(myTree);
  }

  public void clearTree() {
    CssTreeNode root = (CssTreeNode)myTreeModel.getRoot();
    root.removeAllChildren();
    myTreeModel.nodeStructureChanged(root);
  }

  private void deleteNode(CssTreeNode node) {
    CssTreeNode parent = (CssTreeNode)node.getParent();
    if (parent != null) {
      int index = parent.getIndex(node);
      parent.remove(node);
      myTreeModel.nodesWereRemoved(parent, new int[]{index}, new CssTreeNode[]{node});
      if (node instanceof CssDeclarationNode) {
        // notify that file node is changed (update the number of changes in file)
        myTreeModel.nodeChanged(parent.getParent());
      }
      if (parent.getChildCount() == 0) {
        deleteNode(parent);
      }
    }
  }

  private void executeCommand(final Runnable command) {
    CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
      public void run() {
        ApplicationManager.getApplication().runWriteAction(command);
      }
    }, "Apply CSS", "CSS");

    FileDocumentManager.getInstance().saveAllDocuments();
  }

  private void applyPending() {
    executeCommand(new Runnable() {
      public void run() {
        CssTreeNode root = (CssTreeNode)myTreeModel.getRoot();
        CssTreeNode leaf;

        while (!(leaf = (CssTreeNode)root.getFirstLeaf()).isRoot()) {
          if (leaf instanceof CssDeclarationNode) {
            CssDeclarationNode declarationNode = (CssDeclarationNode)leaf;
            declarationNode.applyToCode();
          }
          myTreeModel.removeNodeFromParent(leaf);
        }

        myTreeModel.nodeStructureChanged(root);
      }
    });
  }


  //
  // TreeViewModel
  //

  public void applySelectedNode() {
    TreePath selectedPath = myTree.getSelectionPath();
    if (selectedPath == null) {
      return;
    }
    Object source = selectedPath.getLastPathComponent();
    if (source instanceof CssDirectoryNode || source instanceof CssFileNode || source instanceof CssSelectorNode) {
      final Collection<CssDeclarationNode> declarations = new ArrayList<CssDeclarationNode>();
      for (CssTreeNode leaf : TreeUtils.iterateLeafs((CssTreeNode)source)) {
        if (leaf instanceof CssDeclarationNode) {
          declarations.add((CssDeclarationNode)leaf);
        }
      }
      executeCommand(new Runnable() {
        public void run() {
          for (CssDeclarationNode declarationNode : declarations) {
            declarationNode.applyToCode();
            deleteNode(declarationNode);
          }
        }
      });
    }
    else if (source instanceof CssDeclarationNode) {
      final CssDeclarationNode declarationNode = (CssDeclarationNode)source;
      executeCommand(new Runnable() {
        public void run() {
          declarationNode.applyToCode();
          deleteSelectedNode();
        }
      });
    }
  }

  public void deleteSelectedNode() {
    TreePath selectedPath = myTree.getSelectionPath();
    if (selectedPath == null) {
      return;
    }
    Object source = selectedPath.getLastPathComponent();
    if (source instanceof CssDirectoryNode || source instanceof CssFileNode || source instanceof CssSelectorNode) {
      final Collection<CssDeclarationNode> declarations = new ArrayList<CssDeclarationNode>();
      for (CssTreeNode leaf : TreeUtils.iterateLeafs((CssTreeNode)source)) {
        if (leaf instanceof CssDeclarationNode) {
          declarations.add((CssDeclarationNode)leaf);
        }
      }
      for (CssDeclarationNode declarationNode : declarations) {
        deleteNode(declarationNode);
      }
    }
    else if (source instanceof CssDeclarationNode) {
      deleteNode((CssDeclarationNode)source);
    }
  }

  public void expandAll() {
    for (CssTreeNode node : TreeUtils.iterateLeafs((CssTreeNode)myTreeModel.getRoot())) {
      myTree.expandPath(new TreePath(((CssTreeNode)node.getParent()).getPath()));
    }
  }

  public void refreshLeafs() {
    for (CssTreeNode node : TreeUtils.iterateLeafs((CssTreeNode)myTreeModel.getRoot())) {
      myTreeModel.nodeChanged(node);
    }
  }

  private static class ShowSettingsAction extends AnAction {
    public ShowSettingsAction() {
      super("Show Settings", "Edit CSS-X-Fire Settings", AllIcons.General.Settings);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      Project project = e.getRequiredData(CommonDataKeys.PROJECT);
      ShowSettingsUtil.getInstance().editConfigurable(project, new CssXFireConfigurable(project));
    }

    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setEnabled(e.getProject() != null);
    }
  }

  private static class AutoExpandToggleAction extends ToggleAction {
    public AutoExpandToggleAction() {
      super("Auto Expand", "Expand entire tree on every incoming change", AllIcons.Actions.ShowAsTree);
    }

    @Nullable
    protected CssXFireSettings getProjectSettings(AnActionEvent event) {
      return CssXFireSettings.getInstance(event.getRequiredData(CommonDataKeys.PROJECT));
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

    @Override
    public void update(@NotNull AnActionEvent e) {
      super.update(e);
      e.getPresentation().setEnabled(e.getProject() != null);
    }
  }

  private static class MyTreeCellRenderer extends NodeRenderer {
    @Override
    public void customizeCellRenderer(JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
      super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
      if (value instanceof CssTreeNode) {
        CssTreeNode cssTreeNode = (CssTreeNode)value;
        setIcon(cssTreeNode.getIcon());
        append(cssTreeNode.getText(), cssTreeNode.getTextAttributes());
      }
    }
  }

  private class MyOccurenceNavigator implements OccurenceNavigator {
    private final JTree myTree;
    private final CssChangesTreeModel myTreeModel;

    private MyOccurenceNavigator(@NotNull JTree tree, @NotNull CssChangesTreeModel model) {
      myTree = tree;
      myTreeModel = model;
    }

    @Override
    public boolean hasPreviousOccurence() {
      int leafCount = TreeUtils.countLeafs((CssTreeNode)myTreeModel.getRoot());
      if (leafCount == 0) return false;
      if (leafCount == 1) {
        return false;
      }
      return true;
    }

    @Override
    public boolean hasNextOccurence() {
      int leafCount = TreeUtils.countLeafs((CssTreeNode)CssToolWindow.this.myTreeModel.getRoot());
      if (leafCount == 0) return false;
      if (leafCount == 1) {
        TreePath selectionPath = myTree.getSelectionPath();
        return selectionPath == null || !(selectionPath.getLastPathComponent() instanceof CssDeclarationNode);
      }
      return true;
    }

    @Override
    public OccurenceInfo goNextOccurence() {
      return goOccurence(true);
    }

    @Override
    public OccurenceInfo goPreviousOccurence() {
      return goOccurence(false);
    }

    private OccurenceInfo goOccurence(boolean next) {
      CssTreeNode root = (CssTreeNode)CssToolWindow.this.myTreeModel.getRoot();
      TreePath selectionPath = myTree.getSelectionPath();
      CssTreeNode anchor = selectionPath == null ? null : (CssTreeNode)selectionPath.getLastPathComponent();
      CssDeclarationNode declarationNode = TreeUtils.seek(root, anchor, next ? 1 : -1);

      if (declarationNode != null) {
        TreePath path = new TreePath(declarationNode.getPath());
        myTree.getSelectionModel().setSelectionPath(path);
        navigateTo(path);
      }
      return null;
    }

    @Override
    public String getNextOccurenceActionName() {
      return "Next change";
    }

    @Override
    public String getPreviousOccurenceActionName() {
      return "Previous change";
    }
  }
}
