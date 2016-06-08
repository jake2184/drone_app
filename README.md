# Android Application for connection to Drone Server

Source code for an Android Application which connects to a [drone management server](https://github.com/jake2184/drone) and IBM's MQTT broker, and presents data and analysis from the drones.


It is built using gradle, although the use of Android Studio is heavily suggested - the project files are included. The installation process for developing Android is most easily explained by following steps found on Google's [Android Developer site.](https://developer.android.com/index.html)

Following compilation, the APK must be run on either an Android advice or simulator. The API target level is 19+.
Again, the execution method is most easily learnt by following steps on Google's developer site.

Once running, use of the app is self-explanatory. Credentials to log into the server are required, as well as MQTT credentials. These are supplied by whomever is administrating the target drone management server.