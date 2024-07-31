/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.ui.core.widget;

import org.apache.commons.lang.StringUtils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.ui.core.ConstUi;
import org.apache.hop.ui.core.FormDataBuilder;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.gui.GuiResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;

public class StyledTextComp extends Composite {
  private static final Class<?> PKG = StyledTextComp.class;

  // Modification for Undo/Redo on Styled Text
  private final Text textWidget;
  private final Menu styledTextPopupmenu;
  private final Composite xParent;
  private Image image;

  public StyledTextComp(IVariables variables, Composite parent, int args) {
    this(variables, parent, args, true, false);
  }

  public StyledTextComp(IVariables variables, Composite parent, int args, boolean varsSensitive) {
    this(variables, parent, args, varsSensitive, false);
  }

  public StyledTextComp(
      IVariables variables,
      Composite parent,
      int args,
      boolean varsSensitive,
      boolean variableIconOnTop) {

    super(parent, SWT.NONE);
    textWidget = new Text(this, args);
    styledTextPopupmenu = new Menu(parent.getShell(), SWT.POP_UP);
    xParent = parent;
    this.setLayout(new FormLayout());

    buildingStyledTextMenu();

    // Default layout without variables
    textWidget.setLayoutData(
        new FormDataBuilder().top().left().right(100, 0).bottom(100, 0).result());

    // Special layout for variables decorator
    if (varsSensitive) {
      textWidget.addKeyListener(new ControlSpaceKeyAdapter(variables, textWidget));
      image = GuiResource.getInstance().getImageVariableMini();
      if (variableIconOnTop) {
        final Label wIcon = new Label(this, SWT.RIGHT);
        PropsUi.setLook(wIcon);
        wIcon.setToolTipText(BaseMessages.getString(PKG, "StyledTextComp.tooltip.InsertVariable"));
        wIcon.setImage(image);
        wIcon.setLayoutData(new FormDataBuilder().top().right(100, 0).result());
        textWidget.setLayoutData(
            new FormDataBuilder()
                .top(new FormAttachment(wIcon, 0, 0))
                .left()
                .right(100, 0)
                .bottom(100, 0)
                .result());
      } else {
        Label controlDecoration = new Label(this, SWT.NONE);
        controlDecoration.setImage(image);
        controlDecoration.setToolTipText(
            BaseMessages.getString(PKG, "StyledTextComp.tooltip.InsertVariable"));
        PropsUi.setLook(controlDecoration);
        controlDecoration.setLayoutData(new FormDataBuilder().top().right(100, 0).result());
        textWidget.setLayoutData(
            new FormDataBuilder()
                .top()
                .left()
                .right(new FormAttachment(controlDecoration, 0, 0))
                .bottom(100, 0)
                .result());
      }
    }
  }

  public String getSelectionText() {
    return textWidget.getSelectionText();
  }

  public String getText() {
    return textWidget.getText();
  }

  public void setText(String text) {
    textWidget.setText(text);
  }

  public void insert(String strInsert) {
    textWidget.insert(strInsert);
  }

  @Override
  public void addListener(int eventType, Listener listener) {
    textWidget.addListener(eventType, listener);
  }

  public void addModifyListener(ModifyListener lsMod) {
    textWidget.addModifyListener(lsMod);
  }

  public void addKeyListener(KeyAdapter keyAdapter) {
    textWidget.addKeyListener(keyAdapter);
  }

  public void addFocusListener(FocusAdapter focusAdapter) {
    textWidget.addFocusListener(focusAdapter);
  }

  public void addMouseListener(MouseAdapter mouseAdapter) {
    textWidget.addMouseListener(mouseAdapter);
  }

  public int getSelectionCount() {
    return textWidget.getSelectionCount();
  }

  public void setSelection(int arg0) {
    textWidget.setSelection(arg0);
  }

  public void setSelection(int arg0, int arg1) {
    textWidget.setSelection(arg0, arg1);
  }

  @Override
  public void setBackground(Color color) {
    super.setBackground(color);
    textWidget.setBackground(color);
  }

  @Override
  public void setForeground(Color color) {
    super.setForeground(color);
    textWidget.setForeground(color);
  }

  @Override
  public void setFont(Font fnt) {
    textWidget.setFont(fnt);
  }

