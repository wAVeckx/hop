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

package org.apache.hop.projects.project;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.logging.LogChannel;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.DescribedVariable;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.core.vfs.HopVfs;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.projects.config.ProjectsConfig;
import org.apache.hop.projects.config.ProjectsConfigSingleton;
import org.apache.hop.projects.util.ProjectsUtil;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.dialog.MessageBox;
import org.apache.hop.ui.core.gui.GuiResource;
import org.apache.hop.ui.core.gui.WindowProperty;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.ComboVar;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.core.widget.TextVar;
import org.apache.hop.ui.hopgui.HopGui;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class ProjectDialog extends Dialog {
  private static final Class<?> PKG = ProjectDialog.class;
  public static final String CONST_PROJECT = "Project '";

  private final Project project;
  private final ProjectConfig projectConfig;

  private String returnValue;

  private Shell shell;
  private final PropsUi props;

  private Text wName;
  private TextVar wHome;
  private ComboVar wParentProject;
  private TextVar wConfigFile;
  private Text wDescription;
  private Text wCompany;
  private Text wDepartment;

  private TextVar wMetadataBaseFolder;
  private TextVar wUnitTestsBasePath;
  private TextVar wDataSetCsvFolder;
  private Button wEnforceHomeExecution;
  private TableView wVariables;

  private final IVariables variables;
  private boolean needingProjectRefresh;

  private final Boolean editMode;

  public ProjectDialog(
      Shell parent,
      Project project,
      ProjectConfig projectConfig,
      IVariables variables,
      Boolean editMode) {
    super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);

    this.project = project;
    this.projectConfig = projectConfig;
    this.editMode = editMode;

    props = PropsUi.getInstance();

    this.variables = new Variables();
    this.variables.initializeFrom(null);
    try {
      project.modifyVariables(variables, projectConfig, Collections.emptyList(), null);
    } catch (Exception e) {
      new ErrorDialog(
          parent,
          BaseMessages.getString(PKG, "ProjectDialog.ProjectDefinitionError.Error.Dialog.Header"),
          BaseMessages.getString(PKG, "ProjectDialog.ProjectDefinitionError.Error.Dialog.Message"),
          e);
    }
  }

  public String open() {

    Shell parent = getParent();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
    shell.setImage(GuiResource.getInstance().getImageHopUi());
    PropsUi.setLook(shell);

    int margin = PropsUi.getMargin() + 2;
    int middle = props.getMiddlePct();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = PropsUi.getFormMargin();
    formLayout.marginHeight = PropsUi.getFormMargin();

    shell.setLayout(new FormLayout());
    shell.setText(BaseMessages.getString(PKG, "ProjectDialog.Shell.Name"));

    // Buttons go at the bottom of the dialog
    //
    Button wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wOk.addListener(SWT.Selection, event -> ok());
    Button wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
    wCancel.addListener(SWT.Selection, event -> cancel());
    BaseTransformDialog.positionBottomButtons(shell, new Button[] {wOk, wCancel}, margin * 3, null);

    ScrolledComposite scroll = new ScrolledComposite(shell, SWT.V_SCROLL);
    scroll.setLayout(new FillLayout());
    scroll.setExpandHorizontal(true);
    scroll.setExpandVertical(true);
    scroll.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_BLACK));
    PropsUi.setLook(scroll);
    shell.setLayoutData(scroll);

    FormData fd = new FormData();
    fd.left = new FormAttachment(0, 0);
    fd.right = new FormAttachment(100, 0);
    fd.top = new FormAttachment(0, 0);
    fd.bottom = new FormAttachment(wOk, 0);
    scroll.setLayoutData(fd);

    Composite comp = new Composite(scroll, SWT.NONE);
    comp.setLayout(new FormLayout());
    PropsUi.setLook(comp);

    Label wlName = new Label(comp, SWT.RIGHT);
    PropsUi.setLook(wlName);
    wlName.setText(BaseMessages.getString(PKG, "ProjectDialog.Label.ProjectName"));
    FormData fdlName = new FormData();
    fdlName.left = new FormAttachment(0, 0);
    fdlName.right = new FormAttachment(middle, 0);
    fdlName.top = new FormAttachment(0, margin * 2);
    wlName.setLayoutData(fdlName);
    wName = new Text(comp, SWT.SINGLE | SWT.BORDER | SWT.LEFT);
    PropsUi.setLook(wName);
    FormData fdName = new FormData();
    fdName.left = new FormAttachment(middle, margin);
    fdName.right = new FormAttachment(99, 0);
    fdName.top = new FormAttachment(wlName, 0, SWT.CENTER);
    wName.setLayoutData(fdName);
    Control lastControl = wName;

    Label wlHome = new Label(comp, SWT.RIGHT);
    PropsUi.setLook(wlHome);
    wlHome.setText(BaseMessages.getString(PKG, "ProjectDialog.Label.HomeFolder"));
    FormData fdlHome = new FormData();
    fdlHome.left = new FormAttachment(0, 0);
    fdlHome.right = new FormAttachment(middle, 0);
    fdlHome.top = new FormAttachment(lastControl, margin);
    wlHome.setLayoutData(fdlHome);
    Button wbHome = new Button(comp, SWT.PUSH);
    PropsUi.setLook(wbHome);
    wbHome.setText(BaseMessages.getString(PKG, "ProjectDialog.Button.Browse"));
    FormData fdbHome = new FormData();
    fdbHome.right = new FormAttachment(99, 0);
    fdbHome.top = new FormAttachment(wlHome, 0, SWT.CENTER);
    wbHome.setLayoutData(fdbHome);
    wbHome.addListener(SWT.Selection, this::browseHomeFolder);
    wHome = new TextVar(variables, comp, SWT.SINGLE | SWT.BORDER | SWT.LEFT);
    PropsUi.setLook(wHome);
    FormData fdHome = new FormData();
    fdHome.left = new FormAttachment(middle, margin);
    fdHome.right = new FormAttachment(wbHome, -margin);
    fdHome.top = new FormAttachment(wlHome, 0, SWT.CENTER);
    wHome.setLayoutData(fdHome);
    lastControl = wHome;

    Label wlConfigFile = new Label(comp, SWT.RIGHT);
    PropsUi.setLook(wlConfigFile);
    wlConfigFile.setText(BaseMessages.getString(PKG, "ProjectDialog.Label.ConfigurationFile"));
    FormData fdlConfigFile = new FormData();
    fdlConfigFile.left = new FormAttachment(0, 0);
    fdlConfigFile.right = new FormAttachment(middle, 0);
    fdlConfigFile.top = new FormAttachment(lastControl, margin);
    wlConfigFile.setLayoutData(fdlConfigFile);
    Button wbConfigFile = new Button(comp, SWT.PUSH);
    PropsUi.setLook(wbConfigFile);
    wbConfigFile.setText(BaseMessages.getString(PKG, "ProjectDialog.Button.Browse"));
    FormData fdbConfigFile = new FormData();
    fdbConfigFile.right = new FormAttachment(99, 0);
    fdbConfigFile.top = new FormAttachment(wlConfigFile, 0, SWT.CENTER);
    wbConfigFile.setLayoutData(fdbConfigFile);
    wbConfigFile.addListener(SWT.Selection, this::browseConfigFolder);
    wConfigFile = new TextVar(variables, comp, SWT.SINGLE | SWT.BORDER | SWT.LEFT);
    PropsUi.setLook(wConfigFile);
    FormData fdConfigFile = new FormData();
    fdConfigFile.left = new FormAttachment(middle, margin);
    fdConfigFile.right = new FormAttachment(wbConfigFile, -margin);
    fdConfigFile.top = new FormAttachment(wlConfigFile, 0, SWT.CENTER);
    wConfigFile.setLayoutData(fdConfigFile);
    lastControl = wConfigFile;

    Label wlParentProject = new Label(comp, SWT.RIGHT);
    PropsUi.setLook(wlParentProject);
    wlParentProject.setText(BaseMessages.getString(PKG, "ProjectDialog.Label.ParentProject"));
    FormData fdlParentProject = new FormData();
    fdlParentProject.left = new FormAttachment(0, 0);
    fdlParentProject.right = new FormAttachment(middle, 0);
    fdlParentProject.top = new FormAttachment(lastControl, margin);
    wlParentProject.setLayoutData(fdlParentProject);
    wParentProject = new ComboVar(variables, comp, SWT.SINGLE | SWT.BORDER | SWT.LEFT);
    PropsUi.setLook(wParentProject);
    FormData fdParentProject = new FormData();
    fdParentProject.left = new FormAttachment(middle, margin);
    fdParentProject.right = new FormAttachment(99, 0);
    fdParentProject.top = new FormAttachment(wlParentProject, 0, SWT.CENTER);
    wParentProject.setLayoutData(fdParentProject);
    lastControl = wParentProject;

    Label wlDescription = new Label(comp, SWT.RIGHT);
    PropsUi.setLook(wlDescription);
    wlDescription.setText(BaseMessages.getString(PKG, "ProjectDialog.Label.Description"));
    FormData fdlDescription = new FormData();
    fdlDescription.left = new FormAttachment(0, 0);
    fdlDescription.right = new FormAttachment(middle, 0);
    fdlDescription.top = new FormAttachment(lastControl, margin);
    wlDescription.setLayoutData(fdlDescription);
    wDescription = new Text(comp, SWT.SINGLE | SWT.BORDER | SWT.LEFT);
    PropsUi.setLook(wDescription);
    FormData fdDescription = new FormData();
    fdDescription.left = new FormAttachment(middle, margin);
    fdDescription.right = new FormAttachment(99, 0);
    fdDescription.top = new FormAttachment(wlDescription, 0, SWT.CENTER);
    wDescription.setLayoutData(fdDescription);
    lastControl = wDescription;

    Label wlCompany = new Label(comp, SWT.RIGHT);
    PropsUi.setLook(wlCompany);
    wlCompany.setText(BaseMessages.getString(PKG, "ProjectDialog.Label.Company"));
    FormData fdlCompany = new FormData();
    fdlCompany.left = new FormAttachment(0, 0);
    fdlCompany.right = new FormAttachment(middle, 0);
    fdlCompany.top = new FormAttachment(lastControl, margin);
    wlCompany.setLayoutData(fdlCompany);
    wCompany = new Text(comp, SWT.SINGLE | SWT.BORDER | SWT.LEFT);
    PropsUi.setLook(wCompany);
    FormData fdCompany = new FormData();
    fdCompany.left = new FormAttachment(middle, margin);
    fdCompany.right = new FormAttachment(99, 0);
    fdCompany.top = new FormAttachment(wlCompany, 0, SWT.CENTER);
    wCompany.setLayoutData(fdCompany);
    lastControl = wCompany;

    Label wlDepartment = new Label(comp, SWT.RIGHT);
    PropsUi.setLook(wlDepartment);
    wlDepartment.setText(BaseMessages.getString(PKG, "ProjectDialog.Label.Department"));
    FormData fdlDepartment = new FormData();
    fdlDepartment.left = new FormAttachment(0, 0);
    fdlDepartment.right = new FormAttachment(middle, 0);
    fdlDepartment.top = new FormAttachment(lastControl, margin);
    wlDepartment.setLayoutData(fdlDepartment);
    wDepartment = new Text(comp, SWT.SINGLE | SWT.BORDER | SWT.LEFT);
    PropsUi.setLook(wDepartment);
    FormData fdDepartment = new FormData();
    fdDepartment.left = new FormAttachment(middle, margin);
    fdDepartment.right = new FormAttachment(99, 0);
    fdDepartment.top = new FormAttachment(wlDepartment, 0, SWT.CENTER);
    wDepartment.setLayoutData(fdDepartment);
    lastControl = wDepartment;

    Label wlMetadataBaseFolder = new Label(comp, SWT.RIGHT);
    PropsUi.setLook(wlMetadataBaseFolder);
    wlMetadataBaseFolder.setText(
        BaseMessages.getString(PKG, "ProjectDialog.Label.MetadataBaseFolder"));
    FormData fdlMetadataBaseFolder = new FormData();
    fdlMetadataBaseFolder.left = new FormAttachment(0, 0);
    fdlMetadataBaseFolder.right = new FormAttachment(middle, 0);
    fdlMetadataBaseFolder.top = new FormAttachment(lastControl, margin);
    wlMetadataBaseFolder.setLayoutData(fdlMetadataBaseFolder);
    wMetadataBaseFolder = new TextVar(variables, comp, SWT.SINGLE | SWT.BORDER | SWT.LEFT);
    PropsUi.setLook(wMetadataBaseFolder);
    FormData fdMetadataBaseFolder = new FormData();
    fdMetadataBaseFolder.left = new FormAttachment(middle, margin);
    fdMetadataBaseFolder.right = new FormAttachment(99, 0);
    fdMetadataBaseFolder.top = new FormAttachment(wlMetadataBaseFolder, 0, SWT.CENTER);
    wMetadataBaseFolder.setLayoutData(fdMetadataBaseFolder);
    wMetadataBaseFolder.addModifyListener(e -> updateIVariables());
    lastControl = wMetadataBaseFolder;

    Label wlUnitTestsBasePath = new Label(comp, SWT.RIGHT);
    PropsUi.setLook(wlUnitTestsBasePath);
    wlUnitTestsBasePath.setText(
        BaseMessages.getString(PKG, "ProjectDialog.Label.UnitTestBaseFolder"));
    FormData fdlUnitTestsBasePath = new FormData();
    fdlUnitTestsBasePath.left = new FormAttachment(0, 0);
    fdlUnitTestsBasePath.right = new FormAttachment(middle, 0);
    fdlUnitTestsBasePath.top = new FormAttachment(lastControl, margin);
    wlUnitTestsBasePath.setLayoutData(fdlUnitTestsBasePath);
    wUnitTestsBasePath = new TextVar(variables, comp, SWT.SINGLE | SWT.BORDER | SWT.LEFT);
    PropsUi.setLook(wUnitTestsBasePath);
    FormData fdUnitTestsBasePath = new FormData();
    fdUnitTestsBasePath.left = new FormAttachment(middle, margin);
    fdUnitTestsBasePath.right = new FormAttachment(99, 0);
    fdUnitTestsBasePath.top = new FormAttachment(wlUnitTestsBasePath, 0, SWT.CENTER);
    wUnitTestsBasePath.setLayoutData(fdUnitTestsBasePath);
    wUnitTestsBasePath.addModifyListener(e -> updateIVariables());
    lastControl = wUnitTestsBasePath;

    Label wlDataSetCsvFolder = new Label(comp, SWT.RIGHT);
    PropsUi.setLook(wlDataSetCsvFolder);
    wlDataSetCsvFolder.setText(BaseMessages.getString(PKG, "ProjectDialog.Label.DatasetCSVFolder"));
    FormData fdlDataSetCsvFolder = new FormData();
    fdlDataSetCsvFolder.left = new FormAttachment(0, 0);
    fdlDataSetCsvFolder.right = new FormAttachment(middle, 0);
    fdlDataSetCsvFolder.top = new FormAttachment(lastControl, margin);
    wlDataSetCsvFolder.setLayoutData(fdlDataSetCsvFolder);
    wDataSetCsvFolder = new TextVar(variables, comp, SWT.SINGLE | SWT.BORDER | SWT.LEFT);
    PropsUi.setLook(wDataSetCsvFolder);
    FormData fdDataSetCsvFolder = new FormData();
    fdDataSetCsvFolder.left = new FormAttachment(middle, margin);
    fdDataSetCsvFolder.right = new FormAttachment(99, 0);
    fdDataSetCsvFolder.top = new FormAttachment(wlDataSetCsvFolder, 0, SWT.CENTER);
    wDataSetCsvFolder.setLayoutData(fdDataSetCsvFolder);
    wDataSetCsvFolder.addModifyListener(e -> updateIVariables());
    lastControl = wDataSetCsvFolder;

    Label wlEnforceHomeExecution = new Label(comp, SWT.RIGHT);
    PropsUi.setLook(wlEnforceHomeExecution);
    wlEnforceHomeExecution.setText(
        BaseMessages.getString(PKG, "ProjectDialog.Label.EnforceExecutionInHome"));
    FormData fdlEnforceHomeExecution = new FormData();
    fdlEnforceHomeExecution.left = new FormAttachment(0, 0);
    fdlEnforceHomeExecution.right = new FormAttachment(middle, 0);
    fdlEnforceHomeExecution.top = new FormAttachment(lastControl, margin);
    wlEnforceHomeExecution.setLayoutData(fdlEnforceHomeExecution);
    wEnforceHomeExecution = new Button(comp, SWT.CHECK | SWT.LEFT);
    PropsUi.setLook(wEnforceHomeExecution);
    FormData fdEnforceHomeExecution = new FormData();
    fdEnforceHomeExecution.left = new FormAttachment(middle, margin);
    fdEnforceHomeExecution.right = new FormAttachment(99, 0);
    fdEnforceHomeExecution.top = new FormAttachment(wlEnforceHomeExecution, 0, SWT.CENTER);
    wEnforceHomeExecution.setLayoutData(fdEnforceHomeExecution);
    lastControl = wlEnforceHomeExecution;

    Label wlVariables = new Label(comp, SWT.LEFT);
    PropsUi.setLook(wlVariables);
    wlVariables.setText(
        BaseMessages.getString(PKG, "ProjectDialog.Group.Label.ProjectVariablesToSet"));
    FormData fdlVariables = new FormData();
    fdlVariables.left = new FormAttachment(1, 0);
    fdlVariables.right = new FormAttachment(99, 0);
    fdlVariables.top = new FormAttachment(lastControl, 2 * margin);
    wlVariables.setLayoutData(fdlVariables);

    ColumnInfo[] columnInfo =
        new ColumnInfo[] {
          new ColumnInfo(
              BaseMessages.getString(PKG, "ProjectDialog.DetailTable.Label.VariableName"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "ProjectDialog.DetailTable.Label.VariableValue"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false,
              false),
          new ColumnInfo(
              BaseMessages.getString(PKG, "ProjectDialog.DetailTable.Label.VariableDescription"),
              ColumnInfo.COLUMN_TYPE_TEXT,
              false,
              false),
        };
    columnInfo[0].setUsingVariables(true);
    columnInfo[1].setUsingVariables(true);

    wVariables =
        new TableView(
            new Variables(),
            comp,
            SWT.BORDER,
            columnInfo,
            Math.max(project.getDescribedVariables().size(), 3),
            null,
            props);
    PropsUi.setLook(wVariables);
    FormData fdVariables = new FormData();
    fdVariables.left = new FormAttachment(1, 0);
    fdVariables.right = new FormAttachment(99, 0);
    fdVariables.top = new FormAttachment(wlVariables, margin);
    fdVariables.bottom = new FormAttachment(100, -margin * 4);
    fdVariables.width = 300;
    wVariables.setLayoutData(fdVariables);
    wVariables.addModifyListener(e -> needingProjectRefresh = true);

    // See if we need a project refresh/reload
    //
    wParentProject.addModifyListener(e -> needingProjectRefresh = true);
    wHome.addModifyListener(e -> needingProjectRefresh = true);

    getData();

    comp.pack();
    scroll.setContent(comp);
    scroll.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    shell.setMinimumSize(comp.getBounds().width, 200);
    shell.setDefaultButton(wOk);
    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return returnValue;
  }

  private void browseHomeFolder(Event event) {
    String homeFolder = BaseDialog.presentDirectoryDialog(shell, wHome, variables);

    // Set the name to the base folder if the name is empty
    //
    try {
      if (homeFolder != null && StringUtils.isEmpty(wName.getText())) {
        FileObject file = HopVfs.getFileObject(homeFolder);
        wName.setText(Const.NVL(file.getName().getBaseName(), ""));
      }
    } catch (Exception e) {
      LogChannel.UI.logError("Error getting base filename of home folder: " + homeFolder, e);
      // Don't change the name
    }
  }

  private void browseConfigFolder(Event event) {
    String configFileStr = null;
    // Set the root of the possible path to config file to project's root
    String rootPath = wHome.getText();

    File configFile =
        new File(
            wHome.getText()
                + File.separator
                + "config"
                + File.separator
                + ProjectsConfig.DEFAULT_PROJECT_CONFIG_FILENAME);
    wConfigFile.setText(rootPath);

    if (configFile.exists()) {
      configFileStr =
          BaseDialog.presentFileDialog(
              shell,
              wConfigFile,
              variables,
              new String[] {"*.json", "*.*"},
              new String[] {
                BaseMessages.getString(PKG, "ProjectDialog.FileList.PrjFiles.Text"),
                BaseMessages.getString(PKG, "ProjectDialog.FileList.AllFiles.Text")
              },
              true);
    } else {
      String configDir = BaseDialog.presentDirectoryDialog(shell, wConfigFile, variables);
      configFileStr =
          (configDir != null ? configDir : "")
              + File.separator
              + ProjectsConfig.DEFAULT_PROJECT_CONFIG_FILENAME;
    }

    // Set the name to the base folder if the name is empty
    //
    if (configFileStr != null) {
      String relativeConfigFile = null;
      if (!configFileStr.startsWith(rootPath)) {
        MessageBox box = new MessageBox(shell, SWT.ICON_QUESTION | SWT.OK);
        box.setText(BaseMessages.getString(PKG, "ProjectGuiPlugin.WrongConfigPath.Dialog.Header"));
        box.setMessage(
            BaseMessages.getString(PKG, "ProjectGuiPlugin.WrongConfigPath.Dialog.Message"));
        box.open();
      } else {
        // Calculate relative path to existing config file
        String tmpConfigFile = StringUtils.difference(rootPath + File.separator, configFileStr);
        relativeConfigFile =
            (tmpConfigFile.startsWith("/") ? tmpConfigFile.substring(1) : tmpConfigFile);
        relativeConfigFile.replace("\\", "/");
      }
      wConfigFile.setText(Const.NVL(relativeConfigFile, ""));
    }
  }

  private void updateIVariables() {
    Project env = new Project();
    ProjectConfig pc = new ProjectConfig();
    try {
      getInfo(env, pc);
      env.modifyVariables(variables, pc, Collections.emptyList(), null);
    } catch (HopException e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "ProjectDialog.ProjectConfigError.Error.Dialog.Header"),
          BaseMessages.getString(PKG, "ProjectDialog.ProjectConfigError.Error.Dialog.Message"),
          e);
    }
  }

  private void ok() {
    try {
      // Do some extra validations to prevent bad data ending up in the projects configuration
      //

      String oriProjectName = projectConfig.getProjectName();
      String oriProjectHome = projectConfig.getProjectHome();

      String homeFolder = wHome.getText();
      boolean projectHomeFolderChanged = this.editMode && !oriProjectHome.equals(homeFolder);

      if (StringUtils.isEmpty(variables.resolve(homeFolder))) {
        throw new HopException("Please specify a home folder for your project");
      }

      // Manage changing in project's home folder
      if (projectHomeFolderChanged) {
        MessageBox box = new MessageBox(shell, SWT.YES | SWT.NO | SWT.ICON_QUESTION);
        box.setText(BaseMessages.getString(PKG, "ProjectDialog.ChangeHome.Dialog.Header"));
        box.setMessage(
            BaseMessages.getString(
                PKG, "ProjectDialog.ChangeHome.Dialog.Message", oriProjectHome, homeFolder));
        int anwser = box.open();
        if ((anwser & SWT.NO) != 0) {
          wHome.setText(oriProjectHome);
          projectHomeFolderChanged = false;
        }
      }

      // If the home folder doesn't exist and project is new aks if want it created
      if (!HopVfs.getFileObject(variables.resolve(homeFolder)).exists()
          && (!this.editMode || projectHomeFolderChanged)) {
        MessageBox box = new MessageBox(shell, SWT.YES | SWT.NO | SWT.ICON_QUESTION);
        box.setText(BaseMessages.getString(PKG, "ProjectDialog.CreateHome.Dialog.Header"));
        box.setMessage(
            BaseMessages.getString(PKG, "ProjectDialog.CreateHome.Dialog.Message", homeFolder));
        int anwser = box.open();
        if ((anwser & SWT.YES) != 0) {
          HopVfs.getFileObject(homeFolder).createFolder();
        }
      }

      // Renaming the project is not supported
      String projectName = wName.getText();
      if (StringUtils.isEmpty(projectName)) {
        throw new HopException("Please give your new project a name");
      }

      if (Utils.isEmpty(wHome.getText())) {
        throw new HopException("Please specify project's home directory path!");
      }

      if (Utils.isEmpty(wConfigFile.getText())) {
        throw new HopException("Please specify project's configuration file relative path!");
      }

      if (wParentProject.getText() != null
          && !wParentProject.getText().isEmpty()
          && projectName.equals(wParentProject.getText())) {
        throw new HopException(
            CONST_PROJECT + projectName + "' cannot be set as a parent project of itself");
      }

      ProjectsConfig prjsCfg = ProjectsConfigSingleton.getConfig();
      List<String> prjs = prjsCfg.listProjectConfigNames();

      // Check if project name is unique otherwise force the user to change it!
      if (StringUtils.isEmpty(oriProjectName)
          || (StringUtils.isNotEmpty(oriProjectName) && !projectName.equals(oriProjectName))) {
        for (String prj : prjs) {
          if (projectName.equals(prj)) {
            throw new HopException(
                CONST_PROJECT + projectName + "' already exists. Project name must be unique!");
          }
        }
      }

      HopGui hopGui = HopGui.getInstance();
      if (wParentProject.getText() != null && !wParentProject.getText().isEmpty()) {

        boolean parentPrjExists = ProjectsUtil.projectExists(wParentProject.getText());
        if (!parentPrjExists)
          throw new HopException(
              CONST_PROJECT
                  + wParentProject.getText()
                  + "' cannot be set as parent project because it does not exists!");

        ProjectConfig parentPrjCfg = prjsCfg.findProjectConfig(wParentProject.getText());
        Project parentPrj = parentPrjCfg.loadProject(hopGui.getVariables());
        if (parentPrj.getParentProjectName() != null
            && parentPrj.getParentProjectName().equals(projectName))
          throw new HopException(
              CONST_PROJECT
                  + projectName
                  + "' cannot reference '"
                  + wParentProject.getText()
                  + "' as parent project because we are going to create a circular reference!");
      }

      // Manage changing in project's home folder
      if (this.editMode && !oriProjectName.equals(projectName)) {
        MessageBox box = new MessageBox(shell, SWT.YES | SWT.NO | SWT.ICON_QUESTION);
        box.setText(BaseMessages.getString(PKG, "ProjectDialog.ChangeProjectName.Dialog.Header"));
        box.setMessage(
            BaseMessages.getString(
                PKG,
                "ProjectDialog.ChangeProjectName.Dialog.Message",
                oriProjectName,
                projectName));
        int anwser = box.open();
        if ((anwser & SWT.NO) != 0) {
          wName.setText(oriProjectName);
        }
      }

      // Change references to project's name if it changed
      if (!oriProjectName.equals(projectName)) {
        List<String> refs = ProjectsUtil.getParentProjectReferences(oriProjectName);

        if (!refs.isEmpty()) {
          ProjectsUtil.changeParentProjectReferences(oriProjectName, projectName);
        }
      }

      getInfo(project, projectConfig);
      returnValue = projectConfig.getProjectName();
      dispose();
    } catch (Exception e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "ProjectDialog.ProjectConfigError.Error.Dialog.Header"),
          BaseMessages.getString(PKG, "ProjectDialog.ProjectConfigError.Error.Dialog.Message"),
          e);
    }
  }

  private void cancel() {
    needingProjectRefresh = false;
    returnValue = null;

    dispose();
  }

  public void dispose() {
    props.setScreen(new WindowProperty(shell));
    shell.dispose();
  }

  private void getData() {
    wName.setText(Const.NVL(projectConfig.getProjectName(), ""));
    wHome.setText(Const.NVL(projectConfig.getProjectHome(), ""));
    wConfigFile.setText(Const.NVL(projectConfig.getConfigFilename(), ""));

    wDescription.setText(Const.NVL(project.getDescription(), ""));
    wCompany.setText(Const.NVL(project.getCompany(), ""));
    wDepartment.setText(Const.NVL(project.getDepartment(), ""));
    wMetadataBaseFolder.setText(Const.NVL(project.getMetadataBaseFolder(), ""));
    wUnitTestsBasePath.setText(Const.NVL(project.getUnitTestsBasePath(), ""));
    wDataSetCsvFolder.setText(Const.NVL(project.getDataSetsCsvFolder(), ""));
    wEnforceHomeExecution.setSelection(project.isEnforcingExecutionInHome());
    for (int i = 0; i < project.getDescribedVariables().size(); i++) {
      DescribedVariable describedVariable = project.getDescribedVariables().get(i);
      TableItem item = wVariables.table.getItem(i);
      item.setText(1, Const.NVL(describedVariable.getName(), ""));
      item.setText(2, Const.NVL(describedVariable.getValue(), ""));
      item.setText(3, Const.NVL(describedVariable.getDescription(), ""));
    }
    wVariables.setRowNums();
    wVariables.optWidth(true);

    // Parent project...
    //
    try {
      wParentProject.setText(Const.NVL(project.getParentProjectName(), ""));

      List<String> names = ProjectsConfigSingleton.getConfig().listProjectConfigNames();
      if (projectConfig.getProjectName() != null) {
        names.remove(projectConfig.getProjectName());
      }
      wParentProject.setItems(names.toArray(new String[0]));
    } catch (Exception e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "ProjectDialog.ProjectList.Error.Dialog.Header"),
          BaseMessages.getString(PKG, "ProjectDialog.ProjectList.Error.Dialog.Message"),
          e);
    }
  }

  private void getInfo(Project project, ProjectConfig projectConfig) throws HopException {

    projectConfig.setProjectName(wName.getText());
    projectConfig.setProjectHome(wHome.getText());
    projectConfig.setConfigFilename(wConfigFile.getText());

    project.setParentProjectName(wParentProject.getText());
    project.setDescription(wDescription.getText());
    project.setCompany(wCompany.getText());
    project.setDepartment(wDepartment.getText());
    project.setMetadataBaseFolder(wMetadataBaseFolder.getText());
    project.setUnitTestsBasePath(wUnitTestsBasePath.getText());
    project.setDataSetsCsvFolder(wDataSetCsvFolder.getText());
    project.setEnforcingExecutionInHome(wEnforceHomeExecution.getSelection());
    project.getDescribedVariables().clear();
    for (int i = 0; i < wVariables.nrNonEmpty(); i++) {
      TableItem item = wVariables.getNonEmpty(i);
      DescribedVariable variable =
          new DescribedVariable(
              item.getText(1), // name
              item.getText(2), // value
              item.getText(3) // description
              );
      project.getDescribedVariables().add(variable);
    }

    // Update the project to the right absolute configuration file
    //
    if (StringUtils.isNotEmpty(projectConfig.getProjectHome())
        && StringUtils.isNotEmpty(projectConfig.configFilename)) {
      project.setConfigFilename(projectConfig.getActualProjectConfigFilename(variables));
    }

    // Check for infinite loops
    //
    project.verifyProjectsChain(projectConfig.getProjectName(), variables);
  }

  /**
   * Gets variablesChanged
   *
   * @return value of variablesChanged
   */
  public boolean isNeedingProjectRefresh() {
    return needingProjectRefresh;
  }

  /**
   * @param needingProjectRefresh The variablesChanged to set
   */
  public void setNeedingProjectRefresh(boolean needingProjectRefresh) {
    this.needingProjectRefresh = needingProjectRefresh;
  }
}
