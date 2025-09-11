# Spark DataFrame Viewer <img src="src/main/resources/META-INF/pluginIcon.svg" align="right" width="25%"/>

<a href="https://paypal.me/rumswinkel"><img src="https://img.shields.io/static/v1?label=Donate&message=%E2%9D%A4&logo=PayPal&color=%23009cde"/></a>
[![Version](https://img.shields.io/jetbrains/plugin/v/28448-spark-dataframe-viewer.svg)](https://plugins.jetbrains.com/plugin/28448-spark-dataframe-viewer)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/28448-spark-dataframe-viewer.svg)](https://plugins.jetbrains.com/plugin/28448-spark-dataframe-viewer)

<!-- Plugin description -->
[Spark DataFrame Viewer](https://github.com/srwi/PyCharm-SparkDataFrameViewer) lets you inspect Spark DataFrames directly in PyCharm.

In the debugger, right-click a variable and select <kbd>View Spark DataFrame</kbd> to open the data in PyCharm's built-in DataFrame viewer.

The plugin works by executing your Spark query with a configurable row limit, converting the result to a pandas DataFrame using <code>.toPandas()</code>, and displaying it in PyCharm's DataFrame viewer.

<!-- Plugin description end -->

## Installation

- Get it from the [JetBrain Marketplace](https://plugins.jetbrains.com/plugin/28448-spark-dataframe-viewer)

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Spark DataFrame Viewer"</kbd> > <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/srwi/PyCharm-SparkDataFrameViewer/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## License

This project is licensed under the [MIT](https://github.com/srwi/PyCharm-SparkDataFrameViewer/blob/master/LICENSE) license.
