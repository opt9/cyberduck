package ch.cyberduck.ui.cocoa;

/*
 *  Copyright (c) 2002 David Kocher. All rights reserved.
 *  http://icu.unizh.ch/~dkocher/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import ch.cyberduck.core.History;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Message;
import ch.cyberduck.core.Queue;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.ftp.FTPSession;
import ch.cyberduck.core.sftp.SFTPSession;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.ftp.FTPPath;
import ch.cyberduck.core.sftp.SFTPPath;
import com.apple.cocoa.application.*;
import com.apple.cocoa.foundation.*;
import org.apache.log4j.Logger;

import java.util.Observable;
import java.util.Observer;

/**
* @version $Id$
*/
public class CDBrowserController implements Observer {
    private static Logger log = Logger.getLogger(CDBrowserController.class);
    
    // ----------------------------------------------------------
    // Outlets
    // ----------------------------------------------------------
    
    private NSWindow mainWindow; // IBOutlet
    public void setMainWindow(NSWindow mainWindow) {
	this.mainWindow = mainWindow;
//	this.mainWindow.setDelegate(this);
    }

//    private CDBrowserView browserTable; // IBOutlet
//    public void setBrowserTable(CDBrowserView browserTable) {
//	this.browserTable = browserTable;
  //  }

    private NSTableView browserTable; // IBOutlet
    public void setBrowserTable(NSTableView browserTable) {
	this.browserTable = browserTable;
    }

    private CDBrowserTableDataSource browserModel;
    
    private NSTextField quickConnectField; // IBOutlet
    public void setQuickConnectField(NSTextField quickConnectField) {
	this.quickConnectField = quickConnectField;
    }
    
    private NSPopUpButton pathPopup; // IBOutlet
    public void setPathPopup(NSPopUpButton pathPopup) {
	this.pathPopup = pathPopup;
    }

    private NSDrawer drawer; // IBOutlet
    public void setDrawer(NSDrawer drawer) {
	this.drawer = drawer;
    }

    private NSProgressIndicator progressIndicator; // IBOutlet
    public void setProgressIndicator(NSProgressIndicator progressIndicator) {
	this.progressIndicator = progressIndicator;
	this.progressIndicator.setIndeterminate(true);
	this.progressIndicator.setUsesThreadedAnimation(true);
    }

    private NSTextField statusLabel; // IBOutlet
    public void setStatusLabel(NSTextField statusLabel) {
	this.statusLabel = statusLabel;
    }

//    private NSMutableDictionary toolbarItems;

    /**
	* Keep references of controller objects because otherweise they get garbage collected
     * if not referenced here.
     */
    private NSArray references = new NSArray();
    
    private CDConnectionSheet connectionSheet;
    private CDPathController pathController;

    private NSToolbar toolbar;
    private NSToolbarItem pathItem;
    private NSToolbarItem quickConnectItem;

    /**
     * The host this browser windowis associated with
     */
    private Host host;

    // ----------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------
    
    public CDBrowserController() {
	log.debug("CDBrowserController");
        if (false == NSApplication.loadNibNamed("Browser", this)) {
            log.fatal("Couldn't load Browser.nib");
            return;
        }
	this.init();
    }

    private void init() {
	log.debug("init");

	browserTable.setDataSource(browserModel = new CDBrowserTableDataSource());
	browserTable.setDelegate(this);
	browserTable.registerForDraggedTypes(new NSArray(NSPasteboard.FilenamesPboardType));
	browserTable.setTarget(this);
	browserTable.setDrawsGrid(false);
	browserTable.setAutoresizesAllColumnsToFit(true);
	browserTable.setDoubleAction(new NSSelector("tableViewDidClickTableRow", new Class[] {Object.class}));
	browserTable.tableColumnWithIdentifier("TYPE").setDataCell(new NSImageCell());
	browserTable.setAutosaveTableColumns(true);

	pathController = new CDPathController(pathPopup);
	connectionSheet = new CDConnectionSheet(this);

	this.setupToolbar();
    }

