package io.mattw.jports.ui;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

public class ClipboardUtil {

	private Clipboard systemClipboard;

	public ClipboardUtil() {}

	private Clipboard systemClipboard() {
		if (systemClipboard == null) {
			systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		}
		return systemClipboard;
	}

	public String getClipboard() throws UnsupportedFlavorException, IOException {
        return (String) systemClipboard().getData(DataFlavor.stringFlavor);
	}

	/**
	 * Sets clipboard to string value.
	 */
	public void setClipboard(String string) {
		var selection = new StringSelection(string);
		systemClipboard().setContents(selection, selection);
	}

	/**
	 * Converts list into a line.separator delimited string and sets to clipboard.
	 */
	public void setClipboard(Collection<?> collection) {
		var strList = collection.stream().map(Object::toString).collect(Collectors.toList());

		setClipboard(strList.stream().collect(Collectors.joining(System.lineSeparator())));
	}

	/**
	 * Coverts object to string value and sets to clipboard.
	 */
	public void setClipboard(Object object) {
		setClipboard(String.valueOf(object));
	}

}
