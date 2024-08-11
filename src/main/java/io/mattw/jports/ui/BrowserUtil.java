package io.mattw.jports.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.net.URL;

/**
 * Opens URLs on the system.
 */
public class BrowserUtil {

    private static final Logger logger = LogManager.getLogger();

    public BrowserUtil() {}

    public void open(URL url) {
        open(url.toString());
    }

    public void open(String link) {
        link = link.replace(" ", "%20");
        try {
            var url = new URL(link);
            if (Desktop.isDesktopSupported()) {
                var desktop = Desktop.getDesktop();
                desktop.browse(url.toURI());
            } else {
                Runtime runtime = Runtime.getRuntime();
                runtime.exec("xdg-open "+url.getPath());
            }
        } catch (Throwable e) {
            logger.warn("Failed to open URL in default browser [link={}]", link, e);
        }
    }
}