    // ----------------------------------------------------------
    // BrowserTable delegate methods
    // ----------------------------------------------------------

    public void tableViewDidClickTableRow(Object sender) {
	log.debug("tableViewDidClickTableRow");
	if(browserTable.clickedRow() != -1) { //table header clicked
	    Path p = (Path)browserModel.getEntry(browserTable.clickedRow());
	    if(p.isFile()) {
		this.downloadButtonClicked(sender);
	    }
	    if(p.isDirectory())
		p.list();
	}
    }

    public void tableViewDidClickTableColumn(NSTableView tableView, NSTableColumn tableColumn) {
	log.debug("tableViewDidClickTableColumn");
	String identifier = (String)tableColumn.identifier();
	NSArray columns = tableView.tableColumns();
	java.util.Enumeration enumerator = columns.objectEnumerator();
	while (enumerator.hasMoreElements()) {
	    NSTableColumn c = (NSTableColumn)enumerator.nextElement();
	    if(c.identifier()!=identifier)
		tableView.setIndicatorImage(null, c);
	}
	boolean a = true;
	if(tableView.indicatorImage(tableColumn).name().equals("NSAscendingSortIndicator")) {
	    tableView.setIndicatorImage(NSImage.imageNamed("NSDescendingSortIndicator"), tableColumn);
	    a = false;
	}
	else {
	    tableView.setIndicatorImage(NSImage.imageNamed("NSAscendingSortIndicator"), tableColumn);
	}
	final boolean ascending = a;
	final int higher = ascending ? 1 : -1 ;
	final int lower = ascending ? -1 : 1;
	if(tableColumn.identifier().equals("FILENAME")) {
	    java.util.Collections.sort((java.util.List)browserModel.list(),
		      new java.util.Comparator() {
			  public int compare(Object o1, Object o2) {
			      Path p1 = (Path)o1;
			      Path p2 = (Path)o2;
			      if(ascending) {
				  return p1.getName().compareTo(p2.getName());
			      }
			      else {
				  return -p1.getName().compareTo(p2.getName());
			      }
			  }
		      }
		      );
	    browserTable.reloadData();
	}
	else if(tableColumn.identifier().equals("SIZE")) {
	    java.util.Collections.sort((java.util.List)browserModel.list(),
				new java.util.Comparator() {
				    public int compare(Object o1, Object o2) {
					int p1 = ((Path)o1).status.getSize();
					int p2 = ((Path)o2).status.getSize();
					if (p1 > p2) {
					    return lower;
					}
					else if (p1 < p2) {
					    return higher;
					}
					else if (p1 == p2) {
					    return 0;
					}
					return 0;
				    }
				}
				);
	    tableView.reloadData();
	}
    }


    private static final NSColor TABLE_CELL_SHADED_COLOR = NSColor.colorWithCalibratedRGB(0.929f, 0.953f, 0.996f, 1.0f);

    public void tableViewWillDisplayCell(NSTableView view, Object cell, NSTableColumn column, int row) {
	if(cell instanceof NSTextFieldCell) {
	    if (! (view == null || cell == null || column == null)) {
		if (row % 2 == 0) {
		    ((NSTextFieldCell)cell).setDrawsBackground(true);
		    ((NSTextFieldCell)cell).setBackgroundColor(TABLE_CELL_SHADED_COLOR);
		}
		else
		    ((NSTextFieldCell)cell).setBackgroundColor(view.backgroundColor());
	    }
	}
    }

    /**	Returns true to permit aTableView to select the row at rowIndex, false to deny permission.
	* The delegate can implement this method to disallow selection of particular rows.
	*/
    public  boolean tableViewShouldSelectRow( NSTableView aTableView, int rowIndex) {
	return true;
    }


    /**	Returns true to permit aTableView to edit the cell at rowIndex in aTableColumn, false to deny permission.
	*The delegate can implemen this method to disallow editing of specific cells.
	*/
    public boolean tableViewShouldEditLocation( NSTableView view, NSTableColumn tableColumn, int row) {
        String identifier = (String)tableColumn.identifier();
//	if(identifier.equals("FILENAME"))
//	    return true;
	return false;
    }


