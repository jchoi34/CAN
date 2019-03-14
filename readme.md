Description:

As long as the server program is running in the background a node may receive a join request to the CAN (no other command will reach it). The client program just sends commands to the nodes in the CAN via the node it is running on.

Ssh to each of these nodes from zeus.vse.gmu.edu -> Medusa.vsnet.gmu.edu -> n0000 - n0009 and run the background program below on each of them.

The background java program should be running in the background on nodes n0000 - n0009. Then the client program can be run on any of the nodes. The name of the server should be supplied as an argument.

Must run this background program on each node. "node" folder should be in another folder like "src".


Compiling the program:

from /src/node run the command "javac *.java"

However, the folder should already include the compiled java classes


Running the program:

RMI server background program:

nohup java -cp "<path to the folder containing the node folder>" node.Server n##### &

* Where #### is the node's number

Example: nohup java -cp "/home/jchoi34/src" node.Server n0000 &

This will run the background program for n0000

Make sure to supply the name of the current server you're on as the argument
*The "nohup" and "&" are important for making this server run constantly in the background.


Client program to interact with CAN:

java -cp "<path to the folder containing the node folder>" node.Main n####

Where #### is the node's number

Example: java -cp "/home/jchoi34/src" node.Main n0000

This will run the client program for n0000
* supply the name of the current server you're on as the argument

*if join is called with no args all nodes will try to join. If a node is already in the CAN nothing is done for it but it will still tell the client that the join is successful for that node.



Killing the program:

To kill background program. Must run for each node the background program is running.
pkill -u <username>

E.G, pkill -u jchoi34 
*from each node



Commands:

1)
Insert command:

insert <keyword>

* Keyword must be 5 letters or more

2)
Search command:

search <keyword>

* Keyword must be 5 letters or more

3)
View command:

view n####

Or

view

Where # is a digit from 0 - 9

4)
Join command:

join n####

Or

join

Where # is a digit from 0 - 9

5)
Leave command:

leave

