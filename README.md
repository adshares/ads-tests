# ADS tests
---
## QA Tests

### Requirements
Tests require Java 1.8 and Maven.

### Commands to run tests
Command to run tests:
```
mvn clean test -f pom-qa.xml
```

Available options:
- `-Dgenesis.file=PATH_TO_GENESIS_FILE` starts test with custom genesis file, default is `genesis.json` in working directory,
- `-Dcucumber.options` sets cucumber framework options, could be used to specify test category,
- `-Dis.docker`:
  - 0 - (default) test with local binary (must be added to PATH),
  - 1 - test on Docker,
- -`Ddir.data` sets directory in which is stored nodes and users data, default is `/ads-data`.

Available test categories are:
* `transfer` - local and remote transfers, both groups have detail categories `transfer_local` and `transfer_remote`,
* `retrieve_funds` - funds retrieval from inactive account,
* `broadcast` - broadcast message,
* `non_existent` - transaction to non-existent user or node,
* `account` - change account key, and create account in local and remote node,
* `node` - change node key, create node,
* `status` - change account/node status,
* `vip_key` - check vip keys.

By default all tests will be run. To run specific category of tests `cucumber.options` must be defined. Below is example to run `transfer` tests:
```
mvn clean test -f pom-qa.xml -Dcucumber.options="--tags @transfer"
```
---
