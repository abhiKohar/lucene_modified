1) Purpose of the software // What is the software for? 

Current lucene/elastic search implementations do not support boosting for nested span queries. Though you can specify boost as an argument but it has no effect on search results.  This is an effort to extend lucene to support such nested span queries. This is useful for entity semantic search and entity semantic document search. 

For example: 

Span{ 

Span{ 

#professor 

} 

Span{ 

Data mining 

Boost : 5 

} 

Span{ 

Illinois 

Boost : 2 

} 

Span { 

#img 

Boost : 20 

} 

} 

We can adjust our search results/scores based on the above boosting factors. 

 
2) System requirements and dependencies // What machine/OS/library requirements? How to set up the requirements/dependencies? 

This has been tested on linux ubuntu – 16.04 

Requirements: 

Java – jre / jdk 1.8 

Ant – latest 

Gradle – 4.4 

Intellij -latest 

Ivy-bootstrap 

 
3) Execution // How to run the code? 
To compile the sources run 'ant compile' [can do from intellij also] 

To run all the tests run 'ant test' 

To setup your ide run 'ant idea' [used with intellij before import], 'ant netbeans', or 'ant eclipse' 

For Maven info, see dev-tools/maven/README.maven 

4) Design/Code organization 

To incorporate the changes following design and implementations have been made. 

Changes to spanboostQuery so that we can tell whether the nested query is boosted. This inherently uses spannearsunordered and spannearsordered - which needs to be changed to modify and pass the nested parameter. once that is identified - we need to pass the boost to spanweight class and span scorer so that while calculating the span we can adjust the score and weight of each span accordingly.  

This has been tested by writing test cases in the TestSpanBoostAbhinav and TestSpanBoostQuery.
