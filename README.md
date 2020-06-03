# Covid19 Notification App - Android

This repository contains the Android sources for the Covid19 Notification App that's being developed by the Dutch Government.

## Disclaimer

This repository is a work in progress. It is not a code dump of the final version of the app.

Keep in mind that the Google Exposure Notification API is only accessible by verified health authorities. Other devices trying to access the API using the code in this repository will fail to do so.

## How this application is being developed

There are two Git environments for this repository. One is private and hosted on Azure DevOps. The other one is public and hosted on Github. They are linked using Git.

Contributions from the members of the core team are done in Azure DevOps. The master branch is periodically pushed to Github, keeping the history intact.

External contributions are done in Github. A contributor creates a fork and opens a Pull Request. All checks and reviews are performed in Github. When all checks succeed, the branch is pulled into Azure DevOps and merged into the master branch. In the next push of the master branch, the Pull Request will automatically be closed.

In case of non-trivial changes, we kindly ask you to open an issue so we can discuss it.
