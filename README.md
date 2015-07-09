# elastic-searching
How to use ElasticSearch for searching data. This is the adaptation of the ElasticSearch documentation to 
Elastic4s DSL formatted in a nice SBT project. It uses the Akka ElasticSearch extension to launch a node.

[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

# Chapters

## Structured Search
With structured search, the answer to your question is always a yes or no; something either belongs in 
the set or it does not. Structured search does not worry about document relevance or scoring — 
it simply includes or excludes documents.

This should make sense logically. A number can’t be “more” in a range than any other number which falls in the same range. It is either in the range… or it isn’t. Similarly, for structured text, a value is either equal or it isn’t. There is no concept of “more similar”.
 - [Finding Exact Values](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/_finding_exact_values.html) -> [com.github.dnvriend.structured.FindingExactValuesTest](https://github.com/dnvriend/elastic-searching/blob/master/src/test/scala/com/github/dnvriend/FindingExactValuesTest.scala)
 - [Combining Filters](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/combining-filters.html) -> [com.github.dnvriend.structured.CombiningFiltersTest](https://github.com/dnvriend/elastic-searching/blob/master/src/test/scala/com/github/dnvriend/CombiningFiltersTest.scala)
 - [Finding multiple exact values](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/_finding_multiple_exact_values.html) -> [com.github.dnvriend.structured.FindingMultipleExactValuesTest](https://github.com/dnvriend/elastic-searching/blob/master/src/test/scala/com/github/dnvriend/FindingMultipleExactValuesTest.scala)
 - [Ranges](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/_ranges.html) -> [RangesTest]() 
## Partial Matching

 - [Wildcard and Regexp Queries](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/_literal_wildcard_literal_and_literal_regexp_literal_queries.html) -> [com.github.dnvriend.WildcardAndRegexpQueriesTest.scala](https://github.com/dnvriend/elastic-searching/blob/master/src/test/scala/com/github/dnvriend/partialmatching/WildcardAndRegexpQueriesTest.scala) 