# CHAT APP

Simple serverless chat app including a login screen and the chat screen, intended to cover a group
chat scenario.

- Login screen, using Auth Service it's possible to login with 3rd party providers, your own server
  or anonymously. The credentials are already stored into Auth Service but we are saving them on
  Cloud DB too in order to manipulate the info for the chat.

![](https://github.com/FStranieri/CloudySamples/blob/main/login_screen.png)

- Chat screen using Cloud DB to store and read messages.

![](https://github.com/FStranieri/CloudySamples/blob/main/chat_screen.png)

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

Sample project -> [link](https://github.com/FStranieri/CloudySamples_CloudFunction)

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

# SETUP:

1) Follow the Auth Service getting started
   guide: [link](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-auth-android-getstarted-0000001053053922)
   and enable the 3rd party login providers you want to support;
2) Follow the CloudDB getting started
   guide: [link](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-clouddb-get-started-0000001127676473)
   and add your tables with the data you want (remember to export the tables as models in your app
   project);
3) add an ids.xml file into the 'values' folder with the info of the 3rd party login providers you
   want to support in your app
