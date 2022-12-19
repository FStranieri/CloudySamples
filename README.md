# CHAT APP

Simple serverless chat app including a login screen and the chat screen, intended to cover a group
chat scenario.

- Login screen, using [Auth Service](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-auth-introduction-0000001053732605) it's possible to login with 3rd party providers, your own server
  or anonymously. The credentials are already stored into Auth Service but we are saving them on
  [Cloud DB](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-clouddb-overview-0000001127558223) too in order to manipulate the info for the chat. The whole business logic is managed by the [Cloud Functions](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-cloudfunction-introduction-0000001059279544) .

![](https://github.com/FStranieri/CloudySamples/blob/main/login_screen.png)

- Chat screen using Cloud DB to store and read messages.

![](https://github.com/FStranieri/CloudySamples/blob/main/chat_screen.png)

# SETUP

1) Follow the setup at this [link](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-auth-creat-project-and-app-0000001324725529) ;
2) in order to support the HUAWEI ID login, you MUST enable the 'Account Kit' API under the 'MANAGE API' section on AGC Console;
3) create an 'ids.xml' file under 'res/values' folder with the ids needed for [Google](https://developers.google.com/identity/sign-in/android/start-integrating#configure_a_project) and [Facebook](https://developers.facebook.com/docs/android/getting-started#app-id) login providers;
4) import the ObjectTypes into your Cloud DB section following this [guide](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-clouddb-agcconsole-objecttypes-0000001127675459#section3873193085413);
5) import the following Cloud Functions to your project -> [link](https://github.com/FStranieri/chat_sample_cloud_functions)

# ViewModels

1) [AuthViewModel](https://github.com/FStranieri/CloudySamples/blob/main/app/src/main/java/com/fs/cloudapp/viewmodels/AuthViewModel.kt)
   to manage all the login flows and data;
2) [CloudDBViewModel](https://github.com/FStranieri/CloudySamples/blob/main/app/src/main/java/com/fs/cloudapp/viewmodels/CloudDBViewModel.kt)
   to manage all the CloudDB flows and data

# Data

- [users]: the users info table;
- [input_messages]: the messages table that the app will use ONLY to update the data on CloudDB;
- [full_messages]: a join table between [users] and [input_messages] that can only be updated by the
  Cloud Functions and the app reads to show the message cards.
- [poll_lunch_choices]: list of the restaurants where we want to organize the lunch
- [poll_lunch]: the users' choices for the lunch

# Cloud Functions

Sample Cloud Function project -> [link](https://github.com/FStranieri/CloudySamples_CloudFunction)
The Cloud Functions used for the chat app -> [link](https://github.com/FStranieri/chat_sample_cloud_functions)

# Login flow:

1) the [AuthViewModel] will check if the user is already logged in jump to step 7;
2) if the user is not logged in, it clicks on a 3rd party login provider;
3) the [login()] function from [AuthViewModel] will be invoked;
4) with the login, the AUTH TRIGGER on AGConnect Console will be fired, starting a Cloud Function;
5) the Cloud Function will set a random color for the user and then stores user data into the Cloud
   DB [users] table;
6) a jetpack compose logic with a mutablestate ([loggedIn]) will proceed to the the final step;
7) the app will check the data on Cloud DB with [getUserDataAvailability()]
9) a jetpack compose logic with a mutablestate ([userDataAvailable]) will redirect to the chat
   screen

# Send Message flow:

1) user sends a [input_messages] object to Cloud DB using the [sendMessage(..)] function;
2) on Cloud DB there's a configured trigger that runs a Cloud Function in order to create a new
   record in the [full_messages] table using the info from [input_messages] and [users], generating
   a random ID;
3) since we are listening for changes on the [full_messages] table through the
   [subscribeSnapshot(..)] function, the list will add the message card as soon as the listener
   notifies it.

# Edit Message flow:

pretty similar to the 'Send Message flow' only taking care about the primary key which needs to be
the same of the record we want to modify.

# Delete Message flow:

invoke the [deleteMessage(..)] function from [CloudDBViewModel] passing the [full_message] data
we want to delete
