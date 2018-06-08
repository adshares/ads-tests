# ADS tests
---
## QA Tests

### Requirements
Tests require Java 1.8 and Maven.

### Commands to run tests
To run tests use commands below.

If genesis file (genesis.json) is in main directory of esc:
```
mvn clean test -f pom-qa.xml
```
In case of custom location of genesis file:
```
mvn clean test -f pom-qa.xml -Dgenesis.file=PATH_TO_GENESIS_FILE
```

Currently available test categories are:
* `transfer` - local and remote transfers, both groups have detail categories `transfer_local` and `transfer_remote`,
* `retrieve_funds` - funds retrieval from inactive account,
* `broadcast` - broadcast message,
* `non_existent` - transaction to non-existent user or node,
* `account` - change account key, and create account in local and remote node,
* `node` - change node key, create node,
* `status` - change account/node status.

By default all tests will be run. To run specific category of tests `cucumber.options` must be defined. Below is example to run `transfer` tests:
```
mvn clean test -f pom-qa.xml -Dcucumber.options="--tags @transfer"
```
---