    // ----------------------------------------------------------
    // 
    // ----------------------------------------------------------
    
    public void finalize() throws Throwable {
	super.finalize();
	log.debug("finalize");
    }

    public NSWindow window() {
	return this.mainWindow;
    }


    public void update(Observable o, Object arg) {
	log.debug("update:"+o+","+arg);
	if(o instanceof Session) {
	    if(arg instanceof Message) {
//		Host host = (Host)o;
		Message msg = (Message)arg;
		if(msg.getTitle().equals(Message.ERROR)) {
		    //public static void beginAlertSheet( String title, String defaultButton, String alternateButton, String otherButton, NSWindow docWindow, Object modalDelegate, NSSelector didEndSelector, NSSelector didDismissSelector, Object contextInfo, String message)
		    NSAlertPanel.beginAlertSheet(
				   "Error", //title
				   "OK",// defaultbutton
				   null,//alternative button
				   null,//other button
				   mainWindow, //docWindow
				   null, //modalDelegate
				   null, //didEndSelector
				   null, // dismiss selector
				   null, // context
				   msg.getDescription() // message
				   );
		    progressIndicator.stopAnimation(this);
		    statusLabel.setStringValue(msg.getDescription());
		}
		// update status label
		else if(msg.getTitle().equals(Message.PROGRESS)) {
		    statusLabel.setStringValue(msg.getDescription());
		}
		else if(msg.getTitle().equals(Message.TRANSCRIPT)) {
		    statusLabel.setStringValue(msg.getDescription());
		}
		else if(msg.getTitle().equals(Message.OPEN)) {
		    browserModel.clear();
		    browserTable.reloadData();

		    progressIndicator.startAnimation(this);
		    mainWindow.setTitle(host.getProtocol()+":"+host.getName());
		    History.instance().add(host);
		}
		else if(msg.getTitle().equals(Message.CLOSE)) {
		    browserModel.clear();
		    browserTable.reloadData();

		    progressIndicator.stopAnimation(this);
		}
	    }
	    if(arg instanceof Path) {
		java.util.List cache = ((Path)arg).cache();
		java.util.Iterator i = cache.iterator();
//		log.debug("List size:"+cache.size());
		browserModel.clear();
		while(i.hasNext()) {
		    browserModel.addEntry((Path)i.next());
		}
		browserTable.reloadData();
	    }	    
	}
    }
    

    // ----------------------------------------------------------
    // Selector methods for the toolbar items
    // ----------------------------------------------------------

    public void toggleDrawer(Object sender) {
	drawer.toggle(this);
    }
    
    public void folderButtonClicked(Object sender) {
        log.debug("folderButtonClicked");
//	CDFolderSheet sheet = new CDFolderSheet(host.getSession().workdir());
	Path parent = (Path)pathController.getItem(0);
	CDFolderSheet sheet = new CDFolderSheet(parent);
	NSApplication.sharedApplication().beginSheet(
					      sheet.window(),//sheet
					      mainWindow, //docwindow
					      sheet, //modal delegate
					      new NSSelector(
			  "newfolderSheetDidEnd",
			  new Class[] { NSPanel.class, int.class, Object.class }
			  ),// did end selector
					      null); //contextInfo
    }


    public void infoButtonClicked(Object sender) {
	log.debug("infoButtonClicked");
	if(browserTable.selectedRow() != -1) {
	    Path path = (Path)browserModel.getEntry(browserTable.selectedRow());
	    CDInfoController controller = new CDInfoController(path);
	    this.references = references.arrayByAddingObject(controller);
	    controller.window().makeKeyAndOrderFront(null);
	}
    }

