//==============================================================
// Distributed System
// Name: Yingyao Lu
// ID: a1784870
// Semester: S2
// Year: 2020
// Assignment2: ATOM feeds aggregation and distribution system
//==============================================================

This project demonstrates that how to build a simple client/server system,
by building a simple system that aggregates and distributes ATOM feeds.

The project has three servers:
    AggregationServer;
    ContentServer;
    GETClient;

AggregationServer has following methods:
    recoverFeeds();
    aggregationFeeds(String, String);
    processPUT(String, String);
    processGET();
    timer();
    updateFeeds();
    incrementLamport();
    updateLamport(int);
    backupFeeds();
    returnStatusCode(String);

ContentServer has following methods:
    PUT();
    incrementLampoet(int);
    updateLamport(int);
    createNewsFeeds();
    readXML();

GETClient has following methods:
    GET();
    incrementLampoet();
    updateLamport(int);
    displayXML();

A automated testing and one manually testing are provided.

The test file is named as "TesterMultipleServer"
The port number is set as "4567" as default

//How to run tests
1. Run TesterSingleServer
	Console Command: java TesterMultipleServer

// How to test manually
1. Run AggregationServer
	Console Command: java AggregationServer atom 4567

2.Run ContentServer
	Console Command:  java ContentServer 0.0.0.0:4567 input.txt feedXML.xml 123

3.Run GETClient
	Console Command:  java GETClient 0.0.0.0:4567

Checklist
Basic functionality:
√  XML parsing works
√ client, Atom server and content server processes start up and communicate
√ PUT operation works for one content server
√ GET operation works for many read clients
√ Atom server expired feeds works (12s)
√ Retry on errors (server not available etc) works

Full functionality:

√ Lamport clocks are implemented
√ All error codes are implemented: empty XML, malformed XML
√ Content servers are replicated and fault tolerant