  private void buildingStyledTextMenu() {

    final MenuItem cutItem = new MenuItem(styledTextPopupmenu, SWT.PUSH);
    cutItem.setText(
        OsHelper.customizeMenuitemText(BaseMessages.getString(PKG, "WidgetDialog.Styled.Cut")));
    cutItem.setImage(
        GuiResource.getInstance()
            .getImage("ui/images/cut.svg", ConstUi.SMALL_ICON_SIZE, ConstUi.SMALL_ICON_SIZE));
    cutItem.addListener(SWT.Selection, e -> textWidget.cut());

    final MenuItem copyItem = new MenuItem(styledTextPopupmenu, SWT.PUSH);
    copyItem.setText(
        OsHelper.customizeMenuitemText(BaseMessages.getString(PKG, "WidgetDialog.Styled.Copy")));
    copyItem.setImage(
        GuiResource.getInstance()
            .getImage("ui/images/copy.svg", ConstUi.SMALL_ICON_SIZE, ConstUi.SMALL_ICON_SIZE));
    copyItem.addListener(SWT.Selection, e -> textWidget.copy());

    final MenuItem pasteItem = new MenuItem(styledTextPopupmenu, SWT.PUSH);
    pasteItem.setText(
        OsHelper.customizeMenuitemText(BaseMessages.getString(PKG, "WidgetDialog.Styled.Paste")));
    pasteItem.setImage(
        GuiResource.getInstance()
            .getImage("ui/images/paste.svg", ConstUi.SMALL_ICON_SIZE, ConstUi.SMALL_ICON_SIZE));
    pasteItem.addListener(SWT.Selection, e -> textWidget.paste());

    new MenuItem(styledTextPopupmenu, SWT.SEPARATOR);

    MenuItem selectAllItem = new MenuItem(styledTextPopupmenu, SWT.PUSH);
    selectAllItem.setText(
        OsHelper.customizeMenuitemText(
            BaseMessages.getString(PKG, "WidgetDialog.Styled.SelectAll")));
    selectAllItem.setImage(
        GuiResource.getInstance()
            .getImage(
                "ui/images/select-all.svg", ConstUi.SMALL_ICON_SIZE, ConstUi.SMALL_ICON_SIZE));
    selectAllItem.addListener(SWT.Selection, e -> textWidget.selectAll());

    textWidget.addMenuDetectListener(
        e -> {
          pasteItem.setEnabled(checkPaste());
          if (textWidget.getSelectionCount() > 0) {
            cutItem.setEnabled(true);
            copyItem.setEnabled(true);
          } else {
            cutItem.setEnabled(false);
            copyItem.setEnabled(false);
          }
        });
    textWidget.setMenu(styledTextPopupmenu);
  }

  // Check if something is stored inside the Clipboard
  private boolean checkPaste() {
    try {
      Clipboard clipboard = new Clipboard(xParent.getDisplay());
      TextTransfer transfer = TextTransfer.getInstance();
      String text = (String) clipboard.getContents(transfer);
      if (text != null && text.length() > 0) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      return false;
    }
  }

  public Image getImage() {
    return image;
  }

  public Text getTextWidget() {
    return textWidget;
  }

  public boolean isEditable() {
    return textWidget.getEditable();
  }

  public void setEditable(boolean canEdit) {
    textWidget.setEditable(canEdit);
  }

  @Override
  public void setEnabled(boolean enabled) {
    textWidget.setEnabled(enabled);
    if (Display.getDefault() != null) {
      Color foreground =
          Display.getDefault().getSystemColor(enabled ? SWT.COLOR_BLACK : SWT.COLOR_DARK_GRAY);
      Color background =
          Display.getDefault()
              .getSystemColor(enabled ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_BACKGROUND);
      GuiResource guiResource = GuiResource.getInstance();
      textWidget.setForeground(
          guiResource.getColor(foreground.getRed(), foreground.getGreen(), foreground.getBlue()));
      textWidget.setBackground(
          guiResource.getColor(background.getRed(), background.getGreen(), background.getBlue()));
    }
  }

  /**
   * @return The caret line number, starting from 1.
   */
  public int getLineNumber() {
    String text = textWidget.getText();
    if (StringUtils.isEmpty(text)) {
      return 1;
    }

    int rowNumber = 1;
    int textPosition = textWidget.getCaretPosition();
    while (textPosition > 0) {
      if (text.charAt(textPosition - 1) == '\n') {
        rowNumber++;
      }
      textPosition--;
    }

    return rowNumber;
  }

  /**
   * @return The caret column number, starting from 1.
   */
  public int getColumnNumber() {
    String text = textWidget.getText();
    if (StringUtils.isEmpty(text)) {
      return 1;
    }

    int columnNumber = 1;
    int textPosition = textWidget.getCaretPosition();
    while (textPosition > 0
        && text.charAt(textPosition - 1) != '\n'
        && text.charAt(textPosition - 1) != '\r') {
      textPosition--;
      columnNumber++;
    }

    return columnNumber;
  }
}
