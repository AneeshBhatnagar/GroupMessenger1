# Group Messenger in Android
A group messenger in Android to send and recieve messages between 5 AVDs (Each message sent to each device). Built as a part of the CSE 586 Distributed Computers course at University at Buffalo.

### Usage
* Create the 2 AVDs using the create_avd.py script: python2.7 create_avd.py 2 /PATH_TO_SDK/
* Run the 2 AVDs using the run_avd.py script: python2.7 run_avd.py 2
* Run the port redirection script using: python2.7 set_redir.py 10000
* Now, run the grading script as follows: 
./groupmessenger1-grading.linux -p /path/to/sdk -n [number of avds to use, default 5]

For more information about the grading script, run ./groupmessenger1-grading.linux -h