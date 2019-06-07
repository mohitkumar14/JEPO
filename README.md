# JEPO
Java Energy Profiler &amp; Optimizer is an eclipse plugin that provides method energy profiling and energy-saving suggestions for Java language. It can generate energy profile of methods by leveraging [Javassist](https://www.javassist.org/) library to insert energy measurement code at the start and end of each method. Software developers can use it to either get real-time suggestions while typing the code or get energy-saving suggestions for a selected project. Energy profiling functionality is supported only on Linux. Energy optimization suggestions functionality is supported on all OS. 


## Installation
- Move the jar file located in plugin folder above to **eclipse dropin** folder
- Restart the eclipse

## How to use?
- **Real-time energy-saving suggestions**
  - Click ![JEPO](/icons/jepo.png) icon located in Eclipse tool-bar menu
- **Energy-saving suggestions for a project**
  - Right-click on a project
  - Select JEPO -> Run Optimizer option
- **Energy profiling of methods in a project**
  - Right-click on a project
  - Select JEPO -> Run Profiler option