    public void deleteButtonClicked(Object sender) {
	log.debug("deleteButtonClicked");
	Path path = (Path)browserModel.getEntry(browserTable.selectedRow());
	NSAlertPanel.beginCriticalAlertSheet(
					   "Delete", //title
					   "Delete",// defaultbutton
					   "Cancel",//alternative button
					   null,//other button
					   mainWindow,//window
					   this, //delegate
					   new NSSelector
					   (
	 "deleteSheetDidEnd",
	 new Class[]
	 {
	     NSWindow.class, int.class, Object.class
	 }
	 ),// end selector
					   null, // dismiss selector
					   path, // contextInfo
					   "Really delete the file '"+path.getName()+"'? This cannot be undone." // message
					   );
    }

    public void deleteSheetDidEnd(NSWindow sheet, int returnCode, Object contextInfo) {
	log.debug("deleteSheetDidEnd");
	sheet.orderOut(null);
	switch(returnCode) {
	    case(NSAlertPanel.DefaultReturn):
		Path path = (Path)contextInfo;
		path.delete();
	    case(NSAlertPanel.AlternateReturn):
		//
	}
    }

    public void refreshButtonClicked(Object sender) {
	log.debug("refreshButtonClicked");
//	Path p = host.getSession().workdir();
	Path p = (Path)pathController.getItem(0);
	p.list(true);
    }

    public void downloadButtonClicked(Object sender) {
	log.debug("downloadButtonClicked");
	Path path = (Path)browserModel.getEntry(browserTable.selectedRow());
	//@todo keep reference?
	CDTransferController controller = new CDTransferController(path, Queue.KIND_DOWNLOAD);
	controller.start();
//	controller.window().makeKeyAndOrderFront(null);
//	path.download();
    }

    public void uploadButtonClicked(Object sender) {
	log.debug("uploadButtonClicked");
	NSOpenPanel panel = new NSOpenPanel();
	panel.setCanChooseDirectories(true);
	panel.setCanChooseFiles(true);
	panel.setAllowsMultipleSelection(false);
	panel.beginSheetForDirectory(System.getProperty("user.home"), null, null, mainWindow, this, new NSSelector("openPanelDidEnd", new Class[]{NSOpenPanel.class, int.class, Object.class}), null);
    }

    public void openPanelDidEnd(NSOpenPanel sheet, int returnCode, Object contextInfo) {
	sheet.orderOut(null);
	switch(returnCode) {
	    case(NSPanel.OKButton): {
		NSArray selected = sheet.filenames();
		String filename;
		if((filename = (String)selected.lastObject()) != null) { // only one selection allowed
		    log.debug(filename+" selected to upload");
		    Session session = host.getSession().copy();
		    Path path = null;
		    Path parent = (Path)pathController.getItem(0);
		    if(session instanceof ch.cyberduck.core.ftp.FTPSession) {
			path = new FTPPath((FTPSession)session, parent.getAbsolute(), new java.io.File(filename));
		    }
		    else if(session instanceof ch.cyberduck.core.sftp.SFTPSession) {
			path = new SFTPPath((SFTPSession)session, parent.getAbsolute(), new java.io.File(filename));
		    }
		    	//@todo keep reference?
		    CDTransferController controller = new CDTransferController(path, Queue.KIND_UPLOAD);
		    controller.start();
//		    controller.window().makeKeyAndOrderFront(null);
//		    path.upload();
		}
		break;
	    }
	    case(NSPanel.CancelButton): {
		break;
	    }
	}
    }
	
    public void backButtonClicked(Object sender) {
	log.debug("backButtonClicked");
	//@todoHistory
    }

     public void upButtonClicked(Object sender) {
	 log.debug("upButtonClicked");
	 Path p = (Path)pathController.getItem(0);
	 p.getParent().list();
     }
    
    public void drawerButtonClicked(Object sender) {
	log.debug("drawerButtonClicked");
	drawer.toggle(mainWindow);
    }

    public void connectFieldClicked(Object sender) {
	log.debug("connectFieldClicked");
	Host host = new Host(((NSControl)sender).stringValue(), new CDLoginController(this.window()));
	this.mount(host);
    }

