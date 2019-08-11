# jports-ui
Portable, fast, multithreaded port scanning tool in JavaFX.

![GitHub latest release](https://img.shields.io/github/release/mattwright324/jports-ui.svg?style=flat-square)
![Github total download](https://img.shields.io/github/downloads/mattwright324/jports-ui/total?style=flat-square)

![preview](https://i.imgur.com/H45pIAb.png)

## Features
Simple scanning with many options.
- Java 8
- Scan addresses many ways
  - Single address(es) `x.x.x.x` or `x.x.x.x, x.x.y.y, x.x.y.z`
  - Address range `x.x.x.x - x.y.y.y`
  - CIDR `x.x.x.x/yy`
  - Endless increase `x.x.x.x -> ...`
  - Endless decrease `... <- x.x.x.x`
- Multiple options for ports
  - Custom list of ports
  - Port range
  - Optionally check open address:port for webpages
- Finds logged to log file with extra detail (page title, page size)
