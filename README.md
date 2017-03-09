
Coffee Roaster UI
Written by Anthony DiPilato

This project is the user interface for the coffee roaster that I built.
You can see the build log details at [url]

The UI runs on a Raspberry Pi connected to an arduino via USB
The arduino acts as slave to the Raspberry Pi
   
USE AT YOUR OWN RISK
I am posting this project for educational use only.
This project involves, electricity, moving parts, propane, and fire.
I will not be held liable for damages and/or injuries resulting from the use of this code
or from reproducing this project.

Commands and Adresses for arduino
Commands
| 0 | Status |
| 1	|	Relay on 		|
| 2	|	Relay off 		|
| 3	|	Set proportional valve 	|

Addresses
| 0	|	All 			|
| 1	|	Drum Temperature 	|
| 2	|	Chamber Temperature 	|
| 3	|	Exhaust Temperature	|
| 4	|	Flame Status		|
| 5	|	Drum Relay		|
| 6	|	Cooling Relay		|
| 7	|	Exhaust Relay		|
| 8	|	Gas Relay		|
| 9	|	Ignitor			|
| 10	|	Proportional Valve	|

Required Libraries
- JavaFX Medusa
- jSSC 2.7.0
- Colors 1.4