    public void connectButtonClicked(Object sender) {
	log.debug("connectButtonClicked");
//keep a reference instead	CDConnectionSheet sheet = new CDConnectionSheet(this);
	NSApplication.sharedApplication().beginSheet(
					      connectionSheet.window(),//sheet
					      mainWindow, //docwindow
					      connectionSheet, //modal delegate
					      new NSSelector(
		      "connectionSheetDidEnd",
		      new Class[] { NSWindow.class, int.class, NSWindow.class }
		      ),// did end selector
					      null); //contextInfo
    }

    public void disconnectButtonClicked(Object sender) {
	this.unmount();
	//@todo show in disconnected state in gui
    }

    public void mount(Host host) {
	if(this.host != null)
	    this.unmount();
	this.host = host;
	Session session = host.getSession();
	
	session.addObserver((Observer)this);
	session.addObserver((Observer)pathController);

	session.mount();
    }

    public void unmount() {
	this.host.getSession().deleteObservers();
	this.host.closeSession();
    }

        // ----------------------------------------------------------
    // Toolbar
    // ----------------------------------------------------------

    private void setupToolbar() {
	this.toolbar = new NSToolbar("Cyberduck Toolbar");
	toolbar.setDelegate(this);
	toolbar.setAllowsUserCustomization(true);
	toolbar.setAutosavesConfiguration(true);
//	toolbar.setDisplayMode(NSToolbar.NSToolbarDisplayModeIconAndLabel);
	this.window().setToolbar(toolbar);
    }

    
    // ----------------------------------------------------------
    // Toolbar delegate methods
    // ----------------------------------------------------------

