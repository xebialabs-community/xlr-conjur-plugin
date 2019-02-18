# XL Release Conjur Plugin

# CI status #

[![Build Status][xlr-conjur-travis-image] ][xlr-conjur-travis-url]

[xlr-conjur-travis-image]: https://travis-ci.org/xebialabs-community/xlr-conjur-plugin.svg?branch=master
[xlr-conjur-travis-url]: https://travis-ci.org/xebialabs-community/xlr-conjur-plugin


# Overview #

This document describes the functionality provided by the xlr-conjur-plugin.
This plugin provides the ability to populate a release variable with secret retrieved from a Conjur server. The release variable can then be used by subsequent tasks in the release. 

See the **XL Release Reference Manual** for background information on XL Release and release concepts.

* **Requirements**
  * **XL Release** 7.5.0+
  * **Conjur** 4.9+


# Installation #

* Copy the latest JAR file from the [releases page](https://github.com/xebialabs-community/xlr-conjur-plugin/releases) into the `XL_RELEASE_SERVER/plugins/__local__` directory.
* Restart the XL Release server.


# Usage #

## Configure Server ##

Begin by configuring one or more Conjur servers.  Navigate to **Settings -> Shared configuration** and add a Conjur: Server.

![ConjurServerConfig](images/conjur-xlr-server-config.png)

Enter a descriptive name for this server.

### URL ###

Enter the full URL to the Conjur server from which secrets can be retrieved.  Include protocol (http or https) and port number if applicable.  For example: http://conjur

### Authentication Method ###

Choose the Basic Method from the drop down list. You may need to click the entry area to activate the drop down.

### Account ###

Enter the enter the name of the Conjur account where the secrets are stored.  For example: quick-start

### Username ###

Enter the account username that has permissions to retrieve the secret or secrets.

### Password ###

Enter the user KEY in the password entry field.

### Proxy ###

Optional proxy information if you access the Conjur server through a proxy.

---
## Using the xlr-conjur Plugin ##
The Conjur plugin provides a task called 'Get Secret' which will populate a single release variable with a single Conjur secret. As you create your template, you will need to create as many release variables as the number of distinct secrets you will need to run your tasks. 

Name the variable, make sure it is of type 'Password' and uncheck the boxes for 'Required' and 'Show on Release Form'.

![ConjurVar1Config](images/conjur-xlr-release-var-1.png)
![ConjurVar2Config](images/conjur-xlr-release-var-2.png)

## Get Secret Task ##
Within your template, you will need to add as many 'Get Secret' tasks as the number of release variables you will need to populate. To configure a task choose the Conjur Server, provide the name of the secret as stored in Conjur, and choose a release variable of type password to populate. 

![ConjurGetSecret](images/conjur-xlr-getSecret-task.png)

The Get Secret task should be placed in the template so that it runs right before the task that will need the secret. In this example, the Get Secret task retrieves a secret password that will be used in the following JIRA 'Create Issue' task. 

### Example Template ###

![ConjurGetSecretTemplate](images/conjur-xlr-template.png)



## Use the Secret as Part of Another Task ##
In this example, the Conjur Get Secret task is followed by a JIRA Create Issue task. The JIRA task makes use of the release variable 'newSecret' (previously populated by Conjur) as the JIRA password (overriding the password configured for the JIRA server).

![ConjurGetSecretUseExample](images/conjur-xlr-exampleUseSecret.png)


---


# Developers #

Build and package the plugins with...

```bash
./gradle assemble
```

Run end to end integration tests - note: You will need to have docker and docker-compose installed. The most recent version of your plug-in jar should be in your project build/libs directory. Running the gradle task 'assemble' before running the integrationTest will create the jar and place it in the libs directory.

```bash
./gradlew clean assemble integrationTesting
```
