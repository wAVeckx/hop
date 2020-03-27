/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.apache.hop.ui.hopui.trans;

import org.apache.hop.core.Const;
import org.apache.hop.core.Props;
import org.apache.hop.core.RowMetaAndData;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopStepException;
import org.apache.hop.core.extension.ExtensionPointHandler;
import org.apache.hop.core.row.RowMetaInterface;
import org.apache.hop.core.row.ValueMetaInterface;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.i18n.GlobalMessages;
import org.apache.hop.trans.ExecutionAdapter;
import org.apache.hop.trans.Trans;
import org.apache.hop.trans.TransMeta;
import org.apache.hop.trans.engine.IEngine;
import org.apache.hop.trans.engine.IEngineComponent;
import org.apache.hop.trans.step.RowAdapter;
import org.apache.hop.trans.step.StepInterface;
import org.apache.hop.trans.step.StepMeta;
import org.apache.hop.ui.core.PropsUI;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.gui.GUIResource;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.hopui.HopUi;
import org.apache.hop.ui.hopui.XulHopUiSettingsManager;
import org.apache.hop.ui.hopui.delegates.HopUiDelegate;
import org.apache.hop.ui.xul.HopXulLoader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.pentaho.ui.xul.XulDomContainer;
import org.pentaho.ui.xul.XulLoader;
import org.pentaho.ui.xul.containers.XulToolbar;
import org.pentaho.ui.xul.impl.XulEventHandler;
import org.pentaho.ui.xul.swt.tags.SwtRadio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static java.util.stream.Collectors.toList;

public class TransPreviewDelegate extends HopUiDelegate implements XulEventHandler {
  private static Class<?> PKG = HopUi.class; // for i18n purposes, needed by Translator!!

  private static final String XUL_FILE_TRANS_PREVIEW_TOOLBAR = "ui/trans-preview-toolbar.xul";

  private TransGraph transGraph;

  private CTabItem transPreviewTab;

  private XulToolbar toolbar;
  private Composite transPreviewComposite;

  protected Map<String, RowMetaInterface> previewMetaMap;
  protected Map<String, List<RowMetaAndData>> previewDataMap;
  protected Map<String, String> previewLogMap;
  private Composite previewComposite;

  private Text logText;
  private TableView tableView;

  public enum PreviewMode {
    FIRST, LAST, OFF,
  }

  private PreviewMode previewMode;

  private StepMeta selectedStep;
  protected StepMeta lastSelectedStep;
  private SwtRadio firstRadio;
  private SwtRadio lastRadio;
  private SwtRadio offRadio;

  /**
   * @param hopUi
   * @param transGraph
   */
  public TransPreviewDelegate( HopUi hopUi, TransGraph transGraph ) {
    super( hopUi );
    this.transGraph = transGraph;

    previewMetaMap = new HashMap<>();
    previewDataMap = new HashMap<>();
    previewLogMap = new HashMap<>();

    previewMode = PreviewMode.FIRST;
  }

  public void showPreviewView() {

    if ( transPreviewTab == null || transPreviewTab.isDisposed() ) {
      addTransPreview();
    } else {
      transPreviewTab.dispose();

      transGraph.checkEmptyExtraView();
    }
  }

  /**
   * Add a grid with the execution metrics per step in a table view
   */
  public void addTransPreview() {

    // First, see if we need to add the extra view...
    //
    if ( transGraph.extraViewComposite == null || transGraph.extraViewComposite.isDisposed() ) {
      transGraph.addExtraView();
    } else {
      if ( transPreviewTab != null && !transPreviewTab.isDisposed() ) {
        // just set this one active and get out...
        //
        transGraph.extraViewTabFolder.setSelection( transPreviewTab );
        return;
      }
    }

    transPreviewTab = new CTabItem( transGraph.extraViewTabFolder, SWT.NONE );
    transPreviewTab.setImage( GUIResource.getInstance().getImagePreview() );
    transPreviewTab.setText( BaseMessages.getString( PKG, "HopGui.TransGraph.PreviewTab.Name" ) );

    transPreviewComposite = new Composite( transGraph.extraViewTabFolder, SWT.NONE );
    transPreviewComposite.setLayout( new FormLayout() );
    PropsUI.getInstance().setLook( transPreviewComposite, Props.WIDGET_STYLE_TOOLBAR );

    addToolBar();

    Control toolbarControl = (Control) toolbar.getManagedObject();

    toolbarControl.setLayoutData( new FormData() );
    FormData fd = new FormData();
    fd.left = new FormAttachment( 0, 0 ); // First one in the left top corner
    fd.top = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );
    toolbarControl.setLayoutData( fd );

