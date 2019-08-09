# jports-ui
Portable, fast, multithreaded port scanning tool in JavaFX.

![](https://i.imgur.com/H45pIAb.png)

## Features
Simple scanning with many options.
- Scan addresses many ways
  - Single address `(x.x.x.x)`
  - Address range `(x.x.x.x - x.y.y.y)`
  - CIDR `(x.x.x.x/yy)`
  - Endless increase `(x.x.x.x -> ...)`
  - Endless decrease `(... <- x.x.x.x)`
- Multiple options for ports
  - Custom list of ports
  - Port range
  - Optionally check open address:port for webpages
- Finds logged to log file with extra detail (page title, page size)
