# JEPO
Java Energy Profiler &amp; Optimizer is an eclipse plugin that provides method energy profiling and energy-saving suggestions for Java language. It can generate energy profile of methods by leveraging [Javassist](https://www.javassist.org/) library to insert energy measurement code at the start and end of each method. Software developers can use it to either get real-time suggestions while typing the code or get energy-saving suggestions for a selected project. Energy profiling functionality is supported only on Linux. Energy optimization suggestions functionality is supported on all OS. 


## Installation
- Allow modprobe and rdmsr commands run without sudo, like [this](https://askubuntu.com/questions/72267/how-to-allow-execution-without-prompting-for-password-using-sudo)
- Move the jar file located in plugin folder above to **eclipse dropin** folder
- Restart the eclipse

## How to use?
- **Real-time energy-saving suggestions**
  - Click ![JEPO](/icons/jepo.png) icon located in Eclipse tool-bar menu
- **Energy-saving suggestions for a project**
  - Right-click on a project
  - Select JEPO -> Run Optimizer option
- **Energy profiling of methods in a project**
  - Add all libraries in lib folder above to project build path
  - Right-click on the project
  - Select JEPO -> Run Profiler option

## Suggestions
  Java Component | Suggestion
  ---------------|---------------
  Variables | int is the most efficient primitive data type. Static variables consume up to 17,700% more energy compared to local variables. Access modifiers have no impact on a variable energy consumption. Scientific notation results in lower energy consumption of decimal numbers.
Operators | Modulus arithmetic operator consumes up to 1,620% more energy than other arithmetic operators. Putting most common cases first in short circuit operators can save up to 87% energy. Normal and compound assignments have the same amount of energy consumption. Post-increment, pre-increment , post- decrement and pre-decrement operators consume same energy.
Control Statements |  Ternary operator consume up to 37% more energy than if-then-else statement. int is the most energy efficient iteration variable in a for loop. for, while and do-while loop statements consume same energy. Enhanced for statement consumes the same energy as the for statement. Method termination expression can consume higher energy than the variable termination expression.
Exception | Try-catch block scope can change how it consume energy. Executing a catch block results in higher energy consumption.
String | StringBuilder append method consumes up to 1,48,069% lower energy than String concatenation operator. Integer toString, and String valueOf consume same energy. String compareTo method consumes up to 33% more energy than String equals method.
Object | Wrapper classes object are more energy expensive as compared to primitive data types. Double wrapper class object consume up to 115% more energy than Integer wrapper class object.
Thread | Creating Threads by implementing a Runnable interface or extending Thread class consume same energy.
Arrays | System.arraycopy() is the most energy efficient way to copy Arrays. Two-dimensional Array column traversal result in up to 793% more energy.
