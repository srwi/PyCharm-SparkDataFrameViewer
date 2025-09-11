# Spark DataFrame Viewer

<a href="https://paypal.me/rumswinkel"><img src="https://img.shields.io/static/v1?label=Donate&message=%E2%9D%A4&logo=PayPal&color=%23009cde"/></a>
[![Version](https://img.shields.io/jetbrains/plugin/v/25039-pixellens.svg)](https://plugins.jetbrains.com/plugin/25039-pixellens)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/25039-pixellens.svg)](https://plugins.jetbrains.com/plugin/25039-pixellens)

<!-- Plugin description -->
[Spark DataFrameViewer]() lets you inspect Spark DataFrames directly in PyCharm. In the debugger, right-click a variable and select <kbd>View Spark DataFrame</kbd> to open the data in PyCharm's built-in DataFrame viewer.

Its functionality is straightforward: it executes the Spark query, applies a configurable row limit, converts the result to a pandas DataFrame via <code>.toPandas()</code>, and displays it in PyCharm's DataFrame viewer.

<!-- Plugin description end -->

## Installation

- Get it from the [JetBrain Marketplace](https://plugins.jetbrains.com/plugin/25039-pixellens)

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Spark DataFrame Viewer"</kbd> > <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/srwi/PyCharm-PixelLens/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## License

This project is licensed under the [GPLv3](https://github.com/srwi/PyCharm-PixelLens/blob/master/LICENSE) license.