    toolbarControl.setParent( transPreviewComposite );

    previewComposite = new Composite( transPreviewComposite, SWT.NONE );
    previewComposite.setLayout( new FillLayout() );
    FormData fdPreview = new FormData();
    fdPreview.left = new FormAttachment( 0, 0 );
    fdPreview.right = new FormAttachment( 100, 0 );
    if ( Const.isLinux() ) {
      fdPreview.top = new FormAttachment( (Control) toolbar.getManagedObject(), 4 );
    } else {
      fdPreview.top = new FormAttachment( (Control) toolbar.getManagedObject(), 10 );
    }
    fdPreview.bottom = new FormAttachment( 100, 0 );
    previewComposite.setLayoutData( fdPreview );

    transPreviewTab.setControl( transPreviewComposite );

    transGraph.extraViewTabFolder.setSelection( transPreviewTab );

    transGraph.extraViewTabFolder.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent e ) {
        refreshView();
      }
    } );
    TransPreviewExtension extension = new TransPreviewExtension(
      transPreviewComposite, toolbarControl, previewComposite );
    try {
      ExtensionPointHandler.callExtensionPoint( log, "TransPreviewCreated", extension );
    } catch ( HopException ex ) {
      log.logError( "Extension point call failed.", ex );
    }
  }

  private void addToolBar() {

    try {
      XulLoader loader = new HopXulLoader();
      loader.setSettingsManager( XulHopUiSettingsManager.getInstance() );
      ResourceBundle bundle = GlobalMessages.getBundle( "org/apache/hop/ui/hopgui/messages/messages" );
      XulDomContainer xulDomContainer = loader.loadXul( XUL_FILE_TRANS_PREVIEW_TOOLBAR, bundle );
      xulDomContainer.addEventHandler( this );
      toolbar = (XulToolbar) xulDomContainer.getDocumentRoot().getElementById( "nav-toolbar" );
      ToolBar swtToolBar = (ToolBar) toolbar.getManagedObject();
      hopUi.props.setLook( swtToolBar, Props.WIDGET_STYLE_TOOLBAR );
      swtToolBar.layout( true, true );
      swtToolBar.pack();

      firstRadio = (SwtRadio) xulDomContainer.getDocumentRoot().getElementById( "preview-first" );
      lastRadio = (SwtRadio) xulDomContainer.getDocumentRoot().getElementById( "preview-last" );
      offRadio = (SwtRadio) xulDomContainer.getDocumentRoot().getElementById( "preview-off" );

      PropsUI.getInstance().setLook( (Control) firstRadio.getManagedObject(), Props.WIDGET_STYLE_TOOLBAR );
      PropsUI.getInstance().setLook( (Control) lastRadio.getManagedObject(), Props.WIDGET_STYLE_TOOLBAR );
      PropsUI.getInstance().setLook( (Control) offRadio.getManagedObject(), Props.WIDGET_STYLE_TOOLBAR );

    } catch ( Throwable t ) {
      log.logError( toString(), Const.getStackTracker( t ) );
      new ErrorDialog( transPreviewComposite.getShell(),
        BaseMessages.getString( PKG, "HopGui.Exception.ErrorReadingXULFile.Title" ),
        BaseMessages.getString( PKG, "HopGui.Exception.ErrorReadingXULFile.Message", XUL_FILE_TRANS_PREVIEW_TOOLBAR ),
        new Exception( t ) );
    }
  }

  /**
   * This refresh is driven by outside influenced using listeners and so on.
   */
  public synchronized void refreshView() {
    if ( transGraph != null && transGraph.extraViewTabFolder != null ) {
      if ( transGraph.extraViewTabFolder.getSelection() != transPreviewTab ) {
        return;
      }
    }

    if ( previewComposite == null || previewComposite.isDisposed() ) {
      return;
    }

    // Which step do we preview...
    //
    StepMeta stepMeta = selectedStep; // copy to prevent race conditions and so on.
    if ( stepMeta == null ) {
      hidePreviewGrid();
      return;
    } else {
      lastSelectedStep = selectedStep;
    }

    // Do we have a log for this selected step?
    // This means the preview work is still running or it error-ed out.
    //
    boolean errorStep = false;
    if ( transGraph.trans != null ) {
      List<StepInterface> steps = transGraph.trans.findBaseSteps( stepMeta.getName() );
      if ( steps != null && steps.size() > 0 ) {
        errorStep = steps.get( 0 ).getErrors() > 0;
      }
    }

    String logText = previewLogMap.get( stepMeta );
    if ( errorStep && logText != null && logText.length() > 0 ) {
      showLogText( stepMeta, logText );
      return;
    }

    // If the preview work is done we have row meta-data and data for each step.
    //
    RowMetaInterface rowMeta = previewMetaMap.get( stepMeta );
    if ( rowMeta != null ) {
      List<RowMetaAndData> rowData = previewDataMap.get( stepMeta );

      try {
        showPreviewGrid( transGraph.getManagedObject(), stepMeta, rowMeta, rowData );
      } catch ( Exception e ) {
        e.printStackTrace();
        logText += Const.getStackTracker( e );
        showLogText( stepMeta, logText );
      }
    }
  }

  protected void hidePreviewGrid() {
    if ( tableView != null && !tableView.isDisposed() ) {
      tableView.dispose();
    }
  }

  protected void showPreviewGrid( TransMeta transMeta, StepMeta stepMeta, RowMetaInterface rowMeta,
                                  List<RowMetaAndData> rowsData ) throws HopException {
    clearPreviewComposite();

    ColumnInfo[] columnInfo = new ColumnInfo[ rowMeta.size() ];
    for ( int i = 0; i < columnInfo.length; i++ ) {
      ValueMetaInterface valueMeta = rowMeta.getValueMeta( i );
      columnInfo[ i ] = new ColumnInfo( valueMeta.getName(), ColumnInfo.COLUMN_TYPE_TEXT, false, true );
      columnInfo[ i ].setValueMeta( valueMeta );
    }

    tableView =
      new TableView( transMeta, previewComposite, SWT.NONE, columnInfo, rowsData.size(), null, PropsUI
        .getInstance() );

    // Put data on it...
    //
    for ( int rowNr = 0; rowNr < rowsData.size(); rowNr++ ) {
      RowMetaAndData rowMetaAndData = rowsData.get( rowNr );
      RowMetaInterface dataRowMeta = rowMetaAndData.getRowMeta();
      Object[] rowData = rowMetaAndData.getData();
      TableItem item;
      if ( rowNr < tableView.table.getItemCount() ) {
        item = tableView.table.getItem( rowNr );
      } else {
        item = new TableItem( tableView.table, SWT.NONE );
      }
      for ( int colNr = 0; colNr < rowMeta.size(); colNr++ ) {
        int dataIndex = dataRowMeta.indexOfValue( rowMeta.getValueMeta( colNr ).getName() );
        String string;
        ValueMetaInterface valueMetaInterface;
        try {
          valueMetaInterface = dataRowMeta.getValueMeta( dataIndex );
          if ( valueMetaInterface.isStorageBinaryString() ) {
            Object nativeType = valueMetaInterface.convertBinaryStringToNativeType( (byte[]) rowData[ dataIndex ] );
            string = valueMetaInterface.getStorageMetadata().getString( nativeType );
          } else {
            string = dataRowMeta.getString( rowData, dataIndex );
          }
        } catch ( Exception e ) {
          string = "Conversion error: " + e.getMessage();
        }
        if ( string == null ) {
          item.setText( colNr + 1, "<null>" );
          item.setForeground( colNr + 1, GUIResource.getInstance().getColorBlue() );
        } else {
          item.setText( colNr + 1, string );
        }
      }
    }

    tableView.setRowNums();
    tableView.setShowingConversionErrorsInline( true );
    tableView.optWidth( true );

    previewComposite.layout( true, true );
  }

  protected void showLogText( StepMeta stepMeta, String loggingText ) {
    clearPreviewComposite();

    logText = new Text( previewComposite, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL );
    logText.setText( loggingText );

    previewComposite.layout( true, true );
  }

  private void clearPreviewComposite() {
    // First clear out the preview composite, then put in a text field showing the log text
    //
    //
    for ( Control control : previewComposite.getChildren() ) {
      control.dispose();
    }
  }

  public CTabItem getTransGridTab() {
    return transPreviewTab;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.ui.xul.impl.XulEventHandler#getData()
   */
  public Object getData() {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.ui.xul.impl.XulEventHandler#getName()
   */
  public String getName() {
    return "transpreview";
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.ui.xul.impl.XulEventHandler#getXulDomContainer()
   */
  public XulDomContainer getXulDomContainer() {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.ui.xul.impl.XulEventHandler#setData(java.lang.Object)
   */
  public void setData( Object data ) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.ui.xul.impl.XulEventHandler#setName(java.lang.String)
   */
  public void setName( String name ) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.ui.xul.impl.XulEventHandler#setXulDomContainer(org.pentaho. ui.xul.XulDomContainer)
   */
  public void setXulDomContainer( XulDomContainer xulDomContainer ) {
    // TODO Auto-generated method stub

  }

  /**
   * @return the active
   */
  public boolean isActive() {
    return previewMode != PreviewMode.OFF;
  }

  public void setPreviewMode( PreviewMode previewMode ) {
    this.previewMode = previewMode;
  }

  public void capturePreviewData( final Trans trans, List<StepMeta> stepMetas ) {
    final StringBuffer loggingText = new StringBuffer();

    // First clean out previous preview data. Otherwise this method leaks memory like crazy.
    //
    previewLogMap.clear();
    previewMetaMap.clear();
    previewDataMap.clear();

    try {
      final TransMeta transMeta = trans.getTransMeta();

      for ( final StepMeta stepMeta : stepMetas ) {
        String stepname = stepMeta.getName();
        final RowMetaInterface rowMeta = transMeta.getStepFields( stepMeta ).clone();
        previewMetaMap.put( stepname, rowMeta );
        final List<RowMetaAndData> rowsData;
        if ( previewMode == PreviewMode.LAST ) {
          rowsData = new LinkedList<>();
        } else {
          rowsData = new ArrayList<>();
        }

        previewDataMap.put( stepname, rowsData );
        previewLogMap.put( stepname, loggingText.toString() );

        StepInterface step = trans.findRunThread( stepMeta.getName() );

        if ( step != null ) {

          switch ( previewMode ) {
            case LAST:
              step.addRowListener( new RowAdapter() {
                @Override
                public void rowWrittenEvent( RowMetaInterface rowMeta, Object[] row ) throws HopStepException {
                  try {
                    rowsData.add( new RowMetaAndData( rowMeta, rowMeta.cloneRow( row ) ) );
                    if ( rowsData.size() > PropsUI.getInstance().getDefaultPreviewSize() ) {
                      rowsData.remove( 0 );
                    }
                  } catch ( Exception e ) {
                    throw new HopStepException( "Unable to clone row for metadata : " + rowMeta, e );
                  }
                }
              } );
              break;
            default:
              step.addRowListener( new RowAdapter() {

                @Override
                public void rowWrittenEvent( RowMetaInterface rowMeta, Object[] row ) throws HopStepException {
                  if ( rowsData.size() < PropsUI.getInstance().getDefaultPreviewSize() ) {
                    try {
                      rowsData.add( new RowMetaAndData( rowMeta, rowMeta.cloneRow( row ) ) );
                    } catch ( Exception e ) {
                      throw new HopStepException( "Unable to clone row for metadata : " + rowMeta, e );
                    }
                  }
                }
              } );
              break;
          }
        }

      }
    } catch ( Exception e ) {
      loggingText.append( Const.getStackTracker( e ) );
    }

    // In case there were errors during preview...
    //
    trans.addTransListener( new ExecutionAdapter<TransMeta>() {
      @Override
      public void finished( IEngine<TransMeta> engine ) throws HopException {
        // Copy over the data from the previewDelegate...
        //
        if ( engine.getErrors() != 0 ) {
          // capture logging and store it...
          //
          for ( IEngineComponent component : engine.getComponents() ) {
            if ( component.getCopyNr() == 0 ) {
              previewLogMap.put( component.getName(), component.getLogText() );
            }
          }
        }
      }
    } );
  }

  public void addPreviewData( String stepname, RowMetaInterface rowMeta, List<Object[]> rowsData,
                              StringBuffer buffer ) {
    previewLogMap.put( stepname, buffer.toString() );
    previewMetaMap.put( stepname, rowMeta );
    List<RowMetaAndData> rowsMetaAndData =
      rowsData.stream().map( data -> new RowMetaAndData( rowMeta, data ) ).collect( toList() );
    previewDataMap.put( stepname, rowsMetaAndData );
  }

  /**
   * @return the selectedStep
   */
  public StepMeta getSelectedStep() {
    return selectedStep;
  }

  /**
   * @param selectedStep the selectedStep to set
   */
  public void setSelectedStep( StepMeta selectedStep ) {
    this.selectedStep = selectedStep;
  }

  public PreviewMode getPreviewMode() {
    return previewMode;
  }

  public void first() {
    previewMode = PreviewMode.FIRST;
    firstRadio.setSelected( true );
    lastRadio.setSelected( false );
    offRadio.setSelected( false );
  }

  public void last() {
    previewMode = PreviewMode.LAST;
    firstRadio.setSelected( false );
    lastRadio.setSelected( true );
    offRadio.setSelected( false );
  }

  public void off() {
    previewMode = PreviewMode.OFF;
    firstRadio.setSelected( false );
    lastRadio.setSelected( false );
    offRadio.setSelected( true );
  }
}
