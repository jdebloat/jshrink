The best way to build the app is with the following command:

mvn compile -pl jshrink-app -am

The resulting jar can be found in 
"jshrink-app/target/jshrink-app-1.0-SHAPSHOT-jar-with-dependencies.jar".

This will also compile the jshrink-lib library which jshrink-app builds
upon. It can be found in
"jshrink-lib/target/jshrink-lib-1.0-SNAPSHOT-jar-with-dependencies.jar".

To build jshrink-lib exclusively run the following command:

mvn compile -pl jshrink-lib -am

Notes:
- If you want to open this project in an IDE, please ensure that you
copy "jshrink-lib/src/main/resources/poa.properties" to 
"<your home directory>/.tamiflex/poa.properties". 
- To save time during testing, please run "../deploy_caches.sh". This
will deploy the caches to the project.