    public NSToolbarItem toolbarItemForItemIdentifier(NSToolbar toolbar, String itemIdentifier, boolean flag) {
//    return (NSToolbarItem)toolbarItems.objectForKey(itemIdentifier);

	NSToolbarItem item = new NSToolbarItem(itemIdentifier);

	if (itemIdentifier.equals("New Connection")) {
	    item.setLabel("New Connection");
	    item.setPaletteLabel("New Connection");
	    item.setToolTip("Connect to remote host");
	    item.setImage(NSImage.imageNamed("server.tiff"));
	    item.setTarget(this);
	    item.setAction(new NSSelector("connectButtonClicked", new Class[] {Object.class}));
	}
	else if (itemIdentifier.equals("Path")) {
	    item.setLabel("Path");
	    item.setPaletteLabel("Path");
	    item.setToolTip("Change working directory");
	    item.setView(pathPopup);
	    item.setMinSize(pathPopup.frame().size());
	    item.setMaxSize(pathPopup.frame().size());
	}
	else if (itemIdentifier.equals("Quick Connect")) {
	    item.setLabel("Quick Connect");
	    item.setPaletteLabel("Quick Connect");
	    item.setToolTip("Connect to host");
	    item.setView(quickConnectField);
	    item.setMinSize(quickConnectField.frame().size());
	    item.setMaxSize(quickConnectField.frame().size());
	}
	else if (itemIdentifier.equals("Back")) {
	    item.setLabel("Back");
	    item.setPaletteLabel("Back");
	    item.setToolTip("Show previous directory");
	    item.setImage(NSImage.imageNamed("back.tiff"));
	    item.setTarget(this);
	    item.setAction(new NSSelector("backButtonClicked", new Class[] {Object.class}));
	}
	else if (itemIdentifier.equals("Refresh")) {
	    item.setLabel("Refresh");
	    item.setPaletteLabel("Refresh");
	    item.setToolTip("Refresh directory listing");
	    item.setImage(NSImage.imageNamed("refresh.tiff"));
	    item.setTarget(this);
	    item.setAction(new NSSelector("refreshButtonClicked", new Class[] {Object.class}));
	}
	else if (itemIdentifier.equals("Download")) {
	    item.setLabel("Download");
	    item.setPaletteLabel("Download");
	    item.setToolTip("Download file");
	    item.setImage(NSImage.imageNamed("download.tiff"));
	    item.setTarget(this);
	    item.setAction(new NSSelector("downloadButtonClicked", new Class[] {Object.class}));
	}
	else if (itemIdentifier.equals("Up")) {
	    item.setLabel("Up");
	    item.setPaletteLabel("Up");
	    item.setToolTip("Show parent directory");
	    item.setImage(NSImage.imageNamed("up.tiff"));
	    item.setTarget(this);
	    item.setAction(new NSSelector("upButtonClicked", new Class[] {Object.class}));
	}
	else if (itemIdentifier.equals("Upload")) {
	    item.setLabel("Upload");
	    item.setPaletteLabel("Upload");
	    item.setToolTip("Upload local file to the remote host");
	    item.setImage(NSImage.imageNamed("upload.tiff"));
	    item.setTarget(this);
	    item.setAction(new NSSelector("uploadButtonClicked", new Class[] {Object.class}));
	}
	else if (itemIdentifier.equals("Get Info")) {
	    item.setLabel("Get Info");
	    item.setPaletteLabel("Get Info");
	    item.setToolTip("Show file attributes");
	    item.setImage(NSImage.imageNamed("info.tiff"));
	    item.setTarget(this);
	    item.setAction(new NSSelector("infoButtonClicked", new Class[] {Object.class}));
	}
	else if (itemIdentifier.equals("Delete")) {
	    item.setLabel("Delete");
	    item.setPaletteLabel("Delete");
	    item.setToolTip("Delete file");
	    item.setImage(NSImage.imageNamed("delete.tiff"));
	    item.setTarget(this);
	    item.setAction(new NSSelector("deleteButtonClicked", new Class[] {Object.class}));
	}
	else if (itemIdentifier.equals("New Folder")) {
	    item.setLabel("New Folder");
	    item.setPaletteLabel("New Folder");
	    item.setToolTip("Create New Folder");
	    item.setImage(NSImage.imageNamed("newfolder.icns"));
	    item.setTarget(this);
	    item.setAction(new NSSelector("folderButtonClicked", new Class[] {Object.class}));
	}
	else if (itemIdentifier.equals("Disconnect")) {
	    item.setLabel("Disconnect");
	    item.setPaletteLabel("Disconnect");
	    item.setToolTip("Disconnect");
	    item.setImage(NSImage.imageNamed("disconnect.tiff"));
	    item.setTarget(this);
	    item.setAction(new NSSelector("disconnectButtonClicked", new Class[] {Object.class}));
	}
	else if (itemIdentifier.equals("Toggle Drawer")) {
	    item.setLabel("Toggle Drawer");
	    item.setPaletteLabel("Toggle Drawer");
	    item.setToolTip("Show connection transcript");
	    item.setImage(NSImage.imageNamed("transcript.icns"));
	    item.setTarget(this);
	    item.setAction(new NSSelector("drawerButtonClicked", new Class[] {Object.class}));
	}
	else {
	    // itemIdent refered to a toolbar item that is not provide or supported by us or cocoa.
	    // Returning null will inform the toolbar this kind of item is not supported.
	    item = null;
	}
	return item;
	
	/*
	
//	this.addToolbarItem(toolbarItems, "Back", "Back", "Back", "Go back", this, new NSSelector("backButtonClicked", new Class[] {null}), NSImage.imageNamed("back.tiff"));

	 */
    }

	/*
    private void addToolbarItem(NSMutableDictionary toolbarItems, String identifier, String label, String paletteLabel, String toolTip, Object target, NSSelector action, NSImage image) {
	NSToolbarItem item = new NSToolbarItem(identifier);
	item.setLabel(label);
	item.setPaletteLabel(paletteLabel);
	item.setToolTip(toolTip);
	item.setImage(image);
	item.setTarget(target);
	item.setAction(action);
	item.setEnabled(true);

	toolbarItems.setObjectForKey(item, identifier);
    }
	 */

	 
    public NSArray toolbarDefaultItemIdentifiers(NSToolbar toolbar) {
	return new NSArray(new Object[] {"New Connection", NSToolbarItem.SeparatorItemIdentifier, "Quick Connect", NSToolbarItem.SeparatorItemIdentifier, "Path", "Refresh", "Download", "Upload", "Delete", "New Folder", "Get Info", NSToolbarItem.FlexibleSpaceItemIdentifier, "Toggle Drawer"});
    }

