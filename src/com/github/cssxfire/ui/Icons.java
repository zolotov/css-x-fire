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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public interface Icons {
  Icon FIREFOX_16 = AllIcons.Xml.Browsers.Firefox16;
  Icon FIREBUG_16 = IconLoader.findIcon("/com/github/cssxfire/images/firebug16.png");
  Icon TRASHCAN = IconLoader.findIcon("/com/github/cssxfire/images/gc.png");
  Icon COMMIT = IconLoader.findIcon("/com/github/cssxfire/images/commit.png");
  Icon UP = IconLoader.findIcon("/com/github/cssxfire/images/up.png");
  Icon DOWN = IconLoader.findIcon("/com/github/cssxfire/images/down.png");
  Icon EXPAND_ALL = IconLoader.findIcon("/com/github/cssxfire/images/expandall.png");
  Icon COLLAPSE_ALL = IconLoader.findIcon("/com/github/cssxfire/images/collapseall.png");
  Icon HELP = IconLoader.findIcon("/com/github/cssxfire/images/help.png");
  Icon SETTINGS = IconLoader.findIcon("/com/github/cssxfire/images/applicationSettings.png");
}