    public NSArray toolbarAllowedItemIdentifiers(NSToolbar toolbar) {
	return new NSArray(new Object[] {"New Connection", "Quick Connect", NSToolbarItem.SeparatorItemIdentifier, "Path", "Up", "Refresh", "Download", "Upload", "Delete", "New Folder", "Get Info", NSToolbarItem.FlexibleSpaceItemIdentifier, "Disconnect", "Toggle Drawer", NSToolbarItem.CustomizeToolbarItemIdentifier, NSToolbarItem.SpaceItemIdentifier});
    }

    public void toolbarWillAddItem(NSNotification notification) {
	NSToolbarItem addedItem = (NSToolbarItem) notification.userInfo().objectForKey("item");
	if(addedItem.itemIdentifier().equals("Path")) {
	    pathItem = addedItem;
	    pathItem.setTarget(pathController);
	    pathItem.setAction(new NSSelector("selectionChanged", new Class[] { Object.class } ));
	}
	if(addedItem.itemIdentifier().equals("Quick Connect")) {
	    quickConnectItem = addedItem;
	    quickConnectItem.setTarget(this);
	    quickConnectItem.setAction(new NSSelector("connectFieldClicked", new Class[] { Object.class } ));
	}    
    }

    public void toolbarDidRemoveItem(NSNotification notif) {
	NSToolbarItem removedItem = (NSToolbarItem) notif.userInfo().objectForKey("item");
	if (removedItem == pathItem) {
	    pathItem = null;
	}
	if (removedItem == quickConnectItem) {
	    quickConnectItem = null;
	}
    }

    public boolean validateToolbarItem(NSToolbarItem item) {
//	log.debug("validateToolbarItem");
	String label = item.label();
	if(label.equals("Path")) {
	    return pathController.numberOfItems() > 0;
	}
	else if(label.equals("Up")) {
	    return pathController.numberOfItems() > 0;
	}
	else if(label.equals("Refresh")) {
	    return pathController.numberOfItems() > 0;
	}
	else if(label.equals("Download")) {
	    return browserTable.selectedRow() != -1;
	}
	else if(label.equals("Upload")) {
	    return pathController.numberOfItems() > 0;
	}
	else if(label.equals("Delete")) {
	    return browserTable.selectedRow() != -1;
	}
	else if(label.equals("New Folder")) {
	    return pathController.numberOfItems() > 0;
	}
	else if(label.equals("Get Info")) {
	    return browserTable.selectedRow() != -1;
	}
	else if (label.equals("Disconnect")) {
	    return this.host != null;
	}
	return true;
    }


    // ----------------------------------------------------------
    // Window delegate methods
    // ----------------------------------------------------------

    public boolean windowShouldClose(NSWindow sender) {
	if(host != null) {
	    if(host.getSession().isConnected()) {
		NSAlertPanel.beginAlertSheet(
			       "End session?", //title
			       "Close",// defaultbutton
			       "Cancel",//alternative button
			       null,//other button
			       sender,//window
			       this, //delegate
			       new NSSelector
			       (
	   "closeSheetDidEnd",
	   new Class[]
	   {
	       NSWindow.class, int.class, NSWindow.class
	   }
	   ),// end selector
			       null, // dismiss selector
			       sender, // context
			       "The connection to the remote host will be closed." // message
			       );
	//@todo return the actual selection
		return false;
	    }
	}
	return true;
    }
    
    // ----------------------------------------------------------
    // IB action methods
    // ----------------------------------------------------------

    public void closeSheetDidEnd(NSWindow sheet, int returncode, NSWindow main) {
	// if multi window app only close the one window with main.close()
	sheet.orderOut(null);
	if(returncode == NSAlertPanel.DefaultReturn) {
	    this.unmount();
	    this.window().close();
	}
    }
}